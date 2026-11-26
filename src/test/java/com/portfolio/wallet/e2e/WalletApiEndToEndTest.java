package com.portfolio.wallet.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.wallet.infrastructure.messaging.EventEnvelope;
import com.portfolio.wallet.infrastructure.messaging.RabbitMqConfig;
import com.portfolio.wallet.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.portfolio.wallet.interfaces.rest.dto.AmountRequest;
import com.portfolio.wallet.interfaces.rest.dto.LoginRequest;
import com.portfolio.wallet.interfaces.rest.dto.RegisterRequest;
import com.portfolio.wallet.interfaces.rest.dto.TokenPairResponse;
import com.portfolio.wallet.interfaces.rest.dto.TransactionResponse;
import com.portfolio.wallet.interfaces.rest.dto.TransferRequest;
import com.portfolio.wallet.interfaces.rest.dto.UserResponse;
import com.portfolio.wallet.interfaces.rest.dto.WalletResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class WalletApiEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_e2e")
            .withUsername("wallet")
            .withPassword("secret");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Test
    @DisplayName("Should execute full digital wallet workflow: register, login, deposit, idempotent replay, transfer and outbox event publishing")
    void shouldExecuteFullWalletLifecycle() throws Exception {
        // 1. Register Alice and Bob
        String aliceEmail = "alice_" + UUID.randomUUID() + "@example.com";
        String bobEmail = "bob_" + UUID.randomUUID() + "@example.com";

        ResponseEntity<UserResponse> regAlice = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("Alice Smith", aliceEmail, "StrongPassword123!"),
                UserResponse.class);
        assertThat(regAlice.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(regAlice.getBody()).isNotNull();
        UUID aliceUserId = regAlice.getBody().id();

        ResponseEntity<UserResponse> regBob = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("Bob Jones", bobEmail, "StrongPassword123!"),
                UserResponse.class);
        assertThat(regBob.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(regBob.getBody()).isNotNull();
        UUID bobUserId = regBob.getBody().id();

        // 2. Login Alice and obtain JWT
        ResponseEntity<TokenPairResponse> loginAlice = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(aliceEmail, "StrongPassword123!"),
                TokenPairResponse.class);
        assertThat(loginAlice.getStatusCode()).isEqualTo(HttpStatus.OK);
        String aliceToken = loginAlice.getBody().accessToken();

        // Login Bob and fetch Bob's wallet ID
        ResponseEntity<TokenPairResponse> loginBob = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(bobEmail, "StrongPassword123!"),
                TokenPairResponse.class);
        String bobToken = loginBob.getBody().accessToken();

        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.setBearerAuth(bobToken);
        ResponseEntity<WalletResponse> bobWalletRes = restTemplate.exchange(
                "/api/v1/wallets/me",
                HttpMethod.GET,
                new HttpEntity<>(bobHeaders),
                WalletResponse.class);
        UUID bobWalletId = bobWalletRes.getBody().id();

        // 3. Deposit 1000.00 BRL to Alice's wallet with Idempotency-Key
        String depositKey = UUID.randomUUID().toString();
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.setBearerAuth(aliceToken);
        aliceHeaders.set("Idempotency-Key", depositKey);
        aliceHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AmountRequest> depositEntity = new HttpEntity<>(new AmountRequest("1000.00"), aliceHeaders);
        ResponseEntity<TransactionResponse> depositRes = restTemplate.postForEntity(
                "/api/v1/wallets/me/deposits",
                depositEntity,
                TransactionResponse.class);

        assertThat(depositRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(depositRes.getBody().amount().amount()).isEqualTo("1000.00");
        assertThat(depositRes.getBody().balanceAfter().amount()).isEqualTo("1000.00");

        // 4. Re-send exact deposit with same Idempotency-Key -> assert Idempotent-Replayed: true
        ResponseEntity<TransactionResponse> replayedDeposit = restTemplate.postForEntity(
                "/api/v1/wallets/me/deposits",
                depositEntity,
                TransactionResponse.class);

        assertThat(replayedDeposit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replayedDeposit.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replayedDeposit.getBody().balanceAfter().amount()).isEqualTo("1000.00");

        // 5. Transfer 350.00 BRL from Alice to Bob with Idempotency-Key
        String transferKey = UUID.randomUUID().toString();
        HttpHeaders transferHeaders = new HttpHeaders();
        transferHeaders.setBearerAuth(aliceToken);
        transferHeaders.set("Idempotency-Key", transferKey);
        transferHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransferRequest> transferEntity = new HttpEntity<>(
                new TransferRequest(bobWalletId, "350.00", "Payment for groceries"),
                transferHeaders);

        ResponseEntity<TransactionResponse> transferRes = restTemplate.postForEntity(
                "/api/v1/transfers",
                transferEntity,
                TransactionResponse.class);

        assertThat(transferRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(transferRes.getBody().amount().amount()).isEqualTo("350.00");
        assertThat(transferRes.getBody().balanceAfter().amount()).isEqualTo("650.00");

        // 6. Verify Alice balance is 650.00 and Bob balance is 350.00
        ResponseEntity<WalletResponse> aliceWallet = restTemplate.exchange(
                "/api/v1/wallets/me",
                HttpMethod.GET,
                new HttpEntity<>(aliceHeaders),
                WalletResponse.class);
        assertThat(aliceWallet.getBody().balance().amount()).isEqualTo("650.00");

        ResponseEntity<WalletResponse> bobWallet = restTemplate.exchange(
                "/api/v1/wallets/me",
                HttpMethod.GET,
                new HttpEntity<>(bobHeaders),
                WalletResponse.class);
        assertThat(bobWallet.getBody().balance().amount()).isEqualTo("350.00");

        // 7. Verify outbox contains transfer event
        assertThat(outboxRepository.findAll()).anyMatch(o -> "wallet.transfer.completed.v1".equals(o.getEventType()));
    }
}
