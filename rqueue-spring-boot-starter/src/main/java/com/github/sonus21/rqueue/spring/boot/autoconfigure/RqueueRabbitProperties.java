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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Rqueue with RabbitMQ.
 *
 * @author Sonu Kumar
 */
@Data
@ConfigurationProperties(prefix = "rqueue")
public class RqueueRabbitProperties {

  /**
   * Whether Rqueue is enabled.
   */
  private boolean enabled = true;

  /**
   * Key prefix for Rqueue operations.
   */
  private String keyPrefix = "__rq::";

  /**
   * Whether job functionality is enabled.
   */
  private boolean jobEnabled = true;

  /**
   * Job durability in terminal state in seconds.
   */
  private long jobDurabilityInTerminalState = 1800;

  /**
   * Job key prefix.
   */
  private String jobKeyPrefix = "job::";

  /**
   * Jobs collection name prefix.
   */
  private String jobsCollectionNamePrefix = "jobs::";

  /**
   * Whether cluster mode is enabled.
   */
  private boolean clusterMode = true;

  /**
   * Simple queue prefix.
   */
  private String simpleQueuePrefix = "";

  /**
   * Scheduled queue prefix.
   */
  private String scheduledQueuePrefix = "";

  /**
   * Completed queue prefix.
   */
  private String completedQueuePrefix = "";

  /**
   * Scheduled queue channel prefix.
   */
  private String scheduledQueueChannelPrefix = "";

  /**
   * Processing queue name prefix.
   */
  private String processingQueuePrefix = "";

  /**
   * Processing queue channel prefix.
   */
  private String processingQueueChannelPrefix = "";

  /**
   * Queues key suffix.
   */
  private String queuesKeySuffix = "queues";

  /**
   * Lock key prefix.
   */
  private String lockKeyPrefix = "lock::";

  /**
   * Queue statistics key prefix.
   */
  private String queueStatKeyPrefix = "q-stat::";

  /**
   * Queue configuration key prefix.
   */
  private String queueConfigKeyPrefix = "q-config::";

  /**
   * Retry per poll count.
   */
  private int retryPerPoll = 1;

  /**
   * Message durability in minutes.
   */
  private long messageDurability = 10080;

  /**
   * Message durability in terminal state in seconds.
   */
  private long messageDurabilityInTerminalState = 1800;

  /**
   * System mode (PRODUCER, CONSUMER, BOTH).
   */
  private String systemMode = "BOTH";

  /**
   * Internal communication channel name prefix.
   */
  private String internalChannelNamePrefix = "i-channel";

  /**
   * Completed job cleanup interval in milliseconds.
   */
  private long completedJobCleanupInterval = 30000;

  /**
   * Whether reactive mode is enabled.
   */
  private boolean reactiveEnabled = false;

  /**
   * Whether latest version check is enabled.
   */
  private boolean latestVersionCheckEnabled = true;
}