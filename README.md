# 💳 Payment Service

A production-ready Spring Boot microservice for handling payments via Stripe and Razorpay.  
It is part of a distributed e-commerce backend system built using Java, Kafka, and Spring Cloud components.

---

## 📘 Description

This service is responsible for:
- Creating and managing payment links via Stripe/Razorpay
- Processing webhook callbacks with retry and idempotency logic
- Publishing payment success/failure events to Kafka
- Logging detailed payment activity for auditing and reporting
- Exposing payment metrics and secured APIs

---

## 🧰 Tech Stack

- Java 17
- Spring Boot 3.x
- Stripe / Razorpay SDKs
- Kafka
- Micrometer + Prometheus
- Bucket4j (rate limiting)
- Logback (JSON + MDC)
- Docker + GitHub Actions

---

## 🚀 Features

### ✅ Core Features
- JWT-based secured controller endpoints
- Stripe and Razorpay payment link creation
- Idempotent webhook processing (event ID deduplication)
- Webhook retry queue backed by database
- Auto-expire INITIATED payments older than 15 minutes

### 📊 Observability
- Structured JSON logging with MDC (traceId, orderId, userId)
- Prometheus-compatible custom metrics
- Audit logs via `PaymentAuditLog` table

### 🛡️ Resilience
- Rate limiting on public webhook endpoints (via Bucket4j)
- Retry job scheduler for failed webhook events
- Config-based feature toggles (`feature.retry.enabled`, etc.)

---

## 🧾 Configuration Properties

| Property Name                       | Description                            | Example               |
|------------------------------------|----------------------------------------|------------------------|
| `stripe.webhook.secret`           | Secret used to verify Stripe webhooks  | `whsec_abc123`         |
| `razorpay.webhook.secret`         | Secret for verifying Razorpay webhooks | `xyz@Rzp!key`          |
| `feature.retry.enabled`           | Toggle retry scheduler on/off          | `true`                 |
| `feature.payment.expiry.enabled`  | Toggle auto-expiry job for INITIATED   | `true`                 |

---

## 📈 Exposed Metrics

- `payments.created.total`
- `payments.succeeded.total`
- `payments.failed.total`
- `webhook.retry.count`
- `webhook.rate.limited.count`

These can be scraped by Prometheus and visualized via Grafana.

---

## ⚙️ How to Run Locally

```bash
# Start MySQL and Kafka (use docker-compose or local setup)

# Run the service
./mvnw spring-boot:run
```

---

## ✅ Production Checklist

- [x] Stripe & Razorpay Integration
- [x] Retry logic with DB + scheduler
- [x] Audit logs for payment actions
- [x] Rate-limited webhooks
- [x] Kafka publishing of events
- [x] Spring Security + JWT
- [x] CI pipeline using GitHub Actions
- [x] Swagger/OpenAPI docs with examples
- [x] Dockerfile
- [x] Feature flags for job toggles
- [ ] Unit and Integration Tests (WIP)
- [ ] Email notification (future)
- [ ] DLQ / alerting on retry failure

---

## 📤 API Endpoints

All endpoints are secured with JWT.

| Method | Path                                | Description                    |
|--------|-------------------------------------|--------------------------------|
| POST   | `/api/payment/initiate`            | Create payment link            |
| POST   | `/api/payment/webhook/stripe`      | Stripe webhook receiver        |
| POST   | `/api/payment/webhook/razorpay`    | Razorpay webhook receiver      |
| GET    | `/api/payment/status/{orderId}`    | Get payment status by order ID |

---

## 👨‍💻 Author

**Aayush Kumar**  
GitHub: [@Aayush20](https://github.com/Aayush20)

