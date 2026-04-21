package com.nice.cataloguevastra.viewmodel

data class SignUpFormState(
    val usernameError: String? = null,
    val businessNameError: String? = null,
    val emailError: String? = null,
    val phoneNumberError: String? = null,
    val passwordError: String? = null,
    val userImage: SelectedImageUi? = null,
    val businessLogo: SelectedImageUi? = null
)

data class SelectedImageUi(
    val uri: String,
    val label: String
)
