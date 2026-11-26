package com.portfolio.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.wallet.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Test
    @DisplayName("Should create valid Money instance")
    void shouldCreateValidMoney() {
        Money money = Money.of("100.50", "BRL");
        assertThat(money.amount()).isEqualTo("100.50");
        assertThat(money.currency()).isEqualTo("BRL");
        assertThat(money.asBigDecimal()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("Should reject invalid money inputs including scientific notation, excess decimals, bounds and currencies")
    void shouldRejectInvalidMoneyInputs() {
        assertThatThrownBy(() -> Money.of("10.005", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("1e2", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("0.00", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("1000000.01", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("100.00", "USD")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("-50.00", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("abc", "BRL")).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.of("10", "BRL")).isInstanceOf(InvalidMoneyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "100.1", "100.123", "1,000.00", " 50.00 ", ""})
    @DisplayName("Should reject improperly formatted money strings")
    void shouldRejectImproperFormats(String invalidAmount) {
        assertThatThrownBy(() -> Money.of(invalidAmount, "BRL")).isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("Should correctly perform addition and subtraction")
    void shouldPerformArithmeticOperations() {
        Money a = Money.of("150.25", "BRL");
        Money b = Money.of("49.75", "BRL");

        Money sum = a.add(b);
        assertThat(sum.amount()).isEqualTo("200.00");

        Money diff = a.subtract(b);
        assertThat(diff.amount()).isEqualTo("100.50");
    }

    @Test
    @DisplayName("Should compare money values properly")
    void shouldCompareValues() {
        Money smaller = Money.of("10.00", "BRL");
        Money larger = Money.of("20.00", "BRL");

        assertThat(larger.isGreaterThan(smaller)).isTrue();
        assertThat(smaller.isLessThan(larger)).isTrue();
        assertThat(smaller.compareTo(larger)).isNegative();
    }

    @Test
    @DisplayName("Should produce valid zero money")
    void shouldProduceZeroMoney() {
        Money zero = Money.zero();
        assertThat(zero.amount()).isEqualTo("0.00");
        assertThat(zero.currency()).isEqualTo("BRL");
        assertThat(zero.isZero()).isTrue();
    }
}
