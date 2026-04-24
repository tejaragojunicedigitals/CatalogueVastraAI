package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class DeleteCataloguesRequest(
    @SerializedName("garment_ids")
    val garmentIds: List<Int>
)

data class DeleteCatalogueResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("deleted_garment_ids")
    val deletedGarmentIds: List<Int>?,
    @SerializedName("removed_rows")
    val removedRows: Int?
)
