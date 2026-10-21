package application.infrastructure.security;

import application.domain.valuesObjects.UserRole;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Wires the stateless, token-based security of the API.
 *
 * <p>The public catalogue and the authentication endpoints are open: the catalogue is the public
 * face of the marketplace, and a visitor must be able to browse it before registering. Everything
 * else demands an authenticated user, which is the entry-boundary counterpart of RG-01.</p>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   SecurityErrorResponder securityErrorResponder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorResponder)
                        .accessDeniedHandler(securityErrorResponder))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Defence in depth: each role prefix is closed to the other roles here, before
                        // the domain validates role and ownership again (RG-03).
                        .requestMatchers("/api/v1/buyer/**").hasRole(UserRole.BUYER.code())
                        .requestMatchers("/api/v1/seller/**").hasRole(UserRole.SELLER.code())
                        .requestMatchers("/api/v1/logistics/**").hasRole(UserRole.LOGISTICS_OPERATOR.code())
                        .requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMINISTRATOR.code())
                        .requestMatchers("/api/v1/supervisor/**").hasRole(UserRole.SUPERVISOR.code())
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Allows a browser-based frontend served from another origin to call this API.
     *
     * <p>The allowed origins are configured, never a wildcard: this API is authenticated with a
     * bearer token, and opening it to any origin would let any site spend a user's session.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${nexusmarket.security.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
