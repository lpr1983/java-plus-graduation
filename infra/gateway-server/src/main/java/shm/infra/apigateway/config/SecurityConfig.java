package shm.infra.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new MapReactiveUserDetailsService(
                User.builder()
                        .username("ivan")
                        .password(passwordEncoder.encode("ivan"))
                        .roles("USER")
                        .build(),
                User.builder()
                        .username("anna")
                        .password(passwordEncoder.encode("anna"))
                        .roles("USER", "ADMIN")
                        .build()
        );
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(exchanges -> {
                    configureCommonRoutes(exchanges);
                    configureProductRoutes(exchanges);
                    configureCategoryRoutes(exchanges);
                    configureInventoryRoutes(exchanges);
                    configureOrderRoutes(exchanges);
                    exchanges.anyExchange().denyAll();
                })
                .build();
    }

    private void configureCommonRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
    }

    private void configureProductRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges.pathMatchers(HttpMethod.GET, "/api/products/**").permitAll();
        exchanges.pathMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN");
    }

    private void configureCategoryRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges.pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll();
        exchanges.pathMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PATCH, "/api/categories/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN");
    }

    private void configureInventoryRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges.pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll();
        exchanges.pathMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.PATCH, "/api/inventory/**").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.DELETE, "/api/inventory/**").hasRole("ADMIN");
    }

    private void configureOrderRoutes(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges.pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN");
        exchanges.pathMatchers(HttpMethod.GET, "/api/orders/by-email").hasRole("USER");
        exchanges.pathMatchers(HttpMethod.GET, "/api/orders/{id}").hasRole("USER");
        exchanges.pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER");
    }
}
