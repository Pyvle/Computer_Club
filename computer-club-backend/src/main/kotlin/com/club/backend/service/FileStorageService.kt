package com.club.backend.service

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.SetBucketPolicyArgs
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class FileStorageService(
    private val minioClient: MinioClient,
    @Value("\${app.minio.endpoint}") private val endpoint: String,
    @Value("\${app.minio.bucket}") private val bucket: String
) {

    @PostConstruct
    fun init() {
        val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            // разрешаем публичное чтение объектов — клиенты загружают картинки напрямую из MinIO
            val policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::$bucket/*"]
                    }]
                }
            """.trimIndent()
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build()
            )
        }
    }

    fun uploadClubImage(clubId: Long, file: MultipartFile): String {
        val ext = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        return upload("clubs/$clubId/${UUID.randomUUID()}.$ext", file)
    }

    fun uploadProductImage(productId: Long, file: MultipartFile): String {
        val ext = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        return upload("products/$productId/${UUID.randomUUID()}.$ext", file)
    }

    private fun upload(objectName: String, file: MultipartFile): String {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType ?: "application/octet-stream")
                .build()
        )
        return "$endpoint/$bucket/$objectName"
    }
}
