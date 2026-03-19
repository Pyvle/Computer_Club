package com.club.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(
    private val storageProperties: StorageProperties
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath = Paths.get(storageProperties.uploadDir).toAbsolutePath().toUri().toString()
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath)
    }
}
