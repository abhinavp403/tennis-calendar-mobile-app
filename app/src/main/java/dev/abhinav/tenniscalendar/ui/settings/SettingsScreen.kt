package dev.abhinav.tenniscalendar.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhinav.tenniscalendar.data.ThemeMode
import dev.abhinav.tenniscalendar.model.Tour
import dev.abhinav.tenniscalendar.ui.common.FullScreenDialog
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Convert a stored UTC time-of-day to the device's local time-of-day (using today's offset). */
private fun LocalTime.utcToLocal(): LocalTime =
    atDate(LocalDate.now(ZoneOffset.UTC)).toInstant(ZoneOffset.UTC)
        .atZone(ZoneId.systemDefault()).toLocalTime()

/** Convert a locally-picked time-of-day back to the UTC time-of-day we store and schedule against. */
private fun LocalTime.localToUtc(): LocalTime =
    atDate(LocalDate.now(ZoneId.systemDefault())).atZone(ZoneId.systemDefault())
        .toInstant().atZone(ZoneOffset.UTC).toLocalTime()

private val SLOT_LABELS = listOf("First check", "Second check", "Third check", "Fourth check")

@Composable
fun SettingsScreen(onDismiss: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    FullScreenDialog(title = "Settings", onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionHeader("Appearance")
            SettingRow("Theme") {
                ChoiceChips(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { it.label },
                    onSelect = viewModel::setTheme,
                )
            }

            SectionDivider()
            SectionHeader("Calendar")
            SettingRow("Default tour") {
                ChoiceChips(
                    options = Tour.entries,
                    selected = settings.defaultTour,
                    label = { it.label },
                    onSelect = viewModel::setDefaultTour,
                )
            }

            SectionDivider()
            SectionHeader("Notifications")
            NotificationsToggle(
                enabled = settings.notificationsEnabled,
                onToggle = viewModel::setNotificationsEnabled,
            )
            if (settings.notificationsEnabled) {
                Text(
                    "Checks for finished tournaments run at these times (your local time). " +
                        "By default they run every six hours: 01:00, 07:00, 13:00 and 19:00 UTC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                settings.slotTimes.forEachIndexed { i, slot ->
                    TimeRow(SLOT_LABELS[i], slot.utcToLocal()) { pickedLocal ->
                        viewModel.setSlotTime(i, pickedLocal.localToUtc())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup(),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun NotificationsToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* enabling proceeds regardless; posting checks the grant at runtime */ }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Tournament result alerts", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = enabled,
            onCheckedChange = { checked ->
                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onToggle(checked)
            },
        )
    }
}

@Composable
private fun TimeRow(label: String, localTime: LocalTime, onPickedLocal: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val formatter = remember(is24Hour) {
        java.time.format.DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onPickedLocal(LocalTime.of(hour, minute)) },
                    localTime.hour,
                    localTime.minute,
                    is24Hour,
                ).show()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            localTime.format(formatter),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        control()
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
