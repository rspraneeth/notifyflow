# NotifyFlow

An enterprise-grade, event-driven customer notification pipeline built with Spring Boot and Apache Kafka. NotifyFlow ingests structured customer communication payloads from upstream systems, validates and enriches them with customer profile data, applies conditional routing logic, and delivers them to the correct notification channel.

This project mirrors real-world production systems used at large enterprises for customer communications — built to demonstrate backend engineering patterns including event-driven architecture, payload transformation pipelines, Redis caching, PostgreSQL audit logging, and microservices communication.

---

## Architecture

NotifyFlow consists of four independent Spring Boot microservices that communicate via Kafka topics.

```
Swagger UI / REST Client
        │
        ▼
┌─────────────────────┐
│   payload-producer  │  :8081  Accepts HTTP payloads, publishes to Kafka
└─────────┬───────────┘
          │ Kafka: customer-events
          ▼
┌─────────────────────┐         ┌─────────────────────┐
│   core-processor    │ :8082   │   customer-service  │ :8084
│                     │────────▶│                     │
│  validate           │  REST   │  Returns customer   │
│  enrich (Redis)     │         │  profile data       │
│  route              │         └─────────────────────┘
│  audit log (PG)     │
└─────────┬───────────┘
          │ Kafka: email-events / sms-events / push-events
          ▼
┌─────────────────────┐
│  delivery-service   │ :8083  Renders Handlebars templates, sends email
└─────────────────────┘
          │
          ▼
      Mailhog (local) / AWS SES (production)
```

### Data flow

1. Client sends POST to `/api/events/publish` on payload-producer
2. payload-producer validates and publishes to Kafka `customer-events` topic
3. core-processor consumes the message and runs a 6-step pipeline:
   - Idempotency check — skip if already processed
   - Log received — create audit log entry in PostgreSQL
   - Validate — check required fields
   - Enrich — fetch customer profile via RestTemplate, cache in Redis
   - Route — apply switch logic on `notificationType`, publish to downstream topic
   - Log routed — update audit log with routing decision
4. delivery-service consumes from the routed topic, renders the Handlebars template, and sends the email via JavaMailSender to Mailhog

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 / 4.0.5 |
| Messaging | Apache Kafka 3.6 |
| Database | PostgreSQL 15 + Spring Data JPA + HikariCP |
| Caching | Redis 7 + Spring Cache + @Cacheable |
| Email | JavaMailSender + Handlebars.java templates |
| Service communication | RestTemplate |
| Testing | JUnit 5 + Mockito |
| API Docs | Swagger / OpenAPI 3 |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Services

### payload-producer (port 8081)
Simulates upstream systems sending customer communication payloads. Exposes a REST API that accepts JSON payloads and publishes them to the `customer-events` Kafka topic. Includes Swagger UI for interactive testing.

### core-processor (port 8082)
The main processing engine. Consumes from `customer-events`, runs the full validation-enrichment-routing pipeline, persists audit logs to PostgreSQL, and caches customer profiles in Redis. Applies conditional routing based on `notificationType` to determine downstream channel.

**Routing logic:**
- `PROMOTIONAL` → email-events topic
- `TRANSACTIONAL` → email-events topic
- `ALERT` → push-events topic
- `SMS` → sms-events topic
- Unknown → dead letter queue (customer-events-dlq)

### customer-service (port 8084)
Lightweight service returning customer profile data (name, email, phone) by customer ID. In production this would connect to a CRM or customer database. Results are cached in Redis by core-processor with a 10-minute TTL.

### delivery-service (port 8083)
Consumes from routed Kafka topics, renders email content using Handlebars.java templates with customer data substitution, and sends via JavaMailSender. Locally emails are caught by Mailhog and viewable at `http://localhost:8025`. SMS and push channels log to output simulating handoff to third-party vendors.

---

## Prerequisites

- Java 21 (Temurin recommended)
- Docker Desktop
- Git

---

## Running Locally

### 1. Clone the repository

```bash
git clone https://github.com/rspraneeth/notifyflow.git
cd notifyflow
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

This starts Kafka, Zookeeper, PostgreSQL, Redis, and Mailhog. Verify all containers are running:

```bash
docker ps
```

### 3. Start services

Open four terminal windows or run each as a background process. Start in this order:

```bash
# Terminal 1
cd customer-service && mvn spring-boot:run

# Terminal 2
cd core-processor && mvn spring-boot:run

# Terminal 3
cd delivery-service && mvn spring-boot:run

# Terminal 4
cd payload-producer && mvn spring-boot:run
```

Alternatively open each service in IntelliJ and run from the IDE.

### 4. Send a test event

Open Swagger UI at `http://localhost:8081/swagger-ui.html`

POST to `/api/events/publish` with this payload:

```json
{
  "eventId": "EVT-001",
  "customerId": "CUST-123",
  "notificationType": "PROMOTIONAL",
  "subject": "Special offer for you",
  "body": "Hello {{customerName}}, check out this exclusive offer!",
  "channel": "EMAIL"
}
```

### 5. View the delivered email

Open Mailhog at `http://localhost:8025` — you should see the rendered email with `{{customerName}}` replaced by `John Doe`.

### 6. Verify audit log

The event lifecycle is persisted to PostgreSQL. Connect to the database to inspect:

```
Host: localhost:5432
Database: notifyflow
Username: notifyflow_user
Password: notifyflow_pass
```

Query: `SELECT event_id, status, routed_to_channel, created_at FROM audit_log;`

---

## Testing

Unit tests use JUnit 5 and Mockito with no Spring context or running infrastructure required.

```bash
cd core-processor
mvn test
```

Tests cover:
- `ValidationService` — required field validation, blank/null handling
- `RoutingService` — all routing paths, case insensitivity, dead letter routing
- `AuditLogService` — received/routed/failed lifecycle, idempotency check

---

## Project Structure

```
notifyflow/
├── payload-producer/          REST API + Kafka producer
│   └── src/main/java/com/notifyflow/payloadproducer/
│       ├── controller/        EventController
│       ├── model/             EventPayload, ErrorResponse
│       ├── service/           EventService
│       ├── kafka/             KafkaProducerService
│       └── config/            KafkaConfig, GlobalExceptionHandler
│
├── core-processor/            Main processing engine
│   └── src/main/java/com/notifyflow/coreprocessor/
│       ├── kafka/consumer/    CustomerEventConsumer
│       ├── kafka/producer/    RoutingProducer
│       ├── service/           ValidationService, EnrichmentService, RoutingService, AuditLogService
│       ├── cache/             CustomerProfileCache (@Cacheable + Redis)
│       ├── entity/            AuditLog
│       ├── repository/        AuditLogRepository
│       ├── exception/         PayloadValidationException, CustomerProfileNotFoundException, RoutingException
│       └── config/            KafkaConfig, CacheConfig, GlobalExceptionHandler
│
├── customer-service/          Customer profile REST API
│   └── src/main/java/com/notifyflow/customerservice/
│       ├── controller/        CustomerController
│       ├── model/             CustomerProfile
│       └── service/           CustomerService
│
├── delivery-service/          Template rendering + email delivery
│   └── src/main/java/com/notifyflow/deliveryservice/
│       ├── kafka/consumer/    DeliveryEventConsumer
│       ├── handler/           EmailHandler, SmsHandler, PushHandler
│       ├── template/          HandlebarsTemplateEngine
│       └── model/             EnrichedEvent
│
├── docker-compose.yml         Full local infrastructure stack
└── .github/workflows/ci.yml   GitHub Actions CI pipeline
```

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

Every event is tracked through its full lifecycle in PostgreSQL:

```
RECEIVED → VALIDATED → ENRICHED → ROUTED
                                 ↓
                               FAILED (on any error)
```

---

## Coming Soon

- AWS deployment — RDS (PostgreSQL), ElastiCache (Redis), MSK (Kafka), SES (email)
- AWS Lambda DLQ sweeper — retries failed messages from dead letter queue
- Dockerfiles for each service
- Live deployment URL

---

## Author

Satya Praneeth Reddi — [GitHub](https://github.com/rspraneeth) · [LinkedIn](https://linkedin.com/in/rspraneeth) · [LeetCode](https://leetcode.com/u/rspraneeth)
