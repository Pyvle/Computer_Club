package com.club.backend.api.controller.admin

import com.club.backend.api.dto.admin.*
import com.club.backend.service.GlobalCatalogAdminService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/global/catalog")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
class GlobalCatalogAdminController(
    private val globalCatalogAdminService: GlobalCatalogAdminService
) {

    @GetMapping("/categories")
    fun listCategories(): List<AdminCategoryResponse> = globalCatalogAdminService.listCategories()

    @PostMapping("/categories")
    fun createCategory(@RequestBody req: CreateCategoryRequest): AdminCategoryResponse =
        globalCatalogAdminService.createCategory(req)

    @PutMapping("/categories/{categoryId}")
    fun updateCategory(
        @PathVariable categoryId: Long,
        @RequestBody req: UpdateCategoryRequest
    ): AdminCategoryResponse = globalCatalogAdminService.updateCategory(categoryId, req)

    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@PathVariable categoryId: Long) = globalCatalogAdminService.deleteCategory(categoryId)

    @GetMapping("/products")
    fun listProducts(): List<AdminProductResponse> = globalCatalogAdminService.listProducts()

    @PostMapping("/products")
    fun createProduct(@RequestBody req: CreateProductRequest): AdminProductResponse =
        globalCatalogAdminService.createProduct(req)

    @PutMapping("/products/{productId}")
    fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody req: UpdateProductRequest
    ): AdminProductResponse = globalCatalogAdminService.updateProduct(productId, req)

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable productId: Long) = globalCatalogAdminService.deleteProduct(productId)
}
