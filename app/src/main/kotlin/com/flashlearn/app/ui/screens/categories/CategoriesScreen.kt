package com.flashlearn.app.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashlearn.app.presentation.categories.CategoriesViewModel
import com.flashlearn.domain.model.Category

/**
 * Category management (Phase 28). List + create + rename only — no
 * delete, see [com.flashlearn.app.presentation.categories.CategoriesUiState]'s
 * KDoc for why. Reached from Settings → "مدیریت دسته‌بندی‌ها".
 *
 * Wiring category *selection* into AddWord (choosing a Category for a
 * new Concept) is not part of this screen and is a separate, still-open
 * gap in AddWordScreen (Phase 24 built it without a category field).
 */
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("دسته‌بندی‌ها", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNavigateBack) { Text("بازگشت") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.newCategoryName,
                onValueChange = viewModel::onNewCategoryNameChanged,
                label = { Text("دسته‌بندی جدید") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = viewModel::addCategory, modifier = Modifier.padding(start = 8.dp)) {
                Text("افزودن")
            }
        }

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (uiState.categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز دسته‌بندی‌ای ثبت نشده")
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    isEditing = uiState.editingCategoryId == category.id,
                    editingName = uiState.editingCategoryName,
                    onEditingNameChanged = viewModel::onEditingNameChanged,
                    onStartEditing = { viewModel.startEditing(category.id, category.name) },
                    onSave = viewModel::saveEditing,
                    onCancel = viewModel::cancelEditing
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isEditing: Boolean,
    editingName: String,
    onEditingNameChanged: (String) -> Unit,
    onStartEditing: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = onEditingNameChanged,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSave) { Text("ذخیره") }
                TextButton(onClick = onCancel) { Text("انصراف") }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onStartEditing) { Text("ویرایش") }
            }
        }
    }
}
