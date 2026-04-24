package com.nice.cataloguevastra.api

import com.nice.cataloguevastra.model.AddBackgroundModel
import com.nice.cataloguevastra.model.LoginRequest
import com.nice.cataloguevastra.model.LoginResponse
import com.nice.cataloguevastra.model.LogoutResponse
import com.nice.cataloguevastra.model.GarmentSubcategoriesResponse
import com.nice.cataloguevastra.model.SignUpResponse
import com.nice.cataloguevastra.model.ThemeListResponse
import com.nice.cataloguevastra.model.UploadModelResponse
import com.nice.cataloguevastra.model.CataloguesResponse
import com.nice.cataloguevastra.model.DeleteCatalogueResponse
import com.nice.cataloguevastra.model.DeleteCataloguesRequest
import com.nice.cataloguevastra.model.GenerateCatalogueResponse
import com.nice.cataloguevastra.model.GeneratePollResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Headers
import com.nice.cataloguevastra.model.ModelProcessTypesResponse
import com.nice.cataloguevastra.model.AssetsResponse
import com.nice.cataloguevastra.model.ProfileResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiServices {

    @POST("api/webtool/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/webtool/logout")
    suspend fun logout(
        @Header("X-Api-Token") apiToken: String
    ): Response<LogoutResponse>

    @Multipart
    @POST("api/webtool/create_user")
    suspend fun signUp(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody,
        @Part("gmail") email: RequestBody,
        @Part("phoneno") phoneNumber: RequestBody,
        @Part("businessname") businessName: RequestBody,
        @Part userImage: MultipartBody.Part?,
        @Part businessLogo: MultipartBody.Part?
    ): Response<SignUpResponse>

    @GET("api/webtool/theme_list")
    suspend fun getThemeList(): Response<ThemeListResponse>

    @GET("api/webtool/garment_subcategories/{themeFor}")
    suspend fun getGarmentSubcategories(
        @Path("themeFor") themeFor: String
    ): Response<GarmentSubcategoriesResponse>

    @GET("api/webtool/model_process_types/{themeFor}")
    suspend fun getModelProcessTypes(
        @Path("themeFor") themeFor: String,
        @Query("dress_name") dressName: String
    ): Response<ModelProcessTypesResponse>

    @Multipart
    @POST("api/webtool/model_create")
    suspend fun uploadModel(
        @Header("X-Api-Token") apiToken: String,
        @Part("name") name: RequestBody,
        @Part("category") category: RequestBody,
        @Part modelImage: MultipartBody.Part
    ): Response<UploadModelResponse>

    @Multipart
    @POST("api/webtool/background_create")
    suspend fun uploadBackground(
        @Header("X-Api-Token") apiToken: String,
        @Part("name") name: RequestBody,
        @Part("category") category: RequestBody,
        @Part modelImage: MultipartBody.Part
    ): Response<AddBackgroundModel>

    @FormUrlEncoded
    @POST("api/webtool/assets")
    suspend fun getAssets(
        @Header("X-Api-Token") apiToken: String,
        @Field("category") category: String,
        @Field("dress_name") dressName: String? = null
    ): Response<AssetsResponse>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/x-www-form-urlencoded"
    )
    @POST("api/webtool/catalogues")
    suspend fun getCatalogues(
        @Header("X-Api-Token") apiToken: String,
        @Query("page") page: Int,
        @Query("format") format: String = "json"
    ): Response<CataloguesResponse>

    @Multipart
    @POST("api/webtool/generate")
    suspend fun generateCatalogue(
        @Header("X-Api-Token") apiToken: String,
        @Part("catalogue_for") catalogueFor: RequestBody,
        @Part("dress_type") dressType: RequestBody,
        @Part("model_id") modelId: RequestBody,
        @Part("bg_id") backgroundId: RequestBody,
        @Part("platform") platform: RequestBody,
        @Part("aspect_ratio") aspectRatio: RequestBody,
        @Part("width") width: RequestBody,
        @Part("height") height: RequestBody,
        @Part("pose_id") poseIds: RequestBody,
        @Part("pose_garment_same_as_product") sameGarmentPoseIds: RequestBody?,
        @Part productImages: List<MultipartBody.Part>,
        @Part poseGarmentImages: List<MultipartBody.Part>
    ): Response<GenerateCatalogueResponse>

    @FormUrlEncoded
    @POST("api/webtool/generate_poll")
    suspend fun pollGenerateCatalogue(
        @Header("X-Api-Token") apiToken: String,
        @Field("job_id") jobId: String
    ): Response<GeneratePollResponse>

    @GET("api/webtool/generation_watch")
    suspend fun watchGenerateCatalogue(
        @Header("X-Api-Token") apiToken: String,
        @Query("garment_id") garmentId: Int,
        @Query("job_id") jobId: String,
        @Query("expected_total") expectedTotal: Int
    ): Response<GeneratePollResponse>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("api/webtool/delete_catalogues")
    suspend fun deleteCatalogue(
        @Header("X-Api-Token") apiToken: String,
        @Body request: DeleteCataloguesRequest
    ): Response<DeleteCatalogueResponse>


    @GET("api/webtool/profile")
    suspend fun getProfile(
        @Header("X-Api-Token") apiToken: String
    ): Response<ProfileResponse>

    @Multipart
    @POST("api/webtool/profile")
    suspend fun updateProfile(
        @Header("X-Api-Token") apiToken: String,
        @Part("username") username: RequestBody,
        @Part("gmail") email: RequestBody,
        @Part("phoneno") phoneNumber: RequestBody,
        @Part("businessname") businessName: RequestBody,
        @Part("useruniqueid") userUniqueId: RequestBody,
        @Part("current_password") currentPassword: RequestBody?,
        @Part("new_password") newPassword: RequestBody?,
        @Part("new_password_confirm") newPasswordConfirm: RequestBody?,
        @Part userImage: MultipartBody.Part?,
        @Part businessLogo: MultipartBody.Part?
    ): Response<ProfileResponse>

}
