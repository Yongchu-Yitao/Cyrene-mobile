package ai.cyrene.mobile

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.os.LocaleList
import android.net.Uri
import android.provider.OpenableColumns
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.cyrene.mobile.data.SecureStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.noties.markwon.Markwon
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = SecureStore(newBase).uiLanguage()
        if (language.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = newBase.resources.configuration.apply { setLocale(locale) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MainViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            CyreneTheme(state.uiTheme) {
                var showStartup by remember { mutableStateOf(savedInstanceState == null) }
                LaunchedEffect(showStartup) {
                    if (showStartup) {
                        delay(700)
                        showStartup = false
                    }
                }
                if (showStartup) {
                    StartupScreen()
                } else {
                    CyreneMobile(state, model)
                }
            }
        }
    }
}

@Composable
private fun StartupScreen() {
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF8F8FC)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_full),
                contentDescription = "Cyrene",
                modifier = Modifier.size(124.dp).clip(RoundedCornerShape(27.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Cyrene",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.startup_tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CyreneTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFFB8C4FF),
                onPrimary = Color(0xFF10225D),
                primaryContainer = Color(0xFF293E7A),
                background = Color(0xFF111318),
                surface = Color(0xFF191B20),
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = Color(0xFF4059AD),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDDE2FF),
                secondary = Color(0xFF5C5F72),
                background = Color(0xFFF8F8FC),
                surface = Color(0xFFFFFFFF),
            )
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyreneMobile(state: MobileUiState, model: MainViewModel) {
    if (state.peer == null) {
        PairingScreen(state, model)
        return
    }
    var tab by remember { mutableIntStateOf(2) }
    val tabs = listOf(
        DrawerDestination(0, stringResource(R.string.nav_devices), Icons.Outlined.Devices),
        DrawerDestination(2, stringResource(R.string.nav_chats), Icons.Outlined.ChatBubbleOutline),
        DrawerDestination(3, stringResource(R.string.nav_tasks), Icons.Outlined.CheckCircleOutline),
        DrawerDestination(4, stringResource(R.string.nav_terminal), Icons.Outlined.Terminal),
        DrawerDestination(5, stringResource(R.string.nav_settings), Icons.Outlined.Settings),
    )
    val recentSessions = remember(state.chats, state.tasks) {
        (
            state.chats.map { RecentSession("chat", it) } +
                state.tasks.map { RecentSession("task", it) }
            )
            .sortedByDescending { recentSessionTimestamp(it.data) }
            .take(8)
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    Text(
                        "Cyrene",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(tabs, key = { it.screen }) { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.label) },
                            selected = tab == destination.screen,
                            onClick = {
                                tab = destination.screen
                                if (destination.screen == 2) {
                                    model.showChatList()
                                }
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 14.dp))
                        Text(
                            stringResource(R.string.menu_recent_sessions),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (recentSessions.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.menu_no_recent_sessions),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(
                            items = recentSessions,
                            key = { "${it.kind}-${it.data.optString("id")}" },
                        ) { session ->
                            val data = session.data
                            val itemId = data.optString("id")
                            val isChat = session.kind == "chat"
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            data.optString(
                                                "title",
                                                stringResource(
                                                    if (isChat) R.string.chat_unnamed
                                                    else R.string.task_unnamed,
                                                ),
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (isChat) {
                                                stringResource(
                                                    R.string.menu_chat_summary,
                                                    data.optInt("message_count"),
                                                    localizedStatus(data.optString("status")),
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.menu_task_summary,
                                                    localizedStatus(data.optString("status")),
                                                )
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                                selected = if (isChat) {
                                    tab == 2 && state.selectedChat?.optString("id") == itemId
                                } else {
                                    tab == 3 && state.selectedTask?.optString("id") == itemId
                                },
                                onClick = {
                                    if (isChat) {
                                        tab = 2
                                        if (state.selectedChat?.optString("id") != itemId) {
                                            model.openChat(data)
                                        }
                                    } else {
                                        tab = 3
                                        if (state.selectedTask?.optString("id") != itemId) {
                                            model.openTask(data)
                                        }
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        if (isChat) Icons.Outlined.ChatBubbleOutline
                                        else Icons.Outlined.CheckCircleOutline,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = stringResource(R.string.menu_open),
                            )
                        }
                    },
                    title = {
                        val title = if (tab == 2 && state.selectedChat != null) {
                            state.selectedChat.optString("title")
                                .takeIf(String::isNotBlank)
                                ?: stringResource(R.string.chat_unnamed)
                        } else {
                            tabs.firstOrNull { it.screen == tab }?.label
                                ?: stringResource(R.string.app_name)
                        }
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)
                    ),
                    actions = {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.padding(16.dp).width(22.dp))
                        }
                    },
                )
            },
            floatingActionButton = {
                if (tab == 2 && state.selectedProject != null && state.selectedChat == null) {
                    FloatingActionButton(
                        onClick = {
                            if (!state.busy) {
                                model.createChat()
                            }
                        },
                        shape = CircleShape,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.chat_create_new),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> DeviceScreen(state, model)
                    1 -> ProjectScreen(state, model) { tab = 2 }
                    2 -> ChatScreen(state, model)
                    3 -> TaskScreen(state, model)
                    4 -> TerminalScreen(state, model)
                    else -> SettingsScreen(state, model)
                }
                state.error?.let { error ->
                    AlertDialog(
                        onDismissRequest = model::dismissError,
                        confirmButton = {
                            Button(onClick = model::dismissError) {
                                Text(stringResource(R.string.action_ok))
                            }
                        },
                        title = { Text(stringResource(R.string.error_title)) },
                        text = { Text(error) },
                    )
                }
            }
        }
    }
}

private data class DrawerDestination(
    val screen: Int,
    val label: String,
    val icon: ImageVector,
)

private data class RecentSession(
    val kind: String,
    val data: JSONObject,
)

private fun recentSessionTimestamp(item: JSONObject): Long {
    val raw = item.optString("updated_at")
        .ifBlank { item.optString("updatedAt") }
        .ifBlank { item.optString("created_at") }
        .ifBlank { item.optString("createdAt") }
    return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrDefault(0L)
}

@Composable
private fun PairingScreen(state: MobileUiState, model: MainViewModel) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("37841") }
    var key by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.pair_brand), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.pair_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.pair_description),
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                host, { host = it }, label = { Text(stringResource(R.string.pair_host_label)) },
                placeholder = { Text(stringResource(R.string.pair_host_placeholder)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    port, { port = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.pair_port_label)) }, singleLine = true,
                    modifier = Modifier.weight(.38f),
                )
                OutlinedTextField(
                    key, { key = it.uppercase() },
                    label = { Text(stringResource(R.string.pair_key_label)) }, singleLine = true,
                    modifier = Modifier.weight(.62f),
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { model.beginPairing(host, port, key) },
                enabled = !state.busy && host.isNotBlank() && key.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.pair_submit))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
            Text(
                stringResource(R.string.pair_help),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    state.pairingOffer?.let { offer ->
        AlertDialog(
            onDismissRequest = model::cancelPairing,
            title = { Text(stringResource(R.string.pair_verify_title)) },
            text = {
                Column {
                    Text(offer.peer.name, fontWeight = FontWeight.SemiBold)
                    SelectionContainer {
                        Text(
                            offer.peer.fingerprint,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                    Text(stringResource(R.string.pair_verify_description))
                }
            },
            confirmButton = {
                Button(onClick = model::confirmPairing) { Text(stringResource(R.string.pair_verify_confirm)) }
            },
            dismissButton = {
                OutlinedButton(onClick = model::cancelPairing) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun DeviceScreen(state: MobileUiState, model: MainViewModel) {
    val peer = state.peer ?: return
    var showDetails by remember(peer.deviceId) { mutableStateOf(false) }
    if (showDetails) {
        DeviceDetailScreen(state, model, onBack = { showDetails = false })
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.devices_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.devices_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            DataCard(
                title = peer.name,
                subtitle = if (state.backgroundSyncing) {
                    stringResource(
                        R.string.device_sync_progress,
                        (state.backgroundSyncProgress * 100).toInt(),
                    )
                } else if (state.busy) {
                    stringResource(R.string.device_syncing)
                } else {
                    stringResource(R.string.device_online_projects, state.projects.size)
                },
                trailing = stringResource(R.string.action_details),
                progress = state.backgroundSyncProgress.takeIf { state.backgroundSyncing },
                showIndeterminateProgress = state.busy && !state.backgroundSyncing,
            ) { showDetails = true }
        }
    }
}

@Composable
private fun DeviceDetailScreen(
    state: MobileUiState,
    model: MainViewModel,
    onBack: () -> Unit,
) {
    val peer = state.peer ?: return
    var showAdvanced by remember(peer.deviceId) { mutableStateOf(false) }
    var confirmForget by remember { mutableStateOf(false) }
    val chatCount = peer.capabilities.count {
        it.startsWith("chat:") || it.startsWith("approval:")
    }
    val taskCount = peer.capabilities.count {
        it.startsWith("task:") || it.startsWith("artifact:")
    }
    val toolCount = peer.capabilities.count { it.startsWith("toolpack:") }
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.action_back))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.device_detail_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.device_connected),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        peer.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${peer.host}:${peer.port}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.device_security_title)) {
                Text(
                    stringResource(R.string.device_security_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider()
                InfoRow(
                    stringResource(R.string.device_encryption),
                    stringResource(R.string.device_encryption_value),
                )
                InfoRow(
                    stringResource(R.string.device_identity_verified),
                    stringResource(R.string.device_identity_verified_value),
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.device_permissions_title)) {
                Text(
                    stringResource(
                        R.string.device_permissions_summary,
                        peer.projectScopes.size,
                        peer.capabilities.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PermissionRow(
                    stringResource(R.string.device_permissions_projects),
                    stringResource(R.string.device_permissions_projects_desc),
                    peer.projectScopes.size,
                )
                PermissionRow(
                    stringResource(R.string.device_permissions_chat),
                    stringResource(R.string.device_permissions_chat_desc),
                    chatCount,
                )
                PermissionRow(
                    stringResource(R.string.device_permissions_tasks),
                    stringResource(R.string.device_permissions_tasks_desc),
                    taskCount,
                )
                PermissionRow(
                    stringResource(R.string.device_permissions_tools),
                    stringResource(R.string.device_permissions_tools_desc),
                    toolCount,
                )
            }
        }
        item {
            DataCard(
                title = stringResource(R.string.device_advanced_title),
                subtitle = stringResource(R.string.device_advanced_description),
                trailing = if (showAdvanced) "−" else "+",
            ) { showAdvanced = !showAdvanced }
        }
        if (showAdvanced) {
            item {
                SectionCard(stringResource(R.string.device_advanced_title)) {
                    LabelValue(stringResource(R.string.device_endpoint), "${peer.host}:${peer.port}")
                    LabelValue(stringResource(R.string.device_id), peer.deviceId)
                    LabelValue(stringResource(R.string.device_fingerprint), peer.fingerprint)
                    LabelValue(stringResource(R.string.device_protocol), "Signed + E2EE Envelope v1")
                }
            }
        }
        item {
            Button(
                onClick = model::refreshProjects,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(R.string.device_reconnect))
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmForget = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(R.string.device_forget), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text(stringResource(R.string.device_forget_title)) },
            text = { Text(stringResource(R.string.device_forget_message)) },
            confirmButton = {
                Button(onClick = model::forgetDevice) {
                    Text(stringResource(R.string.device_forget))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmForget = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PermissionRow(title: String, description: String, count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    count.toString(),
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProjectScreen(state: MobileUiState, model: MainViewModel, openChats: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.projects_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.projects_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = model::refreshProjects) { Text(stringResource(R.string.action_refresh)) }
            }
        }
        if (state.projects.isEmpty()) item { EmptyCard(stringResource(R.string.projects_empty)) }
        items(state.projects, key = { it.optString("id") }) { project ->
            DataCard(
                title = project.optString("name", stringResource(R.string.project_unnamed)),
                subtitle = localizedStatus(project.optString("status", "active")),
                trailing = if (state.selectedProject?.optString("id") == project.optString("id")) {
                    stringResource(R.string.project_current)
                } else {
                    stringResource(R.string.action_open)
                },
            ) {
                model.selectProject(project)
                openChats()
            }
        }
    }
}

@Composable
private fun ChatScreen(state: MobileUiState, model: MainViewModel) {
    val project = state.selectedProject
    if (project == null) {
        CenterMessage(stringResource(R.string.project_select_first))
        return
    }
    if (state.selectedChat == null) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.chats.isEmpty()) item { EmptyCard(stringResource(R.string.chats_empty)) }
            items(state.chats, key = { it.optString("id") }) { chat ->
                DataCard(
                    chat.optString("title", stringResource(R.string.chat_unnamed)),
                    stringResource(
                        R.string.chat_message_count,
                        chat.optInt("message_count"),
                        localizedStatus(chat.optString("status")),
                    ),
                    stringResource(R.string.action_open),
                ) { model.openChat(chat) }
            }
        }
    } else {
        ChatDetail(state, model)
    }
}

@Composable
private fun ChatDetail(state: MobileUiState, model: MainViewModel) {
    val chat = state.selectedChat ?: return
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var pendingAttachments by remember { mutableStateOf<List<PendingAttachment>>(emptyList()) }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val added = uris.mapNotNull { uri -> pendingAttachment(context, uri) }
        pendingAttachments = (pendingAttachments + added)
            .distinctBy { it.uri.toString() }
            .take(5)
    }
    val messages = chat.optJSONArray("messages")
    val pendingQuestionId = if (messages == null) "" else
        (messages.length() - 1 downTo 0).firstNotNullOfOrNull { index ->
            messages.optJSONObject(index)?.optString("question_id")?.takeIf(String::isNotBlank)
        }.orEmpty()
    Column(Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        val eventReply = state.runEvents
            .filter { it.optString("type") == "reply_delta" }
            .joinToString("") { it.optString("delta") }
            .ifBlank {
                state.runEvents.lastOrNull { it.optString("type") == "reply_done" }
                    ?.optString("response").orEmpty()
            }
        val liveTrace = state.runEvents.filter {
            val type = it.optString("type")
            type.startsWith("tool_call") || type == "tool_progress"
        }
        val transcriptCount = (messages?.length() ?: 0) +
            (if (liveTrace.isNotEmpty() || state.activeRunId != null) 1 else 0) +
            (if (eventReply.isNotBlank()) 1 else 0)
        LaunchedEffect(transcriptCount, eventReply.length) {
            if (transcriptCount > 0) listState.animateScrollToItem(transcriptCount - 1)
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 20.dp,
                    end = 18.dp,
                    bottom = 178.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (messages != null) {
                    items(messages.length()) { index ->
                        val item = messages.optJSONObject(index) ?: JSONObject()
                        ConversationMessage(
                            message = item,
                            onEdit = { message = displayMessageContent(item.optString("content")) },
                            onRetry = {
                                val content = displayMessageContent(item.optString("content"))
                                if (content.isNotBlank() && !state.busy && state.activeRunId == null) {
                                    model.sendMessage(content, plan)
                                }
                            },
                        )
                    }
                }
                if (liveTrace.isNotEmpty() || state.activeRunId != null) {
                    item {
                        ExecutionCard(
                            entries = liveTrace,
                            running = state.activeRunId != null,
                        )
                    }
                }
                if (eventReply.isNotBlank()) {
                    item {
                        AssistantMessage(
                            content = eventReply,
                            timestamp = "",
                            running = state.activeRunId != null,
                        )
                    }
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (chat.optBoolean("awaiting_user") && pendingQuestionId.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                        ),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            OutlinedTextField(
                                answer, { answer = it },
                                label = { Text(stringResource(R.string.chat_answer_prompt)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    model.answerChat(pendingQuestionId, answer)
                                    answer = ""
                                },
                                enabled = answer.isNotBlank(),
                                modifier = Modifier.padding(top = 6.dp),
                            ) { Text(stringResource(R.string.chat_submit_answer)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                ChatComposer(
                    message = message,
                    onMessageChange = { message = it },
                    plan = plan,
                    onPlanChange = { plan = it },
                    running = state.activeRunId != null,
                    busy = state.busy,
                    attachments = pendingAttachments,
                    onAddAttachment = { attachmentPicker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { attachment ->
                        pendingAttachments = pendingAttachments - attachment
                    },
                    onSend = {
                        if (state.activeRunId == null) {
                            model.sendMessage(message, plan, pendingAttachments)
                        }
                        else model.guideRun(message)
                        message = ""
                        pendingAttachments = emptyList()
                    },
                    onInterrupt = model::interruptRun,
                )
            }
        }
    }
}

data class PendingAttachment(
    val uri: Uri,
    val name: String,
    val size: Long,
)

private fun pendingAttachment(context: Context, uri: Uri): PendingAttachment? {
    var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    var size = -1L
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex).orEmpty()
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return name.takeIf(String::isNotBlank)?.let {
        PendingAttachment(uri, it, size)
    }
}

@Composable
private fun ChatComposer(
    message: String,
    onMessageChange: (String) -> Unit,
    plan: Boolean,
    onPlanChange: (Boolean) -> Unit,
    running: Boolean,
    busy: Boolean,
    attachments: List<PendingAttachment>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (PendingAttachment) -> Unit,
    onSend: () -> Unit,
    onInterrupt: () -> Unit,
    placeholderRes: Int = R.string.chat_composer_desktop,
    runningPlaceholderRes: Int = R.string.chat_guide_composer,
    modeMenuEnabled: Boolean = true,
    interruptWhileRunning: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var modeMenuOpen by remember { mutableStateOf(false) }
    val submit = {
        when {
            running && (message.isBlank() || interruptWhileRunning) -> onInterrupt()
            message.isNotBlank() && !busy -> onSend()
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (attachments.isNotEmpty()) {
                attachments.forEach { attachment ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    ) {
                        Row(
                            Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                attachment.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            IconButton(
                                onClick = { onRemoveAttachment(attachment) },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.chat_remove_attachment),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 160.dp)
                    .padding(horizontal = 2.dp, vertical = 3.dp),
                enabled = !busy,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (message.isBlank()) {
                            Text(
                                stringResource(
                                    if (running) runningPlaceholderRes else placeholderRes,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f),
            )
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onAddAttachment,
                    enabled = !running && !busy,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = stringResource(R.string.chat_add_attachment),
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(enabled = !running && modeMenuEnabled) {
                                modeMenuOpen = true
                            },
                        color = if (plan) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .7f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(9.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (plan) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(
                                    if (plan) R.string.chat_mode_plan
                                    else R.string.chat_mode_default,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (plan) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = modeMenuOpen,
                        onDismissRequest = { modeMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.chat_mode_default))
                                    Text(
                                        stringResource(R.string.chat_mode_default_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onPlanChange(false)
                                modeMenuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(stringResource(R.string.chat_mode_plan))
                                    Text(
                                        stringResource(R.string.chat_mode_plan_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onPlanChange(true)
                                modeMenuOpen = false
                            },
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = submit,
                    enabled = (running || message.isNotBlank()) && !busy,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        if (running && message.isBlank()) Icons.Outlined.StopCircle
                        else if (running && interruptWhileRunning) Icons.Outlined.StopCircle
                        else Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(
                            when {
                                running && (message.isBlank() || interruptWhileRunning) ->
                                    R.string.action_stop
                                running -> R.string.chat_guide
                                else -> R.string.action_send
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskScreen(state: MobileUiState, model: MainViewModel) {
    if (state.selectedProject == null) {
        CenterMessage(stringResource(R.string.project_select_first))
        return
    }
    if (state.selectedTask == null) {
        var title by remember { mutableStateOf("") }
        var goal by remember { mutableStateOf("") }
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCard(stringResource(R.string.task_create_title)) {
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.task_title_label)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(goal, { goal = it }, label = { Text(stringResource(R.string.task_goal_label)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Button(onClick = { model.createTask(title, goal); title = ""; goal = "" }, enabled = goal.isNotBlank()) {
                        Text(stringResource(R.string.action_create))
                    }
                }
            }
            items(state.tasks, key = { it.optString("id") }) { task ->
                DataCard(
                    task.optString("title"),
                    "${localizedStatus(task.optString("status"))} · ${task.optString("priority")}",
                    stringResource(R.string.action_details),
                ) {
                    model.openTask(task)
                }
            }
        }
    } else {
        val task = state.selectedTask
        val context = LocalContext.current
        var message by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }
        var pendingAttachments by remember {
            mutableStateOf<List<PendingAttachment>>(emptyList())
        }
        val attachmentPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            val added = uris.mapNotNull { uri -> pendingAttachment(context, uri) }
            pendingAttachments = (pendingAttachments + added)
                .distinctBy { it.uri.toString() }
                .take(5)
        }
        val question = task?.optJSONObject("pending_question")
        val running = task?.optString("status").orEmpty().equals("running", ignoreCase = true)
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = 178.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HeroCard(
                        task?.optString("title").orEmpty(),
                        task?.optString("goal").orEmpty(),
                        task?.optString("status").orEmpty(),
                    )
                }
                if (question != null) {
                    item {
                        SectionCard(
                            question.optString(
                                "title",
                                stringResource(R.string.task_waiting_answer),
                            )
                        ) {
                            Text(question.optString("prompt", question.optString("question")))
                            OutlinedTextField(
                                answer,
                                { answer = it },
                                label = { Text(stringResource(R.string.task_answer_label)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    model.answerTask(
                                        question.optString(
                                            "id",
                                            question.optString("questionId"),
                                        ),
                                        answer,
                                    )
                                    answer = ""
                                },
                                enabled = answer.isNotBlank(),
                            ) { Text(stringResource(R.string.chat_submit_answer)) }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { model.taskAction("tasks.approve_plan") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.task_approve_plan))
                    }
                }
                val plan = task?.optJSONArray("plan")
                if (plan != null && plan.length() > 0) {
                    item {
                        Text(
                            stringResource(R.string.task_plan_steps),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(plan.length()) { index ->
                        val step = plan.optJSONObject(index) ?: JSONObject()
                        DataCard(
                            step.optString(
                                "title",
                                stringResource(R.string.task_step_default, index + 1),
                            ),
                            localizedStatus(step.optString("status", "pending")),
                            stringResource(R.string.action_run),
                        ) { model.runTaskStep(step.optString("id"), message) }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { model.taskAction("tasks.pause") },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.action_pause)) }
                        OutlinedButton(
                            onClick = { model.taskAction("tasks.resume") },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.action_resume)) }
                        OutlinedButton(
                            onClick = { model.taskAction("tasks.cancel") },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.action_cancel_task)) }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.task_artifacts),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (state.artifacts.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.task_artifacts_empty)) }
                }
                items(state.artifacts, key = { it.optString("id") }) { artifact ->
                    val progress = state.downloadProgress[artifact.optString("id")]
                    DataCard(
                        artifact.optString(
                            "name",
                            stringResource(R.string.artifact_unnamed),
                        ),
                        progress?.let {
                            stringResource(R.string.artifact_progress, (it * 100).toInt())
                        } ?: "${localizedStatus(artifact.optString("status"))} · " +
                            "${artifact.optLong("size")} bytes",
                        stringResource(
                            if (progress == 1f) R.string.artifact_saved
                            else R.string.action_download,
                        ),
                    ) { model.downloadArtifact(artifact) }
                }
                if (state.downloadedFiles.isNotEmpty()) {
                    item {
                        SectionCard(stringResource(R.string.artifact_downloaded)) {
                            state.downloadedFiles.forEach { path ->
                                SelectionContainer {
                                    Text(
                                        path,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ChatComposer(
                message = message,
                onMessageChange = { message = it },
                plan = false,
                onPlanChange = {},
                running = running,
                busy = state.busy,
                attachments = pendingAttachments,
                onAddAttachment = { attachmentPicker.launch(arrayOf("*/*")) },
                onRemoveAttachment = { removed ->
                    pendingAttachments = pendingAttachments.filterNot {
                        it.uri == removed.uri
                    }
                },
                onSend = {
                    model.taskAction(
                        "tasks.dispatch",
                        message,
                        pendingAttachments,
                    )
                    message = ""
                    pendingAttachments = emptyList()
                },
                onInterrupt = { model.taskAction("tasks.pause") },
                placeholderRes = R.string.task_composer_placeholder,
                runningPlaceholderRes = R.string.task_composer_running,
                modeMenuEnabled = false,
                interruptWhileRunning = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun TerminalScreen(state: MobileUiState, model: MainViewModel) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val terminalBackground = Color(0xFF10141D)
    val terminalText = Color.White
    val terminalMuted = Color.White.copy(alpha = .72f)
    LaunchedEffect(
        state.peer?.deviceId,
        state.selectedProject?.optString("id"),
    ) {
        model.ensureTerminalShell()
    }
    LaunchedEffect(state.terminalLines.size) {
        if (state.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(state.terminalLines.lastIndex)
        }
    }
    LaunchedEffect(state.terminalSessionStatus) {
        if (state.terminalSessionStatus == "running") {
            delay(120)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    fun submit() {
        val command = input.trim()
        if (
            command.isNotBlank() &&
            !state.terminalBusy &&
            state.terminalSessionStatus == "running"
        ) {
            model.sendTerminalCommand(command)
            input = ""
        }
    }
    Surface(Modifier.fillMaxSize(), color = terminalBackground) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.terminalLines) { line ->
                SelectionContainer {
                    Text(
                        line,
                        color = terminalText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (
                state.selectedProject == null ||
                state.terminalSessionStatus == "error"
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.selectedProject == null) {
                                    R.string.terminal_no_project
                                } else {
                                    R.string.terminal_disconnected
                                }
                            ),
                            color = terminalMuted,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (state.selectedProject != null && !state.terminalBusy) {
                            OutlinedButton(
                                onClick = { model.ensureTerminalShell(force = true) },
                            ) {
                                Text(stringResource(R.string.action_reconnect))
                            }
                        }
                    }
                }
            }
            if (
                state.selectedProject != null &&
                state.terminalSessionStatus == "running"
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            "${state.terminalPrompt} ",
                            color = terminalText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = terminalText,
                                fontFamily = FontFamily.Monospace,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Send,
                                autoCorrectEnabled = false,
                            ),
                            keyboardActions = KeyboardActions(onSend = { submit() }),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MobileUiState, model: MainViewModel) {
    val context = LocalContext.current
    val desktop = state.desktopSettings
    val schema = state.desktopSettingsSchema
    var editingField by remember { mutableStateOf<JSONObject?>(null) }
    var editorValue by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf(false) }
    LaunchedEffect(state.peer?.deviceId) {
        model.loadDesktopSettings()
    }

    editingField?.let { field ->
        val type = field.optString("type")
        val label = localizedSettingText(field, "label")
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text(label) },
            text = {
                if (type == "enum") {
                    Column(
                        Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        jsonObjects(field.optJSONArray("options")).forEach { option ->
                            val optionValue = option.opt("value")?.toString().orEmpty()
                            ChoiceRow(
                                localizedSettingText(option, "label"),
                                desktop?.opt(field.optString("key"))?.toString() == optionValue,
                                enabled = !state.busy,
                            ) {
                                model.updateDesktopSetting(
                                    field.optString("key"),
                                    option.opt("value") ?: "",
                                )
                                editingField = null
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editorValue,
                            onValueChange = {
                                editorValue = it
                                editorError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = editorError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (type == "integer") {
                                    KeyboardType.Number
                                } else {
                                    KeyboardType.Decimal
                                },
                            ),
                            label = { Text(label) },
                        )
                        val minimum = field.opt("minimum")
                        val maximum = field.opt("maximum")
                        if (minimum != null || maximum != null) {
                            Text(
                                stringResource(
                                    R.string.settings_value_range,
                                    minimum?.toString().orEmpty(),
                                    maximum?.toString().orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (editorError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (type != "enum") {
                    Button(
                        onClick = {
                            val value: Any? = if (type == "integer") {
                                editorValue.toIntOrNull()
                            } else {
                                editorValue.toDoubleOrNull()
                            }
                            if (value == null) {
                                editorError = true
                            } else {
                                model.updateDesktopSetting(field.optString("key"), value)
                                editingField = null
                            }
                        },
                    ) { Text(stringResource(R.string.action_save)) }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingField = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val sections = jsonObjects(schema?.optJSONArray("sections"))
    // Older desktop versions may still expose compatibility-only per-tool
    // settings. Mobile follows the desktop Workbench UI and only shows packs.
    val fields = jsonObjects(schema?.optJSONArray("fields")).filterNot {
        it.optString("section") == "tools" || it.optString("key").startsWith("tool::")
    }
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SectionCard(stringResource(R.string.settings_mobile_section)) {
                Text(
                    stringResource(R.string.settings_mobile_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.settings_appearance),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                ChoiceRow(
                    stringResource(R.string.settings_theme_system),
                    state.uiTheme == "system",
                ) { model.setUiTheme("system") }
                ChoiceRow(
                    stringResource(R.string.settings_theme_light),
                    state.uiTheme == "light",
                ) { model.setUiTheme("light") }
                ChoiceRow(
                    stringResource(R.string.settings_theme_dark),
                    state.uiTheme == "dark",
                ) { model.setUiTheme("dark") }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.settings_language),
                    fontWeight = FontWeight.SemiBold,
                )
                listOf(
                    "" to stringResource(R.string.settings_language_system),
                    "zh-CN" to stringResource(R.string.settings_language_chinese),
                    "en" to stringResource(R.string.settings_language_english),
                ).forEach { (code, label) ->
                    ChoiceRow(label, state.uiLanguage == code) {
                        model.setUiLanguage(code)
                        (context as? Activity)?.let { applyAppLanguage(it, code) }
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_desktop_section)) {
                Text(
                    stringResource(R.string.settings_desktop_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (desktop == null) {
                    Text(
                        stringResource(R.string.settings_desktop_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = model::loadDesktopSettings) {
                        Text(stringResource(R.string.action_refresh))
                    }
                } else {
                    Text(
                        stringResource(R.string.settings_desktop_count, fields.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    stringResource(R.string.settings_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.desktopModels?.let { models ->
            item {
                ModelSettingsCard(models, state.busy, model)
            }
        }
        if (desktop != null) {
            items(sections, key = { it.optString("id") }) { section ->
                val sectionFields = fields.filter {
                    it.optString("section") == section.optString("id")
                }
                if (sectionFields.isNotEmpty()) {
                    SectionCard(localizedSettingText(section, "label")) {
                        sectionFields.forEachIndexed { index, field ->
                            DesktopSettingFieldRow(
                                field = field,
                                value = desktop.opt(field.optString("key")),
                                enabled = !state.busy,
                                onBooleanChanged = {
                                    model.updateDesktopSetting(field.optString("key"), it)
                                },
                                onEdit = {
                                    editorValue = desktop.opt(field.optString("key"))
                                        ?.toString().orEmpty()
                                    editorError = false
                                    editingField = field
                                },
                            )
                            if (index < sectionFields.lastIndex) {
                                HorizontalDivider(Modifier.padding(vertical = 3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ModelEditTarget(val role: String, val index: Int)

@Composable
private fun ModelSettingsCard(
    models: JSONObject,
    busy: Boolean,
    model: MainViewModel,
) {
    val customModels = models.optJSONArray("custom_models") ?: JSONArray()
    val visionModels = models.optJSONArray("vision_models") ?: JSONArray()
    val codexModel = models.optJSONObject("codex_model")
    val secondaryModel = models.optJSONObject("secondary_model")
    var editTarget by remember { mutableStateOf<ModelEditTarget?>(null) }

    fun candidateFor(target: ModelEditTarget): JSONObject = when (target.role) {
        "custom" -> customModels.optJSONObject(target.index) ?: JSONObject()
        "vision" -> visionModels.optJSONObject(target.index) ?: JSONObject()
        "codex" -> codexModel ?: JSONObject()
        else -> secondaryModel ?: JSONObject()
    }

    editTarget?.let { target ->
        val candidate = candidateFor(target)
        var identifier by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("model"))
        }
        var baseUrl by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("base_url"))
        }
        var apiKey by remember(target) { mutableStateOf("") }
        var description by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("description"))
        }
        var contextWindow by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("context"))
        }
        var price by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("price"))
        }
        var reasoning by remember(target, candidate.toString()) {
            mutableStateOf(candidate.optString("reasoning_effort"))
        }
        var makePrimary by remember(target) {
            mutableStateOf(target.role == "custom" && target.index == 0)
        }
        val isCodex = target.role == "codex"
        val roleTitle = when (target.role) {
            "custom" -> if (target.index == 0) {
                stringResource(R.string.settings_model_primary)
            } else {
                stringResource(R.string.settings_model_fallback)
            }
            "vision" -> stringResource(R.string.settings_model_vision)
            "secondary" -> stringResource(R.string.settings_model_secondary)
            else -> stringResource(R.string.settings_model_codex)
        }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(roleTitle) },
            text = {
                Column(
                    Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_model_identifier)) },
                        singleLine = true,
                        enabled = !isCodex,
                    )
                    if (!isCodex) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.settings_model_api_key)) },
                            placeholder = {
                                Text(
                                    if (candidate.optBoolean("api_key_configured")) {
                                        stringResource(R.string.settings_model_key_keep)
                                    } else {
                                        "sk-…"
                                    },
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Base URL") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                    }
                    OutlinedTextField(
                        value = reasoning,
                        onValueChange = { reasoning = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_model_reasoning)) },
                        placeholder = { Text("low / medium / high / max") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_model_description)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = contextWindow,
                        onValueChange = { contextWindow = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_model_context)) },
                        placeholder = { Text("128K / 1M") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_model_price)) },
                        singleLine = true,
                    )
                    if (target.role == "custom" && target.index > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = makePrimary,
                                onCheckedChange = { makePrimary = it },
                            )
                            Text(stringResource(R.string.settings_model_make_primary))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = identifier.isNotBlank() && !busy,
                    onClick = {
                        val next = JSONObject(models.toString())
                        val updated = JSONObject(candidate.toString())
                            .put("model", identifier.trim())
                            .put("name", identifier.trim())
                            .put("base_url", baseUrl.trim())
                            .put("description", description.trim())
                            .put("context", contextWindow.trim())
                            .put("price", price.trim())
                            .put("reasoning_effort", reasoning.trim())
                        if (apiKey.isNotBlank()) updated.put("api_key", apiKey.trim())
                        when (target.role) {
                            "custom" -> {
                                val array = next.getJSONArray("custom_models")
                                if (target.index < array.length()) {
                                    array.put(target.index, updated)
                                } else {
                                    array.put(updated)
                                }
                                if (makePrimary && target.index in 1 until array.length()) {
                                    val reordered = JSONArray().put(updated)
                                    (0 until array.length())
                                        .filter { it != target.index }
                                        .forEach { reordered.put(array.getJSONObject(it)) }
                                    next.put("custom_models", reordered)
                                }
                            }
                            "vision" -> {
                                val array = next.getJSONArray("vision_models")
                                if (target.index < array.length()) {
                                    array.put(target.index, updated)
                                } else {
                                    array.put(updated)
                                }
                            }
                            "secondary" -> next.put("secondary_model", updated)
                            "codex" -> next.put("codex_model", updated)
                        }
                        model.updateDesktopModels(next)
                        editTarget = null
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val canDelete = when (target.role) {
                        "custom" -> customModels.length() > 1
                        "vision" -> visionModels.length() > 1
                        "secondary" -> secondaryModel != null
                        else -> false
                    }
                    if (canDelete && target.index >= 0) {
                        OutlinedButton(
                            onClick = {
                                val next = JSONObject(models.toString())
                                when (target.role) {
                                    "custom" -> next.getJSONArray("custom_models")
                                        .remove(target.index)
                                    "vision" -> next.getJSONArray("vision_models")
                                        .remove(target.index)
                                    "secondary" -> next.put(
                                        "secondary_model",
                                        JSONObject.NULL,
                                    )
                                }
                                model.updateDesktopModels(next)
                                editTarget = null
                            },
                        ) { Text(stringResource(R.string.action_delete)) }
                    }
                    OutlinedButton(onClick = { editTarget = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }

    SectionCard(stringResource(R.string.settings_models_title)) {
        Text(
            stringResource(R.string.settings_models_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.settings_model_source),
            fontWeight = FontWeight.Medium,
        )
        ChoiceRow(
            stringResource(R.string.settings_model_custom),
            models.optString("source", "custom") == "custom",
            enabled = !busy,
        ) {
            model.updateDesktopModels(
                JSONObject(models.toString()).put("source", "custom"),
            )
        }
        if (codexModel != null) {
            ChoiceRow(
                stringResource(R.string.settings_model_codex),
                models.optString("source") == "codex",
                enabled = !busy,
            ) {
                model.updateDesktopModels(
                    JSONObject(models.toString()).put("source", "codex"),
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        Text(
            stringResource(R.string.settings_model_primary_and_fallbacks),
            fontWeight = FontWeight.Medium,
        )
        jsonObjects(customModels).forEachIndexed { index, candidate ->
            ModelCandidateRow(
                candidate,
                if (index == 0) {
                    stringResource(R.string.settings_model_primary)
                } else {
                    stringResource(R.string.settings_model_fallback_number, index)
                },
            ) { editTarget = ModelEditTarget("custom", index) }
        }
        OutlinedButton(
            onClick = { editTarget = ModelEditTarget("custom", customModels.length()) },
            enabled = !busy && customModels.length() < 10,
        ) { Text(stringResource(R.string.settings_model_add_fallback)) }
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        Text(stringResource(R.string.settings_model_vision), fontWeight = FontWeight.Medium)
        jsonObjects(visionModels).forEachIndexed { index, candidate ->
            ModelCandidateRow(candidate, if (index == 0) {
                stringResource(R.string.settings_model_primary)
            } else {
                stringResource(R.string.settings_model_fallback_number, index)
            }) { editTarget = ModelEditTarget("vision", index) }
        }
        OutlinedButton(
            onClick = { editTarget = ModelEditTarget("vision", visionModels.length()) },
            enabled = !busy && visionModels.length() < 10,
        ) { Text(stringResource(R.string.settings_model_add_vision)) }
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        Text(stringResource(R.string.settings_model_secondary), fontWeight = FontWeight.Medium)
        if (secondaryModel == null) {
            OutlinedButton(
                onClick = { editTarget = ModelEditTarget("secondary", 0) },
                enabled = !busy,
            ) { Text(stringResource(R.string.settings_model_configure_secondary)) }
        } else {
            ModelCandidateRow(
                secondaryModel,
                stringResource(R.string.settings_model_secondary),
            ) { editTarget = ModelEditTarget("secondary", 0) }
        }
        Text(
            stringResource(R.string.settings_model_secret_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelCandidateRow(
    candidate: JSONObject,
    role: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(role, style = MaterialTheme.typography.labelMedium)
                Text(
                    candidate.optString("model"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.optString("base_url"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DesktopSettingFieldRow(
    field: JSONObject,
    value: Any?,
    enabled: Boolean,
    onBooleanChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val type = field.optString("type")
    val description = localizedSettingText(field, "description")
    val valueLabel = if (type == "enum") {
        jsonObjects(field.optJSONArray("options"))
            .firstOrNull { it.opt("value")?.toString() == value?.toString() }
            ?.let { localizedSettingText(it, "label") }
            ?: value?.toString().orEmpty()
    } else {
        value?.toString().orEmpty()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && type != "boolean", onClick = onEdit)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(localizedSettingText(field, "label"), fontWeight = FontWeight.Medium)
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (type != "boolean") {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (type == "boolean") {
            Switch(
                checked = value as? Boolean ?: field.optBoolean("default"),
                onCheckedChange = onBooleanChanged,
                enabled = enabled,
            )
        } else {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun localizedSettingText(value: JSONObject, key: String): String {
    val localizedKey = if (Locale.getDefault().language == "zh") "${key}_zh" else key
    return if (value.has(localizedKey)) {
        value.optString(localizedKey)
    } else {
        value.optString(key)
    }
}

private fun jsonObjects(array: JSONArray?): List<JSONObject> =
    if (array == null) emptyList()
    else (0 until array.length()).mapNotNull { array.optJSONObject(it) }

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
        },
        shape = RoundedCornerShape(13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
            Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

private fun applyAppLanguage(activity: Activity, language: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        activity.getSystemService(LocaleManager::class.java).applicationLocales =
            if (language.isBlank()) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(language)
    } else {
        activity.recreate()
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, badge: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(badge.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun DataCard(
    title: String,
    subtitle: String,
    trailing: String,
    progress: Float? = null,
    showIndeterminateProgress: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    trailing,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (showIndeterminateProgress) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ConversationMessage(
    message: JSONObject,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
) {
    val role = message.optString("role")
    val content = displayMessageContent(message.optString("content"))
    val timestamp = formatChatTime(
        message.optString("createdAt").ifBlank { message.optString("created_at") }
    )
    if (role == "user") {
        UserMessage(content, timestamp, onEdit, onRetry)
        return
    }
    val trace = message.optJSONArray("trace")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (trace != null && trace.length() > 0) {
            ExecutionCard(
                entries = List(trace.length()) { trace.optJSONObject(it) ?: JSONObject() },
                running = false,
            )
        } else if (message.optBoolean("activityCard") && content.isBlank()) {
            ExecutionCard(entries = emptyList(), running = false)
        }
        if (content.isNotBlank()) {
            AssistantMessage(content, timestamp, running = false)
        }
    }
}

@Composable
private fun UserMessage(
    content: String,
    timestamp: String,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End,
        ) {
            if (timestamp.isNotBlank()) {
                Text(
                    timestamp,
                    modifier = Modifier.padding(end = 8.dp, bottom = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = Color(0xFFF2ECFF),
                shape = RoundedCornerShape(13.dp, 13.dp, 4.dp, 13.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9C7FF)),
                modifier = Modifier.widthIn(max = 310.dp),
            ) {
                Text(
                    content,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.chat_edit_message),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onRetry, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = stringResource(R.string.chat_retry_message),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(content: String, timestamp: String, running: Boolean) {
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MarkdownMessage(content)
        if (running) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .width(7.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(content)) },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.chat_copy_message),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            if (timestamp.isNotBlank()) {
                Text(
                    timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MarkdownMessage(content: String) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val markwon = remember(context) { Markwon.create(context) }
    AndroidView(
        factory = {
            TextView(it).apply {
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                includeFontPadding = false
                textSize = 16f
                setLineSpacing(0f, 1.42f)
                setPadding(0, 0, 0, 0)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.setLinkTextColor(linkColor)
            markwon.setMarkdown(view, content)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ExecutionCard(entries: List<JSONObject>, running: Boolean) {
    val normalized = normalizeTraceEntries(entries)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF5FAF7),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8DDC7)),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.chat_execution),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            if (normalized.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        stringResource(
                            if (running) R.string.chat_thinking else R.string.chat_execution_complete
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            } else {
                normalized.forEach { entry ->
                    Row(verticalAlignment = Alignment.Top) {
                        if (entry.running) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                if (entry.failed) "×" else "✓",
                                color = if (entry.failed) MaterialTheme.colorScheme.error
                                else Color(0xFF16864B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            entry.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

private data class TraceDisplay(
    val label: String,
    val running: Boolean,
    val failed: Boolean,
)

private fun normalizeTraceEntries(entries: List<JSONObject>): List<TraceDisplay> {
    val byId = linkedMapOf<String, JSONObject>()
    entries.forEachIndexed { index, item ->
        val id = item.optString("toolCallId").ifBlank {
            item.optString("tool_call_id").ifBlank { "trace-$index" }
        }
        val existing = byId[id]
        if (existing == null) {
            byId[id] = item
        } else {
            item.keys().forEach { key -> existing.put(key, item.opt(key)) }
        }
    }
    return byId.values.map { item ->
        val tool = item.optString("text").ifBlank {
            item.optString("tool").ifBlank { item.optString("name") }
        }
        val preview = item.optString("preview").ifBlank {
            item.optString("query").ifBlank { item.optString("detail") }
        }
        val type = item.optString("type")
        val status = item.optString("status")
        val displayTool = when (tool.lowercase()) {
            "websearch", "web_search", "search_web" -> "网络搜索"
            "read", "read_file" -> "读取文件"
            "bash", "exec", "exec_command" -> "执行命令"
            else -> tool.ifBlank { "工具调用" }
        }
        TraceDisplay(
            label = if (preview.isBlank()) displayTool else "$displayTool（$preview）",
            running = status == "running" || type.endsWith("started"),
            failed = item.optBoolean("failed") || status == "failed" || type.endsWith("failed"),
        )
    }
}

private fun formatChatTime(raw: String): String {
    if (raw.isBlank()) return ""
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw.takeLast(5)
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

@Composable
private fun localizedStatus(raw: String): String = when (raw.lowercase()) {
    "idle" -> stringResource(R.string.server_status_idle)
    "active" -> stringResource(R.string.server_status_active)
    "running" -> stringResource(R.string.server_status_running)
    "pending" -> stringResource(R.string.server_status_pending)
    "paused" -> stringResource(R.string.server_status_paused)
    "completed", "done" -> stringResource(R.string.server_status_completed)
    "failed", "error" -> stringResource(R.string.server_status_failed)
    "cancelled", "canceled" -> stringResource(R.string.server_status_cancelled)
    else -> raw
}

private fun displayMessageContent(raw: String): String {
    var current = raw.trim()
    repeat(3) {
        if (!current.startsWith("{") || !current.endsWith("}")) return current
        val parsed = runCatching { JSONObject(current) }.getOrNull() ?: return current
        val nested = parsed.opt("content")
        current = when (nested) {
            is String -> nested.trim()
            is JSONObject -> nested.optString("content", nested.toString()).trim()
            else -> return current
        }
    }
    return current
}

@Composable
private fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF0F7))) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
