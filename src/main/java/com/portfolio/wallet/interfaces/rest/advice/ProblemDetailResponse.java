package com.portfolio.wallet.interfaces.rest.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        List<ValidationError> errors
) {
    public record ValidationError(String field, String message) {}
}
