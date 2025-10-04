<div>
   <img  align="left" src="https://raw.githubusercontent.com/sonus21/rqueue/master/rqueue-core/src/main/resources/public/rqueue/img/android-chrome-192x192.png" alt="Rqueue Logo" width="90">
   <h1 style="float:left">Rqueue RabbitMQ: RabbitMQ-based Task Queue, Scheduled Queue for Spring and Spring Boot</h1>
</div>

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/sonus21/rqueue-rabbitmq)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/rabbitmq-3.8%2B-orange)](https://www.rabbitmq.com/)

**Rqueue RabbitMQ** is a high-performance, asynchronous task executor built for Spring and Spring Boot applications, using **RabbitMQ** as the message broker. It provides the same intuitive API as the original Rqueue but leverages RabbitMQ's enterprise-grade reliability, clustering, and monitoring capabilities.

## 🚀 **Why Rqueue RabbitMQ?**

- **🔄 API Compatibility**: Same API as original Rqueue for seamless migration
- **🏢 Enterprise Ready**: RabbitMQ's proven reliability and clustering support
- **📊 Rich Monitoring**: Built-in RabbitMQ Management UI for comprehensive monitoring
- **⚡ High Performance**: Optimized for high-throughput message processing
- **🔒 Message Durability**: Guaranteed message persistence and delivery
- **🌐 AMQP Standard**: Industry-standard message queuing protocol

## ✨ **Key Features**

### **📨 Message Processing**
- **⚡ Instant Delivery**: Execute messages immediately in the background
- **⏰ Message Scheduling**: Schedule messages for any future time using RabbitMQ's delayed message plugin
- **🔒 Unique Message Processing**: Prevent duplicate message processing based on message ID
- **🔄 Periodic Messages**: Process the same message at specified intervals
- **🎯 Priority Tasks**: Support for high, medium, and low priority task processing
- **✅ Guaranteed Delivery**: Messages are consumed **at least once**
- **🔄 Automatic Retry**: Messages are retried automatically on failures with configurable retry policies

### **🏗️ Architecture & Performance**
- **📦 Automatic Serialization**: JSON-based message serialization/deserialization
- **👥 Competing Consumers**: Multiple workers can process messages in parallel
- **⚙️ Configurable Concurrency**: Set concurrency levels per listener
- **📊 Priority Queues**: Group-level queue priority with weighted and strict modes
- **🔍 Long-Running Jobs**: Support for jobs that check in periodically
- **📈 Execution Backoff**: Exponential and fixed backoff strategies

### **🔧 RabbitMQ Integration**
- **🏢 RabbitMQ Clustering**: Full support for RabbitMQ cluster deployments
- **🔌 Connection Management**: Flexible RabbitMQ connection configuration
- **💀 Dead Letter Queues**: Automatic handling of failed messages
- **📊 Rich Monitoring**: Leverage RabbitMQ Management UI for comprehensive monitoring
- **🌐 AMQP Compliance**: Industry-standard message queuing protocol

## 📋 **Requirements**

| Component | Version |
|-----------|---------|
| **Java** | 17+ |
| **Spring Framework** | 6.x |
| **Spring Boot** | 3.x |
| **RabbitMQ** | 3.8+ |
| **RabbitMQ Delayed Message Plugin** | 3.12.0+ (for delayed messages) |

## 🚀 **Quick Start**

### **1. Add Dependency**

#### **Gradle**
```groovy
dependencies {
    implementation 'com.github.sonus21:rqueue-rabbitmq-spring-boot-starter:3.4.0-RELEASE'
}
```

#### **Maven**
```xml
<dependency>
    <groupId>com.github.sonus21</groupId>
    <artifactId>rqueue-rabbitmq-spring-boot-starter</artifactId>
    <version>3.4.0-RELEASE</version>
</dependency>
```

### **2. Configure RabbitMQ**

Add RabbitMQ configuration to your `application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

### **3. Start RabbitMQ with Docker**

```bash
docker-compose up -d
```

**That's it!** No additional configuration required. Rqueue RabbitMQ will auto-configure itself.

## 🏗️ **Architecture**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring App    │───▶│   RabbitMQ      │───▶│  Worker/Listener│
│                 │    │   Message Broker│    │                 │
│ @RqueueRabbit   │    │   - Queues      │    │ @RqueueRabbit   │
│ Listener        │    │   - Exchanges   │    │ Listener        │
│                 │    │   - Routing     │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Message Publisher│    │  Dead Letter    │    │   Monitoring    │
│                 │    │     Queues      │    │   Dashboard     │
│ RqueueMessage   │    │                 │    │                 │
│ Enqueuer        │    │ Failed Messages │    │ RabbitMQ Mgmt   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 💼 **Use Cases**

- **📧 Email Notifications**: Send emails asynchronously in background
- **🖼️ Image Processing**: Resize, compress images without blocking requests
- **🔄 Data Synchronization**: Sync data between microservices
- **📊 Report Generation**: Generate complex reports in background
- **⏰ Scheduled Tasks**: Run periodic maintenance and cleanup tasks
- **🔗 Microservice Communication**: Decouple services with reliable messaging
- **📱 Push Notifications**: Send mobile push notifications at scale
- **💰 Payment Processing**: Handle payment transactions asynchronously

## 📝 **Usage Examples**

### **📤 Message Publishing**

All messages are sent using the `RqueueMessageEnqueuer` bean. Use the appropriate method based on your use case:

```java
public class MessageService {

  @Autowired
  private RqueueMessageEnqueuer rqueueMessageEnqueuer;

  public void doSomething() {
    rqueueMessageEnqueuer.enqueue("simple-queue", "Rqueue RabbitMQ is configured");
  }

  // Send job for immediate processing
  public void createJob(Job job) {
    rqueueMessageEnqueuer.enqueue("job-queue", job);
  }

  // Send notification with 30-second delay
  public void sendDelayedNotification(Notification notification) {
    rqueueMessageEnqueuer.enqueueIn("notification-queue", notification, 30 * 1000L);
  }

  // Send message at specific time
  public void createInvoice(Invoice invoice, Instant instant) {
    rqueueMessageEnqueuer.enqueueAt("invoice-queue", invoice, instant);
  }

  // Send message with priority
  public void sendSms(Sms sms, String priority) {
    rqueueMessageEnqueuer.enqueueWithPriority("sms-queue", priority, sms);
  }

  // Send periodic message (every minute)
  public void sendPeriodicEmail(Email email) {
    rqueueMessageEnqueuer.enqueuePeriodic("email-queue", email, 60_000);
  }

  // Send message with retry configuration
  public void sendWithRetry(String queueName, Object message, int retryCount) {
    rqueueMessageEnqueuer.enqueueWithRetry(queueName, message, retryCount);
  }

}
```

### **📥 Message Processing**

Any method in a Spring bean can be marked as a message listener using the `@RqueueRabbitListener` annotation:

```java

@Component
@Slf4j
public class MessageListener {

  @RqueueRabbitListener(value = "simple-queue")
  public void simpleMessage(String message) {
    log.info("simple-queue: {}", message);
  }

  // Job processing with retry and dead letter queue
  @RqueueRabbitListener(value = "job-queue", numRetries = "3",
      deadLetterQueue = "failed-job-queue", concurrency = "5-10")
  public void processJob(Job job) {
    log.info("Processing job: {}", job);
    // Your job processing logic here
  }

  // Notification processing
  @RqueueRabbitListener(value = "notification-queue", numRetries = "3",
      deadLetterQueue = "failed-notification-queue")
  public void processNotification(Notification notification) {
    log.info("Processing notification: {}", notification);
    // Your notification logic here
  }

  // Priority-based SMS processing
  @RqueueRabbitListener(value = "sms-queue", priority = "critical=10,high=8,medium=4,low=1")
  public void processSms(Sms sms) {
    log.info("Processing SMS: {}", sms);
    // Your SMS processing logic here
  }

  // High-priority chat indexing
  @RqueueRabbitListener(value = "chat-indexing-queue", priority = "20", priorityGroup = "chat")
  public void processChatIndexing(ChatIndexing chatIndexing) {
    log.info("Processing chat indexing: {}", chatIndexing);
    // Your indexing logic here
  }

  // Batch processing example
  @RqueueRabbitListener(value = "batch-queue", batchProcessing = true, batchSize = 10)
  public void processBatch(List<Object> messages) {
    log.info("Processing batch of {} messages", messages.size());
    // Your batch processing logic here
  }
}
```

## ⚙️ **Configuration**

### **RabbitMQ Configuration**

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

# Rqueue RabbitMQ specific configuration
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

### **🐰 RabbitMQ Delayed Message Plugin**

For delayed message functionality, install the RabbitMQ Delayed Message Plugin:

#### **Using Docker (Recommended)**
The provided `docker-compose.yml` automatically enables the plugin:

```bash
docker-compose up -d
```

#### **Manual Installation**
```bash
# Download the plugin
wget https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.12.0/rabbitmq_delayed_message_exchange-3.12.0.ez

# Copy to RabbitMQ plugins directory
sudo cp rabbitmq_delayed_message_exchange-3.12.0.ez /usr/lib/rabbitmq/lib/rabbitmq_server-3.12.0/plugins/

# Enable the plugin
sudo rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

## 🐳 **Docker Quick Start**

1. **Start RabbitMQ with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

2. **Run the example application:**
   ```bash
   ./gradlew :rqueue-rabbitmq-example:bootRun
   ```

3. **Access RabbitMQ Management UI:**
   - **URL**: http://localhost:15672
   - **Username**: guest
   - **Password**: guest

4. **Monitor your queues:**
   - Navigate to the **Queues** tab in RabbitMQ Management UI
   - View message rates, consumer counts, and queue statistics
   - Monitor failed messages in dead letter queues

## 🔄 **Migration from Redis-based Rqueue**

| Feature | Original Rqueue (Redis) | Rqueue RabbitMQ |
|---------|------------------------|-----------------|
| **Message Broker** | Redis | RabbitMQ |
| **Delayed Messages** | Redis Sorted Sets | RabbitMQ Delayed Plugin |
| **Priority Queues** | Redis Priority | RabbitMQ Priority |
| **Clustering** | Redis Cluster | RabbitMQ Cluster |
| **Monitoring** | Redis Commands | RabbitMQ Management UI |
| **Persistence** | Redis Persistence | RabbitMQ Persistence |
| **Protocol** | Redis Protocol | AMQP Standard |

### **Migration Benefits:**
- **🏢 Enterprise Features**: Better clustering, monitoring, and management
- **📊 Rich Monitoring**: Built-in web UI for queue management
- **🔒 Message Durability**: Guaranteed message persistence
- **🌐 Standard Protocol**: AMQP compliance for better integration
- **⚡ Performance**: Optimized for high-throughput scenarios

## 📊 **Project Status**

Rqueue RabbitMQ is a **production-ready** implementation that provides the same API as the original Rqueue but leverages RabbitMQ's enterprise-grade features. The project is actively maintained and ready for use in production environments.

## 🆘 **Support & Community**

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/sonus21/rqueue/issues/new/choose)
- **💬 Questions**: [StackOverflow](https://stackoverflow.com/tags/rqueue) with `#rqueue` tag
- **📖 Documentation**: This README and inline code documentation
- **💡 Feature Requests**: [GitHub Issues](https://github.com/sonus21/rqueue/issues/new/choose)

## 🤝 **Contributing**

We welcome contributions! To get started:

1. **Fork** the repository
2. **Create** a feature branch
3. **Make** your changes
4. **Format** code with Google Java formatter
5. **Submit** a pull request

### **Requirements:**
- Java 17+
- Gradle 7+
- RabbitMQ knowledge helpful but not required

## 📄 **License**

© [Sonu Kumar](mailto:sonunitw12@gmail.com) 2019-2024

Licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

<div align="center">
  <p><strong>Built with ❤️ for the Spring and RabbitMQ community</strong></p>
  <p>
    <a href="https://github.com/sonus21/rqueue-rabbitmq">⭐ Star us on GitHub</a> |
    <a href="https://github.com/sonus21/rqueue-rabbitmq/issues">🐛 Report Issues</a> |
    <a href="https://stackoverflow.com/tags/rqueue">💬 Ask Questions</a>
  </p>
</div>