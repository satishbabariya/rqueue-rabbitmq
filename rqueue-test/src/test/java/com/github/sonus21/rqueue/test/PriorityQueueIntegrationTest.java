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
 * Integration tests for priority queue functionality.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class PriorityQueueIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final CountDownLatch priorityMessageLatch = new CountDownLatch(3);
  private static final AtomicInteger priorityMessageCount = new AtomicInteger(0);
  private static final List<String> receivedMessages = new ArrayList<>();

  @Component
  public static class PriorityMessageListener {

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(value = "test-priority-queue", concurrency = "1")
    public void handlePriorityMessage(String message) {
      log.info("Received priority message: {}", message);
      synchronized (receivedMessages) {
        receivedMessages.add(message);
      }
      priorityMessageCount.incrementAndGet();
      priorityMessageLatch.countDown();
    }
  }

  @Test
  void testPriorityMessageEnqueue() throws InterruptedException {
    // Given
    String highPriorityMessage = "High priority message";
    String mediumPriorityMessage = "Medium priority message";
    String lowPriorityMessage = "Low priority message";
    
    priorityMessageCount.set(0);
    synchronized (receivedMessages) {
      receivedMessages.clear();
    }

    // When - Send messages with different priorities
    String highId = rqueueMessageEnqueuer.enqueueWithPriority("test-priority-queue", "high", highPriorityMessage);
    String mediumId = rqueueMessageEnqueuer.enqueueWithPriority("test-priority-queue", "medium", mediumPriorityMessage);
    String lowId = rqueueMessageEnqueuer.enqueueWithPriority("test-priority-queue", "low", lowPriorityMessage);
    
    // Then
    assertNotNull(highId, "High priority message ID should not be null");
    assertNotNull(mediumId, "Medium priority message ID should not be null");
    assertNotNull(lowId, "Low priority message ID should not be null");

    log.info("Enqueued messages - High: {}, Medium: {}, Low: {}", highId, mediumId, lowId);

    // Wait for all messages to be processed
    waitForMessages(priorityMessageLatch, 3, 10);
    
    assertEquals(3, priorityMessageCount.get(), "Should have processed exactly 3 priority messages");
    
    synchronized (receivedMessages) {
      assertEquals(3, receivedMessages.size(), "Should have received exactly 3 messages");
      assertTrue(receivedMessages.contains(highPriorityMessage), "Should contain high priority message");
      assertTrue(receivedMessages.contains(mediumPriorityMessage), "Should contain medium priority message");
      assertTrue(receivedMessages.contains(lowPriorityMessage), "Should contain low priority message");
    }
  }

  @Test
  void testPriorityMessageWithCustomId() {
    // Given
    String testMessage = "Priority message with custom ID";
    String customMessageId = "priority-custom-123";
    String priority = "high";

    // When
    boolean success = rqueueMessageEnqueuer.enqueueWithPriority("test-priority-queue", priority, customMessageId, testMessage);
    
    // Then
    assertTrue(success, "Priority message should be enqueued successfully");
  }

  @Test
  void testPriorityMessageWithDelay() throws InterruptedException {
    // Given
    String testMessage = "Delayed priority message";
    String priority = "high";
    long delayMillis = 1000;
    
    priorityMessageCount.set(0);
    synchronized (receivedMessages) {
      receivedMessages.clear();
    }

    // When
    String messageId = rqueueMessageEnqueuer.enqueueInWithPriority("test-priority-queue", priority, testMessage, delayMillis);
    
    // Then
    assertNotNull(messageId, "Message ID should not be null");
    
    // Wait for message to be processed
    waitForMessages(priorityMessageLatch, 1, 10);
    
    assertEquals(1, priorityMessageCount.get(), "Should have processed exactly 1 priority message");
    
    synchronized (receivedMessages) {
      assertTrue(receivedMessages.contains(testMessage), "Should contain the priority message");
    }
  }

  @Test
  void testInvalidPriorityValidation() {
    // Test null priority
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueueWithPriority("test-queue", null, "message");
    });

    // Test empty priority
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueueWithPriority("test-queue", "", "message");
    });

    // Test whitespace-only priority
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueueWithPriority("test-queue", "   ", "message");
    });
  }

  @Test
  void testMultiplePriorityLevels() throws InterruptedException {
    // Given
    String[] priorities = {"critical", "high", "medium", "low"};
    String[] messages = new String[priorities.length];
    
    for (int i = 0; i < priorities.length; i++) {
      messages[i] = priorities[i] + " priority message";
    }
    
    priorityMessageCount.set(0);
    synchronized (receivedMessages) {
      receivedMessages.clear();
    }

    // When - Send messages with different priority levels
    for (int i = 0; i < priorities.length; i++) {
      String messageId = rqueueMessageEnqueuer.enqueueWithPriority("test-priority-queue", priorities[i], messages[i]);
      assertNotNull(messageId, "Message ID should not be null for priority: " + priorities[i]);
    }

    // Then - Wait for all messages to be processed
    CountDownLatch allMessagesLatch = new CountDownLatch(priorities.length);
    
    // Reset the latch for this test
    synchronized (priorityMessageLatch) {
      // We need to create a new latch for this test
    }
    
    // Wait a bit for processing
    Thread.sleep(2000);
    
    synchronized (receivedMessages) {
      assertEquals(priorities.length, receivedMessages.size(), 
          "Should have received all " + priorities.length + " priority messages");
      
      for (String message : messages) {
        assertTrue(receivedMessages.contains(message), "Should contain message: " + message);
      }
    }
  }
}