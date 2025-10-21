package com.example.rental.service;

import com.example.rental.dto.reservation.ReservationCreateRequest;
import com.example.rental.dto.reservation.ReservationUpdateRequest;
import com.example.rental.exception.ConflictException;
import com.example.rental.exception.NotFoundException;
import com.example.rental.model.CarType;
import com.example.rental.model.Reservation;
import com.example.rental.model.ReservationStatus;
import com.example.rental.repository.ReservationRepository;
import com.example.rental.util.TimeUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CapacityService capacityService;

    public ReservationService(ReservationRepository reservationRepository, CapacityService capacityService) {
        this.reservationRepository = reservationRepository;
        this.capacityService = capacityService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = "availability", allEntries = true)
    public Reservation create(Long userId, ReservationCreateRequest req) {
        validateRequest(req.carType(), req.startAt(), req.days());
        Instant endAt = TimeUtil.endFromStartAndDays(req.startAt(), req.days());
        ensureAvailable(req.carType(), req.startAt(), endAt, null);
        Reservation r = Reservation.builder()
                .userId(userId)
                .carType(req.carType())
                .startAt(req.startAt())
                .endAt(endAt)
                .days(req.days())
                .status(ReservationStatus.CONFIRMED)
                .build();
        return reservationRepository.save(r);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = "availability", allEntries = true)
    public Reservation update(Long userId, Long id, ReservationUpdateRequest req) {
        Reservation r = reservationRepository.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!r.getUserId().equals(userId)) throw new NotFoundException("Reservation not found");
        if (r.getStatus() == ReservationStatus.CANCELLED) throw new ConflictException("Cannot edit a cancelled reservation");
        validateRequest(req.carType(), req.startAt(), req.days());
        Instant endAt = TimeUtil.endFromStartAndDays(req.startAt(), req.days());
        ensureAvailable(req.carType(), req.startAt(), endAt, id);
        r.setCarType(req.carType());
        r.setStartAt(req.startAt());
        r.setEndAt(endAt);
        r.setDays(req.days());
        return reservationRepository.save(r);
    }

    @Transactional
    @CacheEvict(value = "availability", allEntries = true)
    public void cancel(Long userId, Long id) {
        Reservation r = reservationRepository.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!r.getUserId().equals(userId)) throw new NotFoundException("Reservation not found");
        if (r.getStatus() == ReservationStatus.CANCELLED) return;
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    public List<Reservation> listByUser(Long userId) { return reservationRepository.findByUserId(userId); }

    @Cacheable(
            value = "availability",
            key =
                    "T(java.lang.String).format(" +
                            "'availability:%s:%s:%s', " +
                            "#type, " +
                            "(#startAt == null ? 'null' : #startAt.truncatedTo(T(java.time.temporal.ChronoUnit).HOURS)), " +
                            "#days)"
    )
    public long available(CarType type, Instant startAt, int days) {
        Instant endAt = TimeUtil.endFromStartAndDays(startAt, days);
        long overlapping = reservationRepository.countOverlappingByTypeExcluding(type, startAt, endAt, ReservationStatus.CONFIRMED, null);
        long capacity = capacityService.capacityOf(type);
        return Math.max(0, capacity - overlapping);
    }

    private void validateRequest(CarType type, Instant startAt, int days) {
        if (type == null) throw new IllegalArgumentException("carType is required");
        if (startAt == null) throw new IllegalArgumentException("startAt is required");
        if (startAt.isBefore(Instant.now())) throw new IllegalArgumentException("startAt must be in the future");
        if (days < 1) throw new IllegalArgumentException("days must be >= 1");
    }

    private void ensureAvailable(CarType type, Instant startAt, Instant endAt, Long excludeReservationId) {
        long overlapping = reservationRepository.countOverlappingByTypeExcluding(type, startAt, endAt, ReservationStatus.CONFIRMED, excludeReservationId);
        long capacity = capacityService.capacityOf(type);
        if (overlapping >= capacity) throw new ConflictException("No availability for requested period");
    }
}
