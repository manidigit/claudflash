package com.claudemani.app.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.claudemani.app.BuildConfig

/**
 * About page (Descriptions §7): app description, Version, Date, History,
 * Creator Name. Everything here is static UI text, not read from a
 * UseCase — there is no domain concept of "app metadata".
 *
 * Per §7's own instruction ("نام سازنده نباید حدس زده شود ... فیلد باید
 * قابل تکمیل باشد"), [creatorNamePlaceholder] is left as an explicit,
 * visibly-unset placeholder rather than a guessed name. Likewise there is
 * no fabricated multi-entry changelog here — [PROGRESS_TRACKER.md] in the
 * repo is the actual, phase-by-phase change history; this screen only
 * points to it rather than duplicating (and inevitably drifting from) it.
 */
private const val CREATOR_NAME_PLACEHOLDER = "— (هنوز مشخص نشده)"

@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("درباره برنامه", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNavigateBack) { Text("بازگشت") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("claudemani", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "یک مربی شخصی هوشمند واژگان، همراه با یک بازی کوچک آموزش زبان. " +
                "فلش‌کارت و مرور زمان‌بندی‌شده برای یادگیری واژگان اسپانیایی، کاملاً آفلاین.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        AboutRow("نسخه", BuildConfig.VERSION_NAME)
        AboutRow("سازنده", CREATOR_NAME_PLACEHOLDER)

        Spacer(modifier = Modifier.height(24.dp))

        Text("تاریخچه تغییرات", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "تاریخچه کامل توسعه در فایل docs/PROGRESS_TRACKER.md همراه پروژه نگهداری می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
