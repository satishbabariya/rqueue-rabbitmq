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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for delayed message functionality.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class DelayedMessageIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final CountDownLatch delayedMessageLatch = new CountDownLatch(1);
  private static final AtomicInteger delayedMessageCount = new AtomicInteger(0);
  private static final AtomicLong messageProcessingTime = new AtomicLong(0);
  private static String receivedDelayedMessage;

  @Component
  public static class DelayedMessageListener {

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(value = "test-delayed-queue", concurrency = "1")
    public void handleDelayedMessage(String message) {
      log.info("Received delayed message: {}", message);
      receivedDelayedMessage = message;
      delayedMessageCount.incrementAndGet();
      messageProcessingTime.set(System.currentTimeMillis());
      delayedMessageLatch.countDown();
    }
  }

  @Test
  void testDelayedMessageDelivery() throws InterruptedException {
    // Given
    String testMessage = "Delayed message";
    long delayMillis = 2000; // 2 seconds delay
    long startTime = System.currentTimeMillis();
    
    delayedMessageCount.set(0);
    receivedDelayedMessage = null;
    messageProcessingTime.set(0);

    // When
    String messageId = rqueueMessageEnqueuer.enqueueIn("test-delayed-queue", testMessage, delayMillis);
    
    // Then
    assertNotNull(messageId, "Message ID should not be null");
    log.info("Enqueued delayed message with ID: {} and delay: {}ms", messageId, delayMillis);

    // Wait for message to be processed
    waitForMessages(delayedMessageLatch, 1, 10);
    
    long actualDelay = messageProcessingTime.get() - startTime;
    
    assertEquals(1, delayedMessageCount.get(), "Should have processed exactly 1 delayed message");
    assertEquals(testMessage, receivedDelayedMessage, "Received message should match sent message");
    
    // Verify delay was approximately correct (allow 500ms tolerance)
    assertTrue(actualDelay >= delayMillis - 500, 
        "Message should be delayed by at least " + (delayMillis - 500) + "ms, but was " + actualDelay + "ms");
    assertTrue(actualDelay <= delayMillis + 2000, 
        "Message should not be delayed by more than " + (delayMillis + 2000) + "ms, but was " + actualDelay + "ms");
  }

  @Test
  void testDelayedMessageWithCustomId() throws InterruptedException {
    // Given
    String testMessage = "Delayed message with custom ID";
    String customMessageId = "delayed-custom-123";
    long delayMillis = 1000; // 1 second delay
    
    delayedMessageCount.set(0);
    receivedDelayedMessage = null;

    // When
    boolean success = rqueueMessageEnqueuer.enqueueIn("test-delayed-queue", customMessageId, testMessage, delayMillis);
    
    // Then
    assertTrue(success, "Delayed message should be enqueued successfully");
    
    // Wait for message to be processed
    waitForMessages(delayedMessageLatch, 1, 10);
    
    assertEquals(1, delayedMessageCount.get(), "Should have processed exactly 1 delayed message");
    assertEquals(testMessage, receivedDelayedMessage, "Received message should match sent message");
  }

  @Test
  void testDelayedMessageWithDuration() throws InterruptedException {
    // Given
    String testMessage = "Delayed message with Duration";
    Duration delay = Duration.ofSeconds(1);
    
    delayedMessageCount.set(0);
    receivedDelayedMessage = null;

    // When
    String messageId = rqueueMessageEnqueuer.enqueueIn("test-delayed-queue", testMessage, delay);
    
    // Then
    assertNotNull(messageId, "Message ID should not be null");
    
    // Wait for message to be processed
    waitForMessages(delayedMessageLatch, 1, 10);
    
    assertEquals(1, delayedMessageCount.get(), "Should have processed exactly 1 delayed message");
    assertEquals(testMessage, receivedDelayedMessage, "Received message should match sent message");
  }

  @Test
  void testDelayedMessageWithInstant() throws InterruptedException {
    // Given
    String testMessage = "Delayed message with Instant";
    Instant futureTime = Instant.now().plusSeconds(1);
    
    delayedMessageCount.set(0);
    receivedDelayedMessage = null;

    // When
    String messageId = rqueueMessageEnqueuer.enqueueAt("test-delayed-queue", testMessage, futureTime);
    
    // Then
    assertNotNull(messageId, "Message ID should not be null");
    
    // Wait for message to be processed
    waitForMessages(delayedMessageLatch, 1, 10);
    
    assertEquals(1, delayedMessageCount.get(), "Should have processed exactly 1 delayed message");
    assertEquals(testMessage, receivedDelayedMessage, "Received message should match sent message");
  }

  @Test
  void testUniqueDelayedMessage() throws InterruptedException {
    // Given
    String testMessage = "Unique delayed message";
    String messageId = "unique-delayed-456";
    long delayMillis = 1000;
    
    delayedMessageCount.set(0);
    receivedDelayedMessage = null;

    // When
    boolean success = rqueueMessageEnqueuer.enqueueUniqueIn("test-delayed-queue", messageId, testMessage, delayMillis);
    
    // Then
    assertTrue(success, "Unique delayed message should be enqueued successfully");
    
    // Wait for message to be processed
    waitForMessages(delayedMessageLatch, 1, 10);
    
    assertEquals(1, delayedMessageCount.get(), "Should have processed exactly 1 delayed message");
    assertEquals(testMessage, receivedDelayedMessage, "Received message should match sent message");
  }

  @Test
  void testInvalidDelayValidation() {
    // Test negative delay
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueueIn("test-queue", "message", -1000L);
    });

    // Test zero delay (should be allowed)
    assertDoesNotThrow(() -> {
      rqueueMessageEnqueuer.enqueueIn("test-queue", "message", 0L);
    });
  }
}