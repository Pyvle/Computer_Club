package com.example.computerclub.data.network

import com.example.computerclub.data.network.dto.*
import retrofit2.http.*

interface AuthApi {
    @POST("/api/v1/auth/otp/request")
    suspend fun requestOtp(@Body dto: OtpRequestDto): OtpRequestResponse

    @POST("/api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body dto: OtpVerifyDto): AuthTokensResponse

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(@Body dto: RefreshDto): AuthTokensResponse

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body dto: LogoutDto)

    @GET("/api/v1/me")
    suspend fun me(): MeResponse
}

interface ClubsApi {
    @GET("/api/v1/clubs")
    suspend fun getClubs(): List<ClubResponseDto>

    @GET("/api/v1/clubs/available")
    suspend fun getAvailableClubs(): List<AvailableClubResponseDto>
}

interface ProductApi {
    @GET("/api/v1/product-categories")
    suspend fun getCategories(): List<ProductCategoryResponseDto>

    @GET("/api/v1/clubs/{clubId}/products")
    suspend fun getClubProducts(@Path("clubId") clubId: Long): List<ClubProductResponseDto>
}

interface CartApi {
    @PUT("/api/v1/cart/club")
    suspend fun selectClub(@Body dto: SelectClubRequestDto): CartResponseDto

    @GET("/api/v1/cart")
    suspend fun getCart(@Query("clubId") clubId: Long): CartResponseDto

    @POST("/api/v1/cart/products")
    suspend fun addProduct(
        @Query("clubId") clubId: Long,
        @Body dto: AddCartProductRequestDto
    ): CartResponseDto

    @POST("/api/v1/cart/bookings")
    suspend fun addBooking(
        @Query("clubId") clubId: Long,
        @Body dto: AddCartBookingRequestDto
    ): CartResponseDto

    @POST("/api/v1/cart/bookings/{lineId}/seats")
    suspend fun setBookingSeats(
        @Path("lineId") lineId: Long,
        @Query("clubId") clubId: Long,
        @Body dto: SetCartBookingSeatsRequestDto
    ): CartResponseDto

    @PATCH("/api/v1/cart/products/{lineId}")
    suspend fun updateQty(
        @Path("lineId") lineId: Long,
        @Query("clubId") clubId: Long,
        @Body dto: UpdateCartProductQtyRequestDto
    ): CartResponseDto

    @DELETE("/api/v1/cart/items/{type}/{id}")
    suspend fun deleteItem(
        @Query("clubId") clubId: Long,
        @Path("type") type: String,
        @Path("id") id: Long
    ): CartResponseDto

    @DELETE("/api/v1/cart")
    suspend fun clear(@Query("clubId") clubId: Long)
}

interface CheckoutApi {
    @POST("/api/v1/checkout")
    suspend fun checkout(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body dto: CheckoutRequestDto
    ): CheckoutResponseDto

    @GET("/api/v1/purchases")
    suspend fun getMyPurchases(): List<PurchaseListItemDto>

    @GET("/api/v1/purchases/{id}")
    suspend fun getPurchaseDetails(@Path("id") id: Long): PurchaseDetailsDto

    @POST("/api/v1/purchases/{id}/pay")
    suspend fun payPurchase(@Path("id") id: Long): PurchaseListItemDto
}

interface SeatApi {
    @GET("/api/v1/clubs/{clubId}/seats")
    suspend fun getSeats(@Path("clubId") clubId: Long): List<SeatResponseDto>

    @POST("/api/v1/clubs/{clubId}/seats/availability")
    suspend fun getAvailability(
        @Path("clubId") clubId: Long,
        @Body dto: SeatAvailabilityRequestDto
    ): List<SeatAvailabilityResponseDto>
}

interface FloorplanApi {
    @GET("/api/v1/clubs/{clubId}/floorplan")
    suspend fun getPublished(@Path("clubId") clubId: Long): FloorplanResponseDto

    @GET("/api/v1/clubs/{clubId}/floorplan-with-availability")
    suspend fun getPublishedWithAvailability(
        @Path("clubId") clubId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): FloorplanWithAvailabilityResponseDto
}