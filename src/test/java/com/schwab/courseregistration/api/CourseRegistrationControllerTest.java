package com.schwab.courseregistration.api;

import com.schwab.courseregistration.model.EnrollmentRequest;
import com.schwab.courseregistration.model.EnrollmentStatus;
import com.schwab.courseregistration.service.CourseRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseRegistrationControllerTest {
    private CourseRegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new CourseRegistrationController(new CourseRegistrationService("JAVA-101", 1));
    }

    @Test
    void mapsEnrollmentAndDropOutcomesToClearHttpResponses() {
        var enrolled = controller.enroll("JAVA-101", new EnrollmentRequest("api-learner", true));
        assertEquals(HttpStatus.CREATED, enrolled.getStatusCode());
        assertEquals(EnrollmentStatus.ENROLLED, enrolled.getBody().status());

        var duplicate = controller.enroll("JAVA-101", new EnrollmentRequest("api-learner", true));
        assertEquals(HttpStatus.OK, duplicate.getStatusCode());
        assertEquals(EnrollmentStatus.ALREADY_ENROLLED, duplicate.getBody().status());

        var prerequisiteFailure = controller.enroll("JAVA-101", new EnrollmentRequest("missing-prerequisite", false));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, prerequisiteFailure.getStatusCode());
        assertEquals(EnrollmentStatus.PREREQUISITE_NOT_MET, prerequisiteFailure.getBody().status());

        var unknownDrop = controller.drop("JAVA-101", "not-enrolled");
        assertEquals(HttpStatus.NOT_FOUND, unknownDrop.getStatusCode());
        assertEquals(EnrollmentStatus.NOT_ENROLLED, unknownDrop.getBody().status());
    }
}
