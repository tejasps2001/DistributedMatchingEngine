package com.tejas.distributedmatchingengine.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String userID,
        @NotNull @Positive BigDecimal price,
        @Positive int quantity) {
}