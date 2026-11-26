package com.portfolio.wallet.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomMetrics {

    private final Counter transfersCounter;
    private final Counter depositsCounter;
    private final Counter withdrawalsCounter;
    private final Counter idempotencyReplayedCounter;

    public CustomMetrics(MeterRegistry registry) {
        this.transfersCounter = Counter.builder("wallet.transfers.count")
                .description("Total number of completed wallet transfers")
                .register(registry);

        this.depositsCounter = Counter.builder("wallet.deposits.count")
                .description("Total number of completed deposits")
                .register(registry);

        this.withdrawalsCounter = Counter.builder("wallet.withdrawals.count")
                .description("Total number of completed withdrawals")
                .register(registry);

        this.idempotencyReplayedCounter = Counter.builder("wallet.idempotency.replayed.count")
                .description("Total number of replayed idempotent responses")
                .register(registry);
    }

    public void incrementTransfers() {
        transfersCounter.increment();
    }

    public void incrementDeposits() {
        depositsCounter.increment();
    }

    public void incrementWithdrawals() {
        withdrawalsCounter.increment();
    }

    public void incrementIdempotencyReplayed() {
        idempotencyReplayedCounter.increment();
    }
}
