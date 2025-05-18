# 💳 Payment Service

## 📌 Overview
Handles payment initiation, webhook processing, auditing, and event publishing. Supports Stripe & Razorpay.

---

## ⚙️ Features

- ✅ JWT-secured payment APIs
- ✅ Stripe & Razorpay integration
- ✅ Idempotent webhook processing
- ✅ Kafka events: `payment.success`, `payment.failed`
- ✅ Retry queue for webhook failures
- ✅ Audit logging (PaymentAuditLog)
- ✅ Rate limiting on webhooks (Bucket4j)
- ✅ Redis TTL cache for `/me/payments`
- ✅ Prometheus metrics via Actuator
- ✅ Redis caching of Razorpay/Stripe metadata
- ✅ SendGrid email notifications
- ✅ Swagger/OpenAPI documentation
- ✅ CI via GitHub Actions
- ✅ Dockerized

---

## 🧰 Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Security (OAuth2 JWT)
- Stripe & Razorpay SDK
- Kafka
- Redis + Spring Cache
- SendGrid Email API
- Micrometer + Prometheus
- Docker + GitHub Actions

---

## 📂 Key APIs

| Method | Path                            | Description                      |
|--------|---------------------------------|----------------------------------|
| POST   | `/api/payment/initiate`         | Start payment                    |
| GET    | `/api/payment/status/{orderId}` | Get payment status               |
| POST   | `/api/payment/webhook/stripe`   | Stripe webhook handler           |
| POST   | `/api/payment/webhook/razorpay` | Razorpay webhook handler         |

---

## 📈 Metrics

- `payments.created.total`
- `payments.succeeded.total`
- `payments.failed.total`
- `webhook.retry.count`
- `webhook.rate.limited.count`

---

## 📊 Redis Cache

- `user:payments:{userId}` – TTL 60s
- `metadata:razorpay:plans` – TTL 12h

---

## 🧪 Setup & Run

```bash
mvn clean install
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

---

## 🐳 Docker

```bash
docker build -t payment-service .
docker run -p 8084:8084 payment-service
```

---

## 🧪 Future Enhancements

- DLQ handling for unrecovered retries
- Sentry / ELK for alerting
- Stripe webhook signature validation
- Test coverage with mocks/testcontainers

---

## 👨‍💻 Author

**Aayush Kumar** – [GitHub](https://github.com/Aayush20)