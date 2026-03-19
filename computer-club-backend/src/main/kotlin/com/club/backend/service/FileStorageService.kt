package com.club.backend.service

import com.club.backend.config.StorageProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileStorageService(private val storageProperties: StorageProperties) {

    private val root: Path = Paths.get(storageProperties.uploadDir).toAbsolutePath()

    fun saveClubImage(file: MultipartFile): String {
        require(!file.isEmpty) { "Файл пустой" }

        val extension = when (file.contentType) {
            "image/jpeg" -> ".jpg"
            "image/png"  -> ".png"
            "image/webp" -> ".webp"
            else -> throw IllegalArgumentException("Неподдерживаемый тип: ${file.contentType}")
        }

        val dir = root.resolve("clubs")
        Files.createDirectories(dir)

        val fileName = "${UUID.randomUUID()}$extension"
        file.inputStream.use { Files.copy(it, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING) }

        return "/uploads/clubs/$fileName"
    }

    /** Удаляет файл, если путь указывает на локальное хранилище. */
    fun deleteIfLocal(path: String?) {
        if (path.isNullOrBlank() || !path.startsWith("/uploads/")) return
        val target = root.resolve(path.removePrefix("/uploads/")).normalize()
        // проверяем что путь не выходит за пределы root (path traversal)
        if (target.startsWith(root)) Files.deleteIfExists(target)
    }
}
