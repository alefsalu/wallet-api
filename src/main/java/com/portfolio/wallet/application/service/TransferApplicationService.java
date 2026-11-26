package com.portfolio.wallet.application.service;

import com.portfolio.wallet.application.port.input.TransferMoneyUseCase;
import com.portfolio.wallet.application.port.output.OutboxRepositoryPort;
import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.event.DomainEvent;
import com.portfolio.wallet.domain.exception.SameWalletTransferException;
import com.portfolio.wallet.domain.exception.WalletNotFoundException;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.Wallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferApplicationService implements TransferMoneyUseCase {

    private final WalletRepositoryPort walletRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final OutboxRepositoryPort outboxRepository;

    public TransferApplicationService(
            WalletRepositoryPort walletRepository,
            TransactionRepositoryPort transactionRepository,
            OutboxRepositoryPort outboxRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public Transaction transfer(TransferCommand command) {
        Wallet sourceWallet = walletRepository.findByOwnerId(command.sourceUserId())
                .orElseThrow(() -> new WalletNotFoundException("Source wallet not found for user: " + command.sourceUserId()));

        Wallet destinationWallet = walletRepository.findById(command.destinationWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Destination wallet not found: " + command.destinationWalletId()));

        if (sourceWallet.id().equals(destinationWallet.id())) {
            throw new SameWalletTransferException("Cannot transfer to the same wallet");
        }

        // Execute domain transfer logic
        Wallet.TransferResult result = sourceWallet.transferTo(
                destinationWallet,
                command.amount(),
                command.description(),
                command.correlationId());

        // Deterministic lock/save order by UUID to prevent PostgreSQL transaction deadlocks (Review Focus #1)
        if (sourceWallet.id().compareTo(destinationWallet.id()) < 0) {
            walletRepository.save(sourceWallet);
            walletRepository.save(destinationWallet);
        } else {
            walletRepository.save(destinationWallet);
            walletRepository.save(sourceWallet);
        }

        // Persist ledger transactions
        transactionRepository.save(result.senderTransaction());
        transactionRepository.save(result.receiverTransaction());

        // Persist transactional outbox domain events
        for (DomainEvent event : sourceWallet.pullDomainEvents()) {
            outboxRepository.save(event, command.correlationId());
        }

        return result.senderTransaction();
    }
}
