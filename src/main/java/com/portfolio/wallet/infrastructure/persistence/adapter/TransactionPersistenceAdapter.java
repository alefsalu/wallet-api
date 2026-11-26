package com.portfolio.wallet.infrastructure.persistence.adapter;

import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.TransactionId;
import com.portfolio.wallet.domain.model.TransactionType;
import com.portfolio.wallet.domain.model.WalletId;
import com.portfolio.wallet.infrastructure.persistence.entity.TransactionJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataTransactionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final SpringDataTransactionRepository transactionRepository;

    public TransactionPersistenceAdapter(SpringDataTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity(
                transaction.getId().value(),
                transaction.getWalletId().value(),
                transaction.getType().name(),
                transaction.getAmount().asBigDecimal(),
                transaction.getAmount().currency(),
                transaction.getBalanceAfter().asBigDecimal(),
                transaction.getCounterpartyWalletId() != null
                        ? transaction.getCounterpartyWalletId().value()
                        : null,
                transaction.getDescription(),
                transaction.getCreatedAt());

        TransactionJpaEntity saved = transactionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public PagedResult<Transaction> findByWalletId(WalletId walletId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionJpaEntity> paged = transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId.value(), pageRequest);

        List<Transaction> content = paged.getContent().stream().map(this::toDomain).toList();
        return new PagedResult<>(content, paged.getNumber(), paged.getSize(), paged.getTotalElements(), paged.getTotalPages());
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                TransactionId.of(entity.getId()),
                WalletId.of(entity.getWalletId()),
                TransactionType.valueOf(entity.getType()),
                Money.of(entity.getAmount()),
                Money.fromBalance(entity.getBalanceAfter()),
                entity.getCounterpartyWalletId() != null
                        ? WalletId.of(entity.getCounterpartyWalletId())
                        : null,
                entity.getDescription(),
                entity.getCreatedAt());
    }
}
