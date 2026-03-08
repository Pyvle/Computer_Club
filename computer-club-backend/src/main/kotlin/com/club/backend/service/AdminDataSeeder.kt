package com.club.backend.service

import com.club.backend.domain.entity.GlobalRole
import com.club.backend.domain.entity.UserEntity
import com.club.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Засевает начального GLOBAL_ADMIN при первом запуске, если его нет. */
@Component
@Order(1)
class AdminDataSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val existing = userRepository.findFirstByGlobalRole(GlobalRole.GLOBAL_ADMIN).orElse(null)
        when {
            existing == null -> {
                userRepository.save(
                    UserEntity(
                        phone = "+70000000000",
                        passwordHash = passwordEncoder.encode("Admin1234!"),
                        globalRole = GlobalRole.GLOBAL_ADMIN
                    )
                )
                log.info("Created initial GLOBAL_ADMIN account (phone: +70000000000)")
            }
            existing.passwordHash == null -> {
                existing.passwordHash = passwordEncoder.encode("Admin1234!")
                userRepository.save(existing)
                log.info("Set missing password for GLOBAL_ADMIN account")
            }
        }
    }
}
