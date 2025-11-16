# Deployment Guide - Appointment Scheduling System

This guide covers deploying the Appointment Scheduling microservices application in various environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Deployment](#local-development-deployment)
3. [Production Deployment](#production-deployment)
4. [Configuration Management](#configuration-management)
5. [Database Management](#database-management)
6. [Monitoring and Logging](#monitoring-and-logging)
7. [Scaling Strategies](#scaling-strategies)
8. [Backup and Disaster Recovery](#backup-and-disaster-recovery)
9. [Security Hardening](#security-hardening)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

- **Docker**: 20.10 or higher
- **Docker Compose**: 2.0 or higher (for local deployment)
- **Kubernetes**: 1.24+ (for production deployment)
- **Java**: JDK 17 or higher (for local builds)
- **Maven**: 3.9+ (for building artifacts)
- **kubectl**: Latest version (for Kubernetes deployments)
- **Helm**: 3.0+ (optional, for Kubernetes package management)

### Required Resources

#### Minimum (Development)
- CPU: 4 cores
- RAM: 8 GB
- Disk: 20 GB

#### Recommended (Production)
- CPU: 8+ cores
- RAM: 16+ GB
- Disk: 100+ GB SSD

---

## Local Development Deployment

### Option 1: Docker Compose (Recommended for Development)

#### 1. Build All Services

```bash
# Build the entire project
mvn clean package -DskipTests

# Or build specific services
mvn clean package -pl scheduling-service -am -DskipTests
mvn clean package -pl encounter-service -am -DskipTests
mvn clean package -pl master-data-service -am -DskipTests
mvn clean package -pl api-gateway -am -DskipTests
mvn clean package -pl service-discovery -am -DskipTests
```

#### 2. Start Infrastructure Services

```bash
# Start only infrastructure (Postgres, Kafka, Zookeeper)
docker-compose up -d postgres zookeeper kafka eureka-server

# Wait for services to be healthy
docker-compose ps

# Check logs
docker-compose logs -f postgres
docker-compose logs -f kafka
```

#### 3. Start Application Services

```bash
# Start all services
docker-compose up -d

# Or start specific services
docker-compose up -d api-gateway scheduling-service encounter-service master-data-service

# View logs
docker-compose logs -f api-gateway
docker-compose logs -f scheduling-service
```

#### 4. Verify Deployment

```bash
# Check all containers are running
docker-compose ps

# Check service health
curl http://localhost:8761  # Eureka Dashboard
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # Scheduling Service
curl http://localhost:8083/actuator/health  # Encounter Service
curl http://localhost:8085/actuator/health  # Master Data Service

# Check Kafka UI
open http://localhost:8090
```

#### 5. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop specific services
docker-compose stop scheduling-service
```

### Option 2: Local Maven Build

#### 1. Start Infrastructure

```bash
# Start only infrastructure
docker-compose up -d postgres zookeeper kafka

# Wait for services to be ready
sleep 30
```

#### 2. Run Services Locally

```bash
# Terminal 1 - Service Discovery
cd service-discovery
mvn spring-boot:run

# Terminal 2 - API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 3 - Scheduling Service
cd scheduling-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4 - Encounter Service
cd encounter-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5 - Master Data Service
cd master-data-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### 3. Configure Local Profile

Create `application-local.yml` in each service's `src/main/resources`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scheduling_db  # Change per service
    username: postgres
    password: postgres

  kafka:
    bootstrap-servers: localhost:9092

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## Production Deployment

### Option 1: Kubernetes Deployment

#### 1. Create Kubernetes Manifests

Create a `k8s/` directory with the following structure:

```
k8s/
├── namespace.yaml
├── configmaps/
├── secrets/
├── deployments/
├── services/
├── ingress/
└── statefulsets/
```

#### 2. Namespace Configuration

**k8s/namespace.yaml**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: appointment-system
```

#### 3. PostgreSQL StatefulSet

**k8s/statefulsets/postgres-statefulset.yaml**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: appointment-system
spec:
  ports:
  - port: 5432
  clusterIP: None
  selector:
    app: postgres
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: appointment-system
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: init-script
          mountPath: /docker-entrypoint-initdb.d
      volumes:
      - name: init-script
        configMap:
          name: postgres-init
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 50Gi
```

#### 4. Kafka StatefulSet

**k8s/statefulsets/kafka-statefulset.yaml**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: appointment-system
spec:
  ports:
  - port: 9092
    name: client
  - port: 29092
    name: internal
  clusterIP: None
  selector:
    app: kafka
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: appointment-system
spec:
  serviceName: kafka
  replicas: 3  # For production, use 3+ replicas
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ports:
        - containerPort: 9092
        - containerPort: 29092
        env:
        - name: KAFKA_BROKER_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: zookeeper:2181
        - name: KAFKA_ADVERTISED_LISTENERS
          value: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
        - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
          value: "3"
        - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
          value: "true"
        volumeMounts:
        - name: kafka-storage
          mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
  - metadata:
      name: kafka-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 100Gi
```

#### 5. Service Discovery Deployment

**k8s/deployments/eureka-deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eureka-server
  namespace: appointment-system
spec:
  replicas: 2  # For high availability
  selector:
    matchLabels:
      app: eureka-server
  template:
    metadata:
      labels:
        app: eureka-server
    spec:
      containers:
      - name: eureka-server
        image: your-registry/eureka-server:latest
        ports:
        - containerPort: 8761
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: kubernetes
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8761
          initialDelaySeconds: 120
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8761
          initialDelaySeconds: 60
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: eureka-server
  namespace: appointment-system
spec:
  selector:
    app: eureka-server
  ports:
  - port: 8761
    targetPort: 8761
  type: ClusterIP
```

#### 6. API Gateway Deployment

**k8s/deployments/api-gateway-deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: appointment-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: your-registry/api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: kubernetes
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: http://eureka-server:8761/eureka/
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 120
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: appointment-system
spec:
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer  # Or use Ingress
```

#### 7. Scheduling Service Deployment

**k8s/deployments/scheduling-service-deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scheduling-service
  namespace: appointment-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: scheduling-service
  template:
    metadata:
      labels:
        app: scheduling-service
    spec:
      containers:
      - name: scheduling-service
        image: your-registry/scheduling-service:latest
        ports:
        - containerPort: 8082
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: kubernetes
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/scheduling_db
        - name: SPRING_DATASOURCE_USERNAME
          value: postgres
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: kafka:9092
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: http://eureka-server:8761/eureka/
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 120
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 5
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: scheduling-service
  namespace: appointment-system
spec:
  selector:
    app: scheduling-service
  ports:
  - port: 8082
    targetPort: 8082
  type: ClusterIP
```

#### 8. Secrets Management

**k8s/secrets/postgres-secret.yaml**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: appointment-system
type: Opaque
data:
  password: <base64-encoded-password>  # Use: echo -n 'your-password' | base64
```

**k8s/secrets/jwt-secret.yaml**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
  namespace: appointment-system
type: Opaque
data:
  secret: <base64-encoded-jwt-secret>  # Generate strong secret
```

#### 9. Ingress Configuration

**k8s/ingress/ingress.yaml**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: appointment-ingress
  namespace: appointment-system
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.appointment-system.com
    secretName: appointment-tls
  rules:
  - host: api.appointment-system.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

#### 10. Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create secrets
kubectl apply -f k8s/secrets/

# Deploy infrastructure
kubectl apply -f k8s/statefulsets/

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n appointment-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n appointment-system --timeout=300s

# Deploy services
kubectl apply -f k8s/deployments/

# Configure ingress
kubectl apply -f k8s/ingress/

# Verify deployment
kubectl get all -n appointment-system
kubectl get ingress -n appointment-system
```

#### 11. Rolling Updates

```bash
# Update image
kubectl set image deployment/scheduling-service \
  scheduling-service=your-registry/scheduling-service:v2.0 \
  -n appointment-system

# Monitor rollout
kubectl rollout status deployment/scheduling-service -n appointment-system

# Rollback if needed
kubectl rollout undo deployment/scheduling-service -n appointment-system
```

### Option 2: Cloud Provider Deployments

#### AWS ECS/EKS

```bash
# Build and push images to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag scheduling-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/scheduling-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/scheduling-service:latest

# For EKS, use eksctl
eksctl create cluster --name appointment-cluster --region us-east-1 --nodes 3

# Deploy using kubectl (same as Kubernetes above)
```

#### Google Cloud Platform (GKE)

```bash
# Create GKE cluster
gcloud container clusters create appointment-cluster \
  --num-nodes=3 \
  --zone=us-central1-a \
  --machine-type=n1-standard-2

# Get credentials
gcloud container clusters get-credentials appointment-cluster --zone=us-central1-a

# Push to GCR
docker tag scheduling-service:latest gcr.io/<project-id>/scheduling-service:latest
docker push gcr.io/<project-id>/scheduling-service:latest

# Deploy using kubectl
```

#### Azure AKS

```bash
# Create AKS cluster
az aks create \
  --resource-group appointment-rg \
  --name appointment-cluster \
  --node-count 3 \
  --enable-managed-identity \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group appointment-rg --name appointment-cluster

# Push to ACR
az acr login --name <registry-name>
docker tag scheduling-service:latest <registry-name>.azurecr.io/scheduling-service:latest
docker push <registry-name>.azurecr.io/scheduling-service:latest
```

---

## Configuration Management

### Environment Variables

**Common Environment Variables Across All Services:**

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=production

# Eureka Configuration
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
EUREKA_INSTANCE_PREFER_IP_ADDRESS=true

# Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_APPOINTMENT=DEBUG
```

**Service-Specific Variables:**

**Scheduling Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/scheduling_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
```

**API Gateway:**
```bash
JWT_SECRET=<your-256-bit-secret>
JWT_EXPIRATION=86400000  # 24 hours in milliseconds
```

### ConfigMaps

**k8s/configmaps/application-config.yaml**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: application-config
  namespace: appointment-system
data:
  application.yml: |
    spring:
      application:
        name: ${SERVICE_NAME}

    eureka:
      client:
        service-url:
          defaultZone: http://eureka-server:8761/eureka/
        register-with-eureka: true
        fetch-registry: true
      instance:
        prefer-ip-address: true
        lease-renewal-interval-in-seconds: 10
        lease-expiration-duration-in-seconds: 30

    management:
      endpoints:
        web:
          exposure:
            include: health,metrics,prometheus
      metrics:
        export:
          prometheus:
            enabled: true

    logging:
      level:
        root: INFO
        com.appointment: DEBUG
```

### External Configuration with Spring Cloud Config

For centralized configuration management, consider implementing Spring Cloud Config Server:

1. Create a Git repository for configurations
2. Deploy Spring Cloud Config Server
3. Point all services to the config server
4. Use encryption for sensitive data

---

## Database Management

### Initial Setup

```bash
# Create databases manually or use init script
psql -h postgres -U postgres -f init-databases.sql
```

### Database Migrations with Flyway

Each service includes Flyway migrations in `src/main/resources/db/migration/`.

**Migration Naming Convention:**
- `V1__Initial_schema.sql`
- `V2__Add_appointment_history.sql`
- `V3__Add_indexes.sql`

**Running Migrations:**

```bash
# Migrations run automatically on application startup
# To run manually:
mvn flyway:migrate -pl scheduling-service

# Check migration status
mvn flyway:info -pl scheduling-service

# Repair migration (if needed)
mvn flyway:repair -pl scheduling-service
```

### Database Backup

**PostgreSQL Backup Script:**

```bash
#!/bin/bash
# backup-databases.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/postgres"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup each database
databases=("scheduling_db" "encounter_db" "master_data_db")

for db in "${databases[@]}"
do
  echo "Backing up $db..."
  pg_dump -h postgres -U postgres -d $db -F c -f "$BACKUP_DIR/${db}_${DATE}.dump"
done

# Compress backups
tar -czf "$BACKUP_DIR/backup_${DATE}.tar.gz" $BACKUP_DIR/*_${DATE}.dump

# Clean up individual dumps
rm $BACKUP_DIR/*_${DATE}.dump

# Keep only last 30 days of backups
find $BACKUP_DIR -name "backup_*.tar.gz" -mtime +30 -delete

echo "Backup completed: backup_${DATE}.tar.gz"
```

**Kubernetes CronJob for Backups:**

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: appointment-system
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: postgres-backup
            image: postgres:15-alpine
            command:
            - /bin/sh
            - -c
            - |
              pg_dump -h postgres -U postgres -d scheduling_db -F c -f /backup/scheduling_db_$(date +%Y%m%d).dump
              # Upload to S3/GCS/Azure Blob
          restartPolicy: OnFailure
```

### Database Restore

```bash
# Restore from backup
pg_restore -h postgres -U postgres -d scheduling_db -c /backup/scheduling_db_20240115.dump
```

---

## Monitoring and Logging

### Prometheus and Grafana

**Deploy Prometheus:**

```bash
# Using Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
```

**ServiceMonitor for Scheduling Service:**

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: scheduling-service-monitor
  namespace: appointment-system
spec:
  selector:
    matchLabels:
      app: scheduling-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### ELK Stack (Elasticsearch, Logstash, Kibana)

**Logback Configuration for Structured Logging:**

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${SERVICE_NAME}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>
</configuration>
```

**Filebeat Configuration:**

```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'

processors:
- add_kubernetes_metadata:
    host: ${NODE_NAME}
    matchers:
    - logs_path:
        logs_path: "/var/lib/docker/containers/"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "appointment-logs-%{+yyyy.MM.dd}"
```

### Application Performance Monitoring (APM)

**Add Spring Cloud Sleuth and Zipkin:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

**Configuration:**

```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # Sample 100% in dev, reduce in prod
  zipkin:
    base-url: http://zipkin:9411
```

---

## Scaling Strategies

### Horizontal Pod Autoscaling (HPA)

**HPA for Scheduling Service:**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: scheduling-service-hpa
  namespace: appointment-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: scheduling-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

### Database Scaling

**Read Replicas for PostgreSQL:**

```yaml
# Master database (write operations)
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-master
spec:
  # ... master configuration

---
# Read replicas (read operations)
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-replica
spec:
  replicas: 2
  # ... replica configuration with streaming replication
```

**Connection Pooling:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Kafka Scaling

```bash
# Scale Kafka brokers
kubectl scale statefulset kafka --replicas=5 -n appointment-system

# Increase partition count for high-traffic topics
kafka-topics --bootstrap-server kafka:9092 \
  --alter --topic appointment-events \
  --partitions 12

# Monitor consumer lag
kafka-consumer-groups --bootstrap-server kafka:9092 \
  --describe --group encounter-service-group
```

---

## Backup and Disaster Recovery

### Backup Strategy

**What to Backup:**
1. PostgreSQL databases (all service databases)
2. Kafka topics (optional, event sourcing)
3. Configuration files
4. Secrets and certificates

**Backup Schedule:**
- Full backup: Daily at 2 AM
- Incremental backup: Every 6 hours
- Retention: 30 days

**Automated Backup Script:**

```bash
#!/bin/bash
# complete-backup.sh

set -e

BACKUP_ROOT="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$BACKUP_ROOT/$DATE"

mkdir -p $BACKUP_DIR

# 1. Backup PostgreSQL
echo "Backing up databases..."
databases=("scheduling_db" "encounter_db" "master_data_db")
for db in "${databases[@]}"; do
  pg_dump -h postgres -U postgres -d $db -F c -f "$BACKUP_DIR/${db}.dump"
done

# 2. Backup Kubernetes resources
echo "Backing up Kubernetes resources..."
kubectl get all -n appointment-system -o yaml > "$BACKUP_DIR/k8s-resources.yaml"
kubectl get configmap -n appointment-system -o yaml > "$BACKUP_DIR/configmaps.yaml"
kubectl get secret -n appointment-system -o yaml > "$BACKUP_DIR/secrets.yaml"

# 3. Backup persistent volumes
echo "Backing up PVCs..."
kubectl get pvc -n appointment-system -o yaml > "$BACKUP_DIR/pvc.yaml"

# 4. Compress
tar -czf "$BACKUP_ROOT/backup_${DATE}.tar.gz" -C $BACKUP_ROOT $DATE

# 5. Upload to cloud storage (S3 example)
aws s3 cp "$BACKUP_ROOT/backup_${DATE}.tar.gz" s3://appointment-backups/

# 6. Cleanup
rm -rf $BACKUP_DIR
find $BACKUP_ROOT -name "backup_*.tar.gz" -mtime +30 -delete

echo "Backup completed: backup_${DATE}.tar.gz"
```

### Disaster Recovery Plan

**RTO (Recovery Time Objective):** 4 hours
**RPO (Recovery Point Objective):** 1 hour

**Recovery Steps:**

1. **Database Recovery:**
```bash
# Restore latest backup
pg_restore -h new-postgres -U postgres -d scheduling_db -c /backup/scheduling_db.dump
```

2. **Redeploy Services:**
```bash
# Apply all Kubernetes manifests
kubectl apply -f k8s/ -R

# Verify pods are running
kubectl get pods -n appointment-system -w
```

3. **Verify Data Integrity:**
```bash
# Check database connectivity
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  psql -h postgres -U postgres -d scheduling_db -c "SELECT COUNT(*) FROM appointments;"

# Verify Kafka topics
kubectl exec -it kafka-0 -n appointment-system -- \
  kafka-topics --list --bootstrap-server localhost:9092
```

4. **Test Critical Workflows:**
```bash
# Create test appointment
curl -X POST http://api.appointment-system.com/api/v1/scheduling/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientId":"test-patient", ...}'
```

---

## Security Hardening

### Network Security

**Network Policies:**

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: scheduling-service-netpol
  namespace: appointment-system
spec:
  podSelector:
    matchLabels:
      app: scheduling-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8082
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: kafka
    ports:
    - protocol: TCP
      port: 9092
```

### SSL/TLS Configuration

**Enable HTTPS in API Gateway:**

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: appointment-gateway
```

**Generate Self-Signed Certificate (Development):**

```bash
keytool -genkeypair -alias appointment-gateway \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 \
  -validity 3650
```

**Use Let's Encrypt (Production):**

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: appointment-tls
  namespace: appointment-system
spec:
  secretName: appointment-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - api.appointment-system.com
```

### Enable Authentication

**Uncomment Security in API Gateway:**

Edit `api-gateway/src/main/java/com/appointment/gateway/config/SecurityConfig.java`:

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchange -> exchange
            .pathMatchers("/api/v1/auth/**").permitAll()
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().authenticated()  // Enable authentication
        )
        .build();
}
```

### Secrets Management

**Use Kubernetes Secrets:**

```bash
# Create secret from literal
kubectl create secret generic jwt-secret \
  --from-literal=secret=$(openssl rand -base64 32) \
  -n appointment-system

# Create secret from file
kubectl create secret generic db-credentials \
  --from-file=username=./db-user.txt \
  --from-file=password=./db-pass.txt \
  -n appointment-system
```

**Use HashiCorp Vault (Advanced):**

```bash
# Install Vault
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault -n vault --create-namespace

# Store secrets in Vault
vault kv put secret/appointment/postgres username=postgres password=secure-password
```

### Security Scanning

**Container Image Scanning:**

```bash
# Scan with Trivy
trivy image your-registry/scheduling-service:latest

# Scan with Snyk
snyk container test your-registry/scheduling-service:latest
```

**Dependency Scanning:**

```bash
# Maven dependency check
mvn org.owasp:dependency-check-maven:check
```

---

## Troubleshooting

### Common Issues

#### 1. Service Not Registering with Eureka

**Symptoms:**
- Service doesn't appear in Eureka dashboard
- Other services can't discover the service

**Solutions:**
```bash
# Check Eureka URL configuration
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  env | grep EUREKA

# Check network connectivity to Eureka
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  curl http://eureka-server:8761/eureka/apps

# Check service logs
kubectl logs -f scheduling-service-xxx -n appointment-system | grep eureka

# Verify DNS resolution
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  nslookup eureka-server
```

#### 2. Database Connection Failures

**Symptoms:**
- `Connection refused` errors
- `Too many connections` errors

**Solutions:**
```bash
# Check database connectivity
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  pg_isready -h postgres -p 5432

# Check connection pool settings
kubectl exec -it scheduling-service-xxx -n appointment-system -- \
  env | grep HIKARI

# Monitor active connections
psql -h postgres -U postgres -c "SELECT count(*) FROM pg_stat_activity;"

# Increase max connections if needed
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();
```

#### 3. Kafka Consumer Lag

**Symptoms:**
- Encounters not being created
- Delayed event processing

**Solutions:**
```bash
# Check consumer group lag
kubectl exec -it kafka-0 -n appointment-system -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group encounter-service-group

# Reset offsets if needed (CAREFUL in production)
kubectl exec -it kafka-0 -n appointment-system -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group encounter-service-group --topic appointment-events \
  --reset-offsets --to-latest --execute

# Scale up consumers
kubectl scale deployment encounter-service --replicas=5 -n appointment-system
```

#### 4. Out of Memory Errors

**Symptoms:**
- Pods being killed with OOMKilled status
- `java.lang.OutOfMemoryError`

**Solutions:**
```bash
# Check current memory usage
kubectl top pods -n appointment-system

# Increase memory limits
kubectl set resources deployment scheduling-service \
  --limits=memory=4Gi \
  --requests=memory=2Gi \
  -n appointment-system

# Configure JVM heap size
# Add to deployment env:
- name: JAVA_OPTS
  value: "-Xms1g -Xmx2g -XX:+UseG1GC"
```

#### 5. Slow API Responses

**Symptoms:**
- High response times
- Timeout errors

**Solutions:**
```bash
# Check pod resource usage
kubectl top pods -n appointment-system

# Check database query performance
psql -h postgres -U postgres -d scheduling_db -c "
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;"

# Enable slow query logging
ALTER DATABASE scheduling_db SET log_min_duration_statement = 1000;

# Add database indexes
CREATE INDEX idx_appointments_resource_time
ON appointments(resource_id, start_time);

# Scale horizontally
kubectl scale deployment scheduling-service --replicas=5 -n appointment-system
```

### Debugging Tools

**Exec into Pod:**
```bash
kubectl exec -it scheduling-service-xxx -n appointment-system -- /bin/sh
```

**Port Forward for Local Access:**
```bash
kubectl port-forward svc/scheduling-service 8082:8082 -n appointment-system
```

**View Logs:**
```bash
# Real-time logs
kubectl logs -f scheduling-service-xxx -n appointment-system

# Previous instance logs
kubectl logs scheduling-service-xxx --previous -n appointment-system

# All replica logs
kubectl logs -l app=scheduling-service -n appointment-system --all-containers=true
```

**Describe Resources:**
```bash
kubectl describe pod scheduling-service-xxx -n appointment-system
kubectl describe deployment scheduling-service -n appointment-system
```

### Health Checks

**Liveness vs Readiness Probes:**

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  initialDelaySeconds: 120
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8082
  initialDelaySeconds: 60
  periodSeconds: 5
  failureThreshold: 3
```

---

## Performance Tuning

### JVM Tuning

```bash
# Recommended JVM options for production
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+DisableExplicitGC \
  -XX:+AlwaysPreTouch \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1HeapRegionSize=8M \
  -XX:G1ReservePercent=20 \
  -XX:G1HeapWastePercent=5 \
  -XX:G1MixedGCCountTarget=4 \
  -XX:InitiatingHeapOccupancyPercent=15 \
  -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:G1RSetUpdatingPauseTimePercent=5 \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8"
```

### Database Query Optimization

```sql
-- Create indexes for frequently queried fields
CREATE INDEX CONCURRENTLY idx_appointments_status ON appointments(status);
CREATE INDEX CONCURRENTLY idx_appointments_patient ON appointments(patient_id);
CREATE INDEX CONCURRENTLY idx_appointments_resource_date ON appointments(resource_id, start_time);
CREATE INDEX CONCURRENTLY idx_time_slots_resource_status ON time_slots(resource_id, status);

-- Enable query statistics
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Analyze slow queries
EXPLAIN ANALYZE SELECT * FROM appointments WHERE resource_id = 'xxx' AND start_time > NOW();
```

---

## Checklist

### Pre-Deployment Checklist

- [ ] All services build successfully
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Database migrations reviewed
- [ ] Environment variables configured
- [ ] Secrets created and secured
- [ ] Resource limits defined
- [ ] Health checks configured
- [ ] Backup strategy implemented
- [ ] Monitoring configured
- [ ] Logging configured
- [ ] SSL/TLS certificates ready
- [ ] Network policies defined
- [ ] Security scanning completed
- [ ] Load testing performed
- [ ] Disaster recovery plan documented
- [ ] Runbook created for operations team

### Post-Deployment Checklist

- [ ] All pods running and ready
- [ ] Services registered with Eureka
- [ ] Database migrations applied
- [ ] Kafka topics created
- [ ] Health endpoints responding
- [ ] Metrics being collected
- [ ] Logs being aggregated
- [ ] Critical workflows tested
- [ ] Alerts configured
- [ ] Documentation updated
- [ ] Team trained on deployment process

---

## Support and Maintenance

### Log Locations

**Local Docker:**
```bash
docker logs appointment-scheduling-service
docker logs appointment-api-gateway
```

**Kubernetes:**
```bash
kubectl logs -f deployment/scheduling-service -n appointment-system
```

### Contact Information

For deployment issues, contact:
- DevOps Team: devops@appointment-system.com
- On-call: +1-xxx-xxx-xxxx

---

## Appendix

### Useful Commands Reference

```bash
# Docker Compose
docker-compose up -d              # Start all services
docker-compose down -v            # Stop and remove volumes
docker-compose logs -f service    # View logs
docker-compose ps                 # List services
docker-compose restart service    # Restart service

# Kubernetes
kubectl get all -n appointment-system              # List all resources
kubectl describe pod <pod-name> -n appointment-system  # Describe pod
kubectl logs -f <pod-name> -n appointment-system   # View logs
kubectl exec -it <pod-name> -n appointment-system -- sh  # Exec into pod
kubectl scale deployment <name> --replicas=5 -n appointment-system  # Scale
kubectl rollout restart deployment <name> -n appointment-system  # Restart

# Database
psql -h postgres -U postgres -d scheduling_db      # Connect to database
pg_dump -h postgres -U postgres -d scheduling_db -F c -f backup.dump  # Backup
pg_restore -h postgres -U postgres -d scheduling_db -c backup.dump  # Restore

# Kafka
kafka-topics --list --bootstrap-server kafka:9092  # List topics
kafka-console-consumer --bootstrap-server kafka:9092 --topic appointment-events --from-beginning  # Consume
```

---

**Version:** 1.0.0
**Last Updated:** 2024-01-15
**Maintained By:** DevOps Team
