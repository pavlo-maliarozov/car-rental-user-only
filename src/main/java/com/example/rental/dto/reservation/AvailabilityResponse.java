package com.example.rental.dto.reservation;

import com.example.rental.model.CarType;
import java.time.Instant;

public record AvailabilityResponse(CarType carType, Instant startAt, int days, long available) {}
