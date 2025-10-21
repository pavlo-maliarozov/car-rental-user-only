package com.example.rental.repository;

import com.example.rental.model.CarType;
import com.example.rental.model.Reservation;
import com.example.rental.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
       select count(r) from Reservation r
       where r.status = :status
         and r.carType = :type
         and r.startAt < :endAt
         and r.endAt > :startAt
         and (:excludeId is null or r.id <> :excludeId)
    """)
    long countOverlappingByTypeExcluding(@Param("type") CarType type,
                                @Param("startAt") Instant startAt,
                                @Param("endAt") Instant endAt,
                                @Param("status") ReservationStatus status,
                                @Param("excludeId") Long excludeId);

    List<Reservation> findByUserId(Long userId);
}
