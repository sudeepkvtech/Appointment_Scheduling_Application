# Appointment Booking Complete Lifecycle Demonstration

This document provides a comprehensive step-by-step demonstration of the complete appointment booking lifecycle, showing exactly how services interact, what code gets executed, and how data flows through the system.

## Table of Contents

1. [System Overview](#system-overview)
2. [Prerequisites Setup](#prerequisites-setup)
3. [Complete Lifecycle Flow](#complete-lifecycle-flow)
4. [Step-by-Step Demonstration](#step-by-step-demonstration)
5. [Code Execution Paths](#code-execution-paths)
6. [Event Flow Through Kafka](#event-flow-through-kafka)
7. [Database Changes](#database-changes)
8. [Testing the Complete Flow](#testing-the-complete-flow)

---

## System Overview

### Architecture Diagram

```
┌─────────────┐
│   Client    │
│ (Web/Mobile)│
└──────┬──────┘
       │ HTTP Request
       ▼
┌─────────────────────────────────────────┐
│         API Gateway (Port 8080)         │
│  Routes: /api/v1/scheduling/**          │
│          /api/v1/encounters/**          │
│          /api/v1/appointment-types/**   │
└──────┬──────────────────────────────────┘
       │
       ├──────────────────┬─────────────────┬──────────────────┐
       ▼                  ▼                 ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐
│  Scheduling  │  │  Encounter   │  │ Master Data │  │   Service    │
│   Service    │  │   Service    │  │   Service   │  │  Discovery   │
│  (Port 8082) │  │  (Port 8083) │  │ (Port 8085) │  │ (Port 8761)  │
└──────┬───────┘  └──────┬───────┘  └──────┬──────┘  └──────────────┘
       │                 │                 │
       │                 │                 │
       ▼                 ▼                 ▼
┌─────────────────────────────────────────────────┐
│         PostgreSQL (Port 5432)                  │
│  Databases: scheduling_db, encounter_db,        │
│             master_data_db                      │
└─────────────────────────────────────────────────┘
       ▲                 │
       │                 │
       │    ┌────────────▼──────────────┐
       │    │   Kafka (Port 9092)       │
       │    │   Topic: appointment-events│
       │    └───────────────────────────┘
       │                 │
       └─────────────────┘
        (Publish Event)  (Consume Event)
```

### Communication Patterns

1. **Synchronous REST (Client → Services)**
   - Client → API Gateway → Master Data Service (Get reference data)
   - Client → API Gateway → Scheduling Service (Create/manage appointments)
   - Client → API Gateway → Encounter Service (Query encounters)

2. **Asynchronous Events (Service → Service)**
   - Scheduling Service → Kafka → Encounter Service (Appointment confirmed event)

---

## Prerequisites Setup

Before booking an appointment, we need reference data in the Master Data Service.

### Step 0.1: Create Appointment Type

**API Call:**
```bash
curl -X POST http://localhost:8080/api/v1/appointment-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "General Consultation",
    "code": "CONSULT-GEN",
    "description": "Standard general consultation with physician",
    "durationMinutes": 30,
    "color": "#4CAF50",
    "requiresPreparation": false,
    "isVirtualAvailable": true,
    "isActive": true,
    "category": "CONSULTATION",
    "defaultPrice": 150.00,
    "requiresReferral": false
  }'
```

**Service Called:** Master Data Service → `MasterDataController.createAppointmentType()`

**Code Path:**
```
api-gateway/src/main/resources/application.yml:62
  → Routes to lb://master-data-service

master-data-service/src/main/java/com/appointment/masterdata/controller/AppointmentTypeController.java:28
  @PostMapping
  public ResponseEntity<AppointmentTypeResponse> createAppointmentType(@Valid @RequestBody CreateAppointmentTypeRequest request)
    └→ appointmentTypeService.createAppointmentType(request)

master-data-service/src/main/java/com/appointment/masterdata/service/AppointmentTypeService.java:34
  public AppointmentTypeResponse createAppointmentType(CreateAppointmentTypeRequest request) {
    1. Validate uniqueness of code
    2. Create new AppointmentType entity
    3. Save to database (master_data_db.appointment_types)
    4. Return DTO response
  }
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "General Consultation",
  "code": "CONSULT-GEN",
  "description": "Standard general consultation with physician",
  "durationMinutes": 30,
  "color": "#4CAF50",
  "requiresPreparation": false,
  "preparationInstructions": null,
  "isVirtualAvailable": true,
  "isActive": true,
  "category": "CONSULTATION",
  "defaultPrice": 150.00,
  "requiresReferral": false,
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

**Database Change:**
```sql
-- In master_data_db
INSERT INTO appointment_types (
  id, name, code, description, duration_minutes, color,
  requires_preparation, is_virtual_available, is_active,
  category, default_price, requires_referral,
  created_at, updated_at
) VALUES (
  '550e8400-e29b-41d4-a716-446655440001',
  'General Consultation',
  'CONSULT-GEN',
  'Standard general consultation with physician',
  30, '#4CAF50', false, true, true,
  'CONSULTATION', 150.00, false,
  '2024-01-15 10:00:00', '2024-01-15 10:00:00'
);
```

---

### Step 0.2: Create Location

**API Call:**
```bash
curl -X POST http://localhost:8080/api/v1/locations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Main Clinic - Downtown",
    "code": "LOC-001",
    "address": "123 Medical Plaza",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA",
    "phone": "+1-555-0100",
    "email": "downtown@clinic.com",
    "isActive": true,
    "isParkingAvailable": true,
    "isWheelchairAccessible": true,
    "operatingHours": {
      "monday": "08:00-18:00",
      "tuesday": "08:00-18:00",
      "wednesday": "08:00-18:00",
      "thursday": "08:00-18:00",
      "friday": "08:00-17:00"
    }
  }'
```

**Service Called:** Master Data Service → `LocationController.createLocation()`

**Code Path:**
```
master-data-service/src/main/java/com/appointment/masterdata/controller/LocationController.java:28
  @PostMapping
  public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody CreateLocationRequest request)
    └→ locationService.createLocation(request)

master-data-service/src/main/java/com/appointment/masterdata/service/LocationService.java:34
  public LocationResponse createLocation(CreateLocationRequest request) {
    1. Validate uniqueness of code
    2. Create new Location entity
    3. Save to database (master_data_db.locations)
    4. Return DTO response
  }
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440002",
  "name": "Main Clinic - Downtown",
  "code": "LOC-001",
  "address": "123 Medical Plaza",
  "city": "New York",
  "state": "NY",
  "postalCode": "10001",
  "country": "USA",
  "phone": "+1-555-0100",
  "email": "downtown@clinic.com",
  "isActive": true,
  "latitude": null,
  "longitude": null,
  "isParkingAvailable": true,
  "isWheelchairAccessible": true,
  "operatingHours": {
    "monday": "08:00-18:00",
    "tuesday": "08:00-18:00",
    "wednesday": "08:00-18:00",
    "thursday": "08:00-18:00",
    "friday": "08:00-17:00"
  }
}
```

---

### Step 0.3: Create Resource (Doctor/Provider)

**API Call:**
```bash
curl -X POST http://localhost:8080/api/v1/resources \
  -H "Content-Type: application/json" \
  -d '{
    "code": "DR-SMITH-001",
    "firstName": "John",
    "lastName": "Smith",
    "title": "Dr.",
    "specialty": "General Medicine",
    "licenseNumber": "MD-123456",
    "email": "dr.smith@clinic.com",
    "phone": "+1-555-0101",
    "isActive": true,
    "bio": "Board-certified physician with 15 years of experience",
    "qualifications": ["MD", "FACP"],
    "languages": ["English", "Spanish"]
  }'
```

**Service Called:** Master Data Service → `ResourceController.createResource()`

**Code Path:**
```
master-data-service/src/main/java/com/appointment/masterdata/controller/ResourceController.java:28
  @PostMapping
  public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest request)
    └→ resourceService.createResource(request)

master-data-service/src/main/java/com/appointment/masterdata/service/ResourceService.java:34
  public ResourceResponse createResource(CreateResourceRequest request) {
    1. Validate uniqueness of code
    2. Create new Resource entity
    3. Save to database (master_data_db.resources)
    4. Return DTO response
  }
```

**Response:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "code": "DR-SMITH-001",
  "firstName": "John",
  "lastName": "Smith",
  "fullName": "Dr. John Smith",
  "title": "Dr.",
  "specialty": "General Medicine",
  "licenseNumber": "MD-123456",
  "email": "dr.smith@clinic.com",
  "phone": "+1-555-0101",
  "isActive": true,
  "avatarUrl": null,
  "bio": "Board-certified physician with 15 years of experience",
  "qualifications": ["MD", "FACP"],
  "languages": ["English", "Spanish"]
}
```

---

### Step 0.4: Create Time Slots for the Resource

**API Call:**
```bash
curl -X POST http://localhost:8080/api/v1/scheduling/time-slots/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": "770e8400-e29b-41d4-a716-446655440003",
    "locationId": "660e8400-e29b-41d4-a716-446655440002",
    "startDate": "2024-01-20",
    "endDate": "2024-01-20",
    "startTime": "09:00",
    "endTime": "17:00",
    "slotDuration": 30,
    "breakTimes": [
      {"startTime": "12:00", "endTime": "13:00"}
    ]
  }'
```

**Service Called:** Scheduling Service → `TimeSlotController.createBulkTimeSlots()`

**Code Path:**
```
api-gateway/src/main/resources/application.yml:54
  → Routes to lb://scheduling-service

scheduling-service/src/main/java/com/appointment/scheduling/controller/TimeSlotController.java:45
  @PostMapping("/bulk")
  public ResponseEntity<BulkTimeSlotsResponse> createBulkTimeSlots(@Valid @RequestBody CreateBulkTimeSlotsRequest request)
    └→ timeSlotService.createBulkTimeSlots(request)

scheduling-service/src/main/java/com/appointment/scheduling/service/TimeSlotService.java:78
  @Transactional
  public BulkTimeSlotsResponse createBulkTimeSlots(CreateBulkTimeSlotsRequest request) {
    1. Validate date range
    2. Call Master Data Service to validate Resource and Location exist
    3. Generate time slots for each day (avoiding breaks)
    4. Save all slots to database (scheduling_db.time_slots)
    5. Return count of created slots
  }
```

**Service-to-Service Call:**
```java
// scheduling-service/src/main/java/com/appointment/scheduling/service/TimeSlotService.java:85
// Validates resource exists by calling Master Data Service
private void validateResource(UUID resourceId) {
    String url = "http://master-data-service/api/v1/resources/" + resourceId;
    try {
        restTemplate.getForObject(url, ResourceResponse.class);
    } catch (HttpClientErrorException.NotFound e) {
        throw new ResourceNotFoundException("Resource not found: " + resourceId);
    }
}
```

**Response:**
```json
{
  "totalSlotsCreated": 14,
  "resourceId": "770e8400-e29b-41d4-a716-446655440003",
  "locationId": "660e8400-e29b-41d4-a716-446655440002",
  "dateRange": {
    "startDate": "2024-01-20",
    "endDate": "2024-01-20"
  },
  "message": "Successfully created 14 time slots"
}
```

**Database Change:**
```sql
-- In scheduling_db, 14 slots created (09:00-12:00, 13:00-17:00 in 30-min intervals)
INSERT INTO time_slots (id, resource_id, location_id, start_time, end_time, status, created_at, updated_at)
VALUES
  ('880e8400-0001', '770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440002', '2024-01-20 09:00:00', '2024-01-20 09:30:00', 'AVAILABLE', NOW(), NOW()),
  ('880e8400-0002', '770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440002', '2024-01-20 09:30:00', '2024-01-20 10:00:00', 'AVAILABLE', NOW(), NOW()),
  -- ... (12 more slots)
  ('880e8400-0014', '770e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440002', '2024-01-20 16:30:00', '2024-01-20 17:00:00', 'AVAILABLE', NOW(), NOW());
```

---

## Complete Lifecycle Flow

Now that prerequisites are set up, let's walk through the complete appointment booking lifecycle:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    APPOINTMENT BOOKING LIFECYCLE                    │
└─────────────────────────────────────────────────────────────────────┘

Step 1: Search Available Time Slots
  Client → API Gateway → Scheduling Service
  ↓
  Response: List of available slots

Step 2: Create Appointment (Status: SCHEDULED)
  Client → API Gateway → Scheduling Service
  ↓
  - Save appointment to database
  - Update time slot status to BOOKED
  - Create history entry
  - Publish AppointmentCreatedEvent to Kafka
  ↓
  Response: Appointment details

Step 3: Confirm Appointment (Status: CONFIRMED)
  Client → API Gateway → Scheduling Service
  ↓
  - Update appointment status to CONFIRMED
  - Create history entry
  - Publish AppointmentConfirmedEvent to Kafka ←───┐
  ↓                                                 │
  Response: Updated appointment details             │
                                                    │
Step 4: Encounter Service Consumes Event           │
  Kafka (appointment-events topic) ─────────────────┘
  ↓
  Encounter Service Listener receives AppointmentConfirmedEvent
  ↓
  - Create new Encounter entity
  - Link to appointment
  - Save to database
  - Set status to PLANNED
  ↓
  Encounter created successfully

Step 5: Query Encounter (Optional)
  Client → API Gateway → Encounter Service
  ↓
  Response: Encounter details

Step 6: Check-in Patient (Optional)
  Client → API Gateway → Scheduling Service
  ↓
  - Update appointment status to CHECKED_IN
  - Publish AppointmentCheckedInEvent
  ↓
  Encounter Service updates encounter status to IN_PROGRESS

Step 7: Complete Appointment (Optional)
  Client → API Gateway → Scheduling Service
  ↓
  - Update appointment status to COMPLETED
  - Publish AppointmentCompletedEvent
  ↓
  Encounter Service updates encounter status to FINISHED
```

---

## Step-by-Step Demonstration

### STEP 1: Search Available Time Slots

**Purpose:** Patient/Client wants to find available appointment times with Dr. Smith on January 20, 2024.

**API Call:**
```bash
curl -X GET "http://localhost:8080/api/v1/scheduling/time-slots/available?resourceId=770e8400-e29b-41d4-a716-446655440003&date=2024-01-20" \
  -H "Accept: application/json"
```

**Service Called:** API Gateway → Scheduling Service

**Code Execution Path:**

```java
// 1. API Gateway receives request
// api-gateway/src/main/resources/application.yml:54-57
spring:
  cloud:
    gateway:
      routes:
        - id: scheduling-service
          uri: lb://scheduling-service  // Load-balanced via Eureka
          predicates:
            - Path=/api/v1/scheduling/**

// 2. Gateway looks up scheduling-service from Eureka
// Service Discovery returns: http://scheduling-service:8082

// 3. Request forwarded to Scheduling Service
// scheduling-service/src/main/java/com/appointment/scheduling/controller/TimeSlotController.java:33
@GetMapping("/available")
public ResponseEntity<List<TimeSlotResponse>> getAvailableTimeSlots(
    @RequestParam UUID resourceId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
) {
    List<TimeSlotResponse> slots = timeSlotService.getAvailableTimeSlots(resourceId, date);
    return ResponseEntity.ok(slots);
}

// 4. Service layer executes query
// scheduling-service/src/main/java/com/appointment/scheduling/service/TimeSlotService.java:45
public List<TimeSlotResponse> getAvailableTimeSlots(UUID resourceId, LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.atTime(23, 59, 59);

    // Query database for available slots
    List<TimeSlot> slots = timeSlotRepository.findAvailableSlots(
        resourceId,
        startOfDay,
        endOfDay,
        TimeSlotStatus.AVAILABLE
    );

    return slots.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
}

// 5. Repository executes SQL query
// scheduling-service/src/main/java/com/appointment/scheduling/repository/TimeSlotRepository.java:15
@Query("""
    SELECT ts FROM TimeSlot ts
    WHERE ts.resourceId = :resourceId
    AND ts.startTime >= :startTime
    AND ts.endTime <= :endTime
    AND ts.status = :status
    ORDER BY ts.startTime
""")
List<TimeSlot> findAvailableSlots(
    @Param("resourceId") UUID resourceId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime,
    @Param("status") TimeSlotStatus status
);
```

**SQL Executed:**
```sql
SELECT * FROM time_slots
WHERE resource_id = '770e8400-e29b-41d4-a716-446655440003'
  AND start_time >= '2024-01-20 00:00:00'
  AND end_time <= '2024-01-20 23:59:59'
  AND status = 'AVAILABLE'
ORDER BY start_time;
```

**Response:**
```json
[
  {
    "id": "880e8400-0001",
    "resourceId": "770e8400-e29b-41d4-a716-446655440003",
    "locationId": "660e8400-e29b-41d4-a716-446655440002",
    "startTime": "2024-01-20T09:00:00",
    "endTime": "2024-01-20T09:30:00",
    "status": "AVAILABLE"
  },
  {
    "id": "880e8400-0002",
    "resourceId": "770e8400-e29b-41d4-a716-446655440003",
    "locationId": "660e8400-e29b-41d4-a716-446655440002",
    "startTime": "2024-01-20T09:30:00",
    "endTime": "2024-01-20T10:00:00",
    "status": "AVAILABLE"
  },
  // ... (12 more available slots)
]
```

---

### STEP 2: Create Appointment

**Purpose:** Patient books an appointment for the 10:00 AM slot.

**API Call:**
```bash
curl -X POST http://localhost:8080/api/v1/scheduling/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-12345",
    "resourceId": "770e8400-e29b-41d4-a716-446655440003",
    "locationId": "660e8400-e29b-41d4-a716-446655440002",
    "appointmentTypeId": "550e8400-e29b-41d4-a716-446655440001",
    "startTime": "2024-01-20T10:00:00",
    "endTime": "2024-01-20T10:30:00",
    "status": "SCHEDULED",
    "notes": "Patient complains of headaches",
    "isVirtual": false
  }'
```

**Service Called:** API Gateway → Scheduling Service

**Complete Code Execution Path:**

```java
// 1. Request arrives at Scheduling Service
// scheduling-service/src/main/java/com/appointment/scheduling/controller/AppointmentController.java:28
@PostMapping
public ResponseEntity<AppointmentResponse> createAppointment(
    @Valid @RequestBody CreateAppointmentRequest request
) {
    log.info("Creating appointment for patient: {}", request.getPatientId());
    AppointmentResponse response = appointmentService.createAppointment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// 2. Service layer processes the request
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:67
@Transactional
public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
    log.info("Creating appointment for patient {} with resource {}",
             request.getPatientId(), request.getResourceId());

    // Step 2a: Validate time range
    if (request.getEndTime().isBefore(request.getStartTime())) {
        throw new BadRequestException("End time must be after start time");
    }

    // Step 2b: Check for conflicting appointments
    if (appointmentRepository.existsConflictingAppointment(
            request.getResourceId(),
            request.getStartTime(),
            request.getEndTime())) {
        throw new BadRequestException("Resource already has a conflicting appointment in this time slot");
    }

    // Step 2c: Validate references exist (calls Master Data Service)
    validateReferences(request);

    // Step 2d: Create appointment entity
    Appointment appointment = new Appointment();
    appointment.setAppointmentNumber(generateAppointmentNumber());
    appointment.setPatientId(request.getPatientId());
    appointment.setResourceId(request.getResourceId());
    appointment.setLocationId(request.getLocationId());
    appointment.setAppointmentTypeId(request.getAppointmentTypeId());
    appointment.setStartTime(request.getStartTime());
    appointment.setEndTime(request.getEndTime());
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setNotes(request.getNotes());
    appointment.setIsVirtual(request.getIsVirtual());

    // Step 2e: Save to database
    Appointment savedAppointment = appointmentRepository.save(appointment);
    log.info("Appointment created with ID: {}", savedAppointment.getId());

    // Step 2f: Update time slot status to BOOKED
    updateTimeSlotStatus(
        request.getResourceId(),
        request.getStartTime(),
        request.getEndTime(),
        TimeSlotStatus.BOOKED
    );
    log.info("Time slots updated to BOOKED");

    // Step 2g: Create history entry
    createHistoryEntry(
        savedAppointment.getId(),
        "CREATED",
        null,
        AppointmentStatus.SCHEDULED.name(),
        "Appointment created"
    );
    log.info("History entry created");

    // Step 2h: Publish event to Kafka
    publishAppointmentCreatedEvent(savedAppointment);
    log.info("AppointmentCreatedEvent published to Kafka");

    return mapToResponse(savedAppointment);
}

// 3. Validate references by calling Master Data Service
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:245
private void validateReferences(CreateAppointmentRequest request) {
    // Validate Appointment Type exists
    String appointmentTypeUrl = "http://master-data-service/api/v1/appointment-types/"
                                + request.getAppointmentTypeId();
    try {
        restTemplate.getForObject(appointmentTypeUrl, Object.class);
        log.debug("Appointment type validated: {}", request.getAppointmentTypeId());
    } catch (HttpClientErrorException.NotFound e) {
        throw new ResourceNotFoundException("Appointment type not found: " + request.getAppointmentTypeId());
    }

    // Validate Resource exists
    String resourceUrl = "http://master-data-service/api/v1/resources/" + request.getResourceId();
    try {
        restTemplate.getForObject(resourceUrl, Object.class);
        log.debug("Resource validated: {}", request.getResourceId());
    } catch (HttpClientErrorException.NotFound e) {
        throw new ResourceNotFoundException("Resource not found: " + request.getResourceId());
    }

    // Validate Location exists
    String locationUrl = "http://master-data-service/api/v1/locations/" + request.getLocationId();
    try {
        restTemplate.getForObject(locationUrl, Object.class);
        log.debug("Location validated: {}", request.getLocationId());
    } catch (HttpClientErrorException.NotFound e) {
        throw new ResourceNotFoundException("Location not found: " + request.getLocationId());
    }
}

// 4. Check for conflicts in database
// scheduling-service/src/main/java/com/appointment/scheduling/repository/AppointmentRepository.java:27
@Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.resourceId = :resourceId
    AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
    AND (
        (a.startTime < :endTime AND a.endTime > :startTime)
    )
""")
boolean existsConflictingAppointment(
    @Param("resourceId") UUID resourceId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime
);

// 5. Generate unique appointment number
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:260
private String generateAppointmentNumber() {
    // Format: APT-YYYYMMDD-NNNN
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long count = appointmentRepository.count();
    return String.format("APT-%s-%04d", date, count + 1);
}

// 6. Update time slot status
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:270
private void updateTimeSlotStatus(UUID resourceId, LocalDateTime startTime,
                                  LocalDateTime endTime, TimeSlotStatus status) {
    List<TimeSlot> slots = timeSlotRepository.findByResourceIdAndTimeRange(
        resourceId, startTime, endTime
    );

    for (TimeSlot slot : slots) {
        slot.setStatus(status);
    }

    timeSlotRepository.saveAll(slots);
}

// 7. Create history entry
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:285
private void createHistoryEntry(UUID appointmentId, String action,
                               String oldValue, String newValue, String notes) {
    AppointmentHistory history = new AppointmentHistory();
    history.setAppointmentId(appointmentId);
    history.setAction(action);
    history.setOldValue(oldValue);
    history.setNewValue(newValue);
    history.setChangedBy("SYSTEM"); // Or from authentication context
    history.setNotes(notes);

    appointmentHistoryRepository.save(history);
}

// 8. Publish event to Kafka
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:300
private void publishAppointmentCreatedEvent(Appointment appointment) {
    AppointmentCreatedEvent event = new AppointmentCreatedEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setAppointmentId(appointment.getId().toString());
    event.setEventTimestamp(LocalDateTime.now());
    event.setEventType("APPOINTMENT_CREATED");

    // Set event-specific fields
    event.setPatientId(appointment.getPatientId());
    event.setResourceId(appointment.getResourceId().toString());
    event.setAppointmentTypeId(appointment.getAppointmentTypeId().toString());
    event.setLocationId(appointment.getLocationId().toString());
    event.setStartTime(appointment.getStartTime());
    event.setEndTime(appointment.getEndTime());
    event.setStatus(appointment.getStatus().name());
    event.setIsVirtual(appointment.getIsVirtual());

    eventPublisher.publishEvent(event);
}

// 9. EventPublisher sends to Kafka
// scheduling-service/src/main/java/com/appointment/scheduling/service/EventPublisher.java:21
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private static final String TOPIC = "appointment-events";
    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;

    public void publishEvent(AppointmentEvent event) {
        log.info("Publishing event: {} for appointment: {}",
                 event.getEventType(), event.getAppointmentId());

        CompletableFuture<SendResult<String, AppointmentEvent>> future =
            kafkaTemplate.send(TOPIC, event.getAppointmentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published event: {} to partition: {} with offset: {}",
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: {} - Error: {}",
                         event.getEventType(), ex.getMessage());
            }
        });
    }
}
```

**Database Changes:**

```sql
-- 1. Insert into appointments table (scheduling_db)
INSERT INTO appointments (
  id, appointment_number, patient_id, resource_id, location_id,
  appointment_type_id, start_time, end_time, status, notes,
  is_virtual, created_at, updated_at
) VALUES (
  '990e8400-e29b-41d4-a716-446655440100',
  'APT-20240115-0001',
  'patient-12345',
  '770e8400-e29b-41d4-a716-446655440003',
  '660e8400-e29b-41d4-a716-446655440002',
  '550e8400-e29b-41d4-a716-446655440001',
  '2024-01-20 10:00:00',
  '2024-01-20 10:30:00',
  'SCHEDULED',
  'Patient complains of headaches',
  false,
  '2024-01-15 10:30:00',
  '2024-01-15 10:30:00'
);

-- 2. Update time_slots status (scheduling_db)
UPDATE time_slots
SET status = 'BOOKED', updated_at = '2024-01-15 10:30:00'
WHERE resource_id = '770e8400-e29b-41d4-a716-446655440003'
  AND start_time >= '2024-01-20 10:00:00'
  AND end_time <= '2024-01-20 10:30:00';

-- 3. Insert into appointment_history table (scheduling_db)
INSERT INTO appointment_history (
  id, appointment_id, action, old_value, new_value,
  changed_by, changed_at, notes
) VALUES (
  'aa0e8400-e29b-41d4-a716-446655440200',
  '990e8400-e29b-41d4-a716-446655440100',
  'CREATED',
  NULL,
  'SCHEDULED',
  'SYSTEM',
  '2024-01-15 10:30:00',
  'Appointment created'
);
```

**Kafka Event Published:**

```json
{
  "eventId": "evt-550e8400-e29b-41d4-a716-123456789abc",
  "appointmentId": "990e8400-e29b-41d4-a716-446655440100",
  "eventTimestamp": "2024-01-15T10:30:00",
  "eventType": "APPOINTMENT_CREATED",
  "patientId": "patient-12345",
  "resourceId": "770e8400-e29b-41d4-a716-446655440003",
  "appointmentTypeId": "550e8400-e29b-41d4-a716-446655440001",
  "locationId": "660e8400-e29b-41d4-a716-446655440002",
  "startTime": "2024-01-20T10:00:00",
  "endTime": "2024-01-20T10:30:00",
  "status": "SCHEDULED",
  "isVirtual": false
}
```

**Response:**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440100",
  "appointmentNumber": "APT-20240115-0001",
  "patientId": "patient-12345",
  "resourceId": "770e8400-e29b-41d4-a716-446655440003",
  "locationId": "660e8400-e29b-41d4-a716-446655440002",
  "appointmentTypeId": "550e8400-e29b-41d4-a716-446655440001",
  "startTime": "2024-01-20T10:00:00",
  "endTime": "2024-01-20T10:30:00",
  "duration": 30,
  "status": "SCHEDULED",
  "notes": "Patient complains of headaches",
  "isVirtual": false,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### STEP 3: Confirm Appointment

**Purpose:** After payment or verification, confirm the appointment. This triggers encounter creation.

**API Call:**
```bash
curl -X PUT http://localhost:8080/api/v1/scheduling/appointments/990e8400-e29b-41d4-a716-446655440100/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "confirmedBy": "receptionist-001"
  }'
```

**Service Called:** API Gateway → Scheduling Service

**Complete Code Execution Path:**

```java
// 1. Request arrives at controller
// scheduling-service/src/main/java/com/appointment/scheduling/controller/AppointmentController.java:50
@PutMapping("/{id}/confirm")
public ResponseEntity<AppointmentResponse> confirmAppointment(
    @PathVariable UUID id,
    @RequestBody ConfirmAppointmentRequest request
) {
    log.info("Confirming appointment: {}", id);
    AppointmentResponse response = appointmentService.confirmAppointment(id, request);
    return ResponseEntity.ok(response);
}

// 2. Service layer confirms appointment
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:125
@Transactional
public AppointmentResponse confirmAppointment(UUID id, ConfirmAppointmentRequest request) {
    log.info("Confirming appointment: {}", id);

    // Step 3a: Find appointment
    Appointment appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

    // Step 3b: Validate current status
    if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
        throw new BadRequestException("Appointment is already confirmed");
    }

    if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
        throw new BadRequestException("Cannot confirm a cancelled appointment");
    }

    // Step 3c: Update status
    AppointmentStatus oldStatus = appointment.getStatus();
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    appointment.setConfirmedAt(LocalDateTime.now());
    appointment.setConfirmedBy(request.getConfirmedBy());

    // Step 3d: Save to database
    Appointment confirmedAppointment = appointmentRepository.save(appointment);
    log.info("Appointment {} confirmed", id);

    // Step 3e: Create history entry
    createHistoryEntry(
        id,
        "STATUS_CHANGED",
        oldStatus.name(),
        AppointmentStatus.CONFIRMED.name(),
        "Appointment confirmed by " + request.getConfirmedBy()
    );
    log.info("History entry created for confirmation");

    // Step 3f: Publish AppointmentConfirmedEvent to Kafka
    // THIS IS THE KEY EVENT THAT TRIGGERS ENCOUNTER CREATION
    publishAppointmentConfirmedEvent(confirmedAppointment);
    log.info("AppointmentConfirmedEvent published to Kafka");

    return mapToResponse(confirmedAppointment);
}

// 3. Publish confirmation event
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:330
private void publishAppointmentConfirmedEvent(Appointment appointment) {
    AppointmentConfirmedEvent event = new AppointmentConfirmedEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setAppointmentId(appointment.getId().toString());
    event.setEventTimestamp(LocalDateTime.now());
    event.setEventType("APPOINTMENT_CONFIRMED");

    // Set event-specific fields needed for encounter creation
    event.setPatientId(appointment.getPatientId());
    event.setResourceId(appointment.getResourceId().toString());
    event.setStartTime(appointment.getStartTime());
    event.setEndTime(appointment.getEndTime());
    event.setConfirmedAt(appointment.getConfirmedAt());
    event.setConfirmedBy(appointment.getConfirmedBy());

    eventPublisher.publishEvent(event);
}
```

**Database Changes:**

```sql
-- 1. Update appointment status (scheduling_db)
UPDATE appointments
SET status = 'CONFIRMED',
    confirmed_at = '2024-01-15 11:00:00',
    confirmed_by = 'receptionist-001',
    updated_at = '2024-01-15 11:00:00'
WHERE id = '990e8400-e29b-41d4-a716-446655440100';

-- 2. Insert history entry (scheduling_db)
INSERT INTO appointment_history (
  id, appointment_id, action, old_value, new_value,
  changed_by, changed_at, notes
) VALUES (
  'bb0e8400-e29b-41d4-a716-446655440201',
  '990e8400-e29b-41d4-a716-446655440100',
  'STATUS_CHANGED',
  'SCHEDULED',
  'CONFIRMED',
  'SYSTEM',
  '2024-01-15 11:00:00',
  'Appointment confirmed by receptionist-001'
);
```

**Kafka Event Published:**

```json
{
  "eventId": "evt-660e8400-e29b-41d4-a716-987654321xyz",
  "appointmentId": "990e8400-e29b-41d4-a716-446655440100",
  "eventTimestamp": "2024-01-15T11:00:00",
  "eventType": "APPOINTMENT_CONFIRMED",
  "patientId": "patient-12345",
  "resourceId": "770e8400-e29b-41d4-a716-446655440003",
  "startTime": "2024-01-20T10:00:00",
  "endTime": "2024-01-20T10:30:00",
  "confirmedAt": "2024-01-15T11:00:00",
  "confirmedBy": "receptionist-001"
}
```

**Response:**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440100",
  "appointmentNumber": "APT-20240115-0001",
  "patientId": "patient-12345",
  "resourceId": "770e8400-e29b-41d4-a716-446655440003",
  "locationId": "660e8400-e29b-41d4-a716-446655440002",
  "appointmentTypeId": "550e8400-e29b-41d4-a716-446655440001",
  "startTime": "2024-01-20T10:00:00",
  "endTime": "2024-01-20T10:30:00",
  "duration": 30,
  "status": "CONFIRMED",
  "confirmedAt": "2024-01-15T11:00:00",
  "confirmedBy": "receptionist-001",
  "notes": "Patient complains of headaches",
  "isVirtual": false,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

---

### STEP 4: Encounter Service Consumes Event (Automatic/Asynchronous)

**Purpose:** When an appointment is confirmed, automatically create an encounter for the visit.

**This happens automatically - no API call needed!**

**Event-Driven Processing:**

```java
// 1. Kafka delivers event to Encounter Service consumer
// encounter-service/src/main/java/com/appointment/encounter/listener/AppointmentEventListener.java:29

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final EncounterService encounterService;

    // Kafka listener for appointment-events topic
    @KafkaListener(
        topics = "appointment-events",
        groupId = "encounter-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentEvent(@Payload AppointmentEvent event) {
        log.info("Received event: {} for appointment: {}",
                 event.getEventType(), event.getAppointmentId());

        // Route to appropriate handler based on event type
        switch (event.getEventType()) {
            case "APPOINTMENT_CONFIRMED":
                handleAppointmentConfirmed((AppointmentConfirmedEvent) event);
                break;
            case "APPOINTMENT_CHECKED_IN":
                handleAppointmentCheckedIn((AppointmentCheckedInEvent) event);
                break;
            case "APPOINTMENT_COMPLETED":
                handleAppointmentCompleted((AppointmentCompletedEvent) event);
                break;
            case "APPOINTMENT_CANCELLED":
                handleAppointmentCancelled((AppointmentCancelledEvent) event);
                break;
            default:
                log.debug("Event type {} does not require encounter action", event.getEventType());
        }
    }

    // 2. Handle AppointmentConfirmedEvent specifically
    private void handleAppointmentConfirmed(AppointmentConfirmedEvent event) {
        log.info("Processing AppointmentConfirmedEvent for appointment: {}",
                 event.getAppointmentId());

        try {
            // Create encounter from the event data
            encounterService.createEncounterFromAppointment(
                UUID.fromString(event.getAppointmentId()),
                UUID.fromString(event.getPatientId()),
                UUID.fromString(event.getResourceId()),
                null, // locationId not in event
                event.getStartTime(),
                null  // serviceType not in event
            );

            log.info("Successfully created encounter for appointment: {}",
                     event.getAppointmentId());

        } catch (Exception e) {
            log.error("Failed to create encounter for appointment {}: {}",
                     event.getAppointmentId(), e.getMessage(), e);
            // In production, you might want to:
            // - Retry the operation
            // - Send to dead letter queue
            // - Trigger alert/notification
        }
    }
}

// 3. Service creates encounter
// encounter-service/src/main/java/com/appointment/encounter/service/EncounterService.java:85
@Transactional
public EncounterResponse createEncounterFromAppointment(
    UUID appointmentId,
    UUID patientId,
    UUID practitionerId,
    UUID locationId,
    LocalDateTime plannedStartDate,
    String serviceType
) {
    log.info("Creating encounter from appointment: {}", appointmentId);

    // Step 4a: Check if encounter already exists for this appointment
    if (encounterRepository.existsByAppointmentId(appointmentId)) {
        log.warn("Encounter already exists for appointment: {}", appointmentId);
        // Return existing encounter instead of creating duplicate
        Encounter existing = encounterRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new RuntimeException("Concurrent modification detected"));
        return mapToResponse(existing);
    }

    // Step 4b: Create new encounter entity
    Encounter encounter = new Encounter();
    encounter.setEncounterNumber(generateEncounterNumber());
    encounter.setAppointmentId(appointmentId);
    encounter.setPatientId(patientId);
    encounter.setPractitionerId(practitionerId);
    encounter.setLocationId(locationId);
    encounter.setStatus(EncounterStatus.PLANNED);  // Initial status
    encounter.setEncounterClass("ambulatory");     // Outpatient visit
    encounter.setServiceType(serviceType);
    encounter.setPlannedStartDate(plannedStartDate);
    encounter.setPlannedEndDate(plannedStartDate != null ?
                                plannedStartDate.plusMinutes(30) : null);
    encounter.setPriority("routine");

    // Step 4c: Save to database
    Encounter savedEncounter = encounterRepository.save(encounter);
    log.info("Encounter created with ID: {} for appointment: {}",
             savedEncounter.getId(), appointmentId);

    // Step 4d: Create history entry
    createHistoryEntry(
        savedEncounter.getId(),
        "CREATED",
        null,
        EncounterStatus.PLANNED.name(),
        "Encounter created from confirmed appointment"
    );
    log.info("Encounter history entry created");

    return mapToResponse(savedEncounter);
}

// 4. Generate unique encounter number
// encounter-service/src/main/java/com/appointment/encounter/service/EncounterService.java:215
private String generateEncounterNumber() {
    // Format: ENC-YYYYMMDD-NNNN
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long count = encounterRepository.count();
    return String.format("ENC-%s-%04d", date, count + 1);
}

// 5. Create history entry
// encounter-service/src/main/java/com/appointment/encounter/service/EncounterService.java:225
private void createHistoryEntry(UUID encounterId, String action,
                               String oldValue, String newValue, String notes) {
    EncounterHistory history = new EncounterHistory();
    history.setEncounterId(encounterId);
    history.setAction(action);
    history.setOldValue(oldValue);
    history.setNewValue(newValue);
    history.setChangedBy("SYSTEM");
    history.setNotes(notes);

    encounterHistoryRepository.save(history);
}
```

**Database Changes (in encounter_db):**

```sql
-- 1. Insert into encounters table
INSERT INTO encounters (
  id, encounter_number, appointment_id, patient_id, practitioner_id,
  location_id, status, encounter_class, service_type,
  planned_start_date, planned_end_date, priority,
  created_at, updated_at
) VALUES (
  'cc0e8400-e29b-41d4-a716-446655440300',
  'ENC-20240115-0001',
  '990e8400-e29b-41d4-a716-446655440100',
  'patient-12345',
  '770e8400-e29b-41d4-a716-446655440003',
  NULL,
  'PLANNED',
  'ambulatory',
  NULL,
  '2024-01-20 10:00:00',
  '2024-01-20 10:30:00',
  'routine',
  '2024-01-15 11:00:05',
  '2024-01-15 11:00:05'
);

-- 2. Insert into encounter_history table
INSERT INTO encounter_history (
  id, encounter_id, action, old_value, new_value,
  changed_by, changed_at, notes
) VALUES (
  'dd0e8400-e29b-41d4-a716-446655440400',
  'cc0e8400-e29b-41d4-a716-446655440300',
  'CREATED',
  NULL,
  'PLANNED',
  'SYSTEM',
  '2024-01-15 11:00:05',
  'Encounter created from confirmed appointment'
);
```

**Log Output (Encounter Service):**

```
2024-01-15 11:00:05.123 INFO  [encounter-service-group] AppointmentEventListener : Received event: APPOINTMENT_CONFIRMED for appointment: 990e8400-e29b-41d4-a716-446655440100
2024-01-15 11:00:05.125 INFO  [encounter-service-group] AppointmentEventListener : Processing AppointmentConfirmedEvent for appointment: 990e8400-e29b-41d4-a716-446655440100
2024-01-15 11:00:05.130 INFO  [encounter-service-group] EncounterService : Creating encounter from appointment: 990e8400-e29b-41d4-a716-446655440100
2024-01-15 11:00:05.245 INFO  [encounter-service-group] EncounterService : Encounter created with ID: cc0e8400-e29b-41d4-a716-446655440300 for appointment: 990e8400-e29b-41d4-a716-446655440100
2024-01-15 11:00:05.250 INFO  [encounter-service-group] EncounterService : Encounter history entry created
2024-01-15 11:00:05.255 INFO  [encounter-service-group] AppointmentEventListener : Successfully created encounter for appointment: 990e8400-e29b-41d4-a716-446655440100
```

---

### STEP 5: Query Encounter

**Purpose:** Verify that the encounter was created successfully.

**API Call:**
```bash
# Get encounter by appointment ID
curl -X GET "http://localhost:8080/api/v1/encounters/appointment/990e8400-e29b-41d4-a716-446655440100" \
  -H "Accept: application/json"
```

**Service Called:** API Gateway → Encounter Service

**Code Execution Path:**

```java
// 1. Request arrives at Encounter Service
// encounter-service/src/main/java/com/appointment/encounter/controller/EncounterController.java:60
@GetMapping("/appointment/{appointmentId}")
public ResponseEntity<EncounterResponse> getEncounterByAppointment(
    @PathVariable UUID appointmentId
) {
    log.info("Fetching encounter for appointment: {}", appointmentId);
    EncounterResponse response = encounterService.getEncounterByAppointment(appointmentId);
    return ResponseEntity.ok(response);
}

// 2. Service layer queries database
// encounter-service/src/main/java/com/appointment/encounter/service/EncounterService.java:195
public EncounterResponse getEncounterByAppointment(UUID appointmentId) {
    Encounter encounter = encounterRepository.findByAppointmentId(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Encounter not found for appointment: " + appointmentId));

    return mapToResponse(encounter);
}

// 3. Repository executes query
// encounter-service/src/main/java/com/appointment/encounter/repository/EncounterRepository.java:15
@Query("SELECT e FROM Encounter e WHERE e.appointmentId = :appointmentId")
Optional<Encounter> findByAppointmentId(@Param("appointmentId") UUID appointmentId);
```

**SQL Executed:**
```sql
SELECT * FROM encounters
WHERE appointment_id = '990e8400-e29b-41d4-a716-446655440100';
```

**Response:**
```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440300",
  "encounterNumber": "ENC-20240115-0001",
  "appointmentId": "990e8400-e29b-41d4-a716-446655440100",
  "patientId": "patient-12345",
  "practitionerId": "770e8400-e29b-41d4-a716-446655440003",
  "locationId": null,
  "status": "PLANNED",
  "encounterClass": "ambulatory",
  "serviceType": null,
  "priority": "routine",
  "plannedStartDate": "2024-01-20T10:00:00",
  "plannedEndDate": "2024-01-20T10:30:00",
  "actualStartDate": null,
  "actualEndDate": null,
  "reasonCode": null,
  "reasonDescription": null,
  "diagnosis": null,
  "createdAt": "2024-01-15T11:00:05",
  "updatedAt": "2024-01-15T11:00:05"
}
```

---

### STEP 6: Check-in Patient (Day of Appointment)

**Purpose:** When patient arrives for appointment on January 20, mark them as checked in.

**API Call:**
```bash
curl -X PUT http://localhost:8080/api/v1/scheduling/appointments/990e8400-e29b-41d4-a716-446655440100/check-in \
  -H "Content-Type: application/json"
```

**Service Called:** API Gateway → Scheduling Service → (Event) → Encounter Service

**Code Execution Path:**

```java
// 1. Scheduling Service receives check-in request
// scheduling-service/src/main/java/com/appointment/scheduling/controller/AppointmentController.java:60
@PutMapping("/{id}/check-in")
public ResponseEntity<AppointmentResponse> checkInAppointment(@PathVariable UUID id) {
    log.info("Checking in appointment: {}", id);
    AppointmentResponse response = appointmentService.checkInAppointment(id);
    return ResponseEntity.ok(response);
}

// 2. Service updates appointment status
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:155
@Transactional
public AppointmentResponse checkInAppointment(UUID id) {
    Appointment appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

    if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
        throw new BadRequestException("Only confirmed appointments can be checked in");
    }

    AppointmentStatus oldStatus = appointment.getStatus();
    appointment.setStatus(AppointmentStatus.CHECKED_IN);
    appointment.setCheckedInAt(LocalDateTime.now());

    appointmentRepository.save(appointment);

    createHistoryEntry(id, "STATUS_CHANGED", oldStatus.name(),
                      AppointmentStatus.CHECKED_IN.name(), "Patient checked in");

    // Publish event to update encounter status
    publishAppointmentCheckedInEvent(appointment);

    return mapToResponse(appointment);
}

// 3. Kafka event consumed by Encounter Service
// encounter-service/src/main/java/com/appointment/encounter/listener/AppointmentEventListener.java:60
private void handleAppointmentCheckedIn(AppointmentCheckedInEvent event) {
    try {
        encounterService.updateEncounterStatus(
            UUID.fromString(event.getAppointmentId()),
            EncounterStatus.IN_PROGRESS
        );
        log.info("Updated encounter to IN_PROGRESS for appointment: {}",
                 event.getAppointmentId());
    } catch (Exception e) {
        log.error("Failed to update encounter status: {}", e.getMessage());
    }
}

// 4. Encounter status updated
// encounter-service/src/main/java/com/appointment/encounter/service/EncounterService.java:135
@Transactional
public EncounterResponse updateEncounterStatus(UUID appointmentId, EncounterStatus newStatus) {
    Encounter encounter = encounterRepository.findByAppointmentId(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

    EncounterStatus oldStatus = encounter.getStatus();
    encounter.setStatus(newStatus);

    if (newStatus == EncounterStatus.IN_PROGRESS && encounter.getActualStartDate() == null) {
        encounter.setActualStartDate(LocalDateTime.now());
    }

    encounterRepository.save(encounter);

    createHistoryEntry(encounter.getId(), "STATUS_CHANGED",
                      oldStatus.name(), newStatus.name(),
                      "Status updated from appointment check-in");

    return mapToResponse(encounter);
}
```

**Database Changes:**

```sql
-- In scheduling_db
UPDATE appointments
SET status = 'CHECKED_IN',
    checked_in_at = '2024-01-20 10:00:00',
    updated_at = '2024-01-20 10:00:00'
WHERE id = '990e8400-e29b-41d4-a716-446655440100';

-- In encounter_db
UPDATE encounters
SET status = 'IN_PROGRESS',
    actual_start_date = '2024-01-20 10:00:00',
    updated_at = '2024-01-20 10:00:00'
WHERE appointment_id = '990e8400-e29b-41d4-a716-446655440100';
```

---

### STEP 7: Complete Appointment

**Purpose:** After the visit is finished, mark appointment and encounter as complete.

**API Call:**
```bash
curl -X PUT http://localhost:8080/api/v1/scheduling/appointments/990e8400-e29b-41d4-a716-446655440100/complete \
  -H "Content-Type: application/json"
```

**Service Called:** API Gateway → Scheduling Service → (Event) → Encounter Service

**Code Execution Path:**

```java
// 1. Scheduling Service completes appointment
// scheduling-service/src/main/java/com/appointment/scheduling/service/AppointmentService.java:185
@Transactional
public AppointmentResponse completeAppointment(UUID id) {
    Appointment appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

    AppointmentStatus oldStatus = appointment.getStatus();
    appointment.setStatus(AppointmentStatus.COMPLETED);
    appointment.setCompletedAt(LocalDateTime.now());

    appointmentRepository.save(appointment);

    // Free up the time slot
    updateTimeSlotStatus(appointment.getResourceId(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        TimeSlotStatus.AVAILABLE);

    createHistoryEntry(id, "STATUS_CHANGED", oldStatus.name(),
                      AppointmentStatus.COMPLETED.name(), "Appointment completed");

    publishAppointmentCompletedEvent(appointment);

    return mapToResponse(appointment);
}

// 2. Encounter Service updates to FINISHED
// encounter-service/src/main/java/com/appointment/encounter/listener/AppointmentEventListener.java:75
private void handleAppointmentCompleted(AppointmentCompletedEvent event) {
    try {
        Encounter encounter = encounterRepository
            .findByAppointmentId(UUID.fromString(event.getAppointmentId()))
            .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        encounter.setStatus(EncounterStatus.FINISHED);
        encounter.setActualEndDate(LocalDateTime.now());

        encounterRepository.save(encounter);

        log.info("Updated encounter to FINISHED for appointment: {}",
                 event.getAppointmentId());
    } catch (Exception e) {
        log.error("Failed to complete encounter: {}", e.getMessage());
    }
}
```

**Database Changes:**

```sql
-- In scheduling_db
UPDATE appointments
SET status = 'COMPLETED',
    completed_at = '2024-01-20 10:30:00',
    updated_at = '2024-01-20 10:30:00'
WHERE id = '990e8400-e29b-41d4-a716-446655440100';

UPDATE time_slots
SET status = 'AVAILABLE',
    updated_at = '2024-01-20 10:30:00'
WHERE resource_id = '770e8400-e29b-41d4-a716-446655440003'
  AND start_time >= '2024-01-20 10:00:00'
  AND end_time <= '2024-01-20 10:30:00';

-- In encounter_db
UPDATE encounters
SET status = 'FINISHED',
    actual_end_date = '2024-01-20 10:30:00',
    updated_at = '2024-01-20 10:30:00'
WHERE appointment_id = '990e8400-e29b-41d4-a716-446655440100';
```

---

## Event Flow Through Kafka

### Kafka Topic: appointment-events

All appointment lifecycle events flow through this single Kafka topic.

**Topic Configuration:**
```yaml
# In docker-compose.yml
KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
KAFKA_NUM_PARTITIONS: 3
KAFKA_DEFAULT_REPLICATION_FACTOR: 1
```

**Event Types and Consumers:**

| Event Type | Producer | Consumer | Action |
|------------|----------|----------|--------|
| APPOINTMENT_CREATED | Scheduling Service | (None - informational) | Log creation |
| APPOINTMENT_CONFIRMED | Scheduling Service | **Encounter Service** | **Create encounter** |
| APPOINTMENT_RESCHEDULED | Scheduling Service | Encounter Service | Update planned dates |
| APPOINTMENT_CHECKED_IN | Scheduling Service | Encounter Service | Update to IN_PROGRESS |
| APPOINTMENT_COMPLETED | Scheduling Service | Encounter Service | Update to FINISHED |
| APPOINTMENT_CANCELLED | Scheduling Service | Encounter Service | Update to CANCELLED |

**Kafka Consumer Group:**
```
Group ID: encounter-service-group
Members: All encounter-service instances (scales horizontally)
```

**Monitoring Kafka:**

```bash
# List topics
docker exec -it appointment-kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages in topic
docker exec -it appointment-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic appointment-events \
  --from-beginning

# Check consumer group status
docker exec -it appointment-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group encounter-service-group
```

---

## Database Changes

### Final State After Complete Lifecycle

**scheduling_db.appointments:**
```sql
SELECT * FROM appointments WHERE id = '990e8400-e29b-41d4-a716-446655440100';

| id         | appointment_number | patient_id     | resource_id    | status    | confirmed_at        | checked_in_at       | completed_at        |
|------------|-------------------|----------------|----------------|-----------|---------------------|---------------------|---------------------|
| 990e8400...| APT-20240115-0001 | patient-12345  | 770e8400...003 | COMPLETED | 2024-01-15 11:00:00 | 2024-01-20 10:00:00 | 2024-01-20 10:30:00 |
```

**scheduling_db.appointment_history:**
```sql
SELECT * FROM appointment_history WHERE appointment_id = '990e8400-e29b-41d4-a716-446655440100' ORDER BY changed_at;

| action         | old_value  | new_value   | changed_at          | notes                          |
|----------------|------------|-------------|---------------------|--------------------------------|
| CREATED        | NULL       | SCHEDULED   | 2024-01-15 10:30:00 | Appointment created            |
| STATUS_CHANGED | SCHEDULED  | CONFIRMED   | 2024-01-15 11:00:00 | Appointment confirmed by ...   |
| STATUS_CHANGED | CONFIRMED  | CHECKED_IN  | 2024-01-20 10:00:00 | Patient checked in             |
| STATUS_CHANGED | CHECKED_IN | COMPLETED   | 2024-01-20 10:30:00 | Appointment completed          |
```

**scheduling_db.time_slots:**
```sql
SELECT * FROM time_slots
WHERE resource_id = '770e8400-e29b-41d4-a716-446655440003'
  AND start_time = '2024-01-20 10:00:00';

| id         | resource_id    | start_time           | end_time             | status    |
|------------|----------------|----------------------|----------------------|-----------|
| 880e8400...| 770e8400...003 | 2024-01-20 10:00:00  | 2024-01-20 10:30:00  | AVAILABLE |
```
*(Status returns to AVAILABLE after appointment completion)*

**encounter_db.encounters:**
```sql
SELECT * FROM encounters WHERE appointment_id = '990e8400-e29b-41d4-a716-446655440100';

| id         | encounter_number  | appointment_id | patient_id     | status   | planned_start_date  | actual_start_date   | actual_end_date     |
|------------|-------------------|----------------|----------------|----------|---------------------|---------------------|---------------------|
| cc0e8400...| ENC-20240115-0001 | 990e8400...100 | patient-12345  | FINISHED | 2024-01-20 10:00:00 | 2024-01-20 10:00:00 | 2024-01-20 10:30:00 |
```

**encounter_db.encounter_history:**
```sql
SELECT * FROM encounter_history WHERE encounter_id = 'cc0e8400-e29b-41d4-a716-446655440300' ORDER BY changed_at;

| action         | old_value   | new_value    | changed_at          | notes                                  |
|----------------|-------------|--------------|---------------------|----------------------------------------|
| CREATED        | NULL        | PLANNED      | 2024-01-15 11:00:05 | Encounter created from confirmed appt  |
| STATUS_CHANGED | PLANNED     | IN_PROGRESS  | 2024-01-20 10:00:00 | Status updated from appointment check-in|
| STATUS_CHANGED | IN_PROGRESS | FINISHED     | 2024-01-20 10:30:00 | Encounter completed                    |
```

---

## Testing the Complete Flow

### Automated Test Script

Create a file `test-complete-lifecycle.sh`:

```bash
#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

API_BASE="http://localhost:8080"

echo -e "${BLUE}Starting Complete Appointment Lifecycle Test${NC}\n"

# Step 1: Create Appointment Type
echo -e "${GREEN}Step 1: Creating Appointment Type...${NC}"
APPOINTMENT_TYPE_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/appointment-types" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "General Consultation",
    "code": "CONSULT-GEN",
    "durationMinutes": 30,
    "isActive": true
  }')
APPOINTMENT_TYPE_ID=$(echo $APPOINTMENT_TYPE_RESPONSE | jq -r '.id')
echo "Appointment Type ID: $APPOINTMENT_TYPE_ID"
echo ""

# Step 2: Create Location
echo -e "${GREEN}Step 2: Creating Location...${NC}"
LOCATION_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/locations" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Main Clinic",
    "code": "LOC-001",
    "isActive": true
  }')
LOCATION_ID=$(echo $LOCATION_RESPONSE | jq -r '.id')
echo "Location ID: $LOCATION_ID"
echo ""

# Step 3: Create Resource
echo -e "${GREEN}Step 3: Creating Resource (Doctor)...${NC}"
RESOURCE_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/resources" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "DR-SMITH-001",
    "firstName": "John",
    "lastName": "Smith",
    "specialty": "General Medicine",
    "isActive": true
  }')
RESOURCE_ID=$(echo $RESOURCE_RESPONSE | jq -r '.id')
echo "Resource ID: $RESOURCE_ID"
echo ""

# Step 4: Create Time Slots
echo -e "${GREEN}Step 4: Creating Time Slots...${NC}"
curl -s -X POST "$API_BASE/api/v1/scheduling/time-slots/bulk" \
  -H "Content-Type: application/json" \
  -d "{
    \"resourceId\": \"$RESOURCE_ID\",
    \"locationId\": \"$LOCATION_ID\",
    \"startDate\": \"2024-01-20\",
    \"endDate\": \"2024-01-20\",
    \"startTime\": \"09:00\",
    \"endTime\": \"17:00\",
    \"slotDuration\": 30
  }" | jq '.'
echo ""

# Step 5: Search Available Slots
echo -e "${GREEN}Step 5: Searching Available Time Slots...${NC}"
curl -s -X GET "$API_BASE/api/v1/scheduling/time-slots/available?resourceId=$RESOURCE_ID&date=2024-01-20" | jq '.[0:3]'
echo ""

# Step 6: Create Appointment
echo -e "${GREEN}Step 6: Creating Appointment...${NC}"
APPOINTMENT_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/scheduling/appointments" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"patient-12345\",
    \"resourceId\": \"$RESOURCE_ID\",
    \"locationId\": \"$LOCATION_ID\",
    \"appointmentTypeId\": \"$APPOINTMENT_TYPE_ID\",
    \"startTime\": \"2024-01-20T10:00:00\",
    \"endTime\": \"2024-01-20T10:30:00\",
    \"status\": \"SCHEDULED\",
    \"notes\": \"Test appointment\"
  }")
APPOINTMENT_ID=$(echo $APPOINTMENT_RESPONSE | jq -r '.id')
echo $APPOINTMENT_RESPONSE | jq '.'
echo ""

# Step 7: Confirm Appointment
echo -e "${GREEN}Step 7: Confirming Appointment (triggers encounter creation)...${NC}"
curl -s -X PUT "$API_BASE/api/v1/scheduling/appointments/$APPOINTMENT_ID/confirm" \
  -H "Content-Type: application/json" \
  -d '{"confirmedBy": "receptionist-001"}' | jq '.'
echo ""

# Wait for async processing
echo "Waiting 2 seconds for encounter creation..."
sleep 2

# Step 8: Query Encounter
echo -e "${GREEN}Step 8: Querying Created Encounter...${NC}"
curl -s -X GET "$API_BASE/api/v1/encounters/appointment/$APPOINTMENT_ID" | jq '.'
echo ""

# Step 9: Check-in
echo -e "${GREEN}Step 9: Checking in Patient...${NC}"
curl -s -X PUT "$API_BASE/api/v1/scheduling/appointments/$APPOINTMENT_ID/check-in" | jq '.'
echo ""

sleep 2

# Step 10: Query Updated Encounter
echo -e "${GREEN}Step 10: Querying Updated Encounter (should be IN_PROGRESS)...${NC}"
curl -s -X GET "$API_BASE/api/v1/encounters/appointment/$APPOINTMENT_ID" | jq '.status'
echo ""

# Step 11: Complete Appointment
echo -e "${GREEN}Step 11: Completing Appointment...${NC}"
curl -s -X PUT "$API_BASE/api/v1/scheduling/appointments/$APPOINTMENT_ID/complete" | jq '.'
echo ""

sleep 2

# Step 12: Final Encounter Status
echo -e "${GREEN}Step 12: Final Encounter Status (should be FINISHED)...${NC}"
curl -s -X GET "$API_BASE/api/v1/encounters/appointment/$APPOINTMENT_ID" | jq '.'
echo ""

echo -e "${BLUE}Complete Lifecycle Test Finished!${NC}"
```

**Run the test:**
```bash
chmod +x test-complete-lifecycle.sh
./test-complete-lifecycle.sh
```

---

## Summary

### Complete Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                   COMPLETE APPOINTMENT LIFECYCLE                     │
└──────────────────────────────────────────────────────────────────────┘

1. CLIENT → API GATEWAY → MASTER DATA SERVICE
   └→ Create Appointment Types, Locations, Resources
   └→ Response: Reference data IDs

2. CLIENT → API GATEWAY → SCHEDULING SERVICE
   └→ Create Time Slots
   └→ Response: Available slots created

3. CLIENT → API GATEWAY → SCHEDULING SERVICE
   └→ Search Available Slots
   └→ Response: List of available time slots

4. CLIENT → API GATEWAY → SCHEDULING SERVICE
   └→ Create Appointment (SCHEDULED)
   └→ Save to scheduling_db
   └→ Update time_slot status to BOOKED
   └→ Publish AppointmentCreatedEvent to Kafka
   └→ Response: Appointment created

5. CLIENT → API GATEWAY → SCHEDULING SERVICE
   └→ Confirm Appointment (CONFIRMED)
   └→ Update appointment in scheduling_db
   └→ Publish AppointmentConfirmedEvent to Kafka ────┐
   └→ Response: Appointment confirmed                 │
                                                      │
6. KAFKA → ENCOUNTER SERVICE (Automatic)              │
   AppointmentConfirmedEvent consumed ←───────────────┘
   └→ Create Encounter in encounter_db
   └→ Status: PLANNED
   └→ Link to appointment

7. CLIENT → API GATEWAY → ENCOUNTER SERVICE
   └→ Query Encounter by Appointment ID
   └→ Response: Encounter details

8. CLIENT → API GATEWAY → SCHEDULING SERVICE
   └→ Check-in Appointment (CHECKED_IN)
   └→ Publish AppointmentCheckedInEvent to Kafka ────┐
   └→ Response: Appointment checked in                │
                                                      │
9. KAFKA → ENCOUNTER SERVICE (Automatic)              │
   AppointmentCheckedInEvent consumed ←───────────────┘
   └→ Update Encounter status to IN_PROGRESS
   └→ Set actual_start_date

10. CLIENT → API GATEWAY → SCHEDULING SERVICE
    └→ Complete Appointment (COMPLETED)
    └→ Free up time slots
    └→ Publish AppointmentCompletedEvent to Kafka ───┐
    └→ Response: Appointment completed                │
                                                      │
11. KAFKA → ENCOUNTER SERVICE (Automatic)             │
    AppointmentCompletedEvent consumed ←──────────────┘
    └→ Update Encounter status to FINISHED
    └→ Set actual_end_date
```

### Key Takeaways

1. **Synchronous Communication** used for:
   - Client-to-service calls (REST APIs)
   - Reference data validation (Scheduling → Master Data)
   - Query operations

2. **Asynchronous Communication** used for:
   - Encounter creation (Scheduling → Kafka → Encounter)
   - Status synchronization between services
   - Loose coupling between services

3. **Three Separate Databases:**
   - `scheduling_db` - Appointments, time slots, history
   - `encounter_db` - Encounters, encounter history
   - `master_data_db` - Appointment types, resources, locations

4. **Event-Driven Architecture Benefits:**
   - Encounter Service doesn't need to know about Scheduling Service
   - Services can be deployed/scaled independently
   - If Encounter Service is down, events are queued
   - Easy to add new consumers (e.g., Notification Service)

5. **Audit Trail:**
   - Complete history in `appointment_history` table
   - Complete history in `encounter_history` table
   - All events logged and traceable

---

**Version:** 1.0.0
**Last Updated:** 2024-01-15
