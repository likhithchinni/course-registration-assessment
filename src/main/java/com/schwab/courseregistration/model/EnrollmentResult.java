package com.schwab.courseregistration.model;

import java.util.List;

public record EnrollmentResult(
        String courseId,
        String learnerId,
        EnrollmentStatus status,
        String message,
        List<String> enrolledLearners,
        List<String> waitlistedLearners,
        int capacity,
        int availableSeats) {
}
