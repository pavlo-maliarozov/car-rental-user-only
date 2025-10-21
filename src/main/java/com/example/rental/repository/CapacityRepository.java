package com.example.rental.repository;

import com.example.rental.model.Capacity;
import com.example.rental.model.CarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CapacityRepository extends JpaRepository<Capacity, Long> {
    Optional<Capacity> findByCarType(CarType type);

    @Query("select coalesce(c.quantity,0) from Capacity c where c.carType = :type")
    Integer quantityByType(CarType type);
}
