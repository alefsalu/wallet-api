package com.portfolio.wallet.interfaces.rest.controller;

import com.portfolio.wallet.application.port.input.DepositUseCase;
import com.portfolio.wallet.application.port.input.WithdrawUseCase;
import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.interfaces.rest.dto.AmountRequest;
import com.portfolio.wallet.interfaces.rest.dto.TransactionResponse;
import com.portfolio.wallet.interfaces.rest.filter.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets/me")
@Tag(name = "Wallet Mutations", description = "Deposit and withdraw funds with distributed idempotency")
@SecurityRequirement(name = "bearerAuth")
public class WalletMutationController {

    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;

    public WalletMutationController(DepositUseCase depositUseCase, WithdrawUseCase withdrawUseCase) {
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
    }

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Deposit funds into wallet",
            parameters = {
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Unique UUID ensuring idempotent execution")
            })
    public TransactionResponse deposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AmountRequest request,
            HttpServletRequest servletRequest) {

        UserId userId = UserId.of(jwt.getSubject());
        Money amount = request.toMoney();
        UUID correlationId = extractCorrelationId(servletRequest);

        Transaction transaction = depositUseCase.deposit(userId, amount, correlationId);
        return TransactionResponse.fromDomain(transaction);
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Withdraw funds from wallet",
            parameters = {
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Unique UUID ensuring idempotent execution")
            })
    public TransactionResponse withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AmountRequest request,
            HttpServletRequest servletRequest) {

        UserId userId = UserId.of(jwt.getSubject());
        Money amount = request.toMoney();
        UUID correlationId = extractCorrelationId(servletRequest);

        Transaction transaction = withdrawUseCase.withdraw(userId, amount, correlationId);
        return TransactionResponse.fromDomain(transaction);
    }

    private UUID extractCorrelationId(HttpServletRequest request) {
        String corrHeader = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (corrHeader != null) {
            try {
                return UUID.fromString(corrHeader);
            } catch (Exception ignored) {
            }
        }
        return UUID.randomUUID();
    }
}
