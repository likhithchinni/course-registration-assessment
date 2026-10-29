package com.schwab.courseregistration.service;

import com.schwab.courseregistration.model.EnrollmentResult;
import com.schwab.courseregistration.model.EnrollmentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory implementation for one course offering. Synchronized operations keep state transitions atomic. */
@Service
public class CourseRegistrationService {
    private final String courseId;
    private final int capacity;
    private final Set<String> enrolled = new LinkedHashSet<>();
    private final ArrayDeque<String> waitlist = new ArrayDeque<>();
    private final Map<String, Boolean> prerequisiteEligibility = new HashMap<>();

    public CourseRegistrationService(
            @Value("${course.id:JAVA-101}") String courseId,
            @Value("${course.capacity:3}") int capacity) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("course.id must not be blank");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("course.capacity must be at least 1");
        }
        this.courseId = courseId;
        this.capacity = capacity;
    }

    public synchronized EnrollmentResult enroll(String requestedCourseId, String learnerId, boolean prerequisitesSatisfied) {
        if (!isValidCourse(requestedCourseId) || learnerId == null || learnerId.isBlank()) {
            return result(learnerId, EnrollmentStatus.INVALID_REQUEST, "courseId and learnerId are required");
        }
        learnerId = learnerId.trim();
        if (enrolled.contains(learnerId)) {
            return result(learnerId, EnrollmentStatus.ALREADY_ENROLLED, "Learner is already enrolled");
        }
        if (waitlist.contains(learnerId)) {
            return result(learnerId, EnrollmentStatus.ALREADY_WAITLISTED, "Learner is already on the waitlist");
        }
        if (!prerequisitesSatisfied) {
            return result(learnerId, EnrollmentStatus.PREREQUISITE_NOT_MET, "Learner does not meet the course prerequisites");
        }
        prerequisiteEligibility.put(learnerId, true);
        if (enrolled.size() < capacity) {
            enrolled.add(learnerId);
            return result(learnerId, EnrollmentStatus.ENROLLED, "Enrollment successful");
        }
        waitlist.addLast(learnerId);
        return result(learnerId, EnrollmentStatus.WAITLISTED, "Course is full; learner added to the waitlist");
    }

    public synchronized EnrollmentResult drop(String requestedCourseId, String learnerId) {
        if (!isValidCourse(requestedCourseId) || learnerId == null || learnerId.isBlank()) {
            return result(learnerId, EnrollmentStatus.INVALID_REQUEST, "courseId and learnerId are required");
        }
        learnerId = learnerId.trim();
        if (!enrolled.remove(learnerId)) {
            return result(learnerId, EnrollmentStatus.NOT_ENROLLED, "Learner is not currently enrolled");
        }
        promoteNextEligible();
        return result(learnerId, EnrollmentStatus.DROPPED, "Enrollment dropped; waitlist promotion processed");
    }

    /**
     * Updates a learner's prerequisite status. If a waitlisted learner becomes ineligible,
     * they remain in queue until a seat opens; promotion scans past and retains them.
     */
    public synchronized EnrollmentResult updateEligibility(String requestedCourseId, String learnerId, boolean eligible) {
        if (!isValidCourse(requestedCourseId) || learnerId == null || learnerId.isBlank()) {
            return result(learnerId, EnrollmentStatus.INVALID_REQUEST, "courseId and learnerId are required");
        }
        learnerId = learnerId.trim();
        if (!waitlist.contains(learnerId)) {
            return result(learnerId, EnrollmentStatus.INVALID_REQUEST,
                    "Eligibility can be updated only for a waitlisted learner");
        }
        prerequisiteEligibility.put(learnerId, eligible);
        return result(learnerId, EnrollmentStatus.ELIGIBILITY_UPDATED, "Prerequisite eligibility updated");
    }

    public synchronized EnrollmentResult getState(String requestedCourseId) {
        if (!isValidCourse(requestedCourseId)) {
            return result(null, EnrollmentStatus.INVALID_REQUEST, "Unknown course offering");
        }
        return result(null, EnrollmentStatus.CURRENT_STATE, "Current course registration state");
    }

    private void promoteNextEligible() {
        Iterator<String> candidates = waitlist.iterator();
        while (enrolled.size() < capacity && candidates.hasNext()) {
            String candidate = candidates.next();
            if (Boolean.TRUE.equals(prerequisiteEligibility.get(candidate))) {
                candidates.remove();
                enrolled.add(candidate);
                return; // A single drop creates one vacancy.
            }
            // Documented policy: retain an ineligible learner's queue position and scan onward.
        }
    }

    private boolean isValidCourse(String requestedCourseId) {
        return courseId.equals(requestedCourseId);
    }

    private EnrollmentResult result(String learnerId, EnrollmentStatus status, String message) {
        List<String> enrolledSnapshot = List.copyOf(enrolled);
        List<String> waitlistSnapshot = List.copyOf(new ArrayList<>(waitlist));
        return new EnrollmentResult(courseId, learnerId, status, message, enrolledSnapshot,
                waitlistSnapshot, capacity, capacity - enrolled.size());
    }
}
