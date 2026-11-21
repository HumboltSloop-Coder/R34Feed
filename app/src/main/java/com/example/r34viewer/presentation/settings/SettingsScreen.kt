package com.example.r34viewer.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.r34viewer.domain.model.CustomColorSlot
import com.example.r34viewer.domain.model.FeedImageSize
import com.example.r34viewer.presentation.common.CompactTabTopBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    forceSetup: Boolean = false,
    onApiKeyConfigured: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showApiKey by remember { mutableStateOf(false) }
    var designExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.hasCredentials, forceSetup) {
        if (forceSetup && state.hasCredentials) {
            onApiKeyConfigured()
        }
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(4_000)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { CompactTabTopBar(if (forceSetup) "Configurar credenciales API" else "Settings") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (forceSetup) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (forceSetup) "Credenciales de Rule34 requeridas" else "Credenciales de API",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Desde 2025 la API de Rule34 exige ambos parámetros: API Key y User ID. " +
                            "Puedes obtenerlos en las opciones de tu cuenta de Rule34 (My Account > Options > API Access Credentials). " +
                            "Las credenciales se guardan de forma segura y cifrada en este dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (forceSetup) {
                        Text(
                            "Debes configurar ambos campos para poder usar el feed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("API Credentials", style = MaterialTheme.typography.titleMedium)

                    val statusText = when {
                        state.hasCredentials -> "API Key y User ID configurados correctamente."
                        state.hasApiKey || state.hasUserId -> "Configuración incompleta: ambos campos (API Key y User ID) son obligatorios."
                        else -> "No hay credenciales configuradas. Ambos campos son obligatorios."
                    }
                    val statusColor = when {
                        state.hasCredentials -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )

                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = viewModel::onApiKeyInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key (obligatoria)") },
                        placeholder = { Text("Introduce tu API Key") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Ocultar API Key" else "Mostrar API Key"
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.userId,
                        onValueChange = viewModel::onUserIdInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("User ID (obligatorio)") },
                        placeholder = { Text("Introduce tu User ID numérico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = viewModel::saveCredentials,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(if (state.isSaving) "Guardando..." else "Guardar")
                        }
                        if ((state.hasApiKey || state.hasUserId) && !forceSetup) {
                            OutlinedButton(
                                onClick = viewModel::clearCredentials,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text("Limpiar")
                            }
                        }
                    }

                    state.message?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!forceSetup) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dark theme")
                                Text(
                                    "Recommended for NSFW content.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.isDarkTheme,
                                onCheckedChange = viewModel::setDarkTheme
                            )
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Diseño", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Elige los colores principales, secundarios y de fondo de la app.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { designExpanded = !designExpanded }) {
                                Text(if (designExpanded) "Ocultar" else "Abrir")
                            }
                        }

                        if (designExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CustomColorSlot.entries.forEach { slot ->
                                    val (title, value) = when (slot) {
                                        CustomColorSlot.PRIMARY -> "Color primario" to state.customColors.primary
                                        CustomColorSlot.SECONDARY -> "Color secundario" to state.customColors.secondary
                                        CustomColorSlot.BACKGROUND -> "Color de fondo" to state.customColors.background
                                    }
                                    ColorEditor(
                                        title = title,
                                        value = value,
                                        onValueChange = { hex -> viewModel.setCustomColor(slot, hex) }
                                    )
                                }
                                TextButton(onClick = viewModel::resetCustomColors) {
                                    Text("Restablecer colores")
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Feed & content", style = MaterialTheme.typography.titleMedium)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Image size", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Large = 1 column · Medium = 2 · Small = 3. Applies to feed, search and favorites.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FeedImageSize.entries.forEach { size ->
                                    FilterChip(
                                        selected = state.feedImageSize == size,
                                        onClick = { viewModel.setFeedImageSize(size) },
                                        label = { Text("${size.label} (${size.columns})") }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hide AI Generated")
                                Text(
                                    "Bloquear imágenes generadas por IA (añade -ai_generated).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.hideAiGenerated,
                                onCheckedChange = viewModel::setHideAiGenerated
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Máxima calidad en pantalla completa")
                                Text(
                                    "Carga la imagen original al entrar en pantalla completa (puede consumir más datos y memoria).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.fullscreenMaxQuality,
                                onCheckedChange = viewModel::setFullscreenMaxQuality
                            )
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tags negativos", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Se añaden como -tag al feed y a todas las búsquedas. No afectan a los favoritos guardados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.negativeTagDraft,
                                onValueChange = viewModel::onNegativeTagInputChange,
                                modifier = Modifier.weight(1f),
                                label = { Text("Tag a excluir") },
                                placeholder = { Text("Ej. ai_generated") },
                                singleLine = true
                            )
                            Button(onClick = viewModel::addNegativeTag) {
                                Text("Añadir")
                            }
                        }
                        if (state.negativeTags.isEmpty()) {
                            Text(
                                "No hay tags negativos configurados.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.negativeTags.forEach { tag ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("-$tag", style = MaterialTheme.typography.bodyLarge)
                                        IconButton(onClick = { viewModel.removeNegativeTag(tag) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar -$tag")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
