package com.portfolio.wallet.domain.model;

import com.portfolio.wallet.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

public record Money(String amount, String currency) implements Comparable<Money> {

    public static final String DEFAULT_CURRENCY = "BRL";
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");

    private static final Pattern STRICT_MONEY_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.\\d{2}$");

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");

        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new InvalidMoneyException("Only currency BRL is supported");
        }

        if (!STRICT_MONEY_PATTERN.matcher(amount).matches()) {
            throw new InvalidMoneyException("Amount must be formatted as decimal with 2 fractional digits, e.g. '100.00'");
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(amount);
        } catch (Exception e) {
            throw new InvalidMoneyException("Invalid numeric format: " + amount);
        }

        if (decimal.compareTo(BigDecimal.ZERO) < 0 || decimal.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidMoneyException("Amount out of allowed range [0.00, 1000000.00]");
        }
    }

    public static Money of(String amount, String currency) {
        if ("0.00".equals(amount)) {
            throw new InvalidMoneyException("Operational amount must be strictly positive");
        }
        Money money = new Money(amount, currency);
        BigDecimal val = money.asBigDecimal();
        if (val.compareTo(MIN_AMOUNT) < 0) {
            throw new InvalidMoneyException("Operational amount must be at least 0.01");
        }
        return money;
    }

    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.scale() > 2) {
            throw new InvalidMoneyException("Amount has more than 2 decimal places");
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.UNNECESSARY);
        return of(scaled.toPlainString(), DEFAULT_CURRENCY);
    }

    public static Money zero() {
        return new Money("0.00", DEFAULT_CURRENCY);
    }

    public static Money fromBalance(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount must not be null");
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return new Money(scaled.toPlainString(), DEFAULT_CURRENCY);
    }

    public static Money fromBalance(String amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public BigDecimal asBigDecimal() {
        return new BigDecimal(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        validateSameCurrency(other);
        BigDecimal sum = this.asBigDecimal().add(other.asBigDecimal());
        if (sum.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidMoneyException("Balance exceeds maximum allowed limit of " + MAX_AMOUNT);
        }
        return new Money(sum.setScale(2, RoundingMode.HALF_UP).toPlainString(), this.currency);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        validateSameCurrency(other);
        BigDecimal result = this.asBigDecimal().subtract(other.asBigDecimal());
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMoneyException("Resulting amount cannot be negative");
        }
        return new Money(result.setScale(2, RoundingMode.HALF_UP).toPlainString(), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.compareTo(other) < 0;
    }

    public boolean isZero() {
        return asBigDecimal().compareTo(BigDecimal.ZERO) == 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new InvalidMoneyException("Cannot operate across different currencies: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money o) {
        Objects.requireNonNull(o, "Cannot compare with null Money");
        validateSameCurrency(o);
        return this.asBigDecimal().compareTo(o.asBigDecimal());
    }
}
