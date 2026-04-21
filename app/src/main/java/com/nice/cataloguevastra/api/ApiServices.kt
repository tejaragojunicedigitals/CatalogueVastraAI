package com.nice.cataloguevastra.api

import com.nice.cataloguevastra.model.AddBackgroundModel
import com.nice.cataloguevastra.model.LoginRequest
import com.nice.cataloguevastra.model.LoginResponse
import com.nice.cataloguevastra.model.GarmentSubcategoriesResponse
import com.nice.cataloguevastra.model.SignUpResponse
import com.nice.cataloguevastra.model.ThemeListResponse
import com.nice.cataloguevastra.model.UploadModelResponse
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
import com.nice.cataloguevastra.model.ModelProcessTypesResponse

interface ApiServices {

    @POST("api/webtool/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

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
}
