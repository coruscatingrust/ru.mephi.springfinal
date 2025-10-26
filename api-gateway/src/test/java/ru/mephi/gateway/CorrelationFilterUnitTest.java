package ru.mephi.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import ru.mephi.gateway.config.GatewayConfig;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationFilterUnitTest {

    @Test
    void addsHeaderWhenMissing() {
        GatewayConfig cfg = new GatewayConfig();
        var filter = cfg.correlationFilter();

        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        GatewayFilterChain chain = exchange -> {
            ServerHttpRequest req = exchange.getRequest();
            assertThat(req.getHeaders().getFirst(GatewayConfig.CORRELATION_HEADER)).isNotBlank();
            return Mono.empty();
        };

        filter.filter(ex, chain).block();
    }

    @Test
    void keepsHeaderWhenPresent() {
        String existing = "abc-123";
        MockServerWebExchange ex = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header(GatewayConfig.CORRELATION_HEADER, existing).build());

        GatewayConfig cfg = new GatewayConfig();
        var filter = cfg.correlationFilter();

        GatewayFilterChain chain = exchange -> {
            assertThat(exchange.getRequest().getHeaders().getFirst(GatewayConfig.CORRELATION_HEADER)).isEqualTo(existing);
            return Mono.empty();
        };

        filter.filter(ex, chain).block();
    }
}
