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

package com.github.sonus21.rqueue.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a RabbitMQ message listener for Rqueue.
 * This annotation is similar to RqueueListener but uses RabbitMQ as the underlying message broker.
 *
 * @author Sonu Kumar
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RqueueRabbitListener {

  /**
   * Queue name on which this listener will listen for messages.
   *
   * @return queue name
   */
  String value();

  /**
   * Number of retries for this listener, if not specified then default retry count will be used.
   *
   * @return number of retries
   */
  String numRetries() default "3";

  /**
   * Dead letter queue name, if message processing fails after all retries then message will be
   * sent to this queue.
   *
   * @return dead letter queue name
   */
  String deadLetterQueue() default "";

  /**
   * Concurrency for this listener, it can be a range like 5-10, 5-10,20-30 or a fixed number like
   * 5.
   *
   * @return concurrency string
   */
  String concurrency() default "1-5";

  /**
   * Priority for this listener, higher priority listeners will be processed first.
   *
   * @return priority string
   */
  String priority() default "";

  /**
   * Priority group for this listener, listeners in the same group will be processed together.
   *
   * @return priority group
   */
  String priorityGroup() default "";

  /**
   * Whether this listener should be enabled or not.
   *
   * @return true if enabled, false otherwise
   */
  boolean enabled() default true;

  /**
   * Whether this listener should process messages in batch or not.
   *
   * @return true if batch processing is enabled, false otherwise
   */
  boolean batchProcessing() default false;

  /**
   * Batch size for batch processing.
   *
   * @return batch size
   */
  int batchSize() default 10;

  /**
   * Whether this listener should acknowledge messages automatically or not.
   *
   * @return true if auto-acknowledge is enabled, false otherwise
   */
  boolean autoAcknowledge() default true;
}