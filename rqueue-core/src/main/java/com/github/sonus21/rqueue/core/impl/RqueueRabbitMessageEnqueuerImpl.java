/*
 * Copyright (c) 2020-2025 Sonu Kumar
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

package com.github.sonus21.rqueue.core.impl;

import com.github.sonus21.rqueue.common.RqueueRabbitTemplate;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RqueueRabbitMessageEnqueuerImpl implements RqueueMessageEnqueuer {

  private final RqueueRabbitTemplate<Serializable> rabbitTemplate;
  private final MessageConverter messageConverter;
  private final MessageHeaders messageHeaders;

  public RqueueRabbitMessageEnqueuerImpl(
      RqueueRabbitTemplate<Serializable> rabbitTemplate,
      MessageConverter messageConverter,
      MessageHeaders messageHeaders) {
    this.rabbitTemplate = rabbitTemplate;
    this.messageConverter = messageConverter;
    this.messageHeaders = messageHeaders;
  }

  private void validateBasic(String queue, Object message) {
    if (queue == null || queue.trim().isEmpty()) {
      throw new IllegalArgumentException("Queue name cannot be null or empty");
    }
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null");
    }
  }

  private void validateWithId(String queue, String messageId, Object message) {
    validateBasic(queue, message);
    if (messageId == null || messageId.trim().isEmpty()) {
      throw new IllegalArgumentException("Message ID cannot be null or empty");
    }
  }

  private String generateMessageId() {
    return UUID.randomUUID().toString();
  }

  private String pushMessage(String queueName, String messageId, Object message, 
                           Integer retryCount, Long delayMillis, boolean unique) {
    try {
      String finalMessageId = messageId != null ? messageId : generateMessageId();
      String exchangeName = "rqueue.exchange";
      String routingKey = queueName;

      // Declare exchange and queue
      rabbitTemplate.declareExchange(exchangeName, "direct");
      rabbitTemplate.declareQueue(queueName);
      rabbitTemplate.bindQueue(queueName, exchangeName, routingKey);

      // Set message properties
      MessageProperties properties = new MessageProperties();
      properties.setMessageId(finalMessageId);
      properties.setTimestamp(new Date());
      
      if (retryCount != null) {
        properties.setHeader("retryCount", retryCount);
      }
      
      if (delayMillis != null && delayMillis > 0) {
        properties.setDelay(Math.min(delayMillis.intValue(), Integer.MAX_VALUE));
      }

      // Send message
      rabbitTemplate.send(exchangeName, routingKey, message, properties);
      
      log.debug("Message sent to queue: {} with ID: {}", queueName, finalMessageId);
      return finalMessageId;
      
    } catch (Exception e) {
      log.error("Failed to send message to queue: {}", queueName, e);
      return null;
    }
  }

  @Override
  public String enqueue(String queueName, Object message) {
    validateBasic(queueName, message);
    return pushMessage(queueName, null, message, null, null, false);
  }

  @Override
  public boolean enqueue(String queueName, String messageId, Object message) {
    validateWithId(queueName, messageId, message);
    return pushMessage(queueName, messageId, message, null, null, false) != null;
  }

  @Override
  public boolean enqueueUnique(String queueName, String messageId, Object message) {
    validateWithId(queueName, messageId, message);
    return Objects.nonNull(pushMessage(queueName, messageId, message, null, null, true));
  }

  @Override
  public String enqueueWithRetry(String queueName, Object message, int retryCount) {
    validateBasic(queueName, message);
    if (retryCount < 0) {
      throw new IllegalArgumentException("Retry count cannot be negative");
    }
    return pushMessage(queueName, null, message, retryCount, null, false);
  }

  @Override
  public boolean enqueueWithRetry(String queueName, String messageId, Object message, int retryCount) {
    validateWithId(queueName, messageId, message);
    if (retryCount < 0) {
      throw new IllegalArgumentException("Retry count cannot be negative");
    }
    return pushMessage(queueName, messageId, message, retryCount, null, false) != null;
  }

  @Override
  public String enqueueWithPriority(String queueName, String priority, Object message) {
    validateBasic(queueName, message);
    if (priority == null || priority.trim().isEmpty()) {
      throw new IllegalArgumentException("Priority cannot be null or empty");
    }
    String priorityQueueName = queueName + "." + priority;
    return pushMessage(priorityQueueName, null, message, null, null, false);
  }

  @Override
  public boolean enqueueWithPriority(String queueName, String priority, String messageId, Object message) {
    validateWithId(queueName, messageId, message);
    if (priority == null || priority.trim().isEmpty()) {
      throw new IllegalArgumentException("Priority cannot be null or empty");
    }
    String priorityQueueName = queueName + "." + priority;
    return pushMessage(priorityQueueName, messageId, message, null, null, false) != null;
  }

  @Override
  public String enqueueIn(String queueName, Object message, long delayInMilliSecs) {
    validateBasic(queueName, message);
    if (delayInMilliSecs < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }
    return pushMessage(queueName, null, message, null, delayInMilliSecs, false);
  }

  @Override
  public boolean enqueueIn(String queueName, String messageId, Object message, long delayInMilliSecs) {
    validateWithId(queueName, messageId, message);
    if (delayInMilliSecs < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }
    return pushMessage(queueName, messageId, message, null, delayInMilliSecs, false) != null;
  }

  @Override
  public boolean enqueueUniqueIn(String queueName, String messageId, Object message, long delayInMillisecond) {
    validateWithId(queueName, messageId, message);
    if (delayInMillisecond < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }
    return Objects.nonNull(pushMessage(queueName, messageId, message, null, delayInMillisecond, true));
  }

  @Override
  public String enqueuePeriodic(String queueName, Object message, long periodInMilliSeconds) {
    validateBasic(queueName, message);
    if (periodInMilliSeconds <= 0) {
      throw new IllegalArgumentException("Period must be positive");
    }
    // For periodic messages, we'll use a different approach with RabbitMQ
    // This could involve using delayed message plugin or scheduling
    return pushMessage(queueName, null, message, null, periodInMilliSeconds, false);
  }

  @Override
  public boolean enqueuePeriodic(String queueName, String messageId, Object message, long periodInMilliSeconds) {
    validateWithId(queueName, messageId, message);
    if (periodInMilliSeconds <= 0) {
      throw new IllegalArgumentException("Period must be positive");
    }
    return pushMessage(queueName, messageId, message, null, periodInMilliSeconds, false) != null;
  }

  @Override
  public String enqueueInWithRetry(String queueName, Object message, int retryCount, long delayInMilliSecs) {
    validateBasic(queueName, message);
    if (retryCount < 0) {
      throw new IllegalArgumentException("Retry count cannot be negative");
    }
    if (delayInMilliSecs < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }
    return pushMessage(queueName, null, message, retryCount, delayInMilliSecs, false);
  }

  @Override
  public boolean enqueueInWithRetry(String queueName, String messageId, Object message, int retryCount, long delayInMilliSecs) {
    validateWithId(queueName, messageId, message);
    if (retryCount < 0) {
      throw new IllegalArgumentException("Retry count cannot be negative");
    }
    if (delayInMilliSecs < 0) {
      throw new IllegalArgumentException("Delay cannot be negative");
    }
    return pushMessage(queueName, messageId, message, retryCount, delayInMilliSecs, false) != null;
  }

  @Override
  public MessageConverter getMessageConverter() {
    return messageConverter;
  }
}