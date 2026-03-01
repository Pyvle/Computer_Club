package com.club.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.club.backend.domain.entity.GlobalRole

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            try {
                if (jwtService.getType(token) == "access") {
                    val userId = jwtService.getUserId(token)
                    val globalRole = jwtService.getGlobalRole(token)
                    val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
                    if (globalRole == GlobalRole.GLOBAL_ADMIN) {
                        authorities += SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN")
                    }
                    val auth = UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        authorities
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (_: Exception) {
                // invalid token -> proceed as anonymous
            }
        }
        filterChain.doFilter(request, response)
    }
}
