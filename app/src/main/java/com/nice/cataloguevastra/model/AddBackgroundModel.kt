package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class AddBackgroundModel(
    @SerializedName("status") val status : Boolean,
    @SerializedName("message") val message : String,
    @SerializedName("data") val data : AddBackgroundData
)

data class AddBackgroundData(

    @SerializedName("id") val id : Int,
    @SerializedName("image") val image : String,
    @SerializedName("modelimage") val modelimage : String
)
