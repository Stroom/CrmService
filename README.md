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
2. Events are published to RabbitMQ topic exchange `crm.update.exchange`
3. Spring Boot application consumes events from queue `debezium_crm_db.change.TABLENAME_queue` with sourceKey `crm_changes`
4. Events are logged with operation type (create/update/delete)
5. Spring Boot application parses the events and forwards to external consumers using exchange name `crm.update.exchange` and routingKey `crm.update.*`

