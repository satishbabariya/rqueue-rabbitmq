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

package com.github.sonus21.rqueue.test.config;

import com.github.sonus21.rqueue.common.RqueueRabbitTemplate;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.github.sonus21.rqueue.core.impl.RqueueRabbitMessageEnqueuerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;
import java.util.Collections;

/**
 * Test configuration for Rqueue RabbitMQ integration tests.
 *
 * @author Sonu Kumar
 */
@Slf4j
@TestConfiguration
public class RqueueRabbitTestConfiguration {

  @Bean
  @Primary
  public MessageConverter testMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  @Primary
  public RqueueRabbitTemplate<Serializable> testRqueueRabbitTemplate(ConnectionFactory connectionFactory) {
    return new RqueueRabbitTemplate<>(connectionFactory);
  }

  @Bean
  @Primary
  public RqueueMessageEnqueuer testRqueueMessageEnqueuer(
      RqueueRabbitTemplate<Serializable> rabbitTemplate,
      MessageConverter messageConverter) {
    return new RqueueRabbitMessageEnqueuerImpl(
        rabbitTemplate,
        messageConverter,
        new MessageHeaders(Collections.emptyMap()));
  }
}