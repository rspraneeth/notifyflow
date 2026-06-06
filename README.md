# NotifyFlow

An enterprise-grade, event-driven customer notification pipeline built with Spring Boot and Apache Kafka. NotifyFlow ingests structured customer communication payloads from upstream systems, validates and enriches them with customer profile data, applies conditional routing logic, and delivers them to the correct notification channel.

Built to demonstrate real-world backend engineering patterns: event-driven architecture, payload transformation pipelines, Redis caching, PostgreSQL audit logging, inter-service HTTP communication, and cloud deployment.

---

## Live Deployment

NotifyFlow is fully deployed and running on AWS.

| Service | URL |
|---|---|
| Swagger UI (test here) | http://18.218.204.81:8081/swagger-ui.html |
| payload-producer API | http://18.218.204.81:8081/api/events/publish |
| core-processor health | http://18.218.204.81:8082/actuator/health |
| customer-service API | http://18.218.204.81:8084/api/customers/CUST-123 |
| delivery-service health | http://18.218.204.81:8083/actuator/health |

> **Note:** The live deployment may be inactive. Aiven Kafka free tier powers off during inactivity and AWS resources expire December 5, 2026. If you want to see the pipeline running live, reach out to me directly and I can bring it back up. To run it yourself, follow the local development setup below — it runs fully on Docker with no cloud accounts needed.

---

## Service Lifetime Notice

| Service | Provider | Expires |
|---|---|---|
| Kafka broker | Aiven free tier | Powers off on inactivity — reactivate at aiven.io |
| Redis cache | Redis Cloud free tier | Permanent free tier — no expiry |
| PostgreSQL | AWS RDS | December 5, 2026 ($100 credits) |
| EC2 (4 services) | AWS EC2 | December 5, 2026 ($100 credits) |
| Email sending | AWS SES sandbox | No expiry |

All AWS resources (EC2, RDS) will be decommissioned when the $100 promotional credit expires on **December 5, 2026**. After that date the live URLs above will be inactive.

---

## Architecture

```
Swagger UI / REST Client
        |
        v
+---------------------+
|   payload-producer  |  :8081  Accepts HTTP payloads, publishes to Kafka
+----------+----------+
           | Kafka: customer-events
           v
+---------------------+         +---------------------+
|   core-processor    | :8082   |   customer-service  | :8084
|                     |-------->|                     |
|  validate           |  REST   |  Returns customer   |
|  enrich (Redis)     |         |  profile data       |
|  route              |         +---------------------+
|  audit log (PG)     |
+----------+----------+
           | Kafka: email-events / sms-events / push-events
           v
+---------------------+
|  delivery-service   | :8083  Renders Handlebars templates, sends email
+---------------------+
           |
           v
      Mailhog (local) / AWS SES (production)
```

### Data flow

1. Client sends POST to `/api/events/publish` on payload-producer
2. payload-producer publishes to Kafka `customer-events` topic
3. core-processor consumes the message and runs a 6-step pipeline:
   - Idempotency check — skip if already processed
   - Audit log created in PostgreSQL with status RECEIVED
   - Validation — check required fields
   - Enrichment — fetch customer profile via RestTemplate, cache in Redis (10-min TTL)
   - Routing — switch on `notificationType`, publish to downstream Kafka topic
   - Audit log updated to ROUTED or FAILED
4. delivery-service consumes, renders Handlebars template, sends email

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 / 4.0.5 |
| Messaging | Apache Kafka |
| Database | PostgreSQL + Spring Data JPA + HikariCP |
| Caching | Redis + Spring Cache + @Cacheable |
| Email | JavaMailSender + Handlebars.java |
| Service communication | RestTemplate |
| Testing | JUnit 5 + Mockito |
| API Docs | Swagger / OpenAPI 3 |
| Containerization | Docker + Docker Compose (local) |
| CI/CD | GitHub Actions |
| Cloud | AWS EC2, RDS, SES + Aiven Kafka + Redis Cloud |

---

## Services

### payload-producer (port 8081)
REST API entry point. Accepts JSON payloads and publishes to `customer-events` Kafka topic. Includes Swagger UI.

### core-processor (port 8082)
Main processing engine. Runs the full validation-enrichment-routing pipeline. Persists audit logs to PostgreSQL. Caches customer profiles in Redis.

**Routing logic:**
- `PROMOTIONAL` / `TRANSACTIONAL` -> email-events
- `ALERT` -> push-events
- `SMS` -> sms-events
- Unknown -> customer-events-dlq (dead letter queue)

### customer-service (port 8084)
Returns customer profile data (name, email, phone) by customer ID. In production this would be a CRM integration.

### delivery-service (port 8083)
Renders Handlebars email templates with customer data and sends via JavaMailSender. Locally uses Mailhog. In production uses AWS SES.

---

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| customer-events | payload-producer | core-processor |
| email-events | core-processor | delivery-service |
| sms-events | core-processor | delivery-service |
| push-events | core-processor | delivery-service |
| customer-events-dlq | core-processor | — |

---

## Audit Log Lifecycle

```
RECEIVED -> VALIDATED -> ENRICHED -> ROUTED
                                    |
                                  FAILED (on any error)
```

---

## Local Development Setup

### Prerequisites

- Java 21 (Temurin recommended)
- Docker Desktop
- Git
- Maven

### 1. Clone the repository

```bash
git clone https://github.com/rspraneeth/notifyflow.git
cd notifyflow
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Starts: Kafka + Zookeeper, PostgreSQL, Redis, Mailhog. Verify with `docker ps`.

### 3. Start services (in this order)

```bash
cd customer-service && mvn spring-boot:run     # port 8084
cd core-processor && mvn spring-boot:run       # port 8082
cd delivery-service && mvn spring-boot:run     # port 8083
cd payload-producer && mvn spring-boot:run     # port 8081
```

### 4. Send a test event

Open Swagger UI at `http://localhost:8081/swagger-ui.html`

POST to `/api/events/publish`:

```json
{
  "eventId": "EVT-001",
  "customerId": "CUST-123",
  "notificationType": "PROMOTIONAL",
  "subject": "Special offer",
  "body": "Hello {{customerName}}, check out this offer!",
  "channel": "EMAIL"
}
```

### 5. Verify email

Open Mailhog at `http://localhost:8025` — rendered email with `{{customerName}}` replaced by `John Doe`.

### 6. Verify audit log

```
Host: localhost:5432
Database: notifyflow
Username: notifyflow_user | Password: notifyflow_pass
```

```sql
SELECT event_id, status, routed_to_channel, created_at FROM audit_log;
```

---

## Local Development Challenges

Problems encountered during local development and how they were solved.

---

### 1. Kafka Deserialization: ClassNotFoundException across services

**Problem:** Spring's JsonSerializer embedded the full producer class name `com.notifyflow.payloadproducer.model.EventPayload` in every Kafka message header. core-processor tried to load that class which doesn't exist in its classpath — resulting in an infinite deserialization error loop.

**Fix:**
```java
// Producer KafkaConfig — disable type headers
config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

// Consumer KafkaConfig — ignore headers, use explicit target type
config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CustomerEvent.class.getName());
```

**Lesson:** In microservices, never rely on class name headers for Kafka deserialization. Use plain JSON and map by field names — the JSON field contract is what matters, not the class name.

---

### 2. Redis Serialization: Cannot serialize error

**Problem:** After enabling `@Cacheable` on the customer profile fetch, Spring threw `Cannot serialize` when trying to store the result in Redis. The default Redis serializer uses Java serialization which requires `Serializable`.

**Fix:** Configure `RedisCacheManager` explicitly with Jackson JSON serializer:
```java
RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(10))
    .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
    .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
```

**Lesson:** Always configure Redis serializer explicitly. Default Java serialization breaks on plain POJOs and makes Redis keys unreadable. JSON serialization is the correct approach for Spring Boot applications.

---

### 3. Maven annotation processor version error on GitHub Actions

**Problem:** CI pipeline failed with `version can neither be null, empty nor blank` when resolving the Lombok annotation processor. Worked perfectly in IntelliJ.

**Root cause:** IntelliJ resolves the Lombok version automatically from the dependency block. Maven CLI on Linux requires the version explicitly declared in `<annotationProcessorPaths>`.

**Fix:** Remove the entire `maven-compiler-plugin` block. Spring Boot's parent pom handles Lombok annotation processing automatically when Lombok is declared as a dependency — no explicit compiler plugin config needed.

**Lesson:** IntelliJ's build system is more forgiving than Maven CLI. Always run `mvn clean package` from the terminal before pushing, not just the IDE run button.

---

### 4. CoreProcessorApplicationTests failing in CI

**Problem:** The auto-generated `@SpringBootTest` context test failed in CI because GitHub Actions has no PostgreSQL available. Passed locally because the developer had Docker running.

**Fix:** Remove `@SpringBootTest` from all auto-generated application test classes. The real unit tests (ValidationServiceTest, RoutingServiceTest, AuditLogServiceTest) use plain Mockito with no Spring context — they run anywhere.

**Lesson:** `@SpringBootTest` loads the full application context which requires all infrastructure (database, Redis, Kafka) to be available. Unit tests with Mockito are environment-independent and far faster.

---

## AWS Production Deployment

### Infrastructure

| Component | Service | Details |
|---|---|---|
| Compute | AWS EC2 t3.micro | Amazon Linux 2023, all 4 services as Java processes |
| Database | AWS RDS PostgreSQL 18.3 | db.t4g.micro, port 5432 open to EC2 |
| Cache | Redis Cloud | Free tier 30MB, no SSL required |
| Kafka | Aiven Kafka 4.1.2 | Free tier, SASL_SSL authentication |
| Email | AWS SES | Sandbox mode, verified sender only |

### Configuration approach

All credentials are environment variables set in `~/.bashrc` on EC2. Never stored in code or config files. `application-aws.properties` files use `${VARIABLE_NAME}` placeholders:

```properties
spring.datasource.url=jdbc:postgresql://${RDS_HOST}:5432/postgres
spring.data.redis.host=${REDIS_HOST}
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
spring.kafka.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}
```

### Running services on EC2

Services run directly as Java processes (not Docker) due to memory constraints on t3.micro:

```bash
java -Xms128m -Xmx450m -XX:+UseG1GC -jar core-processor.jar --spring.profiles.active=aws &
java -Xms64m  -Xmx192m -XX:+UseG1GC -jar payload-producer.jar --spring.profiles.active=aws &
java -Xms64m  -Xmx128m -XX:+UseG1GC -jar customer-service.jar --spring.profiles.active=aws &
java -Xms64m  -Xmx192m -XX:+UseG1GC -jar delivery-service.jar --spring.profiles.active=aws &
```

2GB swap file added to EC2 to handle memory bursts.

---

## AWS Production Challenges

Problems encountered during AWS deployment and how they were solved.

---

### 1. Spring profile properties ignored by custom KafkaConfig beans

**Problem:** `application-aws.properties` had correct `spring.kafka.properties.security.protocol=SASL_SSL` but the running app showed `security.protocol = PLAINTEXT`. The aws profile was active but SASL settings weren't being applied.

**Root cause:** core-processor had a custom `KafkaConfig.java` that programmatically creates `ConsumerFactory` and `ProducerFactory` beans. These beans completely override Spring Boot's Kafka auto-configuration. Properties file `spring.kafka.properties.*` settings are silently ignored.

**Fix:** Add `@Value` fields for every security property in `KafkaConfig.java` and explicitly add them to the config map:
```java
@Value("${spring.kafka.properties.security.protocol:PLAINTEXT}")
private String securityProtocol;

config.put("security.protocol", securityProtocol);
```

**Lesson:** When you define custom Kafka factory beans, ALL Kafka configuration must go through those beans explicitly. Properties file settings only work when using Spring Boot's Kafka auto-configuration — custom beans bypass it entirely.

---

### 2. Aiven Kafka SSL: Bootstrap broker disconnected loop

**Problem:** SSL handshake succeeded (verified with `openssl s_client`, `verify return:1`). Port was reachable (`nc -zv` connected). But Kafka client kept disconnecting every ~170ms in a silent loop — no error message, no stack trace.

**Root cause — two issues:**

**Issue A:** Missing `ssl.endpoint.identification.algorithm=` (empty value). Without this, the Kafka client tries to verify the server hostname against the SSL certificate CN field, which fails silently on Aiven's infrastructure.

**Issue B:** SASL JAAS config with `${KAFKA_USERNAME}` and `${KAFKA_PASSWORD}` placeholders inside a quoted string wasn't being interpolated by Spring. The literal string `${KAFKA_USERNAME}` was being passed to Kafka as the username.

**Fix for A:**
```properties
spring.kafka.properties.ssl.endpoint.identification.algorithm=
```

**Fix for B:** Pass the entire JAAS config as one pre-built environment variable:
```bash
export KAFKA_JAAS_CONFIG='org.apache.kafka.common.security.scram.ScramLoginModule required username="avnadmin" password="yourpassword";'
```
```properties
spring.kafka.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}
```

**Lesson:** When Kafka silently disconnects after SSL handshake succeeds, the issue is SASL authentication, not SSL. Always verify SSL separately with `openssl s_client` before debugging the application layer.

---

### 3. OOM crashes on EC2 t3.micro

**Problem:** Running 4 Spring Boot services in Docker containers on a 1GB t3.micro caused out-of-memory crashes. The JVM default heap (25% of RAM = ~230MB per service) was too large when multiplied by 4 services plus Docker overhead.

**Fix:**
```bash
# Add 2GB swap file
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
```

Run as Java processes directly (not Docker) with explicit heap limits:
```bash
java -Xms128m -Xmx450m -XX:+UseG1GC -jar core-processor.jar
```

**Lesson:** Docker adds 50-100MB overhead per container. On a 1GB instance with 4 Java services, that overhead is significant. Running jars directly is the practical approach for low-memory deployments. `-XX:+UseG1GC` reduces GC pause times on small heaps.

---

### 4. AWS SES: Authentication Credentials Invalid

**Problem:** SES SMTP authentication kept failing with `535 Authentication Credentials Invalid` even with credentials that appeared correct. Tried multiple credential sets — all failed.

**Root cause:** SES SMTP passwords are NOT regular AWS secret access keys. They must be derived from the secret key using a specific HMAC-SHA256 algorithm (AWS Signature Version 4 variant). Credentials created through IAM console are raw access keys — they require manual derivation. Only credentials created through **SES Console -> SMTP Settings -> Create SMTP credentials** are pre-derived and ready to use.

**Derivation algorithm (if needed):**
```python
import hmac, hashlib, base64

def sign(key, msg):
    return hmac.new(key, msg.encode('utf-8'), hashlib.sha256).digest()

signature = sign(sign(sign(sign(sign(
    ('AWS4' + secret_key).encode('utf-8'), '11111111'),
    region), 'ses'), 'aws4_request'), 'SendRawEmail')

smtp_password = base64.b64encode(b'\x04' + signature).decode('utf-8')
```

**Lesson:** Always create SES SMTP credentials through SES Console, not IAM. Test SMTP authentication directly with Python's `smtplib` before debugging the application.

---

### 5. Redis Cloud SSL: record layer failure

**Problem:** `SSL_connect failed: record layer failure` when connecting to Redis Cloud even though the port was reachable. The application config had `spring.data.redis.ssl.enabled=true`.

**Diagnosis:** Tested directly:
```bash
redis6-cli --tls -h host -p port -a password ping  # failed
redis6-cli -h host -p port -a password ping        # PONG
```

**Fix:** Redis Cloud free tier on this plan does not require SSL. Set:
```properties
spring.data.redis.ssl.enabled=false
```

**Lesson:** Don't assume managed services require SSL just because they're in the cloud. Verify empirically by testing both with and without TLS. Free tier services often have different requirements than paid tiers.

---

### 6. Aiven Kafka version incompatibility

**Problem:** core-processor uses spring-kafka 3.1.4 (kafka-clients 3.6.2) but Aiven's free tier runs Kafka 4.1.2. Initially suspected this was causing disconnections.

**Reality:** The disconnections were caused by missing SASL config (Challenge 2 above), not version incompatibility. kafka-clients 3.6.2 is compatible with Kafka broker 4.1.2 for SASL_SSL connections.

**Lesson:** Kafka clients maintain backward compatibility across broker versions for standard operations. Check authentication and SSL config before assuming version incompatibility.

---

## Testing

```bash
cd core-processor
mvn test
```

15 unit tests using JUnit 5 + Mockito. No Spring context or running infrastructure required.

- `ValidationServiceTest` — required field validation, blank/null handling (5 tests)
- `RoutingServiceTest` — all routing paths, case insensitivity, DLQ routing (7 tests)
- `AuditLogServiceTest` — received/routed/failed lifecycle, idempotency (5 tests)

---

## Project Structure

```
notifyflow/
├── payload-producer/
│   └── src/main/java/com/notifyflow/payloadproducer/
│       ├── controller/     EventController
│       ├── model/          EventPayload
│       ├── service/        EventService
│       ├── kafka/          KafkaProducerService
│       └── config/         KafkaConfig, GlobalExceptionHandler
│
├── core-processor/
│   └── src/main/java/com/notifyflow/coreprocessor/
│       ├── kafka/consumer/ CustomerEventConsumer
│       ├── kafka/producer/ RoutingProducer
│       ├── service/        ValidationService, EnrichmentService, RoutingService, AuditLogService
│       ├── cache/          CustomerProfileCache
│       ├── entity/         AuditLog
│       ├── repository/     AuditLogRepository
│       ├── exception/      PayloadValidationException, RoutingException
│       └── config/         KafkaConfig, CacheConfig, GlobalExceptionHandler
│
├── customer-service/
│   └── src/main/java/com/notifyflow/customerservice/
│       ├── controller/     CustomerController
│       ├── model/          CustomerProfile
│       └── service/        CustomerService
│
├── delivery-service/
│   └── src/main/java/com/notifyflow/deliveryservice/
│       ├── kafka/consumer/ DeliveryEventConsumer
│       ├── handler/        EmailHandler, SmsHandler, PushHandler
│       ├── template/       HandlebarsTemplateEngine
│       └── model/          EnrichedEvent
│
├── docker-compose.yml          Local infrastructure stack
└── .github/workflows/ci.yml    GitHub Actions CI pipeline
```

---

## Author

Satya Praneeth Reddi — [GitHub](https://github.com/rspraneeth) · [LinkedIn](https://linkedin.com/in/rspraneeth) · [LeetCode](https://leetcode.com/u/rspraneeth)
