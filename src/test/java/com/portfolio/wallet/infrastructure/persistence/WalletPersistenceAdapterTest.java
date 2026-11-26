package com.portfolio.wallet.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import com.portfolio.wallet.infrastructure.persistence.entity.UserJpaEntity;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class WalletPersistenceAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_test")
            .withUsername("wallet")
            .withPassword("secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WalletRepositoryPort walletRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Test
    @DisplayName("Should persist and retrieve wallet with optimistic locking version increments")
    void shouldPersistAndRetrieveWallet() {
        UUID userId = UUID.randomUUID();
        userRepository.save(new UserJpaEntity(
                userId,
                "Alice Test",
                "alice_" + userId + "@example.com",
                "hashedpassword",
                "USER",
                Instant.now()));

        Wallet wallet = Wallet.create(UserId.of(userId));
        Wallet saved = walletRepository.save(wallet);

        assertThat(saved.id()).isEqualTo(wallet.id());
        assertThat(saved.version()).isEqualTo(0L);

        Optional<Wallet> retrieved = walletRepository.findByOwnerId(UserId.of(userId));
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().balance().amount()).isEqualTo("0.00");

        // Deposit and save
        retrieved.get().deposit(Money.of("100.00", "BRL"));
        Wallet updated = walletRepository.save(retrieved.get());
        assertThat(updated.balance().amount()).isEqualTo("100.00");
        assertThat(updated.version()).isEqualTo(1L);

        // Optimistic locking test: attempt saving stale version
        Wallet staleWallet = Wallet.reconstitute(
                updated.id(),
                updated.ownerId(),
                Money.of("500.00", "BRL"),
                0L, // Stale version
                updated.createdAt());

        assertThatThrownBy(() -> walletRepository.save(staleWallet))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
