# Rqueue RabbitMQ: RabbitMQ-based Task Queue for Spring and Spring Boot

<div>
   <img  align="left" src="https://raw.githubusercontent.com/sonus21/rqueue/master/rqueue-core/src/main/resources/public/rqueue/img/android-chrome-192x192.png" alt="Rqueue Logo" width="90">
   <h1 style="float:left">Rqueue RabbitMQ: RabbitMQ-based Task Queue, Scheduled Queue for Spring and Spring Boot</h1>
</div>

**Rqueue RabbitMQ** is an asynchronous task executor(worker) built for Spring and Spring Boot framework based on RabbitMQ message broker. It provides the same API as the original Rqueue but uses RabbitMQ instead of Redis as the underlying message broker.

## Features

* **Instant delivery** : Instant execute this message in the background
* **Message scheduling** : A message can be scheduled for any arbitrary period using RabbitMQ's delayed message plugin
* **Unique message** : Unique message processing for a queue based on the message id
* **Periodic message** : Process same message at certain interval
* **Priority tasks** : task having some special priority like high, low, medium
* **Message delivery** : It's guaranteed that a message is consumed **at least once**
* **Message retry** : Message would be retried automatically on application crash/failure/restart
* **Automatic message serialization and deserialization**
* **Message Multicasting** : Call multiple message listeners on every message
* **Batch Message Polling** : Fetch multiple messages from RabbitMQ at once
* **Metrics** : In flight messages, waiting for consumption and scheduled messages
* **Competing Consumers** : multiple messages can be consumed in parallel by different workers/listeners
* **Concurrency** : Concurrency of any listener can be configured
* **Queue Priority** : Group level queue priority(weighted and strict)
* **Long execution job** : Long running jobs can check in periodically
* **Execution Backoff** : Exponential and fixed back off (default fixed back off)
* **Middleware** : Add one or more middleware, middlewares are called before listener method
* **Callbacks** : Callbacks for dead letter queue, discard etc
* **Events** : Bootstrap event and Task execution event
* **RabbitMQ connection** : A different RabbitMQ setup can be used for Rqueue
* **RabbitMQ cluster** : RabbitMQ cluster can be used with Rqueue
* **Web Dashboard** : Web dashboard to manage a queue and queue insights including latency

### Requirements

* Spring 5+, 6+
* Java 1.8+, 17
* Spring Boot 2+, 3+
* RabbitMQ 3.8+
* RabbitMQ Delayed Message Plugin (for delayed messages)

## Getting Started

### Dependency

#### Spring Boot

**NOTE:**

* For spring boot 2.x use Rqueue RabbitMQ 2.x
* For spring boot 3.x use Rqueue RabbitMQ 3.x

* Add dependency
  * Gradle
    ```groovy
        implementation 'com.github.sonus21:rqueue-rabbitmq-spring-boot-starter:3.4.0-RELEASE'
    ```
  * Maven
    ```xml
     <dependency>
        <groupId>com.github.sonus21</groupId>
        <artifactId>rqueue-rabbitmq-spring-boot-starter</artifactId>
        <version>3.4.0-RELEASE</version>
    </dependency>
    ```

  No additional configurations are required, only dependency is required.

---

### Message publishing/Task submission

All messages need to be sent using `RqueueMessageEnqueuer` bean's `enqueueXXX`, `enqueueInXXX` and `enqueueAtXXX` methods. It has handful number of `enqueue`, `enqueueIn`, `enqueueAt` methods, we can use any one of them based on the use case.

```java
public class MessageService {

  @AutoWired
  private RqueueMessageEnqueuer rqueueMessageEnqueuer;

  public void doSomething() {
    rqueueMessageEnqueuer.enqueue("simple-queue", "Rqueue RabbitMQ is configured");
  }

  public void createJOB(Job job) {
    rqueueMessageEnqueuer.enqueue("job-queue", job);
  }

  // send notification in 30 seconds
  public void sendNotification(Notification notification) {
    rqueueMessageEnqueuer.enqueueIn("notification-queue", notification, 30 * 1000L);
  }

  // enqueue At example
  public void createInvoice(Invoice invoice, Instant instant) {
    rqueueMessageEnqueuer.enqueueAt("invoice-queue", invoice, instant);
  }

  // enqueue with priority, when sub queues are used as explained in the queue priority section.
  enum SmsPriority {
    CRITICAL("critical"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");
    private String value;
  }

  public void sendSms(Sms sms, SmsPriority priority) {
    rqueueMessageEnqueuer.enqueueWithPriority("sms-queue", priority.value(), sms);
  }

  // Index chat every 1 minute
  public void sendPeriodicEmail(Email email) {
    rqueueMessageEnqueuer.enqueuePeriodic("chat-indexer", chatIndexer, 60_000);
  }

}
```

---

### Worker/Consumer/Task Executor/Listener

Any method that's part of spring bean, can be marked as worker/message listener using `RqueueRabbitListener` annotation

```java

@Component
@Slf4j
public class MessageListener {

  @RqueueRabbitListener(value = "simple-queue")
  public void simpleMessage(String message) {
    log.info("simple-queue: {}", message);
  }

  @RqueueRabbitListener(value = "job-queue", numRetries = "3",
      deadLetterQueue = "failed-job-queue", concurrency = "5-10")
  public void onMessage(Job job) {
    log.info("Job alert: {}", job);
  }

  @RqueueRabbitListener(value = "push-notification-queue", numRetries = "3",
      deadLetterQueue = "failed-notification-queue")
  public void onMessage(Notification notification) {
    log.info("Push notification: {}", notification);
  }

  @RqueueRabbitListener(value = "sms", priority = "critical=10,high=8,medium=4,low=1")
  public void onMessage(Sms sms) {
    log.info("Sms : {}", sms);
  }

  @RqueueRabbitListener(value = "chat-indexing", priority = "20", priorityGroup = "chat")
  public void onMessage(ChatIndexing chatIndexing) {
    log.info("ChatIndexing message: {}", chatIndexing);
  }

  @RqueueRabbitListener(value = "chat-indexing-daily", priority = "10", priorityGroup = "chat")
  public void onMessage(ChatIndexing chatIndexing) {
    log.info("ChatIndexing message: {}", chatIndexing);
  }

  // checkin job example
  @RqueueRabbitListener(value = "chat-indexing-weekly", priority = "5", priorityGroup = "chat")
  public void onMessage(ChatIndexing chatIndexing,
      @Header(RqueueMessageHeaders.JOB) com.github.sonus21.rqueue.core.Job job) {
    log.info("ChatIndexing message: {}", chatIndexing);
    job.checkIn("Chat indexing...");
  }
}
```

---

## Configuration

### RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    connection-timeout: 15000
    listener:
      simple:
        acknowledge-mode: auto
        concurrency: 1
        max-concurrency: 10
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          max-interval: 10000
          multiplier: 2

rqueue:
  enabled: true
  key-prefix: "__rq::"
  job-enabled: true
  cluster-mode: false
  message-durability: 10080
  message-durability-in-terminal-state: 1800
  retry-per-poll: 1
  system-mode: BOTH
  reactive-enabled: false
  latest-version-check-enabled: true
```

### RabbitMQ Delayed Message Plugin

For delayed message functionality, you need to install the RabbitMQ Delayed Message Plugin:

```bash
# Download the plugin
wget https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.12.0/rabbitmq_delayed_message_exchange-3.12.0.ez

# Copy to RabbitMQ plugins directory
sudo cp rabbitmq_delayed_message_exchange-3.12.0.ez /usr/lib/rabbitmq/lib/rabbitmq_server-3.12.0/plugins/

# Enable the plugin
sudo rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

## Quick Start with Docker

1. **Start RabbitMQ with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

2. **Run the example application:**
   ```bash
   ./gradlew :rqueue-rabbitmq-example:bootRun
   ```

3. **Access RabbitMQ Management UI:**
   - URL: http://localhost:15672
   - Username: guest
   - Password: guest

## Differences from Redis-based Rqueue

1. **Message Broker**: Uses RabbitMQ instead of Redis
2. **Delayed Messages**: Uses RabbitMQ Delayed Message Plugin instead of Redis sorted sets
3. **Priority Queues**: Uses RabbitMQ priority queues instead of Redis priority queues
4. **Message Persistence**: Uses RabbitMQ's built-in persistence instead of Redis persistence
5. **Clustering**: Uses RabbitMQ clustering instead of Redis clustering
6. **Monitoring**: Uses RabbitMQ Management UI instead of Redis monitoring

## Status

Rqueue RabbitMQ is a new implementation based on the original Rqueue but using RabbitMQ as the message broker. It provides the same API and functionality as the original Rqueue but with RabbitMQ's reliability and features.

## Support

* Please report bug, question, feature(s) to [issue](https://github.com/sonus21/rqueue/issues/new/choose) tracker.
* Ask question on StackOverflow using [#rqueue](https://stackoverflow.com/tags/rqueue) tag

## Contribution

You are most welcome for any pull requests for any feature/bug/enhancement. You would need Java8 and gradle to start with.

**Please format your code with Google Java formatter.**

## License

© [Sonu Kumar](mailto:sonunitw12@gmail.com) 2019-Instant.now

The Rqueue RabbitMQ is released under version 2.0 of the Apache License.