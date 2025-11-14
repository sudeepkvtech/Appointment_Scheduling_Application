# Appointment Scheduling System - Microservices Architecture

A comprehensive appointment scheduling application built using microservices architecture with Java Spring Boot, featuring encounter management, resource scheduling, and event-driven communication.

## 📋 Table of Contents
- [Architecture Overview](#architecture-overview)
- [Microservices](#microservices)
- [Technology Stack](#technology-stack)
- [Communication Patterns](#communication-patterns)
- [Getting Started](#getting-started)
- [Database Setup](#database-setup)
- [Running the Services](#running-the-services)
- [API Documentation](#api-documentation)

## 🏗️ Architecture Overview

This system follows a microservices architecture with event-driven communication using Apache Kafka for asynchronous operations and REST APIs for synchronous queries.

```
┌─────────────────────────────────────────────────────────────────┐
│                         API GATEWAY                             │
│                   (Spring Cloud Gateway)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴──────────────┐
                │    Service Discovery       │
                │   (Netflix Eureka)         │
                └────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
  Identity Service    Scheduling Service    Encounter Service
        │                     │                     │
        │                     │                     │
        └──────────┬──────────┴──────────┬──────────┘
                   │                     │
              ┌────▼──────┐      ┌──────▼────────┐
              │   Kafka   │      │  PostgreSQL   │
              │  Broker   │      │  (Per Service)│
              └───────────┘      └───────────────┘
```

## 🔧 Microservices

### 1. **Service Discovery** (Port: 8761)
- Service registration and discovery using Netflix Eureka
- Health monitoring
- Load balancing

### 2. **API Gateway** (Port: 8080)
- Single entry point for all client requests
- Request routing
- Authentication/Authorization
- Rate limiting
- CORS handling

### 3. **Identity Service** (Port: 8081)
- User authentication and authorization
- JWT token management
- Patient profile management
- User roles and permissions

**Database**: `identity_db`
- users, patients, roles, permissions

### 4. **Scheduling Service** (Port: 8082) ⭐ **Core Service**
- Time slot management
- Appointment booking
- Availability search
- Rescheduling and cancellation
- Waitlist management

**Database**: `scheduling_db`
- appointments, time_slots, waitlist, appointment_history

**Events Published**:
- `AppointmentCreatedEvent`
- `AppointmentConfirmedEvent`
- `AppointmentCancelledEvent`
- `AppointmentRescheduledEvent`
- `AppointmentCheckedInEvent`
- `AppointmentCompletedEvent`

### 5. **Encounter Service** (Port: 8083)
- Clinical documentation (SOAP notes)
- Encounter lifecycle management
- Vital signs recording
- Diagnosis and procedure coding
- Provider signatures

**Database**: `encounter_db`
- encounters, encounter_vitals, encounter_diagnoses, encounter_procedures

**Events Consumed**:
- `AppointmentConfirmedEvent` → Creates encounter
- `AppointmentCancelledEvent` → Cancels encounter
- `AppointmentCheckedInEvent` → Updates encounter status

### 6. **Notification Service** (Port: 8084)
- Email/SMS notifications
- Appointment reminders
- Confirmation requests
- Template management

**Database**: `notification_db`
- notifications, notification_templates

**Events Consumed**:
- `AppointmentCreatedEvent` → Send confirmation
- `AppointmentConfirmedEvent` → Schedule reminder
- `AppointmentCancelledEvent` → Send cancellation notice
- `AppointmentRescheduledEvent` → Send reschedule notice

## 💻 Technology Stack

### Backend
- **Java**: 17
- **Spring Boot**: 3.2.1
- **Spring Cloud**: 2023.0.0
- **Spring Data JPA**: Database access
- **Spring Security**: Authentication & Authorization
- **Spring Cloud Gateway**: API Gateway
- **Netflix Eureka**: Service Discovery
- **Apache Kafka**: Event streaming
- **PostgreSQL**: Primary database
- **Lombok**: Reduce boilerplate
- **MapStruct**: DTO mapping
- **SpringDoc OpenAPI**: API documentation

### Build Tool
- **Maven**: 3.9+

### DevOps
- **Docker**: Containerization
- **Docker Compose**: Local development

## 🔄 Communication Patterns

### Synchronous Communication (REST)
Used for:
- Read operations (GET requests)
- Immediate validation
- Direct queries where user needs immediate response

**Example**:
```java
// Scheduling Service validates resource exists
GET http://resource-service/api/v1/resources/{id}
```

### Asynchronous Communication (Kafka Events)
Used for:
- State changes (Created, Updated, Deleted)
- Notifications
- Operations that don't need immediate response
- Decoupling services

**Example Flow**:
```
1. Client books appointment → Scheduling Service
2. Scheduling Service:
   - Saves appointment to database
   - Publishes AppointmentCreatedEvent to Kafka
   - Returns 201 Created to client

3. Event Consumers (Independent):
   - Encounter Service listens → Creates encounter
   - Notification Service listens → Sends confirmation email
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL 14+ (or use Docker)
- Apache Kafka (or use Docker)

### Clone the Repository
```bash
git clone <repository-url>
cd Appointment_Scheduling_Application
```

### Build the Project
```bash
# Build all modules
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

## 🗄️ Database Setup

### Using Docker Compose (Recommended)
```bash
# Start all infrastructure (PostgreSQL, Kafka, Zookeeper, Eureka)
docker-compose up -d
```

### Manual Setup
Create databases for each service:
```sql
CREATE DATABASE identity_db;
CREATE DATABASE scheduling_db;
CREATE DATABASE encounter_db;
CREATE DATABASE notification_db;
```

### Database Migrations
Each service uses Flyway for database migrations. Migrations run automatically on startup.

## ▶️ Running the Services

### Option 1: Using Docker Compose (Recommended)
```bash
# Start all services
docker-compose up

# Or start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Option 2: Running Individually

#### 1. Start Infrastructure
```bash
# Start Service Discovery (Eureka)
cd service-discovery
mvn spring-boot:run
```
Access Eureka Dashboard: http://localhost:8761

#### 2. Start Core Services
```bash
# Terminal 1: Identity Service
cd identity-service
mvn spring-boot:run

# Terminal 2: Scheduling Service
cd scheduling-service
mvn spring-boot:run

# Terminal 3: Encounter Service
cd encounter-service
mvn spring-boot:run

# Terminal 4: Notification Service
cd notification-service
mvn spring-boot:run
```

#### 3. Start API Gateway
```bash
cd api-gateway
mvn spring-boot:run
```

### Service Ports
| Service | Port | URL |
|---------|------|-----|
| Service Discovery (Eureka) | 8761 | http://localhost:8761 |
| API Gateway | 8080 | http://localhost:8080 |
| Identity Service | 8081 | http://localhost:8081 |
| Scheduling Service | 8082 | http://localhost:8082 |
| Encounter Service | 8083 | http://localhost:8083 |
| Notification Service | 8084 | http://localhost:8084 |

## 📚 API Documentation

Each service exposes Swagger UI for API documentation:

- **API Gateway Swagger**: http://localhost:8080/swagger-ui.html
- **Identity Service**: http://localhost:8081/swagger-ui.html
- **Scheduling Service**: http://localhost:8082/swagger-ui.html
- **Encounter Service**: http://localhost:8083/swagger-ui.html
- **Notification Service**: http://localhost:8084/swagger-ui.html

### Sample API Endpoints

#### Authentication
```bash
# Register user
POST http://localhost:8080/api/v1/auth/register

# Login
POST http://localhost:8080/api/v1/auth/login
```

#### Scheduling
```bash
# Search availability
POST http://localhost:8080/api/v1/scheduling/availability/search

# Book appointment
POST http://localhost:8080/api/v1/scheduling/appointments

# Get appointment
GET http://localhost:8080/api/v1/scheduling/appointments/{id}

# Cancel appointment
POST http://localhost:8080/api/v1/scheduling/appointments/{id}/cancel
```

#### Encounters
```bash
# Get encounter by appointment
GET http://localhost:8080/api/v1/encounters/appointment/{appointmentId}

# Update encounter
PUT http://localhost:8080/api/v1/encounters/{id}

# Sign encounter
POST http://localhost:8080/api/v1/encounters/{id}/sign
```

## 🔐 Security

### Authentication Flow
1. User logs in via Identity Service
2. Receives JWT token
3. Includes token in Authorization header for subsequent requests
4. API Gateway validates token and routes to appropriate service

### Sample Request with Authentication
```bash
curl -X GET \
  http://localhost:8080/api/v1/scheduling/appointments \
  -H 'Authorization: Bearer <jwt-token>'
```

## 🧪 Testing

### Run Unit Tests
```bash
# Test all modules
mvn test

# Test specific service
cd scheduling-service
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

## 📊 Monitoring

### Eureka Dashboard
Monitor service health and instances:
- URL: http://localhost:8761

### Kafka UI (Optional)
Use Kafka UI for monitoring topics and messages:
```bash
docker run -p 8090:8080 \
  -e KAFKA_CLUSTERS_0_NAME=local \
  -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=localhost:9092 \
  provectuslabs/kafka-ui:latest
```
Access: http://localhost:8090

## 🛠️ Development

### Project Structure
```
appointment-scheduling-system/
├── shared-library/          # Common DTOs, Events, Exceptions
├── service-discovery/       # Eureka Server
├── api-gateway/            # Spring Cloud Gateway
├── identity-service/       # Authentication & Users
├── scheduling-service/     # Core booking logic
├── encounter-service/      # Clinical documentation
├── notification-service/   # Alerts & Reminders
├── docker-compose.yml      # Infrastructure setup
└── pom.xml                # Parent POM
```

### Adding a New Service
1. Create module in parent POM
2. Add dependency on shared-library
3. Configure service discovery client
4. Define REST endpoints
5. Add Kafka listeners if needed
6. Create database migrations
7. Update docker-compose.yml

## 🤝 Contributing

1. Create a feature branch
2. Make changes
3. Write tests
4. Submit pull request

## 📝 License

MIT License

## 📞 Contact

For questions and support, please open an issue in the repository.

---

**Note**: This is a production-ready template. Adjust configurations, security settings, and infrastructure based on your deployment environment.
