package com.pnzgu.electronix.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// region Users
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("userId") val userId: String,
    val email: String,
    val nickname: String? = null,
    val roles: List<String> = emptyList(),
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String? = null,
)

/** Common shape for ASP.NET validation/error JSON: `{ "message": "..." }`. */
@Serializable
data class ApiErrorDto(
    val message: String? = null,
    val retryAfterSeconds: Int? = null,
)

@Serializable
data class RegisterResponse(
    val message: String = "",
    @SerialName("userId") val userId: String = "",
)

@Serializable
data class UserRoleAssignmentDto(
    val roleCode: String,
    val assignedAt: String,
    @SerialName("assignedByUserId") val assignedByUserId: String? = null,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val nickname: String? = null,
    @SerialName("isBlocked") val isBlocked: Boolean = false,
    val roles: List<String> = emptyList(),
    val roleAssignments: List<UserRoleAssignmentDto> = emptyList(),
)

@Serializable
data class PutFcmTokenRequest(
    val token: String,
)

@Serializable
data class PutPushPreferencesRequest(
    val notifyOrderStatus: Boolean,
    val notifySupportReply: Boolean,
    val notifyReviewModeration: Boolean,
    val notifySupportQueue: Boolean,
)

@Serializable
data class RefreshResponse(
    val token: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String,
)

@Serializable
data class PagedUsersResponse(
    val data: List<UserDto> = emptyList(),
    val page: Int,
    @SerialName("pageSize") val pageSize: Int,
)
// endregion

// region Products / categories
@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int,
    @SerialName("isHidden") val isHidden: Boolean = false,
    @SerialName("mainImagePath") val mainImagePath: String? = null,
    @SerialName("categoryId") val categoryId: String,
    val characteristics: List<ProductCharacteristicValueDto> = emptyList(),
    @SerialName("imagePaths") val imagePaths: List<String> = emptyList(),
)

@Serializable
data class ProductCharacteristicValueDto(
    @SerialName("characteristicId") val characteristicId: String,
    val name: String,
    val value: Double,
    val unit: String,
)

@Serializable
data class PagedProductsResponse(
    val data: List<ProductDto> = emptyList(),
    val page: Int,
    @SerialName("pageSize") val pageSize: Int,
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("characteristicValues") val characteristicValues: Map<String, String> = emptyMap(),
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val stock: Int? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("characteristicValues") val characteristicValues: Map<String, String>? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("parentId") val parentId: String? = null,
    @SerialName("displayOrder") val displayOrder: Int = 0,
    val characteristics: List<CategoryCharacteristicDto> = emptyList(),
)

@Serializable
data class CreateCategoryDto(
    val name: String,
    @SerialName("parentId") val parentId: String? = null,
    @SerialName("displayOrder") val displayOrder: Int = 0,
    val characteristics: List<AssignCharacteristicRequest>? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    @SerialName("parentId") val parentId: String? = null,
    @SerialName("displayOrder") val displayOrder: Int? = null,
    val characteristics: List<AssignCharacteristicRequest>? = null,
)

@Serializable
data class CategoryCharacteristicDto(
    val id: String,
    @SerialName("characteristicId") val characteristicId: String,
    @SerialName("characteristicName") val characteristicName: String,
    val unit: String,
    @SerialName("isRequired") val isRequired: Boolean,
)

@Serializable
data class AssignCharacteristicRequest(
    @SerialName("characteristicId") val characteristicId: String,
    @SerialName("isRequired") val isRequired: Boolean = true,
)

@Serializable
data class UpdateCharacteristicAssignmentRequest(
    @SerialName("isRequired") val isRequired: Boolean,
)

@Serializable
data class CharacteristicDto(
    val id: String,
    val name: String,
    val unit: String,
)

@Serializable
data class PagedCharacteristicsResponse(
    val data: List<CharacteristicDto> = emptyList(),
    val page: Int,
    @SerialName("pageSize") val pageSize: Int,
)

@Serializable
data class CreateCharacteristicRequest(
    val name: String,
    val unit: String,
)
// endregion

// region Cart
@Serializable
data class CartItemDto(
    val id: String,
    @SerialName("productId") val productId: String,
    @SerialName("productName") val productName: String,
    @SerialName("productPrice") val productPrice: Double,
    val quantity: Int,
)

@Serializable
data class CartDto(
    val id: String,
    val items: List<CartItemDto> = emptyList(),
    @SerialName("totalPrice") val totalPrice: Double,
)

@Serializable
data class AddToCartRequest(
    @SerialName("productId") val productId: String,
    val quantity: Int,
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int,
)
// endregion

// region Orders
@Serializable
data class OrderDto(
    val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("lastStatusChangedByUserId") val lastStatusChangedByUserId: String? = null,
    @SerialName("totalAmount") val totalAmount: Double,
    val status: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    val items: List<OrderItemDto> = emptyList(),
)

@Serializable
data class OrderItemDto(
    val id: String,
    @SerialName("productId") val productId: String,
    @SerialName("productName") val productName: String? = null,
    val quantity: Int,
    @SerialName("priceAtPurchase") val priceAtPurchase: Double,
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String,
    val notes: String? = null,
)

@Serializable
data class SalesReportDto(
    val period: SalesReportPeriodDto,
    @SerialName("totalOrders") val totalOrders: Int,
    @SerialName("totalRevenue") val totalRevenue: Double,
    @SerialName("averageOrderValue") val averageOrderValue: Double,
    /** "day" or "month" — matches server bucketing */
    val granularity: String = "day",
    val series: List<SalesReportPointDto> = emptyList(),
)

@Serializable
data class SalesReportPointDto(
    @SerialName("periodStart") val periodStart: String,
    val revenue: Double,
    @SerialName("orderCount") val orderCount: Int,
)

@Serializable
data class SalesReportPeriodDto(
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate") val endDate: String,
)
// endregion

// region Reviews
@Serializable
data class ReviewDto(
    val id: String,
    @SerialName("productId") val productId: String,
    @SerialName("userId") val userId: String,
    @SerialName("authorNickname") val authorNickname: String? = null,
    val rating: Int,
    val title: String,
    val content: String,
    @SerialName("isApproved") val isApproved: Boolean,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class PagedReviewsResponse(
    val data: List<ReviewDto> = emptyList(),
    val page: Int,
    @SerialName("pageSize") val pageSize: Int,
)

@Serializable
data class CreateReviewRequest(
    @SerialName("productId") val productId: String,
    val rating: Int,
    val title: String,
    val content: String,
)
// endregion

// region Support
@Serializable
data class QuestionDto(
    val id: String,
    @SerialName("userId") val userId: String,
    val subject: String,
    val content: String,
    @SerialName("isAnswered") val isAnswered: Boolean,
    @SerialName("createdAt") val createdAt: String,
    val answer: AnswerDto? = null,
)

@Serializable
data class AnswerDto(
    val id: String,
    @SerialName("managerUserId") val managerUserId: String,
    val content: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class PagedQuestionsResponse(
    val data: List<QuestionDto> = emptyList(),
    val page: Int,
    @SerialName("pageSize") val pageSize: Int,
)

@Serializable
data class CreateQuestionRequest(
    val subject: String,
    val content: String,
)

@Serializable
data class CreateAnswerRequest(
    val content: String,
)
// endregion

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class UploadImageResponse(
    val message: String,
    val fileName: String,
    @SerialName("imageId") val imageId: String,
)
