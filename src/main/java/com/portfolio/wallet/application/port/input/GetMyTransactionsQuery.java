package com.portfolio.wallet.application.port.input;

import com.portfolio.wallet.application.port.output.TransactionRepositoryPort;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;

public interface GetMyTransactionsQuery {

    TransactionRepositoryPort.PagedResult<Transaction> getTransactions(UserId ownerId, int page, int size);
}
