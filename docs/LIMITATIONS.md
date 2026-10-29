# Limitations and Follow-up Risks

---

- State is held only in memory and is lost at process restart.
- `synchronized` protects one JVM only. A multi-instance deployment would need a durable store and transactional or optimistic-locking capacity control.
- Prerequisite eligibility is caller supplied. This bounded assessment does not integrate with learner records or a prerequisite system.
- An ineligible learner remains on the waitlist. They may be promoted after a later eligibility update; a real product may instead notify them or remove them under an agreed policy.

