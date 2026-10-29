package com.schwab.courseregistration.service;

import com.schwab.courseregistration.model.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CourseRegistrationServiceTest {
    private CourseRegistrationService service;
    private static final String COURSE = "JAVA-101";

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(COURSE, 2);
    }

    @Test
    void enrollsWhenCapacityIsAvailable() {
        var result = service.enroll(COURSE, "A", true);
        assertEquals(EnrollmentStatus.ENROLLED, result.status());
        assertEquals(1, result.availableSeats());
        assertEquals(java.util.List.of("A"), result.enrolledLearners());
    }

    @Test
    void waitlistsEligibleLearnerWhenCourseIsFull() {
        service.enroll(COURSE, "A", true);
        service.enroll(COURSE, "B", true);
        var result = service.enroll(COURSE, "C", true);
        assertEquals(EnrollmentStatus.WAITLISTED, result.status());
        assertEquals(java.util.List.of("C"), result.waitlistedLearners());
        assertEquals(0, result.availableSeats());
    }

    @Test
    void duplicateEnrollmentAndWaitlistRequestsDoNotCreateDuplicates() {
        service.enroll(COURSE, "A", true);
        assertEquals(EnrollmentStatus.ALREADY_ENROLLED, service.enroll(COURSE, "A", true).status());
        service.enroll(COURSE, "B", true);
        service.enroll(COURSE, "C", true);
        assertEquals(EnrollmentStatus.ALREADY_WAITLISTED, service.enroll(COURSE, "C", true).status());
        assertEquals(1, service.getState(COURSE).waitlistedLearners().size());
    }

    @Test
    void droppingLearnerPromotesEarliestWaitlistedLearner() {
        service.enroll(COURSE, "A", true);
        service.enroll(COURSE, "B", true);
        service.enroll(COURSE, "C", true);
        service.enroll(COURSE, "D", true);
        var result = service.drop(COURSE, "A");
        assertEquals(EnrollmentStatus.DROPPED, result.status());
        assertEquals(java.util.List.of("B", "C"), result.enrolledLearners());
        assertEquals(java.util.List.of("D"), result.waitlistedLearners());
        assertEquals(0, result.availableSeats());
    }

    @Test
    void rejectsLearnerWhoDoesNotMeetPrerequisites() {
        var result = service.enroll(COURSE, "A", false);
        assertEquals(EnrollmentStatus.PREREQUISITE_NOT_MET, result.status());
        assertTrue(result.enrolledLearners().isEmpty());
        assertTrue(result.waitlistedLearners().isEmpty());
    }

    @Test
    void skipsLearnerWhoBecomesIneligibleBeforePromotion() {
        service.enroll(COURSE, "A", true);
        service.enroll(COURSE, "B", true);
        service.enroll(COURSE, "C", true);
        service.enroll(COURSE, "D", true);
        service.updateEligibility(COURSE, "C", false);
        var result = service.drop(COURSE, "A");
        assertEquals(java.util.List.of("B", "D"), result.enrolledLearners());
        assertEquals(List.of("C"), result.waitlistedLearners());
        assertEquals(0, result.availableSeats());
        assertEquals(EnrollmentStatus.ALREADY_WAITLISTED, service.enroll(COURSE, "C", true).status());
    }

    @Test
    void repeatedDropDoesNotChangeState() {
        service.enroll(COURSE, "A", true);
        service.enroll(COURSE, "B", true);
        service.enroll(COURSE, "C", true);
        var first = service.drop(COURSE, "A");
        var second = service.drop(COURSE, "A");
        assertEquals(EnrollmentStatus.DROPPED, first.status());
        assertEquals(EnrollmentStatus.NOT_ENROLLED, second.status());
        assertEquals(first.enrolledLearners(), second.enrolledLearners());
        assertEquals(first.availableSeats(), second.availableSeats());
    }

    @Test
    void rejectsUnknownCourseAndBlankLearnerId() {
        assertEquals(EnrollmentStatus.INVALID_REQUEST, service.enroll("OTHER", "A", true).status());
        assertEquals(EnrollmentStatus.INVALID_REQUEST, service.enroll(COURSE, " ", true).status());
    }

    @Test
    void unknownDropDoesNotChangeCapacity() {
        service.enroll(COURSE, "A", true);
        var before = service.getState(COURSE);
        var result = service.drop(COURSE, "UNKNOWN");
        var after = service.getState(COURSE);
        assertEquals(EnrollmentStatus.NOT_ENROLLED, result.status());
        assertEquals(before.enrolledLearners(), after.enrolledLearners());
        assertEquals(before.availableSeats(), after.availableSeats());
    }

    @Test
    void trimsLearnerIdsAndDoesNotPersistFailedPrerequisiteRequests() {
        assertEquals(EnrollmentStatus.ENROLLED, service.enroll(COURSE, " learner ", true).status());
        assertEquals(EnrollmentStatus.ALREADY_ENROLLED, service.enroll(COURSE, "learner", true).status());

        assertEquals(EnrollmentStatus.PREREQUISITE_NOT_MET, service.enroll(COURSE, "ineligible", false).status());
        assertEquals(EnrollmentStatus.INVALID_REQUEST,
                service.updateEligibility(COURSE, "ineligible", true).status());
    }

    @Test
    void concurrentEnrollmentsNeverExceedCapacityOrDuplicateLearners() throws Exception {
        CourseRegistrationService oneSeatService = new CourseRegistrationService(COURSE, 1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<EnrollmentStatus>> tasks = java.util.stream.IntStream.range(0, 20)
                    .<Callable<EnrollmentStatus>>mapToObj(index -> () -> {
                        start.await();
                        return oneSeatService.enroll(COURSE, "learner-" + index, true).status();
                    }).toList();
            List<Future<EnrollmentStatus>> results = tasks.stream().map(executor::submit).toList();
            start.countDown();

            for (Future<EnrollmentStatus> result : results) {
                assertTrue(List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITLISTED).contains(result.get()));
            }
        } finally {
            executor.shutdownNow();
        }

        var state = oneSeatService.getState(COURSE);
        assertEquals(1, state.enrolledLearners().size());
        assertEquals(19, state.waitlistedLearners().size());
        assertEquals(20, java.util.stream.Stream.concat(state.enrolledLearners().stream(), state.waitlistedLearners().stream())
                .distinct().count());
        assertEquals(0, state.availableSeats());
    }
}
