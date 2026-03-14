package com.club.backend.service

import com.club.backend.api.dto.admin.*
import com.club.backend.domain.entity.ClubProductEntity
import com.club.backend.domain.entity.ClubProductId
import com.club.backend.domain.entity.ProductCategoryEntity
import com.club.backend.domain.entity.ProductEntity
import com.club.backend.repository.ClubProductRepository
import com.club.backend.repository.ClubRepository
import com.club.backend.repository.ProductCategoryRepository
import com.club.backend.repository.ProductRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GlobalCatalogAdminService(
    private val categoryRepository: ProductCategoryRepository,
    private val productRepository: ProductRepository,
    private val clubProductRepository: ClubProductRepository
) {
    fun listCategories(): List<AdminCategoryResponse> =
        categoryRepository.findAllByOrderBySortOrderAscIdAsc().map { it.toDto() }

    @Transactional
    fun createCategory(req: CreateCategoryRequest): AdminCategoryResponse =
        categoryRepository.save(
            ProductCategoryEntity(
                title = req.title.trim(),
                sortOrder = req.sortOrder,
                isActive = true
            )
        ).toDto()

    @Transactional
    fun deleteCategory(categoryId: Long) {
        if (productRepository.existsByCategoryId(categoryId))
            throw IllegalStateException("Нельзя удалить категорию: в ней есть товары")
        categoryRepository.deleteById(categoryId)
    }

    @Transactional
    fun updateCategory(categoryId: Long, req: UpdateCategoryRequest): AdminCategoryResponse {
        val c = categoryRepository.findById(categoryId).orElseThrow {
            EntityNotFoundException("Category $categoryId not found")
        }
        c.title = req.title.trim()
        c.sortOrder = req.sortOrder
        c.isActive = req.isActive
        return categoryRepository.save(c).toDto()
    }

    fun listProducts(): List<AdminProductResponse> =
        productRepository.findAllWithCategoryOrderByIdAsc().map { it.toDto() }

    @Transactional
    fun createProduct(req: CreateProductRequest): AdminProductResponse {
        val category = categoryRepository.findById(req.categoryId).orElseThrow {
            EntityNotFoundException("Category ${req.categoryId} not found")
        }
        return productRepository.save(
            ProductEntity(
                category = category,
                title = req.title.trim(),
                description = req.description,
                isActive = req.isActive
            )
        ).toDto()
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        if (clubProductRepository.existsByIdProductId(productId))
            throw IllegalStateException("Нельзя удалить товар: он привязан к каталогу клуба")
        productRepository.deleteById(productId)
    }

    @Transactional
    fun updateProduct(productId: Long, req: UpdateProductRequest): AdminProductResponse {
        val category = categoryRepository.findById(req.categoryId).orElseThrow {
            EntityNotFoundException("Category ${req.categoryId} not found")
        }
        val p = productRepository.findById(productId).orElseThrow {
            EntityNotFoundException("Product $productId not found")
        }
        p.category = category
        p.title = req.title.trim()
        p.description = req.description
        p.isActive = req.isActive
        return productRepository.save(p).toDto()
    }

    private fun ProductCategoryEntity.toDto() = AdminCategoryResponse(
        id = requireNotNull(id),
        title = title,
        sortOrder = sortOrder,
        isActive = isActive
    )

    private fun ProductEntity.toDto() = AdminProductResponse(
        id = requireNotNull(id),
        categoryId = requireNotNull(category.id),
        title = title,
        description = description,
        imageUrl = imageUrl,
        isActive = isActive
    )
}

@Service
class ClubCatalogAdminService(
    private val categoryRepository: ProductCategoryRepository,
    private val productRepository: ProductRepository,
    private val clubRepository: ClubRepository,
    private val clubProductRepository: ClubProductRepository,
    private val auditService: AuditService
) {
    fun getClubCatalog(clubId: Long): AdminClubCatalogResponse {
        val categories = categoryRepository.findAllByOrderBySortOrderAscIdAsc()
            .map { AdminCategoryResponse(it.id!!, it.title, it.sortOrder, it.isActive) }

        val allProducts = productRepository.findAllWithCategoryOrderByIdAsc()
        val clubProducts = clubProductRepository.findAllByClubIdWithProductAndCategory(clubId)
            .associateBy { it.product.id!! }

        val products = allProducts.map { p ->
            val cp = clubProducts[p.id!!]
            AdminClubCatalogProductResponse(
                productId = p.id!!,
                categoryId = p.category.id!!,
                productTitle = p.title,
                description = p.description,
                imageUrl = p.imageUrl,
                productIsActive = p.isActive,
                isLinkedToClub = cp != null,
                clubPriceRub = cp?.priceRub,
                clubIsAvailable = cp?.isAvailable
            )
        }
        return AdminClubCatalogResponse(categories = categories, products = products)
    }

    @Transactional
    fun unlinkClubProduct(actorUserId: Long, clubId: Long, productId: Long) {
        val id = ClubProductId(clubId = clubId, productId = productId)
        val existing = clubProductRepository.findById(id).orElseThrow {
            EntityNotFoundException("ClubProduct $clubId:$productId not found")
        }
        clubProductRepository.delete(existing)
        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "CLUB_PRODUCT_UNLINK",
            entityType = "ClubProduct",
            entityId = "$clubId:$productId",
            before = mapOf("clubId" to clubId, "productId" to productId, "priceRub" to existing.priceRub),
            after = null
        )
    }

    @Transactional
    fun upsertClubProduct(actorUserId: Long, clubId: Long, productId: Long, req: UpsertClubProductRequest) {
        val club = clubRepository.findById(clubId).orElseThrow { EntityNotFoundException("Club $clubId not found") }
        val product = productRepository.findById(productId).orElseThrow { EntityNotFoundException("Product $productId not found") }

        val id = ClubProductId(clubId = clubId, productId = productId)
        val existing = clubProductRepository.findById(id).orElse(null)
        val before = existing?.let {
            mapOf(
                "clubId" to clubId,
                "productId" to productId,
                "priceRub" to it.priceRub,
                "isAvailable" to it.isAvailable
            )
        }
        val entity = existing ?: ClubProductEntity(
            id = id,
            club = club,
            product = product,
            priceRub = req.priceRub,
            isAvailable = req.isAvailable
        )

        entity.priceRub = req.priceRub
        entity.isAvailable = req.isAvailable
        val saved = clubProductRepository.save(entity)

        val after = mapOf(
            "clubId" to clubId,
            "productId" to productId,
            "priceRub" to saved.priceRub,
            "isAvailable" to saved.isAvailable
        )

        auditService.log(
            actorUserId = actorUserId,
            clubId = clubId,
            action = "CLUB_PRODUCT_UPSERT",
            entityType = "ClubProduct",
            entityId = "$clubId:$productId",
            before = before,
            after = after
        )
    }
}
