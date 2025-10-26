package ru.mephi.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Configuration
public class GatewayConfig {

    public static final String CORRELATION_HEADER = "X-Request-Id";

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("booking", r -> r.path("/api/bookings/**", "/booking/**", "/bookings/**", "/user/**")
                        .uri("lb://booking-service"))
                .route("hotel-public", r -> r.path("/api/hotels/**", "/api/rooms", "/api/rooms/recommend")
                        .uri("lb://hotel-service"))
                .build();
    }

    @Bean
    public GlobalFilter correlationFilter() {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            String incoming = request.getHeaders().getFirst(CORRELATION_HEADER);

            // генерим id, если не пришёл
            final String correlationId = (incoming == null || incoming.isBlank())
                    ? UUID.randomUUID().toString()
                    : incoming;

            // если не было — добавим в заголовок запроса (final значение для лямбды)
            if (incoming == null || incoming.isBlank()) {
                final String headerValue = correlationId;
                exchange = exchange.mutate().request(
                        request.mutate().headers(h -> h.add(CORRELATION_HEADER, headerValue)).build()
                ).build();
            }

            MDC.put("traceId", correlationId);
            return chain.filter(exchange).then(Mono.fromRunnable(MDC::clear));
        };
    }
}
