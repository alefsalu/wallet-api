package com.portfolio.wallet.interfaces.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    @DisplayName("Should generate correlation ID when missing and propagate in response header and MDC")
    void shouldGenerateCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            String mdcValue = MDC.get("correlationId");
            assertThat(mdcValue).isNotNull();
            assertThat(UUID.fromString(mdcValue)).isNotNull();
        });

        String headerValue = response.getHeader("X-Correlation-Id");
        assertThat(headerValue).isNotNull();
        assertThat(UUID.fromString(headerValue)).isNotNull();
        assertThat(MDC.get("correlationId")).isNull(); // Cleaned up
    }

    @Test
    @DisplayName("Should preserve existing valid UUID correlation ID")
    void shouldPreserveExistingCorrelationId() throws ServletException, IOException {
        String existingUuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", existingUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get("correlationId")).isEqualTo(existingUuid);
        });

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(existingUuid);
    }
}
