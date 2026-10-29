package com.schwab.courseregistration.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotBlank(message = "learnerId must not be blank") String learnerId,
        @NotNull(message = "prerequisitesSatisfied must be provided") Boolean prerequisitesSatisfied) {
}
