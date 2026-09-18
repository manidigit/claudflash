package com.flashlearn.app.presentation.addword

import com.flashlearn.domain.parser.ParsedEntry

/** Which half of the AddWord screen is active. */
enum class AddWordMode { MANUAL, PASTE }

/** Tally of what happened for each parsed entry after an import pass. */
data class ImportSummary(
    val created: Int = 0,
    val merged: Int = 0,
    val alreadyExists: Int = 0,
    val conflicts: Int = 0
)

data class AddWordUiState(
    val mode: AddWordMode = AddWordMode.MANUAL,

    // Manual mode (Descriptions §8/§14 Edge Case #8 — translation required)
    val manualSourceText: String = "",
    val manualTargetText: String = "",
    val manualNotes: String = "",
    val manualSaveError: String? = null,
    val manualSavedSuccessfully: Boolean = false,

    // Paste Text mode (Algorithms v4.20 Parser P0 + ResolveConceptForParsedEntry)
    val pasteText: String = "",
    val parsedEntries: List<ParsedEntry> = emptyList(),
    val commentCount: Int = 0,
    val orphanCount: Int = 0,
    val duplicateGroupCount: Int = 0,
    val isImporting: Boolean = false,
    val importSummary: ImportSummary? = null
)
