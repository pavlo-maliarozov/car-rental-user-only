package com.example.rental.dto.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(String path, int status, String message, Instant timestamp, Map<String, String> fieldErrors) {}
