/*
 * Copyright (c) 2019-2023 Sonu Kumar
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

package com.github.sonus21.rqueue.example;

import com.github.sonus21.rqueue.annotation.RqueueRabbitListener;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

/**
 * Example application demonstrating Rqueue with RabbitMQ.
 *
 * @author Sonu Kumar
 */
@SpringBootApplication
public class RqueueRabbitExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(RqueueRabbitExampleApplication.class, args);
  }

  @Component
  @Slf4j
  public static class MessageProducer implements CommandLineRunner {

    @Autowired
    private RqueueMessageEnqueuer rqueueMessageEnqueuer;

    @Override
    public void run(String... args) throws Exception {
      log.info("Starting Rqueue RabbitMQ example...");

      // Send immediate message
      String messageId = rqueueMessageEnqueuer.enqueue("simple-queue", "Hello RabbitMQ!");
      log.info("Sent immediate message with ID: {}", messageId);

      // Send delayed message
      messageId = rqueueMessageEnqueuer.enqueueIn("delayed-queue", "Delayed message", 5000L);
      log.info("Sent delayed message with ID: {}", messageId);

      // Send message with retry
      messageId = rqueueMessageEnqueuer.enqueueWithRetry("retry-queue", "Message with retry", 3);
      log.info("Sent message with retry with ID: {}", messageId);

      // Send message with priority
      messageId = rqueueMessageEnqueuer.enqueueWithPriority("priority-queue", "high", "High priority message");
      log.info("Sent high priority message with ID: {}", messageId);

      // Send periodic message
      messageId = rqueueMessageEnqueuer.enqueuePeriodic("periodic-queue", "Periodic message", 10000L);
      log.info("Sent periodic message with ID: {}", messageId);
    }
  }

  @Component
  @Slf4j
  public static class MessageListener {

    @RqueueRabbitListener(value = "simple-queue", concurrency = "1-5")
    public void handleSimpleMessage(String message) {
      log.info("Received simple message: {}", message);
    }

    @RqueueRabbitListener(value = "delayed-queue", concurrency = "1-3")
    public void handleDelayedMessage(String message) {
      log.info("Received delayed message: {}", message);
    }

    @RqueueRabbitListener(value = "retry-queue", numRetries = "3", concurrency = "1-2")
    public void handleRetryMessage(String message) {
      log.info("Received retry message: {}", message);
      // Simulate processing that might fail
      if (Math.random() < 0.3) {
        throw new RuntimeException("Simulated processing failure");
      }
    }

    @RqueueRabbitListener(value = "priority-queue", concurrency = "1-5")
    public void handlePriorityMessage(String message) {
      log.info("Received priority message: {}", message);
    }

    @RqueueRabbitListener(value = "periodic-queue", concurrency = "1-2")
    public void handlePeriodicMessage(String message) {
      log.info("Received periodic message: {}", message);
    }
  }
}