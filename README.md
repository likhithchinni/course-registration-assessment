# Course Registration Assessment

A minimal Java 17 / Spring Boot REST service for one course offering. It implements enrollment, dropping, FIFO waitlist promotion, prerequisite checks, duplicate-request handling, and focused unit tests. The project uses an in-memory store because the assessment allows this when a starter project does not require a database.

This workspace is the supplied project. The implementation deliberately remains limited to its existing one-offering, in-memory architecture.

---

## Requirements

- JDK 17 or later
- Maven 3.9+ (or import the `pom.xml` into Eclipse/IntelliJ and let the IDE resolve Maven dependencies)

---

## Run

From this directory:

```bash
mvn clean test
mvn spring-boot:run
```

The application starts at `http://localhost:8080`. Default course ID is `JAVA-101`; capacity is `3`. Override them with `course.id` and `course.capacity` in `src/main/resources/application.properties`.

---

## API examples

### Enroll or join waitlist

```bash
curl -X POST http://localhost:8080/api/courses/JAVA-101/enrollments \
  -H "Content-Type: application/json" \
  -d '{"learnerId":"learner-101","prerequisitesSatisfied":true}'
```

### Drop an enrollment

```bash
curl -X DELETE http://localhost:8080/api/courses/JAVA-101/enrollments/learner-101
```

### View enrollment and waitlist state

```bash
curl http://localhost:8080/api/courses/JAVA-101
```

### Update eligibility for an existing learner

This small support endpoint makes the documented “skip ineligible learner during promotion” policy observable and testable. It is not required as a separate feature by the assessment.

```bash
curl -X PUT http://localhost:8080/api/courses/JAVA-101/learners/learner-102/eligibility \
  -H "Content-Type: application/json" \
  -d '{"prerequisitesSatisfied":false}'
```

On Windows PowerShell, `curl` may be an alias for `Invoke-WebRequest`; use `curl.exe` or the IDE's HTTP client if necessary.

---

## Behavior and assumptions

- Only the configured single course offering is supported.
- A learner must satisfy prerequisites before initial enrollment or waitlisting.
- Failed prerequisite requests do not create a learner record; the support eligibility endpoint accepts only existing waitlisted learners.
- Duplicate enrollment and waitlist requests return explicit statuses and do not add records.
- When a seat opens, the service scans waitlisted learners in FIFO order. An ineligible learner is retained in their original queue position and skipped for that promotion; the earliest eligible learner is promoted.
- A repeated or unknown drop returns `NOT_ENROLLED` and does not alter state.
- Service methods are synchronized so each in-memory state transition is atomic within this single application instance.
- State is lost when the application restarts. This is an assessment prototype, not production persistence.

HTTP outcomes are also explicit: enrollment is `201 Created`, waitlisting is `202 Accepted`, prerequisite failure is `422 Unprocessable Entity`, malformed input is `400 Bad Request`, and a drop for a learner who is not enrolled is `404 Not Found`. Duplicate requests return `200 OK` together with a business status such as `ALREADY_ENROLLED`.

---

## Tests

`CourseRegistrationServiceTest` covers available capacity, full capacity, duplicate enrollment and waitlist requests, FIFO promotion, retained ineligible waitlist entries, prerequisite failure, invalid input, repeated/unknown drops, and concurrent enrollment. `CourseRegistrationControllerTest` verifies the controller's HTTP-status mapping.

---

## Submission reminder

See `docs/DESIGN-SPECIFICATION.md`, `docs/AI-USAGE-RECORD.md`, and `docs/LIMITATIONS.md` for the accompanying submission deliverables.
