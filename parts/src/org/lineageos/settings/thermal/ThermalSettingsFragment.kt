/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package org.lineageos.settings.thermal

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.animation.Crossfade
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.thermal.ThermalUtils.ThermalState

class ThermalSettingsFragment : Fragment() {

    private lateinit var thermalUtils: ThermalUtils
    private lateinit var launcherApps: LauncherApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thermalUtils = ThermalUtils.getInstance(requireContext())
        launcherApps = requireContext()
            .getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ThermalTheme {
                ThermalScreen(thermalUtils, launcherApps)
            }
        }
    }
}

@Composable
private fun ThermalTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val colorScheme = runCatching {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }.getOrElse {
        if (isDark) darkColorScheme() else lightColorScheme()
    }
    androidx.compose.material3.MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = androidx.compose.material3.MotionScheme.expressive(),
        content = content,
    )
}

@Composable
private fun ThermalScreen(
    thermalUtils: ThermalUtils,
    launcherApps: LauncherApps,
) {
    var enabled by remember { mutableStateOf(thermalUtils.enabled) }
    var query by remember { mutableStateOf("") }
    val apps = remember { mutableStateListOf<AppEntry>() }
    var loaded by remember { mutableStateOf(false) }

    LoadAppsEffect(launcherApps, apps) { loaded = true }
    LauncherCallbackEffect(launcherApps, apps, loaded)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            EnableSwitchRow(
                enabled = enabled,
                onToggle = {
                    enabled = it
                    thermalUtils.enabled = it
                },
            )

            if (!enabled) return@Column

            if (!loaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
                return@Column
            }

            SearchField(query = query, onQueryChange = { query = it })

            val filtered by remember(apps, query) {
                derivedStateOf {
                    val q = query.trim().lowercase(Locale.getDefault())
                    if (q.isBlank()) apps.toList() else apps.filter { it.matches(q) }
                }
            }

            if (filtered.isEmpty()) {
                EmptyView(query = query, hasApps = apps.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 124.dp,
                    ),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement
                        .spacedBy(8.dp),
                ) {
                    items(items = filtered, key = { it.packageName }) { entry ->
                        AppRow(
                            entry = entry,
                            initialState = thermalUtils.getStateForPackage(entry.packageName),
                            onSelect = { state ->
                                thermalUtils.writePackage(entry.packageName, state.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnableSwitchRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onToggle(!enabled) },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = stringResource(R.string.thermal_enable),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.thermal_switch_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                thumbContent = {
                    Crossfade(
                        targetState = enabled,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                        label = "thermal-sw",
                    ) { on ->
                        if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                        else Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.thermal_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary
                    ),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyView(query: String, hasApps: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val text = when {
            !hasApps -> stringResource(R.string.thermal_no_apps)
            query.isNotBlank() ->
                stringResource(R.string.thermal_no_search_results, query.trim())
            else -> stringResource(R.string.thermal_no_apps)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppRow(
    entry: AppEntry,
    initialState: ThermalState,
    onSelect: (ThermalState) -> Unit,
) {
    var state by remember(entry.packageName) { mutableStateOf(initialState) }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { menuOpen = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                entry.iconPainter?.let {
                    androidx.compose.foundation.Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                FilledTonalButton(
                    onClick = { menuOpen = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 12.dp,
                        top = 8.dp, bottom = 8.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(state.label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(start = 4.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    ThermalState.values().forEach { item ->
                        DropdownMenuItem(
                            text = { Text(stringResource(item.label)) },
                            onClick = {
                                menuOpen = false
                                if (item != state) {
                                    state = item
                                    onSelect(item)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadAppsEffect(
    launcherApps: LauncherApps,
    apps: SnapshotStateList<AppEntry>,
    onLoaded: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                launcherApps
                    .getActivityList(null, Process.myUserHandle())
                    .distinctBy { it.componentName.packageName }
                    .map { it.toAppEntry(context) }
                    .sortedBy { it.sortKey }
            }.getOrDefault(emptyList())
        }
        apps.clear()
        apps.addAll(loaded)
        onLoaded()
    }
}

@Composable
private fun LauncherCallbackEffect(
    launcherApps: LauncherApps,
    apps: SnapshotStateList<AppEntry>,
    loaded: Boolean,
) {
    if (!loaded) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DisposableEffect(launcherApps) {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                apps.removeAll { it.packageName == packageName }
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                scope.launch { upsert(launcherApps, context, apps, packageName, user) }
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                scope.launch { upsert(launcherApps, context, apps, packageName, user) }
            }

            override fun onPackagesAvailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {}

            override fun onPackagesUnavailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {}
        }
        launcherApps.registerCallback(callback)
        onDispose { runCatching { launcherApps.unregisterCallback(callback) } }
    }
}

private suspend fun upsert(
    launcherApps: LauncherApps,
    context: Context,
    apps: SnapshotStateList<AppEntry>,
    packageName: String,
    user: UserHandle,
) {
    val entry = withContext(Dispatchers.IO) {
        runCatching {
            launcherApps.getActivityList(packageName, user).firstOrNull()?.toAppEntry(context)
        }.getOrNull()
    }
    apps.removeAll { it.packageName == packageName }
    if (entry != null) {
        val index = apps.indexOfFirst { it.sortKey > entry.sortKey }
        apps.add(if (index == -1) apps.size else index, entry)
    }
}

private fun LauncherActivityInfo.toAppEntry(context: Context): AppEntry {
    val drawable = getIcon(0)
    return AppEntry(
        packageName = componentName.packageName,
        label = label.toString(),
        iconPainter = drawable.toPainter(),
    )
}


private fun Drawable.toPainter(): Painter {
    val w = intrinsicWidth.coerceAtLeast(1)
    val h = intrinsicHeight.coerceAtLeast(1)
    return BitmapPainter(toBitmap(width = w, height = h).asImageBitmap())
}

private data class AppEntry(
    val packageName: String,
    val label: String,
    val iconPainter: Painter?,
) {
    val sortKey: String = label.lowercase(Locale.getDefault())

    fun matches(query: String): Boolean =
        query.isBlank() ||
            label.lowercase(Locale.getDefault()).contains(query) ||
            packageName.lowercase(Locale.getDefault()).contains(query)
}
