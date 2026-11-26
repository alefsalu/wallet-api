package com.portfolio.wallet.interfaces.rest.dto;

import com.portfolio.wallet.domain.model.Money;

public record MoneyDto(String amount, String currency) {

    public static MoneyDto fromDomain(Money money) {
        return new MoneyDto(money.amount(), money.currency());
    }
}
