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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for concurrency handling.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class ConcurrencyIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final AtomicInteger concurrentMessageCount = new AtomicInteger(0);
  private static final AtomicLong totalProcessingTime = new AtomicLong(0);
  private static final List<String> processedMessages = new ArrayList<>();
  private static final List<Long> processingTimes = new ArrayList<>();

  @Component
  public static class ConcurrentMessageListener {

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(value = "test-concurrent-queue", concurrency = "3-5")
    public void handleConcurrentMessage(String message) {
      long startTime = System.currentTimeMillis();
      log.info("Processing message: {} on thread: {}", message, Thread.currentThread().getName());
      
      try {
        // Simulate some processing time
        Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms processing time
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      
      long processingTime = System.currentTimeMillis() - startTime;
      
      synchronized (processedMessages) {
        processedMessages.add(message);
        processingTimes.add(processingTime);
      }
      
      concurrentMessageCount.incrementAndGet();
      totalProcessingTime.addAndGet(processingTime);
      
      log.info("Completed processing message: {} in {}ms", message, processingTime);
    }
  }

  @Test
  void testConcurrentMessageProcessing() throws InterruptedException {
    // Given
    int messageCount = 10;
    CountDownLatch latch = new CountDownLatch(messageCount);
    
    concurrentMessageCount.set(0);
    synchronized (processedMessages) {
      processedMessages.clear();
      processingTimes.clear();
    }
    totalProcessingTime.set(0);

    // When - Send multiple messages concurrently
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < messageCount; i++) {
      String message = "Concurrent message " + i;
      String messageId = rqueueMessageEnqueuer.enqueue("test-concurrent-queue", message);
      assertNotNull(messageId, "Message ID should not be null for message: " + message);
    }
    long enqueueTime = System.currentTimeMillis() - startTime;

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (concurrentMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 10000) { // 10 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - startTime;

    assertEquals(messageCount, concurrentMessageCount.get(), 
        "Should have processed all " + messageCount + " messages");
    
    synchronized (processedMessages) {
      assertEquals(messageCount, processedMessages.size(), 
          "Should have received all " + messageCount + " messages");
    }

    log.info("Processed {} messages in {}ms (enqueue: {}ms, processing: {}ms)", 
        messageCount, totalTime, enqueueTime, totalProcessingTime.get());
    
    // Verify that processing was concurrent (total processing time should be less than sequential)
    long sequentialTime = messageCount * 200; // Average processing time per message
    assertTrue(totalProcessingTime.get() < sequentialTime, 
        "Concurrent processing should be faster than sequential. Concurrent: " + 
        totalProcessingTime.get() + "ms, Sequential would be: " + sequentialTime + "ms");
  }

  @Test
  void testConcurrentDelayedMessages() throws InterruptedException {
    // Given
    int messageCount = 5;
    long delayMillis = 1000;
    
    concurrentMessageCount.set(0);
    synchronized (processedMessages) {
      processedMessages.clear();
      processingTimes.clear();
    }

    // When - Send multiple delayed messages
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < messageCount; i++) {
      String message = "Delayed concurrent message " + i;
      String messageId = rqueueMessageEnqueuer.enqueueIn("test-concurrent-queue", message, delayMillis);
      assertNotNull(messageId, "Message ID should not be null for message: " + message);
    }

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (concurrentMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 15000) { // 15 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - startTime;

    assertEquals(messageCount, concurrentMessageCount.get(), 
        "Should have processed all " + messageCount + " delayed messages");
    
    synchronized (processedMessages) {
      assertEquals(messageCount, processedMessages.size(), 
          "Should have received all " + messageCount + " delayed messages");
    }

    // Verify that messages were delayed appropriately
    assertTrue(totalTime >= delayMillis, 
        "Total time should be at least the delay time: " + delayMillis + "ms, but was " + totalTime + "ms");
  }

  @Test
  void testConcurrentPriorityMessages() throws InterruptedException {
    // Given
    String[] priorities = {"high", "medium", "low"};
    int messagesPerPriority = 3;
    int totalMessages = priorities.length * messagesPerPriority;
    
    concurrentMessageCount.set(0);
    synchronized (processedMessages) {
      processedMessages.clear();
      processingTimes.clear();
    }

    // When - Send messages with different priorities
    for (String priority : priorities) {
      for (int i = 0; i < messagesPerPriority; i++) {
        String message = priority + " priority message " + i;
        String messageId = rqueueMessageEnqueuer.enqueueWithPriority("test-concurrent-queue", priority, message);
        assertNotNull(messageId, "Message ID should not be null for message: " + message);
      }
    }

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (concurrentMessageCount.get() < totalMessages && 
           (System.currentTimeMillis() - waitStartTime) < 10000) { // 10 second timeout
      Thread.sleep(100);
    }

    assertEquals(totalMessages, concurrentMessageCount.get(), 
        "Should have processed all " + totalMessages + " priority messages");
    
    synchronized (processedMessages) {
      assertEquals(totalMessages, processedMessages.size(), 
          "Should have received all " + totalMessages + " priority messages");
    }
  }

  @Test
  void testConcurrentUniqueMessages() throws InterruptedException {
    // Given
    int messageCount = 5;
    String baseMessageId = "unique-concurrent-";
    
    concurrentMessageCount.set(0);
    synchronized (processedMessages) {
      processedMessages.clear();
      processingTimes.clear();
    }

    // When - Send multiple unique messages with the same ID (should overwrite)
    for (int i = 0; i < messageCount; i++) {
      String message = "Unique concurrent message " + i;
      String messageId = baseMessageId + "123"; // Same ID for all messages
      boolean success = rqueueMessageEnqueuer.enqueueUnique("test-concurrent-queue", messageId, message);
      assertTrue(success, "Unique message should be enqueued successfully");
    }

    // Then - Wait for message to be processed (should be only 1 due to uniqueness)
    long waitStartTime = System.currentTimeMillis();
    while (concurrentMessageCount.get() < 1 && 
           (System.currentTimeMillis() - waitStartTime) < 5000) { // 5 second timeout
      Thread.sleep(100);
    }

    assertEquals(1, concurrentMessageCount.get(), 
        "Should have processed only 1 unique message (others should be overwritten)");
    
    synchronized (processedMessages) {
      assertEquals(1, processedMessages.size(), 
          "Should have received only 1 unique message");
    }
  }

  @Test
  void testConcurrentRetryMessages() throws InterruptedException {
    // Given
    int messageCount = 3;
    int retryCount = 2;
    
    concurrentMessageCount.set(0);
    synchronized (processedMessages) {
      processedMessages.clear();
      processingTimes.clear();
    }

    // When - Send multiple messages with retry
    for (int i = 0; i < messageCount; i++) {
      String message = "Retry concurrent message " + i;
      String messageId = rqueueMessageEnqueuer.enqueueWithRetry("test-concurrent-queue", message, retryCount);
      assertNotNull(messageId, "Message ID should not be null for message: " + message);
    }

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (concurrentMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 10000) { // 10 second timeout
      Thread.sleep(100);
    }

    assertEquals(messageCount, concurrentMessageCount.get(), 
        "Should have processed all " + messageCount + " retry messages");
    
    synchronized (processedMessages) {
      assertEquals(messageCount, processedMessages.size(), 
          "Should have received all " + messageCount + " retry messages");
    }
  }
}