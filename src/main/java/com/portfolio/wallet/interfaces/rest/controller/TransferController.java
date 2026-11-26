package com.portfolio.wallet.interfaces.rest.controller;

import com.portfolio.wallet.application.port.input.TransferMoneyUseCase;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.interfaces.rest.dto.TransactionResponse;
import com.portfolio.wallet.interfaces.rest.dto.TransferRequest;
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
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Atomic peer-to-peer wallet transfers with deadlock prevention")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferMoneyUseCase transferMoneyUseCase;

    public TransferController(TransferMoneyUseCase transferMoneyUseCase) {
        this.transferMoneyUseCase = transferMoneyUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Transfer money to another digital wallet",
            parameters = {
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Unique UUID ensuring idempotent execution")
            })
    public TransactionResponse transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest servletRequest) {

        UserId sourceUserId = UserId.of(jwt.getSubject());
        UUID correlationId = extractCorrelationId(servletRequest);

        TransferMoneyUseCase.TransferCommand command = new TransferMoneyUseCase.TransferCommand(
                sourceUserId,
                request.toWalletId(),
                request.toMoney(),
                request.description(),
                correlationId);

        Transaction transaction = transferMoneyUseCase.transfer(command);
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
