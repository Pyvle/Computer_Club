package com.club.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ComputerClubBackendApplication

fun main(args: Array<String>) {
	runApplication<ComputerClubBackendApplication>(*args)
}
