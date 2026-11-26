package com.portfolio.wallet.application.service;

import com.portfolio.wallet.application.port.input.GetMyTransactionsQuery;
import com.portfolio.wallet.application.port.input.GetMyWalletQuery;
import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.exception.WalletNotFoundException;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WalletQueryService implements GetMyWalletQuery, GetMyTransactionsQuery {

    private final WalletRepositoryPort walletRepository;
    private final TransactionRepositoryPort transactionRepository;

    public WalletQueryService(
            WalletRepositoryPort walletRepository,
            TransactionRepositoryPort transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet getWallet(UserId ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + ownerId));
    }

    @Override
    public TransactionRepositoryPort.PagedResult<Transaction> getTransactions(UserId ownerId, int page, int size) {
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed maximum limit of 100");
        }
        if (size <= 0) {
            size = 20;
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }

        Wallet wallet = getWallet(ownerId);
        return transactionRepository.findByWalletId(wallet.id(), page, size);
    }
}
