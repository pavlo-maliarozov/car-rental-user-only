package com.example.rental.dto.reservation;

import com.example.rental.model.CarType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ReservationCreateRequest(@NotNull CarType carType, @NotNull Instant startAt, @Min(1) int days) {}
