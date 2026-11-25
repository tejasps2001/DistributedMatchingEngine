package com.tejas.distributedmatchingengine.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
        @NotBlank String userid,
        @NotNull @Positive double price,
        @Positive int quantity) {
}