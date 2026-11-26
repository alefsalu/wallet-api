package com.portfolio.wallet.interfaces.rest.dto;

import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.WalletId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Destination wallet ID is required")
        UUID destinationWalletId,

        @NotBlank(message = "Amount is required")
        @Pattern(
                regexp = "^(0|[1-9]\\d*)\\.\\d{2}$",
                message = "Amount must be a decimal string with exactly 2 fractional digits, e.g. '100.00'")
        String amount,

        String description
) {
    public Money toMoney() {
        return Money.of(amount, Money.DEFAULT_CURRENCY);
    }

    public WalletId toWalletId() {
        return WalletId.of(destinationWalletId);
    }
}
