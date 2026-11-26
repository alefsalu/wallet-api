package com.portfolio.wallet.application.service;

import com.portfolio.wallet.application.port.input.DepositUseCase;
import com.portfolio.wallet.application.port.input.WithdrawUseCase;
import com.portfolio.wallet.application.port.output.OutboxRepositoryPort;
import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.event.DomainEvent;
import com.portfolio.wallet.domain.exception.WalletNotFoundException;
import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletMutationService implements DepositUseCase, WithdrawUseCase {

    private final WalletRepositoryPort walletRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final OutboxRepositoryPort outboxRepository;

    public WalletMutationService(
            WalletRepositoryPort walletRepository,
            TransactionRepositoryPort transactionRepository,
            OutboxRepositoryPort outboxRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public Transaction deposit(UserId userId, Money amount, UUID correlationId) {
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        Transaction tx = wallet.deposit(amount, correlationId);
        walletRepository.save(wallet);
        transactionRepository.save(tx);

        for (DomainEvent event : wallet.pullDomainEvents()) {
            outboxRepository.save(event, correlationId);
        }

        return tx;
    }

    @Override
    @Transactional
    public Transaction withdraw(UserId userId, Money amount, UUID correlationId) {
        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        Transaction tx = wallet.withdraw(amount, correlationId);
        walletRepository.save(wallet);
        transactionRepository.save(tx);

        for (DomainEvent event : wallet.pullDomainEvents()) {
            outboxRepository.save(event, correlationId);
        }

        return tx;
    }
}
