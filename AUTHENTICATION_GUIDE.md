# Authentication Guide for Multiple Clients

This document explains how to implement authentication for different client types in the Appointment Scheduling microservices system.

## Table of Contents
- [Overview](#overview)
- [Authentication Strategies](#authentication-strategies)
- [JWT Token Structure](#jwt-token-structure)
- [Client Types](#client-types)
- [Implementation Steps](#implementation-steps)
- [API Examples](#api-examples)

---

## Overview

The API Gateway supports authentication for multiple client types:
- **Web Application** (React, Angular, Vue)
- **Mobile Applications** (iOS, Android, React Native)
- **Third-Party APIs** (External integrations)
- **Internal Services** (Service-to-service communication)

### Current State
- **Development Mode**: Authentication is **DISABLED** by default (all requests allowed)
- **Production Mode**: Uncomment security configuration to enable JWT authentication

---

## Authentication Strategies

### 1. JWT (JSON Web Tokens) - Recommended ⭐
**Use for**: Web apps, mobile apps, SPA

**Flow:**
```
1. Client → POST /api/v1/auth/login {username, password}
2. Identity Service validates credentials
3. Returns JWT token
4. Client includes token in subsequent requests:
   Authorization: Bearer <token>
5. API Gateway validates token and forwards to services
```

**Advantages:**
- Stateless (no server-side session storage)
- Scalable across multiple servers
- Contains user claims (roles, permissions)
- Can be refreshed

**Token Payload Example:**
```json
{
  "sub": "user@example.com",
  "userId": "uuid-123",
  "roles": ["PATIENT"],
  "clientType": "WEB",
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

### 2. API Keys
**Use for**: Third-party integrations, server-to-server

**Flow:**
```
1. Admin creates API key for third-party client
2. Client includes key in header:
   X-API-Key: <api-key>
3. API Gateway validates key against database
4. Forwards request with client context
```

**Advantages:**
- Simple for external partners
- Easy to revoke
- Rate limiting per key
- Track usage per client

---

### 3. OAuth 2.0 / OpenID Connect
**Use for**: Enterprise SSO, social login

**Flow:**
```
1. Client redirects to OAuth provider (Google, Microsoft, etc.)
2. User authenticates
3. OAuth provider returns authorization code
4. Exchange code for access token
5. Use token to access API
```

**Advantages:**
- No password handling
- Trusted identity providers
- Standard protocol
- Social login support

---

## JWT Token Structure

### Claims in Token

```java
{
  // Standard claims
  "sub": "john.doe@example.com",      // Subject (username/email)
  "iat": 1234567890,                  // Issued at
  "exp": 1234654290,                  // Expiration

  // Custom claims
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "roles": ["PATIENT"],               // User roles
  "clientType": "WEB",                // Client type
  "clientId": "web-app-1",            // Specific client
  "permissions": ["READ_APPOINTMENTS", "CREATE_APPOINTMENTS"],

  // Optional claims for advanced scenarios
  "organizationId": "org-123",        // Multi-tenant
  "sessionId": "session-xyz"          // Track sessions
}
```

### Token Types

1. **Access Token** (Short-lived: 15 min - 1 hour)
   - Used for API requests
   - Contains user permissions

2. **Refresh Token** (Long-lived: 7-30 days)
   - Used to get new access token
   - Stored securely by client

---

## Client Types

### 1. Web Application

**Authentication Flow:**
```javascript
// Login
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'patient@example.com',
    password: 'password123'
  })
});

const { accessToken, refreshToken } = await response.json();

// Store tokens (localStorage for demo, httpOnly cookie recommended)
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Make authenticated request
const appointments = await fetch('http://localhost:8080/api/v1/scheduling/appointments', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});
```

**Token Storage:**
- **Best**: httpOnly cookies (protects against XSS)
- **Alternative**: localStorage (vulnerable to XSS)
- **Never**: sessionStorage for refresh tokens

---

### 2. Mobile Application

**Authentication Flow:**
```swift
// iOS Example
func login(email: String, password: String) async throws -> AuthTokens {
    let url = URL(string: "http://api.example.com/api/v1/auth/login")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let body = ["email": email, "password": password]
    request.httpBody = try JSONEncoder().encode(body)

    let (data, _) = try await URLSession.shared.data(for: request)
    return try JSONDecoder().decode(AuthTokens.self, from: data)
}

// Store in Keychain
func saveToken(_ token: String, forKey key: String) {
    let data = token.data(using: .utf8)!
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrAccount as String: key,
        kSecValueData as String: data
    ]
    SecItemAdd(query as CFDictionary, nil)
}
```

**Token Storage:**
- **iOS**: Keychain
- **Android**: EncryptedSharedPreferences
- **React Native**: react-native-keychain

---

### 3. Third-Party API Integration

**API Key Authentication:**

```bash
# Third-party client makes request with API key
curl -X GET \
  http://localhost:8080/api/v1/scheduling/appointments \
  -H 'X-API-Key: sk_live_abc123xyz789'
```

**API Gateway Validation:**
```java
// Pseudocode
if (request.hasHeader("X-API-Key")) {
    String apiKey = request.getHeader("X-API-Key");
    ApiKeyDetails details = apiKeyService.validate(apiKey);

    if (details.isValid() && !details.isRevoked()) {
        // Add client context to request
        request.addHeader("X-Client-Id", details.getClientId());
        request.addHeader("X-Client-Type", "API");
        // Forward to downstream service
    } else {
        return 401 Unauthorized;
    }
}
```

---

### 4. Service-to-Service Communication

**Internal JWT (for microservices calling each other):**

```java
// Scheduling Service calls Master Data Service
@Service
public class MasterDataClient {

    @Autowired
    private RestTemplate restTemplate;

    public AppointmentType getAppointmentType(UUID id) {
        // Generate internal service token
        String serviceToken = jwtUtil.generateServiceToken("scheduling-service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
            "http://master-data-service/api/v1/appointment-types/" + id,
            HttpMethod.GET,
            entity,
            AppointmentType.class
        ).getBody();
    }
}
```

---

## Implementation Steps

### Step 1: Create Identity Service (Optional)

If you want full authentication, create an Identity Service:

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Validate credentials
        User user = userService.authenticate(request.getEmail(), request.getPassword());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        // Validate refresh token
        if (jwtUtil.validateToken(request.getRefreshToken())) {
            String userId = jwtUtil.extractUserId(request.getRefreshToken());
            User user = userService.findById(userId);

            String newAccessToken = jwtUtil.generateAccessToken(user);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, request.getRefreshToken()));
        }
        return ResponseEntity.status(401).build();
    }
}
```

### Step 2: Enable Authentication in API Gateway

Uncomment the security configuration in `SecurityConfig.java`:

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                      AuthenticationFilter authFilter) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                    .pathMatchers("/api/v1/auth/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            )
            .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
}
```

### Step 3: Configure Token Validation

The API Gateway already has `JwtUtil` and `AuthenticationFilter` configured.

---

## API Examples

### Public Endpoints (No Authentication Required)

```bash
# Health check
GET http://localhost:8080/actuator/health

# Login
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "patient@example.com",
  "password": "password123"
}
```

### Protected Endpoints (Require Authentication)

```bash
# Get appointments
GET http://localhost:8080/api/v1/scheduling/appointments
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Create appointment
POST http://localhost:8080/api/v1/scheduling/appointments
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "patientId": "uuid-123",
  "resourceId": "uuid-456",
  "locationId": "uuid-789",
  "appointmentTypeId": "uuid-abc",
  "startTime": "2024-12-01T10:00:00",
  "endTime": "2024-12-01T11:00:00"
}
```

---

## Role-Based Access Control (RBAC)

### Roles

```java
public enum UserRole {
    PATIENT,      // Can view/book own appointments
    PROVIDER,     // Can view all appointments, update encounters
    ADMIN,        // Full access
    STAFF         // Schedule appointments for patients
}
```

### Permission Checks

```java
@PreAuthorize("hasRole('PROVIDER')")
@GetMapping("/api/v1/scheduling/appointments/all")
public List<Appointment> getAllAppointments() {
    // Only providers and admins can view all appointments
}

@PreAuthorize("hasAnyRole('PATIENT', 'STAFF')")
@PostMapping("/api/v1/scheduling/appointments")
public Appointment createAppointment() {
    // Patients and staff can create appointments
}
```

---

## Security Best Practices

### 1. Token Security
- ✅ Use HTTPS in production
- ✅ Short access token expiration (15-60 min)
- ✅ Long refresh token expiration (7-30 days)
- ✅ Rotate refresh tokens on use
- ✅ Store tokens securely (httpOnly cookies, Keychain)

### 2. Password Security
- ✅ Use BCrypt for password hashing
- ✅ Enforce password complexity
- ✅ Implement rate limiting on login
- ✅ Account lockout after failed attempts

### 3. API Security
- ✅ Rate limiting per client
- ✅ CORS configuration
- ✅ Input validation
- ✅ SQL injection prevention
- ✅ XSS prevention

### 4. Logging & Monitoring
- ✅ Log failed authentication attempts
- ✅ Monitor suspicious activity
- ✅ Track token usage
- ✅ Alert on unusual patterns

---

## Testing Authentication

### Using Postman

```
1. Create environment with variables:
   - base_url: http://localhost:8080
   - access_token: (will be set automatically)

2. Login request:
   POST {{base_url}}/api/v1/auth/login

   Test script:
   var response = pm.response.json();
   pm.environment.set("access_token", response.accessToken);

3. Use token in other requests:
   GET {{base_url}}/api/v1/scheduling/appointments
   Headers:
   Authorization: Bearer {{access_token}}
```

### Using cURL

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"pass"}' \
  | jq -r '.accessToken')

# Use token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/scheduling/appointments
```

---

## Summary

**For Development:**
- Authentication is currently **disabled** (permitAll)
- All endpoints are accessible without tokens
- Focus on building business logic

**For Production:**
1. Create Identity Service or use existing auth provider
2. Uncomment security configuration in API Gateway
3. Configure JWT secret (use environment variable)
4. Implement proper token storage on clients
5. Add role-based access control
6. Enable HTTPS
7. Implement rate limiting

---

## Next Steps

1. **Keep Development Simple**: Continue without authentication while building features
2. **Add Identity Service Later**: Create a separate service for user management
3. **Enable Gradually**: Start with basic JWT, then add OAuth/API keys as needed
4. **Test Thoroughly**: Use automated tests for auth flows
5. **Monitor Security**: Log auth attempts, track tokens, alert on anomalies

**Current Status**: ✅ API Gateway ready with JWT support (disabled by default)
