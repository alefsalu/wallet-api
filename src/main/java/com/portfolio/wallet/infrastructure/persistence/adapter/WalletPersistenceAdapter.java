package com.portfolio.wallet.infrastructure.persistence.adapter;

import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import com.portfolio.wallet.domain.model.WalletId;
import com.portfolio.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataWalletRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class WalletPersistenceAdapter implements WalletRepositoryPort {

    private final SpringDataWalletRepository walletRepository;

    public WalletPersistenceAdapter(SpringDataWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = new WalletJpaEntity(
                wallet.id().value(),
                wallet.ownerId().value(),
                wallet.balance().asBigDecimal(),
                wallet.balance().currency(),
                wallet.version(),
                wallet.createdAt());

        WalletJpaEntity saved = walletRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(WalletId id) {
        return walletRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findByOwnerId(UserId ownerId) {
        return walletRepository.findByOwnerId(ownerId.value()).map(this::toDomain);
    }

    private Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.reconstitute(
                WalletId.of(entity.getId()),
                UserId.of(entity.getOwnerId()),
                Money.fromBalance(entity.getBalanceAmount()),
                entity.getVersion() != null ? entity.getVersion() : 0L,
                entity.getCreatedAt());
    }
}
