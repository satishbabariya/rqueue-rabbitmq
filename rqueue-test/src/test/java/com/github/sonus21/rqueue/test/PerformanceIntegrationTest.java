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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance baseline tests for Rqueue RabbitMQ implementation.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class PerformanceIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final AtomicInteger performanceMessageCount = new AtomicInteger(0);
  private static final AtomicLong totalProcessingTime = new AtomicLong(0);
  private static final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
  private static final AtomicLong maxProcessingTime = new AtomicLong(0);

  @Component
  public static class PerformanceMessageListener {

    @com.github.sonus21.rqueue.annotation.RqueueRabbitListener(value = "test-performance-queue", concurrency = "5-10")
    public void handlePerformanceMessage(String message) {
      long startTime = System.currentTimeMillis();
      
      try {
        // Simulate minimal processing time
        Thread.sleep(10); // 10ms processing time
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      
      long processingTime = System.currentTimeMillis() - startTime;
      
      performanceMessageCount.incrementAndGet();
      totalProcessingTime.addAndGet(processingTime);
      
      // Update min/max processing times
      long currentMin = minProcessingTime.get();
      while (processingTime < currentMin && !minProcessingTime.compareAndSet(currentMin, processingTime)) {
        currentMin = minProcessingTime.get();
      }
      
      long currentMax = maxProcessingTime.get();
      while (processingTime > currentMax && !maxProcessingTime.compareAndSet(currentMax, processingTime)) {
        currentMax = maxProcessingTime.get();
      }
    }
  }

  @Test
  void testThroughputBaseline() throws InterruptedException {
    // Given
    int messageCount = 100;
    CountDownLatch latch = new CountDownLatch(messageCount);
    
    performanceMessageCount.set(0);
    totalProcessingTime.set(0);
    minProcessingTime.set(Long.MAX_VALUE);
    maxProcessingTime.set(0);

    // When - Send messages and measure throughput
    long enqueueStartTime = System.currentTimeMillis();
    for (int i = 0; i < messageCount; i++) {
      String message = "Performance test message " + i;
      String messageId = rqueueMessageEnqueuer.enqueue("test-performance-queue", message);
      assertNotNull(messageId, "Message ID should not be null for message: " + message);
    }
    long enqueueTime = System.currentTimeMillis() - enqueueStartTime;

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (performanceMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 30000) { // 30 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - enqueueStartTime;

    // Calculate performance metrics
    double throughput = (double) messageCount / (totalTime / 1000.0); // messages per second
    double avgProcessingTime = (double) totalProcessingTime.get() / messageCount;
    double avgLatency = (double) totalTime / messageCount;

    log.info("Performance Results:");
    log.info("  Messages: {}", messageCount);
    log.info("  Total Time: {}ms", totalTime);
    log.info("  Enqueue Time: {}ms", enqueueTime);
    log.info("  Throughput: {:.2f} messages/second", throughput);
    log.info("  Average Processing Time: {:.2f}ms", avgProcessingTime);
    log.info("  Average Latency: {:.2f}ms", avgLatency);
    log.info("  Min Processing Time: {}ms", minProcessingTime.get());
    log.info("  Max Processing Time: {}ms", maxProcessingTime.get());

    // Assertions for baseline performance
    assertEquals(messageCount, performanceMessageCount.get(), 
        "Should have processed all " + messageCount + " messages");
    
    assertTrue(throughput > 10, "Throughput should be at least 10 messages/second, was: " + throughput);
    assertTrue(avgLatency < 5000, "Average latency should be less than 5 seconds, was: " + avgLatency);
    assertTrue(avgProcessingTime < 100, "Average processing time should be less than 100ms, was: " + avgProcessingTime);
  }

  @Test
  void testConcurrentThroughput() throws InterruptedException {
    // Given
    int messageCount = 200;
    int concurrentProducers = 5;
    int messagesPerProducer = messageCount / concurrentProducers;
    
    performanceMessageCount.set(0);
    totalProcessingTime.set(0);
    minProcessingTime.set(Long.MAX_VALUE);
    maxProcessingTime.set(0);

    // When - Send messages concurrently
    long startTime = System.currentTimeMillis();
    
    Thread[] producerThreads = new Thread[concurrentProducers];
    for (int i = 0; i < concurrentProducers; i++) {
      final int producerId = i;
      producerThreads[i] = new Thread(() -> {
        for (int j = 0; j < messagesPerProducer; j++) {
          String message = "Concurrent message from producer " + producerId + " - " + j;
          String messageId = rqueueMessageEnqueuer.enqueue("test-performance-queue", message);
          assertNotNull(messageId, "Message ID should not be null");
        }
      });
      producerThreads[i].start();
    }

    // Wait for all producers to finish
    for (Thread thread : producerThreads) {
      thread.join();
    }
    
    long enqueueTime = System.currentTimeMillis() - startTime;

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (performanceMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 60000) { // 60 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - startTime;

    // Calculate performance metrics
    double throughput = (double) messageCount / (totalTime / 1000.0);
    double enqueueThroughput = (double) messageCount / (enqueueTime / 1000.0);
    double avgProcessingTime = (double) totalProcessingTime.get() / messageCount;

    log.info("Concurrent Performance Results:");
    log.info("  Messages: {}", messageCount);
    log.info("  Concurrent Producers: {}", concurrentProducers);
    log.info("  Total Time: {}ms", totalTime);
    log.info("  Enqueue Time: {}ms", enqueueTime);
    log.info("  Total Throughput: {:.2f} messages/second", throughput);
    log.info("  Enqueue Throughput: {:.2f} messages/second", enqueueThroughput);
    log.info("  Average Processing Time: {:.2f}ms", avgProcessingTime);

    // Assertions
    assertEquals(messageCount, performanceMessageCount.get(), 
        "Should have processed all " + messageCount + " messages");
    
    assertTrue(enqueueThroughput > 50, "Enqueue throughput should be at least 50 messages/second, was: " + enqueueThroughput);
    assertTrue(throughput > 20, "Total throughput should be at least 20 messages/second, was: " + throughput);
  }

  @Test
  void testLargeMessageHandling() throws InterruptedException {
    // Given
    int messageCount = 20;
    StringBuilder largeMessageBuilder = new StringBuilder();
    for (int i = 0; i < 1000; i++) { // 1KB message
      largeMessageBuilder.append("Large message content ").append(i).append(" ");
    }
    String largeMessage = largeMessageBuilder.toString();
    
    performanceMessageCount.set(0);
    totalProcessingTime.set(0);

    // When - Send large messages
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < messageCount; i++) {
      String message = largeMessage + " - " + i;
      String messageId = rqueueMessageEnqueuer.enqueue("test-performance-queue", message);
      assertNotNull(messageId, "Message ID should not be null for large message: " + i);
    }
    long enqueueTime = System.currentTimeMillis() - startTime;

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (performanceMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 30000) { // 30 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - startTime;

    // Calculate performance metrics
    double throughput = (double) messageCount / (totalTime / 1000.0);
    double avgProcessingTime = (double) totalProcessingTime.get() / messageCount;

    log.info("Large Message Performance Results:");
    log.info("  Messages: {}", messageCount);
    log.info("  Message Size: ~{}KB", largeMessage.length() / 1024);
    log.info("  Total Time: {}ms", totalTime);
    log.info("  Enqueue Time: {}ms", enqueueTime);
    log.info("  Throughput: {:.2f} messages/second", throughput);
    log.info("  Average Processing Time: {:.2f}ms", avgProcessingTime);

    // Assertions
    assertEquals(messageCount, performanceMessageCount.get(), 
        "Should have processed all " + messageCount + " large messages");
    
    assertTrue(throughput > 5, "Throughput should be at least 5 messages/second for large messages, was: " + throughput);
  }

  @Test
  void testDelayedMessagePerformance() throws InterruptedException {
    // Given
    int messageCount = 50;
    long delayMillis = 2000; // 2 second delay
    
    performanceMessageCount.set(0);
    totalProcessingTime.set(0);

    // When - Send delayed messages
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < messageCount; i++) {
      String message = "Delayed performance message " + i;
      String messageId = rqueueMessageEnqueuer.enqueueIn("test-performance-queue", message, delayMillis);
      assertNotNull(messageId, "Message ID should not be null for delayed message: " + i);
    }
    long enqueueTime = System.currentTimeMillis() - startTime;

    // Then - Wait for all messages to be processed
    long waitStartTime = System.currentTimeMillis();
    while (performanceMessageCount.get() < messageCount && 
           (System.currentTimeMillis() - waitStartTime) < 60000) { // 60 second timeout
      Thread.sleep(100);
    }
    long totalTime = System.currentTimeMillis() - startTime;

    // Calculate performance metrics
    double enqueueThroughput = (double) messageCount / (enqueueTime / 1000.0);
    double avgProcessingTime = (double) totalProcessingTime.get() / messageCount;

    log.info("Delayed Message Performance Results:");
    log.info("  Messages: {}", messageCount);
    log.info("  Delay: {}ms", delayMillis);
    log.info("  Total Time: {}ms", totalTime);
    log.info("  Enqueue Time: {}ms", enqueueTime);
    log.info("  Enqueue Throughput: {:.2f} messages/second", enqueueThroughput);
    log.info("  Average Processing Time: {:.2f}ms", avgProcessingTime);

    // Assertions
    assertEquals(messageCount, performanceMessageCount.get(), 
        "Should have processed all " + messageCount + " delayed messages");
    
    assertTrue(enqueueThroughput > 20, "Enqueue throughput should be at least 20 messages/second, was: " + enqueueThroughput);
    assertTrue(totalTime >= delayMillis, "Total time should be at least the delay time: " + delayMillis + "ms");
  }
}