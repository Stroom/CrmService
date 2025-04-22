# "CRM" Application

This is a Spring Boot application that demonstrates Change Data Capture (CDC) using Debezium.

## Prerequisites

- Java 17 or later
- Docker and Docker Compose
- Gradle

## Components

### 1. Spring Boot Application
- Swagger UI for API documentation
- RabbitMQ consumer for Debezium events
- RabbitMQ producer for sending events to external consumers

### 2. PostgreSQL Database
- CRM data
- Configured with logical replication for CDC

### 3. RabbitMQ
- Message broker for Debezium events
- Management interface available
- Message broker for consumers

## Access Points

- Spring Boot Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- RabbitMQ Management: http://localhost:15672

## Debezium Event Flow

1. Changes in the tracked tables are captured by Debezium
2. Events are published to RabbitMQ exchange `ee.bigbank.core.exchange` (??? Should be topic exchange ???)
3. Spring Boot application consumes events from queue `customer_changes_queue` with sourceKey `customer_changes`
4. Events are logged with operation type (create/update/delete)
5. Spring Boot application parses the events and forwards to external consumers `customer.change.*`

