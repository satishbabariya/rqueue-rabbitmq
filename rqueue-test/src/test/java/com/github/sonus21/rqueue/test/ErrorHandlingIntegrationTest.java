/*
 * Copyright (c) 2023 Sonu Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.github.sonus21.rqueue.test;

import com.github.sonus21.rqueue.test.config.RqueueRabbitTestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for error handling and dead letter queue functionality.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class ErrorHandlingIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final AtomicInteger failedMessageCount = new AtomicInteger(0);
  private static final AtomicInteger deadLetterMessageCount = new AtomicInteger(0);
  private static final List<String> failedMessages = new ArrayList<>();
  private static final List<String> deadLetterMessages = new ArrayList<>();
  private static final CountDownLatch deadLetterLatch = new CountDownLatch(1);

  @Component
  public static class FailingMessageListener {

    private static int failureCount = 0;
    private static final int MAX_FAILURES = 2; // Fail first 2 attempts, succeed on 3rd

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(
        value = "test-failing-queue", 
        numRetries = "3",
        deadLetterQueue = "test-dead-letter-queue",
        concurrency = "1")
    public void handleFailingMessage(String message) {
      log.info("Processing message: {} (attempt {})", message, failureCount + 1);
      
      failureCount++;
      if (failureCount <= MAX_FAILURES) {
        failedMessageCount.incrementAndGet();
        synchronized (failedMessages) {
          failedMessages.add(message + " (attempt " + failureCount + ")");
        }
        throw new RuntimeException("Simulated processing failure for message: " + message);
      }
      
      log.info("Successfully processed message: {} after {} failures", message, MAX_FAILURES);
    }
  }

  @Component
  public static class DeadLetterMessageListener {

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(value = "test-dead-letter-queue", concurrency = "1")
    public void handleDeadLetterMessage(String message) {
      log.info("Received dead letter message: {}", message);
      deadLetterMessageCount.incrementAndGet();
      synchronized (deadLetterMessages) {
        deadLetterMessages.add(message);
      }
      deadLetterLatch.countDown();
    }
  }

  @Test
  void testMessageRetryAndDeadLetterQueue() throws InterruptedException {
    // Given
    String testMessage = "Message that will fail initially";
    int maxRetries = 3;
    
    failedMessageCount.set(0);
    deadLetterMessageCount.set(0);
    synchronized (failedMessages) {
      failedMessages.clear();
    }
    synchronized (deadLetterMessages) {
      deadLetterMessages.clear();
    }

    // When - Send a message that will fail initially but succeed after retries
    String messageId = rqueueMessageEnqueuer.enqueueWithRetry("test-failing-queue", testMessage, maxRetries);
    assertNotNull(messageId, "Message ID should not be null");

    // Then - Wait for processing to complete
    long waitStartTime = System.currentTimeMillis();
    while (failedMessageCount.get() < 2 && 
           (System.currentTimeMillis() - waitStartTime) < 10000) { // 10 second timeout
      Thread.sleep(100);
    }

    // Verify that the message was retried and eventually succeeded
    assertEquals(2, failedMessageCount.get(), "Should have failed exactly 2 times before succeeding");
    
    synchronized (failedMessages) {
      assertEquals(2, failedMessages.size(), "Should have recorded 2 failed attempts");
    }

    // Verify no dead letter message was created (since it eventually succeeded)
    assertEquals(0, deadLetterMessageCount.get(), "Should not have any dead letter messages");
  }

  @Test
  void testMessageExceedingMaxRetries() throws InterruptedException {
    // Given
    String testMessage = "Message that will always fail";
    int maxRetries = 2;
    
    // Reset failure count for this test
    FailingMessageListener.failureCount = 0;
    failedMessageCount.set(0);
    deadLetterMessageCount.set(0);
    synchronized (failedMessages) {
      failedMessages.clear();
    }
    synchronized (deadLetterMessages) {
      deadLetterMessages.clear();
    }

    // When - Send a message that will always fail
    String messageId = rqueueMessageEnqueuer.enqueueWithRetry("test-failing-queue", testMessage, maxRetries);
    assertNotNull(messageId, "Message ID should not be null");

    // Then - Wait for dead letter message
    waitForMessages(deadLetterLatch, 1, 15); // 15 second timeout for dead letter processing

    // Verify that the message was retried and sent to dead letter queue
    assertTrue(failedMessageCount.get() >= maxRetries, 
        "Should have failed at least " + maxRetries + " times");
    
    assertEquals(1, deadLetterMessageCount.get(), "Should have exactly 1 dead letter message");
    
    synchronized (deadLetterMessages) {
      assertEquals(1, deadLetterMessages.size(), "Should have received 1 dead letter message");
      assertTrue(deadLetterMessages.get(0).contains(testMessage), 
          "Dead letter message should contain the original message");
    }
  }

  @Test
  void testInvalidMessageHandling() {
    // Test with null message
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue("test-failing-queue", null);
    });

    // Test with empty queue name
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue("", "test message");
    });

    // Test with null queue name
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue(null, "test message");
    });
  }

  @Test
  void testConcurrentErrorHandling() throws InterruptedException {
    // Given
    int messageCount = 3;
    int maxRetries = 2;
    
    // Reset failure count for this test
    FailingMessageListener.failureCount = 0;
    failedMessageCount.set(0);
    deadLetterMessageCount.set(0);
    synchronized (failedMessages) {
      failedMessages.clear();
    }
    synchronized (deadLetterMessages) {
      deadLetterMessages.clear();
    }

    // When - Send multiple messages that will fail
    for (int i = 0; i < messageCount; i++) {
      String message = "Concurrent failing message " + i;
      String messageId = rqueueMessageEnqueuer.enqueueWithRetry("test-failing-queue", message, maxRetries);
      assertNotNull(messageId, "Message ID should not be null for message: " + message);
    }

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (deadLetterMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 20000) { // 20 second timeout
      Thread.sleep(100);
    }

    // Verify that all messages were sent to dead letter queue
    assertEquals(messageCount, deadLetterMessageCount.get(), 
        "Should have " + messageCount + " dead letter messages");
    
    synchronized (deadLetterMessages) {
      assertEquals(messageCount, deadLetterMessages.size(), 
          "Should have received " + messageCount + " dead letter messages");
    }
  }

  @Test
  void testDelayedMessageWithRetry() throws InterruptedException {
    // Given
    String testMessage = "Delayed message that will fail";
    long delayMillis = 1000;
    int maxRetries = 2;
    
    // Reset failure count for this test
    FailingMessageListener.failureCount = 0;
    failedMessageCount.set(0);
    deadLetterMessageCount.set(0);
    synchronized (failedMessages) {
      failedMessages.clear();
    }
    synchronized (deadLetterMessages) {
      deadLetterMessages.clear();
    }

    // When - Send a delayed message that will fail
    String messageId = rqueueMessageEnqueuer.enqueueInWithRetry("test-failing-queue", testMessage, maxRetries, delayMillis);
    assertNotNull(messageId, "Message ID should not be null");

    // Then - Wait for dead letter message
    waitForMessages(deadLetterLatch, 1, 20); // 20 second timeout for delayed + retry processing

    // Verify that the delayed message was retried and sent to dead letter queue
    assertTrue(failedMessageCount.get() >= maxRetries, 
        "Should have failed at least " + maxRetries + " times");
    
    assertEquals(1, deadLetterMessageCount.get(), "Should have exactly 1 dead letter message");
    
    synchronized (deadLetterMessages) {
      assertEquals(1, deadLetterMessages.size(), "Should have received 1 dead letter message");
      assertTrue(deadLetterMessages.get(0).contains(testMessage), 
          "Dead letter message should contain the original delayed message");
    }
  }
}