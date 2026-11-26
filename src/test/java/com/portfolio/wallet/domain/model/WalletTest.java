package com.portfolio.wallet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.wallet.domain.event.DepositCompletedEvent;
import com.portfolio.wallet.domain.event.DomainEvent;
import com.portfolio.wallet.domain.event.TransferCompletedEvent;
import com.portfolio.wallet.domain.event.WithdrawalCompletedEvent;
import com.portfolio.wallet.domain.exception.InsufficientFundsException;
import com.portfolio.wallet.domain.exception.SameWalletTransferException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    @DisplayName("Should create wallet with initial zero balance and version 0")
    void shouldCreateWalletWithZeroBalance() {
        UserId ownerId = UserId.generate();
        Wallet wallet = Wallet.create(ownerId);

        assertThat(wallet.id()).isNotNull();
        assertThat(wallet.ownerId()).isEqualTo(ownerId);
        assertThat(wallet.balance()).isEqualTo(Money.zero());
        assertThat(wallet.version()).isEqualTo(0L);
        assertThat(wallet.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should deposit money and generate DEPOSIT transaction and domain event")
    void shouldDepositMoney() {
        Wallet wallet = Wallet.create(UserId.generate());
        Money depositAmount = Money.of("250.75", "BRL");
        UUID correlationId = UUID.randomUUID();

        Transaction tx = wallet.deposit(depositAmount, correlationId);

        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualTo(depositAmount);
        assertThat(tx.getBalanceAfter().amount()).isEqualTo("250.75");
        assertThat(wallet.balance().amount()).isEqualTo("250.75");

        List<DomainEvent> events = wallet.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(DepositCompletedEvent.class);
        DepositCompletedEvent event = (DepositCompletedEvent) events.get(0);
        assertThat(event.amount()).isEqualTo(depositAmount);
        assertThat(event.correlationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should withdraw money when sufficient balance exists")
    void shouldWithdrawMoneyWithSufficientBalance() {
        Wallet wallet = Wallet.create(UserId.generate());
        wallet.deposit(Money.of("500.00", "BRL"));
        wallet.pullDomainEvents(); // clear previous events

        Money withdrawAmount = Money.of("200.00", "BRL");
        UUID correlationId = UUID.randomUUID();
        Transaction tx = wallet.withdraw(withdrawAmount, correlationId);

        assertThat(tx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(tx.getAmount()).isEqualTo(withdrawAmount);
        assertThat(tx.getBalanceAfter().amount()).isEqualTo("300.00");
        assertThat(wallet.balance().amount()).isEqualTo("300.00");

        List<DomainEvent> events = wallet.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(WithdrawalCompletedEvent.class);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when withdrawing more than current balance")
    void shouldFailWithdrawalOnInsufficientBalance() {
        Wallet wallet = Wallet.create(UserId.generate());
        wallet.deposit(Money.of("50.00", "BRL"));

        assertThatThrownBy(() -> wallet.withdraw(Money.of("100.00", "BRL")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("Should transfer money atomically between two distinct wallets")
    void shouldTransferMoneyBetweenWallets() {
        Wallet sender = Wallet.create(UserId.generate());
        Wallet receiver = Wallet.create(UserId.generate());

        sender.deposit(Money.of("1000.00", "BRL"));
        sender.pullDomainEvents();

        Money transferAmount = Money.of("350.00", "BRL");
        UUID correlationId = UUID.randomUUID();

        Wallet.TransferResult result = sender.transferTo(receiver, transferAmount, "Payment for dinner", correlationId);

        assertThat(sender.balance().amount()).isEqualTo("650.00");
        assertThat(receiver.balance().amount()).isEqualTo("350.00");

        assertThat(result.senderTransaction().getType()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(result.senderTransaction().getAmount()).isEqualTo(transferAmount);
        assertThat(result.senderTransaction().getCounterpartyWalletId()).isEqualTo(receiver.id());

        assertThat(result.receiverTransaction().getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(result.receiverTransaction().getAmount()).isEqualTo(transferAmount);
        assertThat(result.receiverTransaction().getCounterpartyWalletId()).isEqualTo(sender.id());

        List<DomainEvent> events = sender.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TransferCompletedEvent.class);
        TransferCompletedEvent event = (TransferCompletedEvent) events.get(0);
        assertThat(event.destinationWalletId()).isEqualTo(receiver.id().toString());
    }

    @Test
    @DisplayName("Should throw SameWalletTransferException when attempting to transfer to self")
    void shouldThrowWhenTransferringToSelf() {
        Wallet wallet = Wallet.create(UserId.generate());
        wallet.deposit(Money.of("100.00", "BRL"));

        assertThatThrownBy(() -> wallet.transferTo(wallet, Money.of("50.00", "BRL"), "Self transfer"))
                .isInstanceOf(SameWalletTransferException.class)
                .hasMessageContaining("Cannot transfer to the same wallet");
    }
}
