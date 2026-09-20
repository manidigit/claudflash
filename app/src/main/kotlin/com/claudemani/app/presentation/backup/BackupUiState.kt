package com.claudemani.app.presentation.backup

data class BackupUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)
