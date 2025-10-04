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

import com.github.sonus21.rqueue.annotation.RqueueRabbitListener;
import com.github.sonus21.rqueue.test.config.RqueueRabbitTestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for basic messaging functionality.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public class BasicMessagingIntegrationTest extends BaseRqueueRabbitIntegrationTest {

  private static final CountDownLatch messageLatch = new CountDownLatch(1);
  private static final AtomicInteger messageCount = new AtomicInteger(0);
  private static String receivedMessage;

  @Component
  public static class TestMessageListener {

    @RqueueRabbitListener(value = "test-basic-queue", concurrency = "1")
    public void handleMessage(String message) {
      log.info("Received message: {}", message);
      receivedMessage = message;
      messageCount.incrementAndGet();
      messageLatch.countDown();
    }
  }

  @Test
  void testBasicMessageEnqueueAndDequeue() throws InterruptedException {
    // Given
    String testMessage = "Hello RabbitMQ!";
    messageCount.set(0);
    receivedMessage = null;

    // When
    String messageId = rqueueMessageEnqueuer.enqueue("test-basic-queue", testMessage);
    
    // Then
    assertNotNull(messageId, "Message ID should not be null");
    log.info("Enqueued message with ID: {}", messageId);

    // Wait for message to be processed
    waitForMessages(messageLatch, 1, 10);
    
    assertEquals(1, messageCount.get(), "Should have processed exactly 1 message");
    assertEquals(testMessage, receivedMessage, "Received message should match sent message");
  }

  @Test
  void testMessageWithCustomId() {
    // Given
    String testMessage = "Message with custom ID";
    String customMessageId = "custom-msg-123";

    // When
    boolean success = rqueueMessageEnqueuer.enqueue("test-basic-queue", customMessageId, testMessage);

    // Then
    assertTrue(success, "Message should be enqueued successfully");
  }

  @Test
  void testUniqueMessageEnqueue() {
    // Given
    String testMessage = "Unique message";
    String messageId = "unique-msg-456";

    // When
    boolean success = rqueueMessageEnqueuer.enqueueUnique("test-basic-queue", messageId, testMessage);

    // Then
    assertTrue(success, "Unique message should be enqueued successfully");
  }

  @Test
  void testMessageWithRetry() {
    // Given
    String testMessage = "Message with retry";
    int retryCount = 3;

    // When
    String messageId = rqueueMessageEnqueuer.enqueueWithRetry("test-basic-queue", testMessage, retryCount);

    // Then
    assertNotNull(messageId, "Message ID should not be null");
  }

  @Test
  void testInvalidInputValidation() {
    // Test null queue name
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue(null, "test message");
    });

    // Test empty queue name
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue("", "test message");
    });

    // Test null message
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueue("test-queue", null);
    });

    // Test negative retry count
    assertThrows(IllegalArgumentException.class, () -> {
      rqueueMessageEnqueuer.enqueueWithRetry("test-queue", "message", -1);
    });
  }
}