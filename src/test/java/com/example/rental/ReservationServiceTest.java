package com.example.rental;

import com.example.rental.dto.reservation.ReservationCreateRequest;
import com.example.rental.dto.reservation.ReservationUpdateRequest;
import com.example.rental.exception.ConflictException;
import com.example.rental.model.CarType;
import com.example.rental.model.Reservation;
import com.example.rental.model.ReservationStatus;
import com.example.rental.repository.ReservationRepository;
import com.example.rental.service.CapacityService;
import com.example.rental.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private CapacityService capacityService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationRepository = Mockito.mock(ReservationRepository.class);
        capacityService = Mockito.mock(CapacityService.class);
        reservationService = new ReservationService(reservationRepository, capacityService);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_succeeds_when_capacity_available() {
        when(capacityService.capacityOf(CarType.SUV)).thenReturn(2L);
        when(reservationRepository.countOverlappingByTypeExcluding(eq(CarType.SUV), any(), any(), eq(ReservationStatus.CONFIRMED), isNull())).thenReturn(1L);
        var req = new ReservationCreateRequest(CarType.SUV, Instant.now().plusSeconds(3600), 2);
        Reservation r = reservationService.create(1L, req);
        assertEquals(CarType.SUV, r.getCarType());
        assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
        verify(reservationRepository).save(any());
    }

    @Test
    void create_conflict_when_full() {
        when(capacityService.capacityOf(CarType.SUV)).thenReturn(1L);
        when(reservationRepository.countOverlappingByTypeExcluding(eq(CarType.SUV), any(), any(), eq(ReservationStatus.CONFIRMED), isNull())).thenReturn(1L);
        var req = new ReservationCreateRequest(CarType.SUV, Instant.now().plusSeconds(3600), 2);
        assertThrows(ConflictException.class, () -> reservationService.create(1L, req));
    }

    @Test
    void update_excludes_current_reservation_in_overlap() {
        when(capacityService.capacityOf(CarType.SEDAN)).thenReturn(1L);
        Reservation existing = Reservation.builder()
                .id(10L).userId(1L).carType(CarType.SEDAN)
                .status(ReservationStatus.CONFIRMED)
                .startAt(Instant.now().plusSeconds(7200))
                .endAt(Instant.now().plusSeconds(10800))
                .days(1).build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reservationRepository.countOverlappingByTypeExcluding(eq(CarType.SEDAN), any(), any(), eq(ReservationStatus.CONFIRMED), eq(10L))).thenReturn(0L);
        var req = new ReservationUpdateRequest(CarType.SEDAN, Instant.now().plusSeconds(7200 + 3600), 1);
        var updated = reservationService.update(1L, 10L, req);
        assertEquals(CarType.SEDAN, updated.getCarType());
        verify(reservationRepository).save(any());
    }

    @Test
    void start_in_past_rejected() {
        var req = new ReservationCreateRequest(CarType.VAN, Instant.now().minusSeconds(10), 1);
        assertThrows(IllegalArgumentException.class, () -> reservationService.create(1L, req));
    }
}
