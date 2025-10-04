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

package com.github.sonus21.rqueue.spring.boot.autoconfigure;

import com.github.sonus21.rqueue.common.RqueueRabbitTemplate;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import com.github.sonus21.rqueue.core.impl.RqueueRabbitMessageEnqueuerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHeaders;

import java.io.Serializable;

/**
 * Auto-configuration for Rqueue with RabbitMQ support.
 *
 * @author Sonu Kumar
 */
@Slf4j
@Configuration
@ConditionalOnClass({ConnectionFactory.class, RqueueRabbitTemplate.class})
@ConditionalOnProperty(prefix = "rqueue", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RqueueRabbitProperties.class)
public class RqueueRabbitAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MessageConverter rqueueMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  @ConditionalOnMissingBean
  public RqueueRabbitTemplate<Serializable> rqueueRabbitTemplate(ConnectionFactory connectionFactory) {
    return new RqueueRabbitTemplate<>(connectionFactory);
  }

  @Bean
  @ConditionalOnMissingBean
  public RqueueMessageEnqueuer rqueueMessageEnqueuer(
      RqueueRabbitTemplate<Serializable> rabbitTemplate,
      MessageConverter messageConverter) {
    return new RqueueRabbitMessageEnqueuerImpl(
        rabbitTemplate,
        messageConverter,
        new MessageHeaders(java.util.Collections.emptyMap()));
  }
}