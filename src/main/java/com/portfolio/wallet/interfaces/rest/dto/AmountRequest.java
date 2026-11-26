package com.portfolio.wallet.interfaces.rest.dto;

import com.portfolio.wallet.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AmountRequest(
        @NotBlank(message = "Amount is required")
        @Pattern(
                regexp = "^(0|[1-9]\\d*)\\.\\d{2}$",
                message = "Amount must be a decimal string with exactly 2 fractional digits, e.g. '100.00'")
        String amount
) {
    public Money toMoney() {
        return Money.of(amount, Money.DEFAULT_CURRENCY);
    }
}
