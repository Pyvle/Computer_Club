package com.club.backend.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v1/ping",
                    "/actuator/health",
                    "/api-docs/**",
                    "/swagger/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/v1/auth/**",
                    "/api/v1/admin/auth/login",
                    // статические файлы (фото клубов и товаров)
                    "/uploads/**",
                    // публичный каталог — просмотр без авторизации
                    "/api/v1/clubs",
                    "/api/v1/clubs/*",
                    "/api/v1/product-categories",
                    "/api/v1/clubs/*/products",
                    "/api/v1/clubs/*/seats",
                    "/api/v1/clubs/*/seats/availability",
                    "/api/v1/clubs/*/seats/max-availability",
                    "/api/v1/clubs/*/floorplan",
                    "/api/v1/clubs/*/floorplan-with-availability",
                    "/api/v1/clubs/*/seat-specs",
                    "/api/v1/clubs/*/time-packages",
                    "/api/v1/clubs/*/seat-prices"
                ).permitAll()

                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()
}
