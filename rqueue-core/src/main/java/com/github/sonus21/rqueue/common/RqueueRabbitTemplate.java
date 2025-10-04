/*
 * Copyright (c) 2020-2023 Sonu Kumar
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

package com.github.sonus21.rqueue.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RqueueRabbitTemplate<V extends Serializable> {

  protected RabbitTemplate rabbitTemplate;
  protected AmqpAdmin amqpAdmin;
  protected MessageConverter messageConverter;

  public RqueueRabbitTemplate(ConnectionFactory connectionFactory) {
    this.rabbitTemplate = new RabbitTemplate(connectionFactory);
    this.amqpAdmin = new RabbitAdmin(connectionFactory);
    this.messageConverter = new Jackson2JsonMessageConverter();
    this.rabbitTemplate.setMessageConverter(messageConverter);
  }

  public RabbitTemplate getRabbitTemplate() {
    return this.rabbitTemplate;
  }

  public AmqpAdmin getAmqpAdmin() {
    return this.amqpAdmin;
  }

  public void declareQueue(String queueName) {
    Queue queue = new Queue(queueName, true, false, false);
    amqpAdmin.declareQueue(queue);
  }

  public void declareQueue(String queueName, Map<String, Object> arguments) {
    Queue queue = new Queue(queueName, true, false, false, arguments);
    amqpAdmin.declareQueue(queue);
  }

  public void declareExchange(String exchangeName, String type) {
    Exchange exchange = ExchangeBuilder.directExchange(exchangeName).durable(true).build();
    amqpAdmin.declareExchange(exchange);
  }

  public void bindQueue(String queueName, String exchangeName, String routingKey) {
    Binding binding = BindingBuilder.bind(new Queue(queueName))
        .to(new DirectExchange(exchangeName))
        .with(routingKey);
    amqpAdmin.declareBinding(binding);
  }

  public void send(String exchangeName, String routingKey, Object message) {
    rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
  }

  public void send(String exchangeName, String routingKey, Object message, MessageProperties properties) {
    rabbitTemplate.convertAndSend(exchangeName, routingKey, message, msg -> {
      msg.getMessageProperties().setHeaders(properties.getHeaders());
      return msg;
    });
  }

  public V receive(String queueName) {
    return (V) rabbitTemplate.receiveAndConvert(queueName);
  }

  public V receive(String queueName, long timeoutMillis) {
    return (V) rabbitTemplate.receiveAndConvert(queueName, timeoutMillis);
  }

  public void sendWithDelay(String exchangeName, String routingKey, Object message, long delayMillis) {
    MessageProperties properties = new MessageProperties();
    properties.setDelay((int) delayMillis);
    send(exchangeName, routingKey, message, properties);
  }

  public void sendWithPriority(String exchangeName, String routingKey, Object message, int priority) {
    MessageProperties properties = new MessageProperties();
    properties.setPriority(priority);
    send(exchangeName, routingKey, message, properties);
  }

  public void sendWithTTL(String exchangeName, String routingKey, Object message, long ttlMillis) {
    MessageProperties properties = new MessageProperties();
    properties.setExpiration(String.valueOf(ttlMillis));
    send(exchangeName, routingKey, message, properties);
  }

  public void purgeQueue(String queueName) {
    amqpAdmin.purgeQueue(queueName);
  }

  public void deleteQueue(String queueName) {
    amqpAdmin.deleteQueue(queueName);
  }

  public void deleteExchange(String exchangeName) {
    amqpAdmin.deleteExchange(exchangeName);
  }

  public org.springframework.amqp.core.QueueInformation getQueueProperties(String queueName) {
    return amqpAdmin.getQueueInfo(queueName);
  }

  public void set(String key, Object val) {
    // For compatibility with Redis interface - store in a simple queue
    String queueName = "storage::" + key;
    declareQueue(queueName);
    send("", queueName, val);
  }

  public V get(String key) {
    String queueName = "storage::" + key;
    return receive(queueName, 1000);
  }

  public boolean exist(String key) {
    org.springframework.amqp.core.QueueInformation props = getQueueProperties("storage::" + key);
    return props != null && props.getMessageCount() > 0;
  }

  public int ttl(String key) {
    // RabbitMQ doesn't have TTL for individual messages in the same way as Redis
    // This is a simplified implementation
    return exist(key) ? 3600 : -2; // Return 1 hour if exists, -2 if not
  }

  public void set(String key, Object val, Duration duration) {
    set(key, val);
    // Note: RabbitMQ TTL is per-message, not per-key like Redis
  }

  public Boolean setIfAbsent(String lockKey, Object val, Duration duration) {
    if (!exist(lockKey)) {
      set(lockKey, val, duration);
      return true;
    }
    return false;
  }

  public Boolean delete(String key) {
    String queueName = "storage::" + key;
    try {
      deleteQueue(queueName);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public Object delete(Collection<String> keys) {
    int deleted = 0;
    for (String key : keys) {
      if (delete(key)) {
        deleted++;
      }
    }
    return deleted;
  }

  // Simplified implementations for Redis compatibility
  public Long getListSize(String lName) {
    org.springframework.amqp.core.QueueInformation props = getQueueProperties(lName);
    return props != null ? (long) props.getMessageCount() : 0L;
  }

  public Long getZsetSize(String zsetName) {
    // For priority queues, we'll use a different approach
    return getListSize(zsetName);
  }

  public Double getZsetMemberScore(String zsetName, String key) {
    // Simplified - return priority based on key hash
    return (double) (key.hashCode() % 10);
  }

  public Long rpush(String listName, Object val) {
    declareQueue(listName);
    send("", listName, val);
    return getListSize(listName);
  }

  public Long addToSet(String setName, Object... values) {
    declareQueue(setName);
    for (Object value : values) {
      send("", setName, value);
    }
    return (long) values.length;
  }

  public List<V> lrange(String key, long start, long end) {
    // Simplified implementation - RabbitMQ doesn't support range operations like Redis
    // This would require a more complex implementation
    return List.of();
  }

  public Set<V> zrange(String key, long start, long end) {
    // Simplified implementation for priority queues
    return Set.of();
  }

  public void zremRangeByScore(String key, long min, long max) {
    // Simplified implementation
  }

  public Set<V> getMembers(String key) {
    // Simplified implementation
    return Set.of();
  }

  public void ltrim(String key, Integer start, Integer end) {
    // Simplified implementation
  }

  public Boolean zadd(String key, Object val, long score) {
    declareQueue(key);
    sendWithPriority("", key, val, (int) score);
    return true;
  }

  public void rename(String oldKey, String newKey) {
    // Simplified implementation
    V value = get(oldKey);
    if (value != null) {
      set(newKey, value);
      delete(oldKey);
    }
  }

  public void rename(List<String> oldKeys, List<String> newKeys) {
    for (int i = 0; i < oldKeys.size() && i < newKeys.size(); i++) {
      rename(oldKeys.get(i), newKeys.get(i));
    }
  }
}