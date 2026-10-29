# Design Specification — Course Registration and Waitlist

## Scope

One configured course offering only. The service supports enrollment, dropping, and FIFO waitlist promotion. There is no frontend, catalog, payment, identity integration, multi-term registration, or database.

---

## Service boundary and API

- `POST /api/courses/{courseId}/enrollments`: enroll or add an eligible learner to the waitlist.
- `DELETE /api/courses/{courseId}/enrollments/{learnerId}`: drop an enrolled learner and process promotion.
- `GET /api/courses/{courseId}`: return the current enrollment and waitlist state.
- `PUT /api/courses/{courseId}/learners/{learnerId}/eligibility`: update a waitlisted learner's prerequisite eligibility to make the policy testable. This is a supporting endpoint, not a core assessment requirement.

The controller handles HTTP input and validation. `CourseRegistrationService` owns the business rules and state transitions. Request/response records are in the `model` package.

---

## Data model

- Course ID and capacity are configuration properties (`course.id`, `course.capacity`).
- Enrolled learner IDs are held in insertion-preserving set.
- Waitlisted learner IDs are held in a FIFO deque.
- A map stores each learner's current prerequisite eligibility.
- Responses include a status, explanatory message, enrolled IDs, waitlist IDs, capacity and available seats.

---

## Invariants

1. Enrolled learner count never exceeds capacity.
2. A learner appears at most once across enrolled and waitlisted states.
3. Only eligible learners can be newly enrolled or waitlisted; a failed prerequisite request creates no learner state.
4. A drop only frees a seat if the learner was enrolled.
5. Promotion chooses the earliest currently eligible learner in FIFO order.
6. Duplicate or invalid requests do not corrupt course state.

---

## Waitlist and prerequisite policy

Eligibility is checked before a learner can enroll or join the waitlist. If an existing waitlisted learner later becomes ineligible, they remain in their original queue position. When a seat opens, the service scans the queue in order and promotes the earliest currently eligible learner, retaining every ineligible learner in place. Ineligible candidates are never promoted. The scan is bounded by the queue iterator, so it terminates even if every learner is ineligible. This policy is applied in the promotion method and tested.

---

## Concurrency and persistence

The service's state-changing and snapshot methods are synchronized, making each operation atomic within a single JVM instance. This is appropriate for the in-memory prototype and avoids two simultaneous requests taking the same seat. It does not coordinate multiple application instances. State is volatile and resets at restart; a production implementation would require persistent storage and a database transaction/locking strategy or equivalent atomic capacity control.

---

## Trade-offs and limitations

- In-memory data is intentionally minimal and non-durable.
- There is one configured course offering, not a multi-course catalog.
- Eligibility is supplied by the caller rather than integrated with a prerequisite system.
- HTTP outcomes are explicit: successful enrollment is `201`, waitlisting is `202`, prerequisite failure is `422`, invalid input is `400`, and dropping a non-enrolled learner is `404`. Idempotent duplicate enrollment/waitlist results are `200` with an explanatory status.
- The eligibility-update endpoint exists only to demonstrate and test a waitlisted learner becoming ineligible. It rejects updates for unknown or enrolled learners, so it cannot silently alter an enrollment.
