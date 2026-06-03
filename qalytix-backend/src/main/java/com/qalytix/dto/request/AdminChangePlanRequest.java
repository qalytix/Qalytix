package com.qalytix.dto.request;

import com.qalytix.entity.enums.Plan;
import jakarta.validation.constraints.NotNull;

public record AdminChangePlanRequest(@NotNull Plan plan) {}
