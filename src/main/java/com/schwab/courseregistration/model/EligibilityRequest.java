package com.schwab.courseregistration.model;

import jakarta.validation.constraints.NotNull;

public record EligibilityRequest(@NotNull(message = "prerequisitesSatisfied must be provided") Boolean prerequisitesSatisfied) {
}
