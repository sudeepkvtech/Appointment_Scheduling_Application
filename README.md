# Appointment Scheduling System - Microservices Architecture

A production-ready appointment scheduling application built using **microservices architecture** with **Java Spring Boot**, featuring **encounter management**, **event-driven communication**, and **master data management**.

## 📋 Table of Contents
- [Architecture Overview](#architecture-overview)
- [Implemented Services](#implemented-services)
- [Technology Stack](#technology-stack)
- [Communication Patterns](#communication-patterns)
- [Quick Start](#quick-start)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Database Schema](#database-schema)
- [Development](#development)

---

## 🏗️ Architecture Overview

Event-driven microservices architecture with service discovery, API gateway, and asynchronous messaging.

```
┌────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port: 8080)                         │
│             Single Entry Point for All Clients                      │
└────────────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴──────────────┐
                │    Service Discovery        │
                │   (Eureka - Port: 8761)     │
                └────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────────┐
        │                     │                         │
  ┌─────▼────────┐   ┌───────▼────────┐    ┌──────────▼─────────┐
  │ Scheduling   │   │ Master Data    │    │   Encounter        │
  │ Service      │   │ Service        │    │   Service          │
  │ (8082)       │   │ (8085)         │    │   (8083)           │
  └──────┬───────┘   └────────────────┘    └──────┬─────────────┘
         │                                          │
         │ Publishes Events                        │ Consumes Events
         └────────────┬──────────────────────────────┘
                     │
              ┌──────▼──────┐
              │    Kafka     │
              │   Broker     │
              └──────────────┘
```

---

## ✅ Implemented Services

### 1. **Service Discovery** (Eureka Server) - Port 8761
- All services register here for automatic discovery
- Health monitoring and load balancing
- Dashboard: http://localhost:8761

### 2. **API Gateway** - Port 8080 ⭐
**Single entry point for all API requests**
- Routes requests to appropriate services
- JWT authentication support (optional, disabled by default)
- CORS handling
- Load balancing via Eureka

**Routes:**
- `/api/v1/scheduling/**` → Scheduling Service
- `/api/v1/encounters/**` → Encounter Service
- `/api/v1/appointment-types/**` → Master Data Service
- `/api/v1/resources/**` → Master Data Service
- `/api/v1/locations/**` → Master Data Service

### 3. **Scheduling Service** - Port 8082
**Core appointment management**
- Create, update, cancel, reschedule appointments
- Time slot management
- Conflict detection
- Audit trail (appointment history)
- **Publishes Events** to Kafka

**Database:** `scheduling_db`
- appointments, time_slots, waitlist, appointment_history

### 4. **Encounter Service** - Port 8083
**Clinical documentation**
- Auto-creates encounters from appointment events
- SOAP notes (Subjective, Objective, Assessment, Plan)
- Vital signs, diagnoses, procedures
- Provider signatures
- **Consumes Events** from Kafka

**Database:** `encounter_db`
- encounters

### 5. **Master Data Service** - Port 8085
**Reference data management**
- Appointment Types (consultation, follow-up, etc.)
- Resources (doctors, nurses, providers)
- Locations (clinics, facilities)
- Complete CRUD operations

**Database:** `master_data_db`
- appointment_types, resources, locations

---

## 💻 Technology Stack

### Backend
- **Java**: 17 (LTS)
- **Spring Boot**: 3.2.1
- **Spring Cloud**: 2023.0.0
  - Gateway (routing)
  - Netflix Eureka (service discovery)
- **Spring Data JPA**: Database access
- **Spring Kafka**: Event streaming
- **PostgreSQL**: 15+ (per-service databases)
- **Flyway**: Database migrations
- **Lombok**: Reduce boilerplate
- **SpringDoc OpenAPI**: API documentation

### Infrastructure
- **Apache Kafka**: Event broker
- **Docker & Docker Compose**: Containerization
- **Maven**: Build tool

---

## 🔄 Communication Patterns

### Synchronous (REST APIs)
**Used for:** Queries, validation, CRUD operations

```java
// Scheduling Service validates data from Master Data Service
GET http://master-data-service/api/v1/appointment-types/{id}
GET http://master-data-service/api/v1/resources/{id}
GET http://master-data-service/api/v1/locations/{id}
```

### Asynchronous (Kafka Events)
**Used for:** State changes, notifications, decoupling

```
Scheduling Service                 Encounter Service
       │                                  │
       │ 1. Create Appointment            │
       │                                  │
       │ 2. Publish Event ────────────────▶ 3. Listen to Event
       │    (Kafka)                       │
       │                                  │ 4. Create Encounter
       │ 5. Return 201 Created            │    (asynchronously)
       │                                  │
```

**Events:**
- `AppointmentCreatedEvent`
- `AppointmentConfirmedEvent` → Triggers encounter creation
- `AppointmentCancelledEvent` → Cancels encounter
- `AppointmentCheckedInEvent` → Starts encounter
- `AppointmentRescheduledEvent`
- `AppointmentCompletedEvent`

---

## 🚀 Quick Start

### Prerequisites
- **Java 17** or higher
- **Maven 3.9+**
- **Docker** and **Docker Compose**

### Option 1: Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd Appointment_Scheduling_Application

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Check service health
curl http://localhost:8761  # Eureka Dashboard
curl http://localhost:8080/actuator/health  # API Gateway Health
```

**Services will start in this order:**
1. PostgreSQL (5432)
2. Zookeeper (2181)
3. Kafka (9092)
4. Eureka Server (8761)
5. API Gateway (8080)
6. Master Data Service (8085)
7. Scheduling Service (8082)
8. Encounter Service (8083)

### Option 2: Run Locally

```bash
# 1. Start Infrastructure
docker-compose up postgres kafka zookeeper -d

# 2. Build all services
mvn clean install -DskipTests

# 3. Start Service Discovery
cd service-discovery
mvn spring-boot:run

# 4. Start services (in separate terminals)
cd api-gateway && mvn spring-boot:run
cd master-data-service && mvn spring-boot:run
cd scheduling-service && mvn spring-boot:run
cd encounter-service && mvn spring-boot:run
```

### Verify Services are Running

```bash
# Check Eureka Dashboard (should show all services)
open http://localhost:8761

# Check API Gateway
curl http://localhost:8080/actuator/health

# Check individual services
curl http://localhost:8082/actuator/health  # Scheduling
curl http://localhost:8083/actuator/health  # Encounter
curl http://localhost:8085/actuator/health  # Master Data
```

---

## 📚 API Endpoints

### Base URL (via API Gateway)
```
http://localhost:8080/api/v1/
```

### Master Data Service

#### Appointment Types
```bash
# Get all appointment types
GET /appointment-types

# Create appointment type
POST /appointment-types
{
  "name": "Initial Consultation",
  "code": "CONSULT-INIT",
  "durationMinutes": 30,
  "category": "Medical",
  "defaultPrice": 150.00
}

# Get by ID
GET /appointment-types/{id}

# Get by code
GET /appointment-types/code/CONSULT-INIT
```

#### Resources (Providers)
```bash
# Get all resources
GET /resources

# Get active resources with specialty filter
GET /resources?activeOnly=true&specialty=Cardiology

# Create resource
POST /resources
{
  "code": "DR-001",
  "firstName": "John",
  "lastName": "Doe",
  "specialty": "Cardiology",
  "email": "john.doe@example.com"
}

# Get by code
GET /resources/code/DR-001
```

#### Locations
```bash
# Get all locations
GET /locations

# Get active locations
GET /locations?activeOnly=true&city=Boston

# Create location
POST /locations
{
  "name": "Main Clinic",
  "code": "LOC-001",
  "addressLine1": "123 Main St",
  "city": "Boston",
  "state": "MA",
  "zipCode": "02101",
  "timezone": "America/New_York"
}
```

### Scheduling Service

#### Appointments
```bash
# Create appointment
POST /scheduling/appointments
{
  "patientId": "patient-uuid",
  "resourceId": "resource-uuid",
  "locationId": "location-uuid",
  "appointmentTypeId": "type-uuid",
  "startTime": "2024-12-15T10:00:00",
  "endTime": "2024-12-15T11:00:00",
  "chiefComplaint": "Annual checkup"
}

# Get appointment by ID
GET /scheduling/appointments/{id}

# Get appointments for patient
GET /scheduling/appointments/patient/{patientId}

# Get appointments for resource (provider)
GET /scheduling/appointments/resource/{resourceId}

# Confirm appointment (triggers encounter creation)
POST /scheduling/appointments/{id}/confirm

# Cancel appointment
POST /scheduling/appointments/{id}/cancel
{
  "reason": "Patient requested cancellation",
  "notifyPatient": true
}

# Reschedule appointment
POST /scheduling/appointments/{id}/reschedule
{
  "newStartTime": "2024-12-16T14:00:00",
  "newEndTime": "2024-12-16T15:00:00",
  "reason": "Provider availability"
}

# Check-in appointment
POST /scheduling/appointments/{id}/check-in

# Complete appointment
POST /scheduling/appointments/{id}/complete
```

### Encounter Service

```bash
# Get encounter by appointment ID
GET /encounters/appointment/{appointmentId}

# Get encounter by ID
GET /encounters/{id}

# Get all encounters for patient
GET /encounters/patient/{patientId}

# Update encounter (add clinical notes)
PUT /encounters/{id}
{
  "historyOfPresentIllness": "Patient reports...",
  "physicalExamination": "Vital signs stable...",
  "assessment": "Diagnosis...",
  "plan": "Treatment plan...",
  "vitalSigns": {
    "temperature": "98.6",
    "bloodPressure": "120/80",
    "heartRate": "72"
  }
}

# Sign encounter
POST /encounters/{id}/sign?signature=Dr.JohnDoe
```

---

## 🔐 Authentication

Authentication is **DISABLED** by default for development.

To enable JWT authentication, see [AUTHENTICATION_GUIDE.md](./AUTHENTICATION_GUIDE.md)

**Supported authentication methods:**
- JWT (Web/Mobile apps)
- API Keys (Third-party integrations)
- OAuth 2.0 (Enterprise SSO)
- Service-to-service (Internal JWT)

---

## 🗄️ Database Schema

### Scheduling Service (`scheduling_db`)

**appointments**
- Core appointment data
- Patient, resource, location, appointment type references
- Status tracking (scheduled, confirmed, completed, cancelled)
- Rescheduling support with audit trail

**time_slots**
- Available time slots for resources
- Conflict management
- Booking capacity

**waitlist**
- Patient waiting list when slots unavailable
- Priority-based matching

**appointment_history**
- Complete audit trail
- Tracks all changes to appointments

### Encounter Service (`encounter_db`)

**encounters**
- Clinical documentation linked to appointments
- SOAP notes
- Vital signs (JSONB)
- Diagnoses and procedure codes (JSONB)
- Provider signatures

### Master Data Service (`master_data_db`)

**appointment_types**
- Types of appointments
- Duration, pricing, category

**resources**
- Providers/practitioners
- Specialty, credentials, availability

**locations**
- Facilities/clinics
- Operating hours, timezone, amenities

---

## 🛠️ Development

### Project Structure

```
appointment-scheduling-system/
├── shared-library/          # Common events, enums, exceptions
├── service-discovery/       # Eureka server
├── api-gateway/            # Spring Cloud Gateway
├── scheduling-service/     # Appointment management
├── encounter-service/      # Clinical documentation
├── master-data-service/    # Reference data
├── notification-service/   # (Skeleton - not required)
├── docker-compose.yml      # All infrastructure
├── init-databases.sql      # Database initialization
└── pom.xml                # Parent POM
```

### Build & Test

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl scheduling-service -am

# Run tests
mvn test

# Skip tests
mvn clean install -DskipTests
```

### Database Migrations

Each service uses **Flyway** for version-controlled database migrations.

Migrations are located in:
- `scheduling-service/src/main/resources/db/migration/`
- `encounter-service/src/main/resources/db/migration/`
- `master-data-service/src/main/resources/db/migration/`

Migrations run automatically on application startup.

### Swagger API Documentation

Each service exposes Swagger UI:

- **Scheduling**: http://localhost:8082/swagger-ui.html
- **Encounter**: http://localhost:8083/swagger-ui.html
- **Master Data**: http://localhost:8085/swagger-ui.html

---

## 🔍 Monitoring

### Eureka Dashboard
Service discovery and health monitoring:
- URL: http://localhost:8761

### Kafka UI (Optional)
Monitor Kafka topics and messages:
```bash
docker run -p 8090:8080 \
  -e KAFKA_CLUSTERS_0_NAME=local \
  -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=localhost:9092 \
  provectuslabs/kafka-ui:latest
```
Access: http://localhost:8090

### Actuator Endpoints

Each service exposes Spring Boot Actuator:
```bash
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

---

## 📝 Example Workflow

### 1. Create Reference Data

```bash
# Create appointment type
curl -X POST http://localhost:8080/api/v1/appointment-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Initial Consultation",
    "code": "CONSULT-INIT",
    "durationMinutes": 30,
    "category": "Medical"
  }'

# Create resource (doctor)
curl -X POST http://localhost:8080/api/v1/resources \
  -H "Content-Type: application/json" \
  -d '{
    "code": "DR-001",
    "firstName": "Jane",
    "lastName": "Smith",
    "specialty": "Cardiology"
  }'

# Create location
curl -X POST http://localhost:8080/api/v1/locations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Downtown Clinic",
    "code": "LOC-001",
    "addressLine1": "100 Main St",
    "city": "Boston",
    "state": "MA",
    "zipCode": "02101"
  }'
```

### 2. Book Appointment

```bash
curl -X POST http://localhost:8080/api/v1/scheduling/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "resourceId": "resource-uuid-from-step-1",
    "locationId": "location-uuid-from-step-1",
    "appointmentTypeId": "type-uuid-from-step-1",
    "startTime": "2024-12-15T10:00:00",
    "endTime": "2024-12-15T10:30:00",
    "chiefComplaint": "Annual checkup"
  }'
```

### 3. Confirm Appointment (Creates Encounter)

```bash
curl -X POST http://localhost:8080/api/v1/scheduling/appointments/{id}/confirm
```

**What happens:**
1. Appointment status → CONFIRMED
2. Event published to Kafka
3. Encounter Service listens to event
4. Encounter created automatically

### 4. View Encounter

```bash
curl http://localhost:8080/api/v1/encounters/appointment/{appointmentId}
```

---

## 🎯 Key Features

✅ **Event-Driven Architecture** - Decoupled services via Kafka
✅ **Service Discovery** - Automatic registration with Eureka
✅ **API Gateway** - Single entry point for all clients
✅ **Master Data Management** - Centralized reference data
✅ **Appointment Scheduling** - Complete booking workflow
✅ **Encounter Management** - Auto-generated clinical records
✅ **Audit Trail** - Full history of all changes
✅ **Docker Support** - Easy local development
✅ **Flyway Migrations** - Version-controlled database schema
✅ **Swagger Documentation** - Interactive API docs
✅ **Health Checks** - Monitoring and observability
✅ **JWT Ready** - Authentication infrastructure in place

---

## 📖 Additional Documentation

- [AUTHENTICATION_GUIDE.md](./AUTHENTICATION_GUIDE.md) - Complete authentication guide for multiple clients
- [Swagger UI](http://localhost:8082/swagger-ui.html) - Interactive API documentation

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📧 Support

For questions and support, please open an issue in the repository.

---

## 🎉 Summary

You now have a **fully functional microservices backend** for appointment scheduling with:

| Service | Status | Purpose |
|---------|--------|---------|
| Service Discovery | ✅ | Service registration (Eureka) |
| API Gateway | ✅ | Single entry point (Port 8080) |
| Scheduling Service | ✅ | Appointment management |
| Encounter Service | ✅ | Clinical documentation |
| Master Data Service | ✅ | Reference data (types, resources, locations) |

**All services are production-ready with:**
- Docker support
- Database migrations
- Event-driven communication
- Health checks
- API documentation
- Audit trails
- Error handling
- Logging

**Start building your frontend and connect to:**
`http://localhost:8080/api/v1/*`
