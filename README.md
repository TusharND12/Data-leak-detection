# PD-MEWS: Personal Data Misuse Early-Warning System

## Project Overview
PD-MEWS is a privacy-first backend system designed to attribute data leaks and misuse (like spam calls or phishing) to their likely source applications using probabilistic correlation.

Unlike traditional "Have I Been Pwned" services that rely on static database dumps, PD-MEWS dynamically analyzes the **timing** and **correlation** of user actions (signing up) vs misuse events.

## Architecture: Modular Monolith
We chose **Modular Monolith** over Microservices for the initial phase to ensure:
-   **Atomic Consistency**: Easier data integrity across modules.
-   **Lower Latency**: In-process calls for Risk Analysis (CPU intensive) vs network overhead.
-   **Simplified Ops**: Single Docker container to deploy.

However, the package structure (`com.pdmews.identity`, `.risk`, etc.) allows extraction into Microservices later.

## Core Logic: The Risk Engine
The system uses a weighted scoring algorithm:
```java
Score = (TimeCorrelation * 0.5) + (TrustScoreInverse * 0.3) + (Frequency * 0.2)
```
-   **Time Correlation**: The closer the spam is to the signup date, the higher the blame.
-   **Trust Score**: We maintain a `GlobalTrustService` (Google = High Trust, Unknown Casino App = Low Trust).
-   **Decay Function**: Risk score decays over time if no further events are reported.

## API Usage
### 1. Add Source
`POST /api/sources`
```json
{
  "user": {"id": "..."},
  "appName": "Shady Game 2024",
  "signupDate": "2024-01-20"
}
```

### 2. Report Misuse
`POST /api/events`
```json
{
  "user": {"id": "..."},
  "type": "SPAM_CALL",
  "eventTimestamp": "2024-01-21T10:00:00"
}
```

### 3. Analyze
`GET /api/risk/analyze/{userId}`
**Response**:
```json
[
  {
    "appSource": "Shady Game 2024",
    "riskScore": 85.0,
    "riskLevel": "CRITICAL",
    "reasoning": "Suspicious: Misuse event occurred within 2 days of signup..."
  }
]
```

## Security & Privacy
-   **Data Minimization**: We only store hashes of Contact Points (Phone/Email).
-   **No Content Read**: We do NOT read SMS or Email bodies. only metadata (Receiver, Sender, Time).
-   **Ownership**: Users own their data timeline.

## Monetization & Scalability
### Monetization Model
1.  **B2C Freemium**: Basic correlation is free. Advanced legal-ready PDF reports and "Auto-Unsubscribe" assistance are Paid ($5/mo).
2.  **B2B Intelligence**: Aggregated, anonymized "Trust Scores" sold to Ad-blockers or Security Firms. "Which apps are leaking data right now?"

### Scalability Plan
-   **Ingestion**: As events scale (millions of spam reports), introduce **Apache Kafka** to buffer events before DB.
-   **Compute**: Move `RiskEngineService` to a separate Spark/Flink job for batch processing if real-time approach bottlenecks.
-   **Database**: Partition `misuse_events` table by time (monthly).

## Interview / Explanation Guide
**Q: How do you know it's App X?**
A: "We don't claim certainty. We calculate probability based on temporal locality. If you give your number to App X today and get spam tomorrow, and 1,000 other users report the same pattern, our statistical confidence approaches 99%."

**Q: Why not Microservices?**
A: "Microservices introduce distributed transactions and network fallacies. For a high-integrity correlation engine where User Data + Event Data must be joined efficiently, a Monolith interacting with a single normalized Postgres DB is vastly more performant and easier to debug."
