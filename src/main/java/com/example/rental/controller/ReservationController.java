package com.example.rental.controller;

import com.example.rental.dto.reservation.*;
import com.example.rental.model.CarType;
import com.example.rental.model.User;
import com.example.rental.service.ReservationService;
import com.example.rental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;

    public ReservationController(ReservationService reservationService, UserService userService) {
        this.reservationService = reservationService;
        this.userService = userService;
    }

    @PostMapping("/reservations")
    public ReservationResponse create(@AuthenticationPrincipal UserDetails user,
                                      @Valid @RequestBody ReservationCreateRequest req) {
        var r = reservationService.create(userId(user), req);
        return new ReservationResponse(r.getId(), r.getUserId(), r.getCarType(), r.getStartAt(), r.getEndAt(), r.getDays(), r.getStatus());
    }

    @PutMapping("/reservations/{id}")
    public ReservationResponse update(@AuthenticationPrincipal UserDetails user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ReservationUpdateRequest req) {
        var r = reservationService.update(userId(user), id, req);
        return new ReservationResponse(r.getId(), r.getUserId(), r.getCarType(), r.getStartAt(), r.getEndAt(), r.getDays(), r.getStatus());
    }

    @DeleteMapping("/reservations/{id}")
    public void cancel(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        reservationService.cancel(userId(user), id);
    }

    @GetMapping("/reservations/my")
    public List<ReservationResponse> my(@AuthenticationPrincipal UserDetails user) {
        return reservationService.listByUser(userId(user))
                .stream().map(r -> new ReservationResponse(r.getId(), r.getUserId(), r.getCarType(), r.getStartAt(), r.getEndAt(), r.getDays(), r.getStatus()))
                .toList();
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(
            @RequestParam("carType") String carType,
            @RequestParam("startAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam("days") int days
    ) {
        CarType type = CarType.from(carType);
        long available = reservationService.available(type, startAt, days);
        AvailabilityResponse response = new AvailabilityResponse(type, startAt, days, available);
        return response;
    }

    private Long userId(UserDetails u) {
        return userService.findByEmail(u.getUsername()).map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
