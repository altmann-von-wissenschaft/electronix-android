package com.pnzgu.electronix.data.api

import com.pnzgu.electronix.data.dto.AddToCartRequest
import com.pnzgu.electronix.data.dto.AssignCharacteristicRequest
import com.pnzgu.electronix.data.dto.CartDto
import com.pnzgu.electronix.data.dto.CategoryCharacteristicDto
import com.pnzgu.electronix.data.dto.CategoryDto
import com.pnzgu.electronix.data.dto.ChangePasswordRequest
import com.pnzgu.electronix.data.dto.CharacteristicDto
import com.pnzgu.electronix.data.dto.CreateAnswerRequest
import com.pnzgu.electronix.data.dto.CreateCategoryDto
import com.pnzgu.electronix.data.dto.CreateCharacteristicRequest
import com.pnzgu.electronix.data.dto.CreateProductRequest
import com.pnzgu.electronix.data.dto.CreateQuestionRequest
import com.pnzgu.electronix.data.dto.CreateReviewRequest
import com.pnzgu.electronix.data.dto.LoginRequest
import com.pnzgu.electronix.data.dto.LoginResponse
import com.pnzgu.electronix.data.dto.MessageResponse
import com.pnzgu.electronix.data.dto.OrderDto
import com.pnzgu.electronix.data.dto.PagedCharacteristicsResponse
import com.pnzgu.electronix.data.dto.PagedProductsResponse
import com.pnzgu.electronix.data.dto.PagedQuestionsResponse
import com.pnzgu.electronix.data.dto.PagedReviewsResponse
import com.pnzgu.electronix.data.dto.PagedUsersResponse
import com.pnzgu.electronix.data.dto.ProductDto
import com.pnzgu.electronix.data.dto.PutFcmTokenRequest
import com.pnzgu.electronix.data.dto.PutPushPreferencesRequest
import com.pnzgu.electronix.data.dto.QuestionDto
import com.pnzgu.electronix.data.dto.RefreshResponse
import com.pnzgu.electronix.data.dto.RegisterRequest
import com.pnzgu.electronix.data.dto.RegisterResponse
import com.pnzgu.electronix.data.dto.ReviewDto
import com.pnzgu.electronix.data.dto.SalesReportDto
import com.pnzgu.electronix.data.dto.UpdateCartItemRequest
import com.pnzgu.electronix.data.dto.UpdateCategoryRequest
import com.pnzgu.electronix.data.dto.UpdateOrderStatusRequest
import com.pnzgu.electronix.data.dto.UpdateCharacteristicAssignmentRequest
import com.pnzgu.electronix.data.dto.UpdateProductRequest
import com.pnzgu.electronix.data.dto.UploadImageResponse
import com.pnzgu.electronix.data.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

@Suppress("ComplexInterface")
interface ElectronixApi {
    @POST("api/users/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/users/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @GET("api/users/me")
    suspend fun me(): UserDto

    @POST("api/users/refresh")
    suspend fun refresh(): RefreshResponse

    @POST("api/users/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): MessageResponse

    @PUT("api/users/me/fcm-token")
    suspend fun putFcmToken(@Body body: PutFcmTokenRequest)

    @HTTP(method = "DELETE", path = "api/users/me/fcm-token", hasBody = true)
    suspend fun deleteFcmToken(@Body body: PutFcmTokenRequest)

    @PUT("api/users/me/push-preferences")
    suspend fun putPushPreferences(@Body body: PutPushPreferencesRequest)

    @GET("api/admin/users")
    suspend fun adminUsers(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedUsersResponse

    @GET("api/admin/users/{id}")
    suspend fun adminUser(@Path("id") id: String): UserDto

    @POST("api/admin/users/{id}/block")
    suspend fun adminBlockUser(@Path("id") id: String, @Body block: Boolean): MessageResponse

    @POST("api/admin/users/{id}/roles")
    suspend fun adminAssignRole(@Path("id") id: String, @Body body: RequestBody): MessageResponse

    @DELETE("api/admin/users/{id}/roles/{roleCode}")
    suspend fun adminRemoveRole(
        @Path("id") id: String,
        @Path("roleCode") roleCode: String,
    ): MessageResponse

    @GET("api/categories")
    suspend fun categories(@Query("parentId") parentId: String?): List<CategoryDto>

    @GET("api/categories/{id}")
    suspend fun category(@Path("id") id: String): CategoryDto

    @POST("api/categories")
    suspend fun createCategory(@Body body: CreateCategoryDto): CategoryDto

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: UpdateCategoryRequest): CategoryDto

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): MessageResponse

    @GET("api/categories/{categoryId}/characteristics")
    suspend fun categoryCharacteristics(@Path("categoryId") categoryId: String): List<CategoryCharacteristicDto>

    @POST("api/categories/{categoryId}/characteristics")
    suspend fun assignCategoryCharacteristic(
        @Path("categoryId") categoryId: String,
        @Body body: AssignCharacteristicRequest,
    ): CategoryCharacteristicDto

    @PUT("api/categories/{categoryId}/characteristics/{characteristicId}")
    suspend fun updateCategoryCharacteristicAssignment(
        @Path("categoryId") categoryId: String,
        @Path("characteristicId") characteristicId: String,
        @Body body: UpdateCharacteristicAssignmentRequest,
    ): CategoryCharacteristicDto

    @DELETE("api/categories/{categoryId}/characteristics/{characteristicId}")
    suspend fun unassignCategoryCharacteristic(
        @Path("categoryId") categoryId: String,
        @Path("characteristicId") characteristicId: String,
    ): MessageResponse

    @GET("api/characteristics")
    suspend fun characteristics(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedCharacteristicsResponse

    @GET("api/characteristics/{id}")
    suspend fun characteristic(@Path("id") id: String): CharacteristicDto

    @POST("api/characteristics")
    suspend fun createCharacteristic(@Body body: CreateCharacteristicRequest): CharacteristicDto

    @PUT("api/characteristics/{id}")
    suspend fun updateCharacteristic(
        @Path("id") id: String,
        @Body body: CreateCharacteristicRequest,
    ): CharacteristicDto

    @GET("api/products")
    suspend fun products(
        @Query("categoryId") categoryId: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("search") search: String? = null,
        @QueryMap(encoded = true) filters: Map<String, String> = emptyMap(),
    ): PagedProductsResponse

    @GET("api/products/{id}")
    suspend fun product(@Path("id") id: String): ProductDto

    @POST("api/products")
    suspend fun createProduct(@Body body: CreateProductRequest): ProductDto

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body body: UpdateProductRequest): ProductDto

    @POST("api/products/{id}/hide")
    suspend fun hideProduct(@Path("id") id: String): MessageResponse

    @POST("api/products/{id}/show")
    suspend fun showProduct(@Path("id") id: String): MessageResponse

    @Multipart
    @POST("api/products/{id}/upload-image")
    suspend fun uploadProductImage(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
    ): UploadImageResponse

    @GET("api/cart")
    suspend fun cart(): CartDto

    @POST("api/cart/items")
    suspend fun addCartItem(@Body body: AddToCartRequest): CartDto

    @PUT("api/cart/items/{itemId}")
    suspend fun updateCartItem(
        @Path("itemId") itemId: String,
        @Body body: UpdateCartItemRequest,
    ): CartDto

    @DELETE("api/cart/items/{itemId}")
    suspend fun removeCartItem(@Path("itemId") itemId: String): CartDto

    @DELETE("api/cart")
    suspend fun clearCart(): retrofit2.Response<Unit>

    @GET("api/orders")
    suspend fun myOrders(): List<OrderDto>

    @GET("api/orders/{id}")
    suspend fun order(@Path("id") id: String): OrderDto

    @POST("api/orders")
    suspend fun createOrder(): OrderDto

    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): OrderDto

    @GET("api/orders/admin/all")
    suspend fun adminOrders(@Query("status") status: String? = null): List<OrderDto>

    @PUT("api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body body: UpdateOrderStatusRequest,
    ): OrderDto

    @GET("api/orders/reports/sales")
    suspend fun salesReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): SalesReportDto

    @GET("api/reviews")
    suspend fun reviews(
        @Query("productId") productId: String? = null,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedReviewsResponse

    @GET("api/reviews/pending")
    suspend fun reviewsPending(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedReviewsResponse

    @POST("api/reviews")
    suspend fun createReview(@Body body: CreateReviewRequest): ReviewDto

    @DELETE("api/reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): MessageResponse

    @POST("api/reviews/{id}/approve")
    suspend fun approveReview(@Path("id") id: String): ReviewDto

    @GET("api/support/questions")
    suspend fun myQuestions(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedQuestionsResponse

    @GET("api/support/questions/unanswered")
    suspend fun unansweredQuestions(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): PagedQuestionsResponse

    @GET("api/support/questions/{id}")
    suspend fun question(@Path("id") id: String): QuestionDto

    @POST("api/support/questions")
    suspend fun createQuestion(@Body body: CreateQuestionRequest): QuestionDto

    @POST("api/support/questions/{questionId}/answer")
    suspend fun answerQuestion(
        @Path("questionId") questionId: String,
        @Body body: CreateAnswerRequest,
    ): QuestionDto

    @DELETE("api/support/questions/{questionId}/answers/{answerId}")
    suspend fun deleteAnswer(
        @Path("questionId") questionId: String,
        @Path("answerId") answerId: String,
    ): MessageResponse
}
