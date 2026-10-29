package com.schwab.courseregistration.api;

import com.schwab.courseregistration.model.EligibilityRequest;
import com.schwab.courseregistration.model.EnrollmentRequest;
import com.schwab.courseregistration.model.EnrollmentResult;
import com.schwab.courseregistration.model.EnrollmentStatus;
import com.schwab.courseregistration.service.CourseRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseRegistrationController {
    private final CourseRegistrationService service;

    public CourseRegistrationController(CourseRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResult> enroll(@PathVariable String courseId, @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResult result = service.enroll(courseId, request.learnerId(), request.prerequisitesSatisfied());
        return ResponseEntity.status(enrollmentHttpStatus(result.status())).body(result);
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public ResponseEntity<EnrollmentResult> drop(@PathVariable String courseId, @PathVariable String learnerId) {
        EnrollmentResult result = service.drop(courseId, learnerId);
        return ResponseEntity.status(dropHttpStatus(result.status())).body(result);
    }

    @PutMapping("/learners/{learnerId}/eligibility")
    public ResponseEntity<EnrollmentResult> updateEligibility(@PathVariable String courseId, @PathVariable String learnerId,
                                              @Valid @RequestBody EligibilityRequest request) {
        EnrollmentResult result = service.updateEligibility(courseId, learnerId, request.prerequisitesSatisfied());
        return ResponseEntity.status(result.status() == EnrollmentStatus.INVALID_REQUEST
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK).body(result);
    }

    @GetMapping
    public ResponseEntity<EnrollmentResult> getState(@PathVariable String courseId) {
        EnrollmentResult result = service.getState(courseId);
        return ResponseEntity.status(result.status() == EnrollmentStatus.INVALID_REQUEST
                ? HttpStatus.NOT_FOUND : HttpStatus.OK).body(result);
    }

    private HttpStatus enrollmentHttpStatus(EnrollmentStatus status) {
        return switch (status) {
            case ENROLLED -> HttpStatus.CREATED;
            case WAITLISTED -> HttpStatus.ACCEPTED;
            case PREREQUISITE_NOT_MET -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
    }

    private HttpStatus dropHttpStatus(EnrollmentStatus status) {
        return switch (status) {
            case DROPPED -> HttpStatus.OK;
            case NOT_ENROLLED -> HttpStatus.NOT_FOUND;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
    }
}
