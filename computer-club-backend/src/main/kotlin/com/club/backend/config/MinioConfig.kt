package com.club.backend.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig(
    @Value("\${app.minio.endpoint}") private val endpoint: String,
    @Value("\${app.minio.access-key}") private val accessKey: String,
    @Value("\${app.minio.secret-key}") private val secretKey: String
) {
    @Bean
    fun minioClient(): MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()
}
