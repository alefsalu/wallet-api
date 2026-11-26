package com.portfolio.wallet.interfaces.rest.controller;

import com.portfolio.wallet.application.port.input.GetMyTransactionsQuery;
import com.portfolio.wallet.application.port.input.GetMyWalletQuery;
import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import com.portfolio.wallet.interfaces.rest.dto.PageResponse;
import com.portfolio.wallet.interfaces.rest.dto.TransactionResponse;
import com.portfolio.wallet.interfaces.rest.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets/me")
@Tag(name = "Wallet Queries", description = "Query authenticated user's wallet and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class WalletQueryController {

    private final GetMyWalletQuery getMyWalletQuery;
    private final GetMyTransactionsQuery getMyTransactionsQuery;

    public WalletQueryController(
            GetMyWalletQuery getMyWalletQuery,
            GetMyTransactionsQuery getMyTransactionsQuery) {
        this.getMyWalletQuery = getMyWalletQuery;
        this.getMyTransactionsQuery = getMyTransactionsQuery;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user's digital wallet details")
    public WalletResponse getMyWallet(@AuthenticationPrincipal Jwt jwt) {
        UserId userId = UserId.of(jwt.getSubject());
        Wallet wallet = getMyWalletQuery.getWallet(userId);
        return WalletResponse.fromDomain(wallet);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get paginated transaction history for authenticated user's wallet")
    public PageResponse<TransactionResponse> getMyTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserId userId = UserId.of(jwt.getSubject());
        TransactionRepositoryPort.PagedResult<Transaction> paged =
                getMyTransactionsQuery.getTransactions(userId, page, size);

        List<TransactionResponse> content =
                paged.content().stream().map(TransactionResponse::fromDomain).toList();

        return new PageResponse<>(
                content,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages());
    }
}
