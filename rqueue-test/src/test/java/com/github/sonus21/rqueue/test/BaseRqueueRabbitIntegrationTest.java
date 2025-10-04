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

import com.github.sonus21.rqueue.common.RqueueRabbitTemplate;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.github.sonus21.rqueue.test.config.RqueueRabbitTestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
// Simplified test without TestContainers for now

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for Rqueue RabbitMQ integration tests.
 * Provides common setup, teardown, and utility methods.
 *
 * @author Sonu Kumar
 */
@Slf4j
@SpringBootTest(classes = {RqueueRabbitTestConfiguration.class})
@ContextConfiguration(classes = {RqueueRabbitTestConfiguration.class})
public abstract class BaseRqueueRabbitIntegrationTest {

  // Simplified test without TestContainers - assumes RabbitMQ is running locally

  @Autowired
  protected RqueueMessageEnqueuer rqueueMessageEnqueuer;

  @Autowired
  protected RqueueRabbitTemplate<Serializable> rabbitTemplate;

  @Autowired
  protected ConnectionFactory connectionFactory;

  protected RabbitAdmin rabbitAdmin;

  @BeforeEach
  void setUp() {
    rabbitAdmin = new RabbitAdmin(connectionFactory);
    log.info("RabbitMQ connection established");
  }

  @AfterEach
  void tearDown() {
    // Clean up queues and exchanges
    try {
      rabbitAdmin.purgeQueue("test-queue");
      rabbitAdmin.deleteQueue("test-queue");
      rabbitAdmin.deleteExchange("rqueue.exchange");
    } catch (Exception e) {
      log.warn("Error during cleanup: {}", e.getMessage());
    }
  }

  /**
   * Wait for a specific number of messages to be processed.
   */
  protected void waitForMessages(CountDownLatch latch, int expectedCount, long timeoutSeconds) throws InterruptedException {
    boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
    if (!completed) {
      throw new AssertionError("Expected " + expectedCount + " messages but only " + 
          (expectedCount - latch.getCount()) + " were processed within " + timeoutSeconds + " seconds");
    }
  }

  /**
   * Wait for a counter to reach expected value.
   */
  protected void waitForCounter(AtomicInteger counter, int expectedValue, long timeoutSeconds) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    while (counter.get() < expectedValue && (System.currentTimeMillis() - startTime) < timeoutSeconds * 1000) {
      Thread.sleep(100);
    }
    
    if (counter.get() < expectedValue) {
      throw new AssertionError("Expected counter to reach " + expectedValue + 
          " but it only reached " + counter.get() + " within " + timeoutSeconds + " seconds");
    }
  }

  /**
   * Create a test queue with specific properties.
   */
  protected void createTestQueue(String queueName, boolean durable, boolean exclusive, boolean autoDelete) {
    rabbitAdmin.declareQueue(new org.springframework.amqp.core.Queue(queueName, durable, exclusive, autoDelete));
  }

  /**
   * Purge all messages from a queue.
   */
  protected void purgeQueue(String queueName) {
    rabbitAdmin.purgeQueue(queueName);
  }

  /**
   * Get queue message count.
   */
  protected int getQueueMessageCount(String queueName) {
    try {
      var queueInfo = rabbitAdmin.getQueueInfo(queueName);
      return queueInfo != null ? queueInfo.getMessageCount() : 0;
    } catch (Exception e) {
      return 0;
    }
  }
}