# AI Prompt and Usage Record

## Actual master prompt

> Act as a senior Java backend engineer, test engineer, and AI-assisted engineering reviewer. Inspect, run, improve, test, and complete this existing course-registration project in place. Do not scaffold a replacement. Follow the supplied Schwab assessment: implement enrollment, dropping, capacity and prerequisite checks, duplicate prevention, FIFO waitlist promotion, a documented policy for ineligible queued learners, focused tests, a proportionate design specification, setup instructions, a limitations summary, and a truthful AI usage record. Review independently and report only verification that was actually performed.

The full request also directed inspection before modification, required Maven verification and a smoke test when feasible, and prohibited unrelated platform features. No secrets or credentials were included in the prompt.

---

## Engineering-review prompt and response summary

**Tool:** Codex / ChatGPT engineering assistant

**Prompt used:** The master prompt above, plus the supplied PDF as the requirements source of truth.

**Relevant response summary:** The review inspected all Java source, tests, configuration, Maven metadata, and existing documents; ran the original tests; identified that the prior implementation removed ineligible learners during promotion despite the request's preference to retain them; recommended a bounded iterator scan that promotes the earliest eligible learner without removing ineligible entries. It also identified that failed prerequisite requests should not leave a mutable learner record, that controller outcomes could use clearer HTTP statuses, and that focused concurrency coverage was appropriate because the service uses `synchronized` state transitions.

**Accepted and edited suggestions:**

- **Suggestion:** Retain ineligible learners and scan past them in FIFO order  
  **Decision:** Accepted  
  **Files / decisions affected:** `CourseRegistrationService`, service tests, README, design specification

- **Suggestion:** Do not persist a failed prerequisite attempt  
  **Decision:** Accepted  
  **Files / decisions affected:** `CourseRegistrationService`, service tests, design specification

- **Suggestion:** Map business results to meaningful HTTP responses  
  **Decision:** Accepted  
  **Files / decisions affected:** `CourseRegistrationController`, controller-adapter tests, README

- **Suggestion:** Add a focused concurrent-enrollment test  
  **Decision:** Accepted  
  **Files / decisions affected:** `CourseRegistrationServiceTest`

- **Suggestion:** Full Spring/MockMvc integration test  
  **Decision:** Edited to a direct controller-adapter test  
  **Files / decisions affected:** The full-context test booted successfully but exceeded this environment's 30-second command window while Mockito attached a JDK agent. The retained test verifies the controller's response mapping without that slow bootstrap.
No suggestion is recorded as rejected. The pre-existing scaffold and its original documentation were treated as unverified input and reviewed rather than trusted.

---

## Verification evidence

### 2026-10-29 21:29 (IST)

- **Command or check:** `mvn clean test` before modifications
- **Actual result:** BUILD SUCCESS; 9 tests, 0 failures, 0 errors
- **Follow-up / changes:** Established the baseline.

### 2026-10-29 21:32 (IST)

- **Command or check:** `mvn -Dtest=CourseRegistrationServiceTest test`
- **Actual result:** BUILD SUCCESS; 11 tests, 0 failures, 0 errors
- **Follow-up / changes:** Confirmed business rules and concurrency coverage after changes.

### 2026-10-29 21:32 (IST)

- **Command or check:** `mvn -Dtest=CourseRegistrationControllerTest test` with initial full Spring context
- **Actual result:** Application context started, then the process exceeded the command-window limit during Mockito dynamic-agent setup; no test result was produced
- **Follow-up / changes:** Replaced with a direct controller-adapter test.

### 2026-10-29 21:34 (IST)

- **Command or check:** `mvn clean test`
- **Actual result:** BUILD SUCCESS; 12 tests, 0 failures, 0 errors, 0 skipped
- **Follow-up / changes:** Final full suite passed after the controller-adapter replacement.

### 2026-10-29 21:34 (IST)

- **Command or check:** `mvn package -DskipTests`
- **Actual result:** BUILD SUCCESS; executable Spring Boot JAR created
- **Follow-up / changes:** Confirms the application packages successfully.

### 2026-10-29 21:35 (IST)

- **Command or check:** `java -jar target/course-registration-0.0.1-SNAPSHOT.jar`
- **Actual result:** Spring initialization reached embedded Tomcat, but startup failed with `java.net.SocketException: Permission denied: connect` while establishing a loopback connection
- **Follow-up / changes:** The sandbox prevents listening on localhost, so live HTTP smoke requests could not be performed here. The controller-adapter test and full suite remain the available verification evidence.

### 2026-10-29 21:36 (IST)

- **Command or check:** `mvn test` after final source and documentation audit
- **Actual result:** BUILD SUCCESS; 12 tests, 0 failures, 0 errors, 0 skipped
- **Follow-up / changes:** Confirms the final source state compiles and passes its automated suite.

### 2026-10-29 22:01 (IST)

- **Command or check:** `mvn package -DskipTests`
- **Actual result:** BUILD SUCCESS; executable Spring Boot JAR rebuilt
- **Follow-up / changes:** The prior `clean test` had removed the JAR, so it was rebuilt before retrying startup.

### 2026-10-29 22:02 (IST)

- **Command or check:** `GET http://localhost:8080/api/courses/JAVA-101`
- **Actual result:** `200 OK` with `CURRENT_STATE` response
- **Follow-up / changes:** Confirmed the user's already-running local instance is the updated API.

### 2026-10-29 22:02 (IST)

- **Command or check:** `POST /api/courses/JAVA-101/enrollments` with an unmet prerequisite
- **Actual result:** `422 Unprocessable Entity`, `PREREQUISITE_NOT_MET`, and state unchanged before versus after
- **Follow-up / changes:** Live verification of the prerequisite-rejection path without mutating the user's running in-memory state.


