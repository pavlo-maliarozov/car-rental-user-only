package com.example.rental.service;

import com.example.rental.model.CarType;
import com.example.rental.repository.CapacityRepository;
import org.springframework.stereotype.Service;

@Service
public class CapacityService {
    private final CapacityRepository capacityRepository;
    public CapacityService(CapacityRepository capacityRepository) { this.capacityRepository = capacityRepository; }

    public long capacityOf(CarType type) {
        Integer q = capacityRepository.quantityByType(type);
        return q == null ? 0L : q.longValue();
    }
}
