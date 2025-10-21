package com.example.rental.dto.reservation;

import com.example.rental.model.CarType;
import com.example.rental.model.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(Long id, Long userId, CarType carType, Instant startAt, Instant endAt, int days, ReservationStatus status) {}
