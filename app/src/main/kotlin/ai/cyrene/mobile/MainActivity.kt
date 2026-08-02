package ai.cyrene.mobile

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
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
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import ai.cyrene.mobile.data.ApkUpdateDownloader
import ai.cyrene.mobile.data.SecureStore
import ai.cyrene.mobile.data.GithubUpdateService
import ai.cyrene.mobile.data.UpdateCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.movement.MovementMethodPlugin
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFFB8C4FF),
                onPrimary = Color(0xFF10225D),
                primaryContainer = Color(0xFF293E7A),
                onPrimaryContainer = Color(0xFFDDE2FF),
                secondary = Color(0xFFCEBDFA),
                onSecondary = Color(0xFF35275A),
                secondaryContainer = Color(0xFF4C3E72),
                onSecondaryContainer = Color(0xFFEBDDFF),
                tertiary = Color(0xFF8CD4A8),
                onTertiary = Color(0xFF00391E),
                tertiaryContainer = Color(0xFF15512F),
                onTertiaryContainer = Color(0xFFA7F2C3),
                background = Color(0xFF111318),
                onBackground = Color(0xFFE3E2E9),
                surface = Color(0xFF191B20),
                onSurface = Color(0xFFE3E2E9),
                surfaceVariant = Color(0xFF44464F),
                onSurfaceVariant = Color(0xFFC5C6D0),
                outline = Color(0xFF8F909A),
                outlineVariant = Color(0xFF44464F),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = Color(0xFF4059AD),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDDE2FF),
                onPrimaryContainer = Color(0xFF00174B),
                secondary = Color(0xFF67548B),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF2ECFF),
                onSecondaryContainer = Color(0xFF2A1B4D),
                tertiary = Color(0xFF2D6A45),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFD7F4E1),
                onTertiaryContainer = Color(0xFF103821),
                background = Color(0xFFF8F8FC),
                onBackground = Color(0xFF1B1B20),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1B1B20),
                surfaceVariant = Color(0xFFE3E2EC),
                onSurfaceVariant = Color(0xFF46464F),
                outline = Color(0xFF777780),
                outlineVariant = Color(0xFFC7C6D0),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
            )
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CyreneMobile(state: MobileUiState, model: MainViewModel) {
    var tab by remember { mutableIntStateOf(2) }
    var settingsPageId by remember { mutableStateOf<String?>(null) }
    var showAddDevice by remember { mutableStateOf(false) }
    LaunchedEffect(state.peer?.deviceId) {
        showAddDevice = false
    }
    if (showAddDevice) {
        PairingScreen(
            state = state,
            model = model,
            onClose = {
                model.cancelPairing()
                showAddDevice = false
            },
        )
        return
    }
    val tabs = listOf(
        DrawerDestination(0, stringResource(R.string.nav_devices), Icons.Outlined.Devices),
        DrawerDestination(2, stringResource(R.string.nav_chats), Icons.Outlined.ChatBubbleOutline),
        DrawerDestination(3, stringResource(R.string.nav_tasks), Icons.Outlined.CheckCircleOutline),
        DrawerDestination(4, stringResource(R.string.nav_terminal), Icons.Outlined.Terminal),
    )
    val aboutAndUpdatesLabel = stringResource(R.string.settings_update_title)
    val sessions = remember(state.projects, state.projectChats, state.projectTasks) {
        recentSessionsForProjects(
            projects = state.projects,
            projectChats = state.projectChats,
            projectTasks = state.projectTasks,
        )
    }
    var chatMenuTarget by remember { mutableStateOf<RecentSession?>(null) }
    var renameChatTarget by remember { mutableStateOf<RecentSession?>(null) }
    var deleteChatTarget by remember { mutableStateOf<RecentSession?>(null) }
    var renameChatTitle by remember { mutableStateOf("") }
    var pendingLocalExportPath by remember { mutableStateOf<String?>(null) }
    val localFileExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { destination ->
        val path = pendingLocalExportPath
        pendingLocalExportPath = null
        if (destination != null && path != null) model.exportLocalChangedFile(path, destination)
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val localChatSelected = tab == 2 && state.selectedChat?.optBoolean("local") == true
    val rightPanelAvailable =
        (tab == 2 && state.selectedChat != null) ||
            (tab == 3 && state.selectedTask != null)
    var rightPanelOpen by remember { mutableStateOf(false) }
    var rightPanelDragging by remember { mutableStateOf(false) }
    var rightPanelRevealPx by remember { mutableFloatStateOf(0f) }
    var suppressLeftDrawerGestures by remember { mutableStateOf(false) }
    val rightPanelWidth = minOf(
        360.dp,
        LocalConfiguration.current.screenWidthDp.dp * .9f,
    )
    val leftPanelWidth = minOf(
        320.dp,
        LocalConfiguration.current.screenWidthDp.dp * .82f,
    )
    val rightPanelWidthPx = with(LocalDensity.current) { rightPanelWidth.toPx() }
    val leftDrawerOpenThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    LaunchedEffect(
        tab,
        state.selectedChat?.optString("id"),
        state.selectedTask?.optString("id"),
    ) {
        if (tab != 5) settingsPageId = null
        rightPanelOpen = false
        rightPanelDragging = false
        rightPanelRevealPx = 0f
        suppressLeftDrawerGestures = false
    }
    LaunchedEffect(rightPanelOpen, localChatSelected, state.selectedChat?.optString("id")) {
        if (rightPanelOpen && localChatSelected) model.loadLocalChangedFiles()
    }
    BackHandler(enabled = tab == 5 && settingsPageId != null) {
        settingsPageId = null
    }
    BackHandler(
        enabled = localChatSelected &&
            drawerState.currentValue == DrawerValue.Closed &&
            drawerState.targetValue == DrawerValue.Closed,
    ) {
        if (rightPanelOpen || rightPanelDragging) {
            rightPanelOpen = false
            rightPanelDragging = false
            rightPanelRevealPx = 0f
        } else {
            rightPanelOpen = true
            rightPanelRevealPx = rightPanelWidthPx
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled =
            !rightPanelOpen && !rightPanelDragging && !suppressLeftDrawerGestures,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(leftPanelWidth)) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = aboutAndUpdatesLabel
                                    }
                                    .clickable {
                                        rightPanelOpen = false
                                        settingsPageId = "about"
                                        tab = 5
                                        scope.launch { drawerState.close() }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(
                                        R.drawable.ic_launcher_full,
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    "Cyrene",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 104.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(tabs, key = { it.screen }) { destination ->
                                NavigationDrawerItem(
                                    label = { Text(destination.label) },
                                    selected = tab == destination.screen,
                                    onClick = {
                                        rightPanelOpen = false
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
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (sessions.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.menu_no_recent_sessions),
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 12.dp,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                items(
                                    items = sessions,
                                    key = {
                                        "${it.project.optString("id")}-${it.kind}-" +
                                            it.data.optString("id")
                                    },
                                ) { session ->
                                    val data = session.data
                                    val project = session.project
                                    val projectId = project.optString("id")
                                    val itemId = data.optString("id")
                                    val isChat = session.kind == "chat"
                                    val selected = if (isChat) {
                                        tab == 2 &&
                                                state.selectedProject?.optString("id") == projectId &&
                                                state.selectedChat?.optString("id") == itemId
                                    } else {
                                        tab == 3 &&
                                                state.selectedProject?.optString("id") == projectId &&
                                                state.selectedTask?.optString("id") == itemId
                                    }
                                    val openSession = {
                                        chatMenuTarget = null
                                        rightPanelOpen = false
                                        if (isChat) {
                                            tab = 2
                                            if (
                                                state.selectedProject?.optString("id") != projectId ||
                                                state.selectedChat?.optString("id") != itemId
                                            ) {
                                                model.openChat(project, data)
                                            }
                                        } else {
                                            tab = 3
                                            if (
                                                state.selectedProject?.optString("id") != projectId ||
                                                state.selectedTask?.optString("id") != itemId
                                            ) {
                                                model.openTask(project, data)
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                        Unit
                                    }
                                    Box {
                                        RecentSessionDrawerItem(
                                            data = data,
                                            projectName = project.optString("name"),
                                            isChat = isChat,
                                            selected = selected,
                                            onClick = openSession,
                                            onLongClick = if (isChat) {
                                                {
                                                    chatMenuTarget = session
                                                }
                                            } else {
                                                null
                                            },
                                        )
                                        DropdownMenu(
                                            expanded =
                                                isChat &&
                                                    chatMenuTarget?.data?.optString("id") == itemId &&
                                                    chatMenuTarget?.project?.optString("id") == projectId,
                                            onDismissRequest = { chatMenuTarget = null },
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(
                                                            R.string.chat_menu_rename
                                                        )
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    chatMenuTarget = null
                                                    renameChatTitle = data.optString("title")
                                                    renameChatTarget = session
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(
                                                            R.string.chat_menu_delete
                                                        )
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.DeleteSweep,
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    chatMenuTarget = null
                                                    deleteChatTarget = session
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.selectedProject != null) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (!state.busy) {
                                    rightPanelOpen = false
                                    model.showChatList()
                                    tab = 2
                                    scope.launch { drawerState.close() }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                                .padding(20.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            icon = {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription =
                                        stringResource(R.string.chat_create_new),
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            text = {
                                Text(
                                    stringResource(R.string.drawer_new_chat_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                        )
                    }
                    FloatingActionButton(
                        onClick = {
                            rightPanelOpen = false
                            settingsPageId = null
                            tab = 5
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(20.dp),
                        shape = CircleShape,
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    }
                }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (drawerState.targetValue == DrawerValue.Closed) {
                        Modifier.pointerInput(
                            rightPanelAvailable,
                            rightPanelOpen,
                            rightPanelWidthPx,
                            leftDrawerOpenThresholdPx,
                        ) {
                            var dragDistance = 0f
                            var openingLeftDrawer = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragDistance = 0f
                                    openingLeftDrawer = false
                                    rightPanelRevealPx =
                                        if (rightPanelOpen) rightPanelWidthPx else 0f
                                    if (rightPanelOpen) suppressLeftDrawerGestures = true
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    dragDistance += dragAmount
                                    val canDragRightPanel =
                                        rightPanelAvailable &&
                                            drawerState.currentValue == DrawerValue.Closed
                                    val handlingRightPanel = canDragRightPanel && (
                                        rightPanelDragging ||
                                            (rightPanelOpen && dragDistance > 4f) ||
                                            (!rightPanelOpen && dragDistance < -4f)
                                        )
                                    if (handlingRightPanel) {
                                        rightPanelDragging = true
                                        rightPanelRevealPx = if (rightPanelOpen) {
                                            (rightPanelWidthPx - dragDistance)
                                                .coerceIn(0f, rightPanelWidthPx)
                                        } else {
                                            (-dragDistance).coerceIn(0f, rightPanelWidthPx)
                                        }
                                        change.consume()
                                    } else if (
                                        !rightPanelOpen &&
                                        !rightPanelDragging &&
                                        dragDistance > 4f
                                    ) {
                                        openingLeftDrawer = true
                                        change.consume()
                                    }
                                },
                                onDragEnd = {
                                    if (rightPanelDragging) {
                                        rightPanelOpen = if (rightPanelOpen) {
                                            rightPanelRevealPx >= rightPanelWidthPx * .65f
                                        } else {
                                            rightPanelRevealPx >= rightPanelWidthPx * .28f
                                        }
                                        rightPanelRevealPx =
                                            if (rightPanelOpen) rightPanelWidthPx else 0f
                                    } else if (
                                        openingLeftDrawer &&
                                        dragDistance >= leftDrawerOpenThresholdPx
                                    ) {
                                        scope.launch { drawerState.open() }
                                    }
                                    rightPanelDragging = false
                                    openingLeftDrawer = false
                                    if (suppressLeftDrawerGestures) {
                                        scope.launch {
                                            delay(180)
                                            suppressLeftDrawerGestures = false
                                        }
                                    }
                                    dragDistance = 0f
                                },
                                onDragCancel = {
                                    rightPanelRevealPx =
                                        if (rightPanelOpen) rightPanelWidthPx else 0f
                                    rightPanelDragging = false
                                    openingLeftDrawer = false
                                    if (suppressLeftDrawerGestures) {
                                        scope.launch {
                                            delay(180)
                                            suppressLeftDrawerGestures = false
                                        }
                                    }
                                    dragDistance = 0f
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                    navigationIcon = {
                        if (tab == 5 && settingsPageId != null) {
                            IconButton(onClick = { settingsPageId = null }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                rightPanelOpen = false
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    Icons.Outlined.Menu,
                                    contentDescription = stringResource(R.string.menu_open),
                                )
                            }
                        }
                    },
                    title = {
                        val title = if (tab == 2) {
                            state.selectedChat?.optString("title")
                                ?.takeIf(String::isNotBlank)
                                ?: stringResource(R.string.chat_new)
                        } else {
                            if (tab == 5) {
                                settingsPageTitle(settingsPageId, state.desktopSettingsSchema)
                            } else {
                                tabs.firstOrNull { it.screen == tab }?.label
                                    ?: stringResource(R.string.app_name)
                            }
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
                        if (state.busy && !(tab == 2 && state.selectedChat == null)) {
                            CircularProgressIndicator(Modifier.padding(16.dp).width(22.dp))
                        }
                    },
                    )
                },
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (tab) {
                        0 -> DeviceScreen(state, model) { showAddDevice = true }
                        1 -> ProjectScreen(state, model) { tab = 2 }
                        2 -> ChatScreen(state, model)
                        3 -> TaskScreen(state, model)
                        4 -> TerminalScreen(state, model)
                        else -> SettingsScreen(
                            state = state,
                            model = model,
                            pageId = settingsPageId,
                            onOpenPage = { settingsPageId = it },
                        )
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
            if ((rightPanelOpen || rightPanelDragging) && rightPanelAvailable) {
                val rightPanelProgress =
                    (rightPanelRevealPx / rightPanelWidthPx).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = .42f * rightPanelProgress))
                        .clickable(enabled = rightPanelOpen && !rightPanelDragging) {
                            rightPanelOpen = false
                            rightPanelRevealPx = 0f
                        },
                )
                val panelModifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset {
                        IntOffset(
                            x = (rightPanelWidthPx - rightPanelRevealPx).roundToInt(),
                            y = 0,
                        )
                    }
                    .fillMaxHeight()
                    .width(rightPanelWidth)
                val closePanel = {
                    rightPanelOpen = false
                    rightPanelRevealPx = 0f
                }
                if (localChatSelected) {
                    LocalChangedFilesSidebar(
                        state = state,
                        onClose = closePanel,
                        onDownload = { path ->
                            pendingLocalExportPath = path
                            localFileExporter.launch(path.substringAfterLast('/').ifBlank { "file" })
                        },
                        modifier = panelModifier,
                    )
                } else {
                    DesktopRightSidebar(
                        state = state,
                        model = model,
                        screen = tab,
                        onClose = closePanel,
                        modifier = panelModifier,
                    )
                }
            }
        }
    }
    renameChatTarget?.let { session ->
        val chat = session.data
        AlertDialog(
            onDismissRequest = { renameChatTarget = null },
            title = { Text(stringResource(R.string.chat_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameChatTitle,
                    onValueChange = { renameChatTitle = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.chat_rename_label)) },
                    singleLine = true,
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { renameChatTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = renameChatTitle.isNotBlank() && !state.busy,
                    onClick = {
                        model.renameChat(session.project, chat, renameChatTitle)
                        renameChatTarget = null
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
        )
    }
    deleteChatTarget?.let { session ->
        val chat = session.data
        val chatTitle = chat.optString("title").ifBlank {
            stringResource(R.string.chat_unnamed)
        }
        AlertDialog(
            onDismissRequest = { deleteChatTarget = null },
            title = { Text(stringResource(R.string.chat_delete_title)) },
            text = {
                Text(stringResource(R.string.chat_delete_message, chatTitle))
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteChatTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.busy,
                    onClick = {
                        model.deleteChat(session.project, chat)
                        deleteChatTarget = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
        )
    }
}

private data class DrawerDestination(
    val screen: Int,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun LocalChangedFilesSidebar(
    state: MobileUiState,
    onClose: () -> Unit,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panelContentColor = rightSidebarContentColor()
    Surface(
        modifier = modifier,
        color = if (MaterialTheme.colorScheme.background.luminance() < .5f) {
            Color(0xFF141F31)
        } else MaterialTheme.colorScheme.background,
        contentColor = panelContentColor,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        stringResource(R.string.local_files_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = panelContentColor,
                    )
                    Text(
                        stringResource(R.string.local_files_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        stringResource(R.string.right_sidebar_close),
                        tint = panelContentColor,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
            when {
                state.localChangedFilesLoading -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.localChangedFiles.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.local_files_empty),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.localChangedFiles, key = { it.optString("path") }) { file ->
                        val path = file.optString("path")
                        val fileName = path.substringAfterLast('/')
                        val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "$fileName, ${file.optLong("size").let(::formatFileSize)}"
                                }
                                .clickable { onDownload(path) },
                            colors = CardDefaults.cardColors(
                                containerColor = rightSidebarCardColor(),
                                contentColor = panelContentColor,
                            ),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .52f),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.AttachFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        fileName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = panelContentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (parentPath.isNotBlank()) {
                                        Text(
                                            parentPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        formatFileSize(file.optLong("size")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            stringResource(R.string.local_files_download),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSessionDrawerItem(
    data: JSONObject,
    projectName: String,
    isChat: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val fallbackTitle = stringResource(
        if (isChat) R.string.chat_unnamed else R.string.task_unnamed,
    )
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isChat) Icons.Outlined.ChatBubbleOutline
            else Icons.Outlined.CheckCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                data.optString("title").ifBlank { fallbackTitle },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            val sessionSummary = if (isChat) {
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
                }
            Text(
                stringResource(
                    R.string.menu_project_session_summary,
                    projectName.ifBlank { stringResource(R.string.project_unnamed) },
                    sessionSummary,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DesktopRightSidebar(
    state: MobileUiState,
    model: MainViewModel,
    screen: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chat = state.selectedChat
    val task = state.selectedTask
    val plan = if (screen == 2) chat?.optJSONObject("active_plan") else null
    val chatFiles = remember(chat) {
        buildList {
            val messages = chat?.optJSONArray("messages")
            if (messages != null) {
                for (messageIndex in 0 until messages.length()) {
                    val message = messages.optJSONObject(messageIndex) ?: continue
                    val role = message.optString("role")
                    val attachments = message.optJSONArray("attachments") ?: continue
                    for (fileIndex in 0 until attachments.length()) {
                        attachments.optJSONObject(fileIndex)?.let { add(role to it) }
                    }
                }
            }
        }
    }
    val chatBranches = remember(chat, state.chats) {
        relatedChatBranches(chat, state.chats)
    }
    val subagents = chat?.optJSONObject("subagents")
    val changes = chat?.optJSONObject("changes")
    val mapData = chat?.optJSONObject("map")
    val hasSubagents = (subagents?.optJSONArray("rounds")?.length() ?: 0) > 0 ||
        (subagents?.optJSONArray("agents")?.length() ?: 0) > 0
    val hasChanges = (changes?.optJSONArray("changeSets")?.length() ?: 0) > 0
    val hasMap = (mapData?.optJSONArray("pins")?.length() ?: 0) > 0 ||
        (mapData?.optJSONArray("routes")?.length() ?: 0) > 0
    val tabs = buildList {
        add("overview" to stringResource(R.string.right_sidebar_overview))
        val hasPlan = if (screen == 2) {
            plan != null
        } else {
            (task?.optJSONArray("plan")?.length() ?: 0) > 0
        }
        if (hasPlan) add("plan" to stringResource(R.string.right_sidebar_plan))
        if (screen == 2 && hasSubagents) {
            add("subagents" to stringResource(R.string.right_sidebar_subagents))
        }
        add("context" to stringResource(R.string.right_sidebar_context))
        val hasArtifacts = if (screen == 2) chatFiles.isNotEmpty() else state.artifacts.isNotEmpty()
        if (hasArtifacts) add("artifacts" to stringResource(R.string.right_sidebar_artifacts))
        if (screen == 2 && hasChanges) {
            add("changes" to stringResource(R.string.right_sidebar_changes))
        }
        if (screen == 2 && chatBranches.size > 1) {
            add("branches" to stringResource(R.string.right_sidebar_branches))
        }
        if (screen == 2 && state.viewerFile != null) {
            add("viewer" to stringResource(R.string.right_sidebar_viewer))
        }
        if (screen == 2 && hasMap) {
            add("map" to stringResource(R.string.right_sidebar_map))
        }
    }
    var selectedTab by remember(
        screen,
        chat?.optString("id"),
        task?.optString("id"),
    ) { mutableStateOf("overview") }
    LaunchedEffect(state.viewerFile, state.viewerFilePath) {
        if (state.viewerFile != null) selectedTab = "viewer"
    }
    val activeTab = selectedTab.takeIf { id -> tabs.any { it.first == id } } ?: "overview"

    Surface(
        modifier = modifier,
        color = if (MaterialTheme.colorScheme.background.luminance() < .5f) {
            Color(0xFF141F31)
        } else {
            MaterialTheme.colorScheme.background
        },
        contentColor = rightSidebarContentColor(),
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (id, label) ->
                    Column(
                        Modifier
                            .clickable { selectedTab = id }
                            .padding(horizontal = 10.dp, vertical = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeTab == id) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = if (activeTab == id) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.right_sidebar_close),
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (activeTab) {
                    "plan" -> item {
                        RightSidebarPlan(
                            plan = if (screen == 2) {
                                plan?.optJSONArray("steps")
                            } else {
                                task?.optJSONArray("plan")
                            },
                            title = if (screen == 2) plan?.optString("title").orEmpty() else "",
                            summary = if (screen == 2) plan?.optString("summary").orEmpty() else "",
                        )
                    }
                    "context" -> item {
                        if (screen == 2) {
                            RightSidebarChatContext(state)
                        } else {
                            RightSidebarTaskContext(state)
                        }
                    }
                    "artifacts" -> {
                        if (screen == 2) {
                            items(chatFiles, key = {
                                "${it.second.optString("id")}:${it.second.optString("name")}"
                            }) { (role, file) ->
                                RightSidebarFile(
                                    file = file,
                                    userUpload = role == "user",
                                    onClick = { model.previewChatAttachment(file) },
                                )
                            }
                        } else {
                            items(state.artifacts, key = { it.optString("id") }) { artifact ->
                                RightSidebarFile(artifact, false)
                            }
                        }
                    }
                    "subagents" -> item {
                        RightSidebarSubagents(subagents ?: JSONObject())
                    }
                    "changes" -> item {
                        RightSidebarChanges(
                            changes = changes ?: JSONObject(),
                            state = state,
                            model = model,
                        )
                    }
                    "branches" -> items(
                        chatBranches,
                        key = { it.chat.optString("id") },
                    ) { branch ->
                        RightSidebarBranch(
                            branch = branch,
                            onClick = {
                                if (branch.relation != ChatBranchRelation.Current) {
                                    model.openChat(branch.chat)
                                    onClose()
                                }
                            },
                        )
                    }
                    "viewer" -> item {
                        RightSidebarViewer(state = state, onClose = model::clearViewer)
                    }
                    "map" -> item {
                        RightSidebarMap(mapData ?: JSONObject())
                    }
                    else -> item {
                        if (screen == 2) {
                            RightSidebarChatOverview(state)
                        } else {
                            RightSidebarTaskOverview(state)
                        }
                    }
                }
            }
        }
    }
}

private enum class ChatBranchRelation {
    Current,
    Parent,
    Child,
    Sibling,
}

private data class ChatBranch(
    val chat: JSONObject,
    val relation: ChatBranchRelation,
)

private fun relatedChatBranches(
    selectedChat: JSONObject?,
    chats: List<JSONObject>,
): List<ChatBranch> {
    val current = selectedChat ?: return emptyList()
    val currentId = current.optString("id")
    if (currentId.isBlank()) return emptyList()
    val currentOrigin = current.optString("forked_from_chat_id")
        .ifBlank { current.optString("parent_chat_id") }
    val result = linkedMapOf(currentId to ChatBranch(current, ChatBranchRelation.Current))
    chats.forEach { candidate ->
        val candidateId = candidate.optString("id")
        if (candidateId.isBlank() || candidateId == currentId) return@forEach
        val candidateOrigin = candidate.optString("forked_from_chat_id")
            .ifBlank { candidate.optString("parent_chat_id") }
        val relation = when {
            currentOrigin.isNotBlank() && candidateId == currentOrigin ->
                ChatBranchRelation.Parent
            candidateOrigin == currentId ->
                ChatBranchRelation.Child
            currentOrigin.isNotBlank() && candidateOrigin == currentOrigin ->
                ChatBranchRelation.Sibling
            else -> null
        }
        if (relation != null) result[candidateId] = ChatBranch(candidate, relation)
    }
    return result.values.toList()
}

@Composable
private fun RightSidebarBranch(
    branch: ChatBranch,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = rightSidebarCardColor(),
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
        ),
        shape = RoundedCornerShape(13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = if (branch.relation == ChatBranchRelation.Current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    branch.chat.optString("title")
                        .ifBlank { stringResource(R.string.chat_unnamed) },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        when (branch.relation) {
                            ChatBranchRelation.Current -> R.string.right_sidebar_current_chat
                            ChatBranchRelation.Parent -> R.string.right_sidebar_parent_chat
                            ChatBranchRelation.Child -> R.string.right_sidebar_child_chat
                            ChatBranchRelation.Sibling -> R.string.right_sidebar_sibling_chat
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RightSidebarChatOverview(state: MobileUiState) {
    val chat = state.selectedChat ?: return
    val context = chat.optJSONObject("context_metrics") ?: JSONObject()
    val usage = context.optJSONObject("usage")
        ?: chat.optJSONObject("usage")
        ?: JSONObject()
    val promptTokens = usage.optLong("prompt_tokens")
    val completionTokens = usage.optLong("completion_tokens")
    val totalTokens = usage.optLong("total_tokens").takeIf { it > 0 }
        ?: (promptTokens + completionTokens)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RightSidebarSection(stringResource(R.string.right_sidebar_run_summary)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF20A464),
                        trackColor = Color(0xFF344153),
                        strokeWidth = 7.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            compactTokenCount(totalTokens),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.right_sidebar_total_tokens),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SidebarUsageRow(
                        stringResource(R.string.right_sidebar_input_tokens),
                        promptTokens,
                        Color(0xFF3B82F6),
                    )
                    SidebarUsageRow(
                        stringResource(R.string.right_sidebar_output_tokens),
                        completionTokens,
                        Color(0xFF9A88C7),
                    )
                    HorizontalDivider()
                    SidebarUsageRow(
                        stringResource(R.string.right_sidebar_total_tokens),
                        totalTokens,
                        Color(0xFF20A464),
                    )
                }
            }
        }
        RightSidebarSection(stringResource(R.string.right_sidebar_session_info)) {
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_status),
                localizedStatus(chat.optString("status")),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_messages),
                chat.optInt(
                    "message_count",
                    chat.optJSONArray("messages")?.length() ?: 0,
                ).toString(),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_model),
                chat.optString("model").ifBlank { "—" },
                monospace = true,
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_session_id),
                chat.optString("id"),
                monospace = true,
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_created),
                formatSidebarDate(chat.optString("created_at")),
            )
        }
        RightSidebarContextWindow(context)
    }
}

@Composable
private fun SidebarUsageRow(label: String, value: Long, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(compactTokenCount(value), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RightSidebarContextWindow(context: JSONObject) {
    if (context.length() == 0) return
    val used = context.optLong("ctxUsed")
    val limit = context.optLong("ctxLimit")
    val ratio = context.optDouble("ratio", if (limit > 0) used.toDouble() / limit else 0.0)
        .toFloat().coerceIn(0f, 1f)
    val triggerRatio = context.optDouble("compactTriggerRatio", .6).toFloat()
    val segments = context.optJSONArray("segments") ?: JSONArray()
    RightSidebarSection(stringResource(R.string.right_sidebar_context_occupancy)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    if (ratio > 0f && ratio < .01f) {
                        "<1%"
                    } else {
                        "${(ratio * 100).roundToInt()}%"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.right_sidebar_context_used),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${compactTokenCount(used)} / ${compactTokenCount(limit)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(
            Modifier.fillMaxWidth().height(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    .background(Color(0xFF1D2A3C)),
            )
            if (ratio > 0f) {
                Box(
                    Modifier.fillMaxWidth(ratio.coerceAtLeast(.012f)).height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF20A464)),
                )
            }
            Row(Modifier.fillMaxWidth().height(12.dp)) {
                Spacer(Modifier.weight(triggerRatio.coerceIn(.01f, .99f)))
                Box(Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF7C8798)))
                Spacer(Modifier.weight((1f - triggerRatio).coerceAtLeast(.01f)))
            }
        }
        Text(
            stringResource(
                R.string.right_sidebar_compaction_triggers_at,
                (triggerRatio * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (ratio >= triggerRatio) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (segments.length() > 0 && used > 0) {
            Text(
                stringResource(R.string.right_sidebar_context_breakdown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (index in 0 until segments.length()) {
                val segment = segments.optJSONObject(index) ?: continue
                val tokens = segment.optLong("tokens")
                if (tokens <= 0) continue
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(7.dp).clip(CircleShape)
                            .background(contextMessageColor(segment.optString("key"))),
                    )
                    Text(
                        localizedContextSegment(segment.optString("key")),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        String.format(Locale.US, "%.1f%%", tokens.toDouble() / used * 100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RightSidebarTaskOverview(state: MobileUiState) {
    val task = state.selectedTask ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RightSidebarSection(stringResource(R.string.right_sidebar_task_overview)) {
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_status),
                localizedStatus(task.optString("status")),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_priority),
                task.optString("priority", "medium"),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_created),
                formatSidebarDate(task.optString("created_at")),
            )
            Text(
                task.optString("goal"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RightSidebarSection(stringResource(R.string.right_sidebar_progress)) {
            val steps = task.optJSONArray("plan")
            val total = steps?.length() ?: 0
            val done = (0 until total).count {
                steps?.optJSONObject(it)?.optString("status") == "completed"
            }
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_steps),
                "$done / $total",
            )
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RightSidebarChatContext(state: MobileUiState) {
    val chat = state.selectedChat ?: return
    val blocks = chat.optJSONObject("context_blocks") ?: JSONObject()
    val layers = blocks.optJSONArray("layers") ?: JSONArray()
    val inbox = chat.optJSONObject("inbox") ?: JSONObject()
    val packages = chat.optJSONArray("used_tool_packages") ?: JSONArray()
    val barTotal = (0 until layers.length()).sumOf {
        layers.optJSONObject(it)?.optLong("totalTokens") ?: 0L
    }
    val messageTokens = blocks.optLong("messageTokens").takeIf { it > 0 } ?: barTotal
    val systemColors = listOf(
        Color(0xFFC9D2DF),
        Color(0xFFA9C7FF),
        Color(0xFFC8C1E8),
        Color(0xFF9ED9B8),
        Color(0xFFE1B36A),
        Color(0xFFC59573),
        Color(0xFF86BCD2),
        Color(0xFFB58AD7),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RightSidebarSection(stringResource(R.string.right_sidebar_conversation_context)) {
            if (layers.length() == 0) {
                Text(
                    stringResource(R.string.right_sidebar_context_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        compactTokenCount(messageTokens),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.right_sidebar_tokens),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (barTotal > 0) {
                    Row(
                        Modifier.fillMaxWidth().height(11.dp).clip(CircleShape),
                    ) {
                        for (layerIndex in 0 until layers.length()) {
                            val layer = layers.optJSONObject(layerIndex) ?: continue
                            val layerId = layer.optString("id")
                            val layerBlocks = layer.optJSONArray("blocks") ?: JSONArray()
                            if (
                                (layerId == "system_prefix" || layerId == "messages") &&
                                layerBlocks.length() > 0
                            ) {
                                for (blockIndex in 0 until layerBlocks.length()) {
                                    val block = layerBlocks.optJSONObject(blockIndex) ?: continue
                                    val tokens = block.optLong("tokens_est")
                                    if (tokens <= 0) continue
                                    val color = if (layerId == "messages") {
                                        contextMessageColor(block.optString("type"))
                                    } else {
                                        systemColors[systemContextShade(block)]
                                    }
                                    Box(
                                        Modifier.weight(tokens.toFloat()).fillMaxHeight()
                                            .background(color),
                                    )
                                }
                            } else {
                                val tokens = layer.optLong("totalTokens")
                                if (tokens > 0) {
                                    Box(
                                        Modifier.weight(tokens.toFloat()).fillMaxHeight()
                                            .background(Color(0xFFE9777C)),
                                    )
                                }
                            }
                        }
                    }
                }
                for (layerIndex in 0 until layers.length()) {
                    val layer = layers.optJSONObject(layerIndex) ?: continue
                    if (layer.optLong("totalTokens") <= 0) continue
                    val layerId = layer.optString("id")
                    Text(
                        localizedContextLayer(layerId, layer.optString("label")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val layerBlocks = layer.optJSONArray("blocks") ?: JSONArray()
                    if (layerBlocks.length() == 0) {
                        ContextLegendRow(
                            color = Color(0xFFE9777C),
                            label = localizedContextLayer(layerId, layer.optString("label")),
                            tokens = layer.optLong("totalTokens"),
                        )
                    } else {
                        for (blockIndex in 0 until layerBlocks.length()) {
                            val block = layerBlocks.optJSONObject(blockIndex) ?: continue
                            val tokens = block.optLong("tokens_est")
                            if (tokens <= 0) continue
                            ContextLegendRow(
                                color = when (layerId) {
                                    "system_prefix" ->
                                        systemColors[systemContextShade(block)]
                                    "messages" ->
                                        contextMessageColor(block.optString("type"))
                                    else -> Color(0xFFE9777C)
                                },
                                label = if (layerId == "messages") {
                                    localizedContextSegment(block.optString("type"))
                                } else {
                                    localizedContextBlock(block.optString("id"))
                                },
                                tokens = tokens,
                            )
                        }
                    }
                }
            }
        }
        RightSidebarSection(
            buildString {
                append(stringResource(R.string.right_sidebar_agent_inbox))
                val counts = inbox.optJSONObject("counts") ?: JSONObject()
                if (counts.optInt("total") == 0) {
                    append("  ·  ")
                    append(stringResource(R.string.right_sidebar_queue_empty))
                }
            },
        ) {
            val events = inbox.optJSONArray("events") ?: JSONArray()
            val tools = inbox.optJSONArray("tools") ?: JSONArray()
            if (events.length() == 0 && tools.length() == 0) {
                Text(
                    stringResource(R.string.right_sidebar_inbox_desktop_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (index in 0 until events.length()) {
                    val event = events.optJSONObject(index) ?: continue
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            event.optString("kind")
                                .ifBlank { event.optString("type", "Inbox") },
                            fontWeight = FontWeight.SemiBold,
                        )
                        event.optString("preview").takeIf(String::isNotBlank)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                for (index in 0 until tools.length()) {
                    val tool = tools.optJSONObject(index) ?: continue
                    SidebarMetricRow(
                        tool.optString("name", stringResource(R.string.right_sidebar_tool)),
                        localizedStatus(tool.optString("state")),
                        monospace = true,
                    )
                }
            }
        }
        RightSidebarSection(stringResource(R.string.right_sidebar_used_tool_packages)) {
            if (packages.length() == 0) {
                Text(
                    stringResource(R.string.right_sidebar_no_tool_packages_desktop),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (index in 0 until packages.length()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF20A464)),
                    )
                    Text(
                        localizedToolPackage(packages.optString(index)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        RightSidebarSection(stringResource(R.string.right_sidebar_chat_stats)) {
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_message_count),
                chat.optInt(
                    "message_count",
                    chat.optJSONArray("messages")?.length() ?: 0,
                ).toString(),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_last_updated),
                formatSidebarDate(chat.optString("updated_at")),
            )
        }
    }
}

@Composable
private fun ContextLegendRow(color: Color, label: String, tokens: Long) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            compactTokenCount(tokens),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RightSidebarTaskContext(state: MobileUiState) {
    val task = state.selectedTask ?: return
    val project = state.selectedProject
    RightSidebarSection(stringResource(R.string.right_sidebar_task_context)) {
        SidebarMetricRow(
            stringResource(R.string.right_sidebar_project),
            project?.optString("name").orEmpty().ifBlank { "—" },
        )
        SidebarMetricRow(
            stringResource(R.string.right_sidebar_last_updated),
            formatSidebarDate(task.optString("updated_at")),
        )
        SidebarMetricRow(
            stringResource(R.string.right_sidebar_artifacts),
            state.artifacts.size.toString(),
        )
        SelectionContainer {
            Text(
                task.optString("goal"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RightSidebarPlan(
    plan: JSONArray?,
    title: String,
    summary: String,
) {
    RightSidebarSection(
        title.ifBlank { stringResource(R.string.right_sidebar_plan) },
    ) {
        if (summary.isNotBlank()) {
            Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (plan == null || plan.length() == 0) {
            Text(
                stringResource(R.string.right_sidebar_plan_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            for (index in 0 until plan.length()) {
                val step = plan.optJSONObject(index) ?: continue
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (step.optString("status") == "completed") {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ) {
                        Text(
                            (index + 1).toString(),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            step.optString("title")
                                .ifBlank { stringResource(R.string.right_sidebar_step, index + 1) },
                            fontWeight = FontWeight.SemiBold,
                        )
                        step.optString("description").takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            localizedStatus(step.optString("status", "pending")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RightSidebarSubagents(data: JSONObject) {
    val rounds = data.optJSONArray("rounds") ?: JSONArray()
    val agents = data.optJSONArray("agents") ?: JSONArray()
    val messages = data.optJSONArray("messages") ?: JSONArray()
    val activeRoundId = data.optString("activeRoundId")
    val activeRound = (0 until rounds.length())
        .mapNotNull(rounds::optJSONObject)
        .firstOrNull { it.optString("id") == activeRoundId }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RightSidebarSection(stringResource(R.string.right_sidebar_subagent_activity)) {
            Text(
                activeRound?.optString("title")
                    .orEmpty()
                    .ifBlank { activeRoundId },
                fontWeight = FontWeight.SemiBold,
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_agents),
                agents.length().toString(),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_active),
                (activeRound?.optInt("activeCount") ?: 0).toString(),
            )
        }
        for (index in 0 until agents.length()) {
            val agent = agents.optJSONObject(index) ?: continue
            RightSidebarSection(
                agent.optString("name").ifBlank {
                    stringResource(R.string.right_sidebar_subagent)
                },
            ) {
                Text(
                    localizedStatus(agent.optString("status")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                agent.optString("task").takeIf(String::isNotBlank)?.let { task ->
                    Text(
                        task,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                agent.optString("result").takeIf(String::isNotBlank)?.let { result ->
                    HorizontalDivider()
                    MarkdownMessage(result)
                }
            }
        }
        if (messages.length() > 0) {
            RightSidebarSection(stringResource(R.string.right_sidebar_agent_messages)) {
                for (index in 0 until messages.length()) {
                    val message = messages.optJSONObject(index) ?: continue
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                buildString {
                                    append(message.optString("from").ifBlank { "Agent" })
                                    message.optString("to").takeIf(String::isNotBlank)?.let {
                                        append(" → ")
                                        append(it)
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                formatSidebarDate(message.optString("timestamp")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MarkdownMessage(message.optString("content"))
                    }
                    if (index < messages.length() - 1) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RightSidebarChanges(
    changes: JSONObject,
    state: MobileUiState,
    model: MainViewModel,
) {
    val changeSets = changes.optJSONArray("changeSets") ?: JSONArray()
    val changeSet = changeSets.optJSONObject(0)
    val files = changeSet?.optJSONArray("files") ?: JSONArray()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RightSidebarSection(stringResource(R.string.right_sidebar_change_summary)) {
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_changed_files),
                changes.optInt("fileCount").toString(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "+${changes.optInt("additions")}",
                    color = Color(0xFF2E9B62),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "−${changes.optInt("deletions")}",
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            changeSet?.optString("completedAt")?.takeIf(String::isNotBlank)?.let {
                Text(
                    formatSidebarDate(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (changeSet != null) model.loadChangeDiff(changeSet, file)
                },
                colors = CardDefaults.cardColors(
                    containerColor = rightSidebarCardColor(),
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
                ),
                shape = RoundedCornerShape(13.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        file.optString("path"),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            localizedChangeType(file.optString("changeType")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "+${file.optInt("additions")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E9B62),
                        )
                        Text(
                            "−${file.optInt("deletions")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        if (state.changeDiffLoading) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.selectedChangeDiff?.let { diff ->
            RightSidebarSection(diff.optString("path")) {
                if (diff.optBoolean("binary")) {
                    Text(
                        stringResource(R.string.right_sidebar_binary_diff),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val text = diff.optString("diff")
                    if (text.isBlank()) {
                        Text(
                            stringResource(R.string.right_sidebar_diff_unavailable),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                text.lineSequence().take(500).forEach { line ->
                                    Text(
                                        line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = when {
                                            line.startsWith("+") && !line.startsWith("+++") ->
                                                Color(0xFF2E9B62)
                                            line.startsWith("-") && !line.startsWith("---") ->
                                                MaterialTheme.colorScheme.error
                                            line.startsWith("@@") ->
                                                MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RightSidebarViewer(
    state: MobileUiState,
    onClose: () -> Unit,
) {
    val file = state.viewerFile ?: return
    val path = state.viewerFilePath
    val mediaType = state.viewerMimeType.orEmpty().lowercase(Locale.ROOT)
    val name = file.optString("name", "file")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(
                    mediaType.ifBlank { stringResource(R.string.right_sidebar_unknown_file_type) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
        if (state.viewerLoading) {
            Box(
                Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (path == null) {
            Text(
                stringResource(R.string.right_sidebar_preview_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val localFile = remember(path) { File(path) }
            when {
                mediaType.startsWith("image/") -> {
                    val bitmap = remember(path) {
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
                        )
                    }
                }
                mediaType == "application/pdf" ||
                    name.endsWith(".pdf", ignoreCase = true) -> {
                    val bitmap = remember(path) { renderPdfFirstPage(localFile)?.asImageBitmap() }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
                        )
                        Text(
                            stringResource(R.string.right_sidebar_pdf_first_page),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                mediaType == "text/html" ||
                    name.endsWith(".html", true) ||
                    name.endsWith(".htm", true) -> {
                    val html = remember(path) {
                        runCatching { localFile.readText().take(1_000_000) }.getOrDefault("")
                    }
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                loadDataWithBaseURL(
                                    localFile.parentFile?.toURI()?.toString(),
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(600.dp),
                    )
                }
                isMarkdownFile(name, mediaType) -> {
                    val content = remember(path) {
                        runCatching { localFile.readText().take(500_000) }.getOrDefault("")
                    }
                    MarkdownMessage(content)
                }
                mediaType.startsWith("text/") || isTextFile(name) -> {
                    val content = remember(path) {
                        runCatching { localFile.readText().take(500_000) }.getOrDefault("")
                    }
                    SelectionContainer {
                        Text(
                            content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }
                else -> {
                    Text(
                        stringResource(R.string.right_sidebar_preview_unsupported),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SidebarMetricRow(
                        stringResource(R.string.right_sidebar_files),
                        formatFileSize(localFile.length()),
                    )
                }
            }
        }
    }
}

@Composable
private fun RightSidebarMap(data: JSONObject) {
    val pins = data.optJSONArray("pins") ?: JSONArray()
    val routes = data.optJSONArray("routes") ?: JSONArray()
    val isDark = MaterialTheme.colorScheme.background.luminance() < .5f
    val html = remember(data.toString(), isDark) {
        mapViewerHtml(data, isDark)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    loadDataWithBaseURL(
                        "https://unpkg.com/leaflet@1.9.4/",
                        html,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            },
            update = {
                it.loadDataWithBaseURL(
                    "https://unpkg.com/leaflet@1.9.4/",
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            },
            modifier = Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(14.dp)),
        )
        RightSidebarSection(stringResource(R.string.right_sidebar_map_summary)) {
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_locations),
                pins.length().toString(),
            )
            SidebarMetricRow(
                stringResource(R.string.right_sidebar_routes),
                routes.length().toString(),
            )
            for (index in 0 until pins.length()) {
                val pin = pins.optJSONObject(index) ?: continue
                Text(
                    "• " + pin.optString("label").ifBlank {
                        pin.optString("name").ifBlank {
                            "${pin.optDouble("lat")}, ${pin.optDouble("lng")}"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RightSidebarFile(
    file: JSONObject,
    userUpload: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = rightSidebarCardColor(),
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
        ),
        shape = RoundedCornerShape(13.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    file.optString("name", "file"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (userUpload) {
                        stringResource(R.string.right_sidebar_user_upload)
                    } else {
                        stringResource(R.string.right_sidebar_agent_file)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            file.optLong("size").takeIf { it > 0 }?.let {
                Text(
                    formatFileSize(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RightSidebarSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = rightSidebarCardColor(),
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun rightSidebarCardColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .5f) {
        Color(0xFF202D40)
    } else {
        MaterialTheme.colorScheme.surface
    }

@Composable
private fun rightSidebarContentColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .5f) {
        Color(0xFFF0F3FA)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun SidebarMetricRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.weight(.44f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.weight(.56f)) {
            SelectionContainer {
                Text(
                    value.ifBlank { "—" },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                )
            }
        }
    }
}

private fun countChatAttachments(chat: JSONObject): Int {
    val messages = chat.optJSONArray("messages") ?: return 0
    var count = 0
    for (index in 0 until messages.length()) {
        count += messages.optJSONObject(index)?.optJSONArray("attachments")?.length() ?: 0
    }
    return count
}

private fun compactTokenCount(value: Long): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
    else -> value.toString()
}

private fun systemContextShade(block: JSONObject): Int {
    val type = block.optString("type")
    if (type != "system") {
        return when (type) {
            "memory" -> 1
            "skills" -> 2
            "runtime" -> 3
            "command_prompt" -> 4
            "spawn_policy" -> 5
            "short_term" -> 6
            else -> 7
        }
    }
    val id = block.optString("id")
    return when {
        id.startsWith("main.system.static_extra") -> 4
        id.startsWith("main.system.language") -> 1
        id.startsWith("memory.") -> 1
        id.startsWith("skills.") -> 2
        id.startsWith("runtime.workspace") -> 3
        id.startsWith("runtime.permission") -> 6
        id.startsWith("runtime.project") -> 1
        id.startsWith("runtime.session") -> 2
        id.startsWith("runtime.spawn") -> 5
        id.startsWith("runtime.goal") -> 4
        id.startsWith("command.") -> 5
        id.startsWith("spawn_policy.") -> 7
        id.startsWith("short_term.") -> 7
        else -> 0
    }
}

private fun contextMessageColor(type: String): Color = when (type) {
    "user" -> Color(0xFF3977EF)
    "assistant" -> Color(0xFF9A88C7)
    "tool" -> Color(0xFF20A464)
    "compacted" -> Color(0xFF88A9D8)
    else -> Color(0xFF7C8798)
}

@Composable
private fun localizedContextSegment(key: String): String = when (key.lowercase(Locale.ROOT)) {
    "compacted" -> stringResource(R.string.right_sidebar_segment_compacted)
    "system", "system_context", "instructions" ->
        stringResource(R.string.right_sidebar_segment_system)
    "user" -> stringResource(R.string.right_sidebar_segment_user)
    "assistant" -> stringResource(R.string.right_sidebar_segment_assistant)
    "tool", "tools" -> stringResource(R.string.right_sidebar_segment_tool)
    "ephemeral" -> stringResource(R.string.right_sidebar_segment_ephemeral)
    else -> key.replace("_", " ").ifBlank { "—" }
}

@Composable
private fun localizedContextLayer(id: String, fallback: String): String = when (id) {
    "system_prefix" -> stringResource(R.string.right_sidebar_layer_system_prefix)
    "ephemeral" -> stringResource(R.string.right_sidebar_layer_ephemeral)
    "messages" -> stringResource(R.string.right_sidebar_layer_messages)
    else -> fallback.ifBlank { id }
}

@Composable
private fun localizedContextBlock(id: String): String = when {
    id == "main.system.base" -> stringResource(R.string.right_sidebar_ctx_base_instructions)
    id == "main.system.effective" -> stringResource(R.string.right_sidebar_ctx_system_prompt)
    id == "main.system.static_extra" -> stringResource(R.string.right_sidebar_ctx_task_framing)
    id == "main.system.language" -> stringResource(R.string.right_sidebar_ctx_language)
    id == "mode.plan.discovery" -> stringResource(R.string.right_sidebar_ctx_plan_discovery)
    id == "memory.context" -> stringResource(R.string.right_sidebar_ctx_memory)
    id == "skills.installed" -> stringResource(R.string.right_sidebar_ctx_installed_skills)
    id == "skills.learned" -> stringResource(R.string.right_sidebar_ctx_learned_skills)
    id == "runtime.workspace_scope" -> stringResource(R.string.right_sidebar_ctx_workspace_scope)
    id == "runtime.permission" -> stringResource(R.string.right_sidebar_ctx_permission)
    id == "runtime.project_context" -> stringResource(R.string.right_sidebar_ctx_project_memory)
    id == "runtime.session_scope" -> stringResource(R.string.right_sidebar_ctx_session_labels)
    id == "runtime.spawn_policy" -> stringResource(R.string.right_sidebar_ctx_subagent_policy)
    id == "runtime.goal" -> stringResource(R.string.right_sidebar_ctx_goal_hint)
    id == "ephemeral.run" -> stringResource(R.string.right_sidebar_ctx_runtime_injection)
    id == "short_term.restored" -> stringResource(R.string.right_sidebar_ctx_short_term_memory)
    id == "spawn_policy.conservative" -> stringResource(R.string.right_sidebar_ctx_conservative)
    id == "spawn_policy.default" -> stringResource(R.string.right_sidebar_ctx_default_policy)
    id == "spawn_policy.off" -> stringResource(R.string.right_sidebar_ctx_subagents_off)
    id.startsWith("history.compacted.") ->
        stringResource(R.string.right_sidebar_segment_compacted)
    id.startsWith("history.tool_result.") ->
        stringResource(R.string.right_sidebar_ctx_tool_result)
    id.startsWith("session.history.") ->
        stringResource(R.string.right_sidebar_ctx_history_message)
    id.startsWith("user.current.") ->
        stringResource(R.string.right_sidebar_ctx_user_message)
    else -> id
        .removePrefix("main.")
        .removePrefix("runtime.")
        .removePrefix("command.")
        .removePrefix("spawn_policy.")
        .replace("_", " ")
        .ifBlank { "—" }
}

@Composable
private fun localizedToolPackage(wireName: String): String = when (wireName) {
    "code_tools" -> stringResource(R.string.right_sidebar_tools_code)
    "browser_tools" -> stringResource(R.string.right_sidebar_tools_browser)
    "desktop_tools" -> stringResource(R.string.right_sidebar_tools_desktop)
    "memory_tools" -> stringResource(R.string.right_sidebar_tools_memory)
    "knowledge_tools" -> stringResource(R.string.right_sidebar_tools_knowledge)
    "task_tools" -> stringResource(R.string.right_sidebar_tools_task)
    "entity_tools" -> stringResource(R.string.right_sidebar_tools_entity)
    "map_tools" -> stringResource(R.string.right_sidebar_tools_map)
    "subagent_tools" -> stringResource(R.string.right_sidebar_tools_subagent)
    "delivery_tools" -> stringResource(R.string.right_sidebar_tools_delivery)
    "skill_tools" -> stringResource(R.string.right_sidebar_tools_skill)
    "remote_tools" -> stringResource(R.string.right_sidebar_tools_remote)
    "integration_tools" -> stringResource(R.string.right_sidebar_tools_integrations)
    else -> wireName.removeSuffix("_tools").replace("_", " ")
}

@Composable
private fun localizedChangeType(type: String): String = when (type) {
    "created" -> stringResource(R.string.right_sidebar_change_created)
    "deleted" -> stringResource(R.string.right_sidebar_change_deleted)
    "modified" -> stringResource(R.string.right_sidebar_change_modified)
    else -> type.ifBlank { "—" }
}

private fun isMarkdownFile(name: String, mediaType: String): Boolean =
    mediaType == "text/markdown" ||
        name.endsWith(".md", true) ||
        name.endsWith(".markdown", true)

private fun isTextFile(name: String): Boolean {
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return extension in setOf(
        "txt", "log", "json", "jsonl", "xml", "yaml", "yml", "toml", "ini",
        "kt", "kts", "java", "py", "js", "jsx", "ts", "tsx", "css", "scss",
        "sh", "zsh", "bash", "sql", "csv", "tsv", "rs", "go", "swift", "c",
        "h", "cpp", "hpp",
    )
}

private fun renderPdfFirstPage(file: File): Bitmap? = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            if (renderer.pageCount == 0) return@use null
            renderer.openPage(0).use { page ->
                val maxWidth = 1800
                val scale = (maxWidth.toFloat() / page.width).coerceAtMost(2f)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).roundToInt().coerceAtLeast(1),
                    (page.height * scale).roundToInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }
}.getOrNull()

private fun mapViewerHtml(data: JSONObject, dark: Boolean): String {
    val json = data.toString().replace("</", "<\\/")
    val tileStyle = if (dark) "dark_all" else "light_all"
    val background = if (dark) "#101116" else "#f6f7fb"
    val foreground = if (dark) "#ece8f1" else "#1f1d25"
    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
          <style>
            html,body,#map{height:100%;margin:0;background:$background;color:$foreground}
            .leaflet-container{font:14px system-ui;background:$background}
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script>
            const data=$json;
            const map=L.map('map',{zoomControl:true,attributionControl:false}).setView([35,105],4);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/$tileStyle/{z}/{x}/{y}{r}.png',{subdomains:'abcd'}).addTo(map);
            const byName={}; const bounds=[];
            const safe=(value)=>String(value||'').replace(/[&<>"']/g,(char)=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
            (data.pins||[]).forEach((pin)=>{
              const lat=Number(pin.lat), lng=Number(pin.lng);
              if(!Number.isFinite(lat)||!Number.isFinite(lng)) return;
              const point=[lat,lng]; bounds.push(point); byName[String(pin.name||'')]=point;
              const marker=L.circleMarker(point,{radius:8,color:'#7c5cff',weight:3,fillColor:'#a992ff',fillOpacity:.95}).addTo(map);
              marker.bindPopup('<b>'+safe(pin.name)+'</b>'+(pin.note?'<br>'+safe(pin.note):''));
            });
            (data.routes||[]).forEach((route)=>{
              const from=byName[String(route.from_name||route.from||'')];
              const to=byName[String(route.to_name||route.to||'')];
              if(!from||!to) return;
              const line=L.polyline([from,to],{color:'#2e9b62',weight:4,opacity:.85,dashArray:'7 7'}).addTo(map);
              const label=[route.transport,route.route_note].filter(Boolean).join(' · ');
              if(label) line.bindPopup(safe(label));
            });
            if(bounds.length) map.fitBounds(bounds,{padding:[28,28],maxZoom:12});
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun formatSidebarDate(raw: String): String {
    if (raw.isBlank()) return "—"
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

@Composable
private fun PairingScreen(
    state: MobileUiState,
    model: MainViewModel,
    onClose: (() -> Unit)? = null,
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("37841") }
    var key by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            if (onClose != null) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.padding(bottom = 20.dp),
                ) {
                    Text(stringResource(R.string.action_back))
                }
            }
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
private fun DeviceScreen(
    state: MobileUiState,
    model: MainViewModel,
    onAddDevice: () -> Unit,
) {
    val activePeer = state.peer
    var detailDeviceId by remember { mutableStateOf<String?>(null) }
    if (activePeer != null && detailDeviceId == activePeer.deviceId) {
        BackHandler { detailDeviceId = null }
        DeviceDetailScreen(state, model)
        return
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
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
            if (state.peers.isEmpty()) {
                item { EmptyCard(stringResource(R.string.devices_empty)) }
            }
            items(state.peers, key = { it.deviceId }) { peer ->
                val isActive = peer.deviceId == activePeer?.deviceId
                DataCard(
                    title = peer.name,
                    subtitle = if (!isActive) {
                        stringResource(R.string.device_trusted_inactive)
                    } else if (state.backgroundSyncing) {
                        stringResource(
                            R.string.device_sync_progress,
                            (state.backgroundSyncProgress * 100).toInt(),
                        )
                    } else if (state.busy) {
                        stringResource(R.string.device_syncing)
                    } else if (!state.desktopConnected) {
                        stringResource(
                            R.string.device_unavailable_projects,
                            state.projects.count { it.optString("id") != LOCAL_PROJECT_ID },
                        )
                    } else {
                        stringResource(
                            R.string.device_online_projects,
                            state.projects.count { it.optString("id") != LOCAL_PROJECT_ID },
                        )
                    },
                    trailing = stringResource(R.string.action_details),
                    progress = state.backgroundSyncProgress.takeIf {
                        isActive && state.backgroundSyncing
                    },
                    showIndeterminateProgress =
                        isActive && state.busy && !state.backgroundSyncing,
                ) {
                    if (!isActive) model.selectDevice(peer)
                    detailDeviceId = peer.deviceId
                }
            }
        }
        FloatingActionButton(
            onClick = onAddDevice,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            shape = CircleShape,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.device_add),
            )
        }
    }
}

@Composable
private fun DeviceDetailScreen(
    state: MobileUiState,
    model: MainViewModel,
) {
    val peer = state.peer ?: return
    var showAdvanced by remember(peer.deviceId) { mutableStateOf(false) }
    var confirmForget by remember { mutableStateOf(false) }
    var permissionDetails by remember(peer.deviceId) {
        mutableStateOf<DevicePermissionDetails?>(null)
    }
    val projectPermissions = peer.projectScopes.map { scope ->
        val projectName = state.projects
            .firstOrNull { it.optString("id") == scope }
            ?.optString("name")
            .orEmpty()
        if (projectName.isBlank()) scope else "$projectName\n$scope"
    }
    val context = LocalContext.current
    val chatPermissions = peer.capabilities.filter {
        it.startsWith("chat:") || it.startsWith("approval:")
    }.map { localizedCapabilityLabel(context, it) }
    val taskPermissions = peer.capabilities.filter {
        it.startsWith("task:") || it.startsWith("artifact:")
    }.map { localizedCapabilityLabel(context, it) }
    val toolPermissions = peer.capabilities
        .filter { it.startsWith("toolpack:") }
        .map { localizedCapabilityLabel(context, it) }
    val projectsTitle = stringResource(R.string.device_permissions_projects)
    val projectsDescription = stringResource(R.string.device_permissions_projects_desc)
    val chatTitle = stringResource(R.string.device_permissions_chat)
    val chatDescription = stringResource(R.string.device_permissions_chat_desc)
    val tasksTitle = stringResource(R.string.device_permissions_tasks)
    val tasksDescription = stringResource(R.string.device_permissions_tasks_desc)
    val toolsTitle = stringResource(R.string.device_permissions_tools)
    val toolsDescription = stringResource(R.string.device_permissions_tools_desc)
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
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
                        stringResource(
                            if (state.desktopConnected) {
                                R.string.device_connected
                            } else {
                                R.string.device_unavailable
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.desktopConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
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
                    projectsTitle,
                    projectsDescription,
                    peer.projectScopes.size,
                ) {
                    permissionDetails = DevicePermissionDetails(
                        title = projectsTitle,
                        description = projectsDescription,
                        items = projectPermissions,
                    )
                }
                PermissionRow(
                    chatTitle,
                    chatDescription,
                    chatPermissions.size,
                ) {
                    permissionDetails = DevicePermissionDetails(
                        title = chatTitle,
                        description = chatDescription,
                        items = chatPermissions,
                    )
                }
                PermissionRow(
                    tasksTitle,
                    tasksDescription,
                    taskPermissions.size,
                ) {
                    permissionDetails = DevicePermissionDetails(
                        title = tasksTitle,
                        description = tasksDescription,
                        items = taskPermissions,
                    )
                }
                PermissionRow(
                    toolsTitle,
                    toolsDescription,
                    toolPermissions.size,
                ) {
                    permissionDetails = DevicePermissionDetails(
                        title = toolsTitle,
                        description = toolsDescription,
                        items = toolPermissions,
                    )
                }
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
    permissionDetails?.let { details ->
        AlertDialog(
            onDismissRequest = { permissionDetails = null },
            title = { Text(details.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        details.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (details.items.isEmpty()) {
                        Text(
                            stringResource(R.string.device_permissions_details_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(details.items) { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = .55f,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    SelectionContainer {
                                        Text(
                                            item,
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { permissionDetails = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

private data class DevicePermissionDetails(
    val title: String,
    val description: String,
    val items: List<String>,
)

private fun localizedCapabilityLabel(context: Context, capability: String): String {
    val stringId = when (capability) {
        "projects:list_shared" -> R.string.device_capability_projects_list_shared
        "approval:clarification" -> R.string.device_capability_approval_clarification
        "approval:respond" -> R.string.device_capability_approval_respond
        "chat:create" -> R.string.device_capability_chat_create
        "chat:guide" -> R.string.device_capability_chat_guide
        "chat:interrupt" -> R.string.device_capability_chat_interrupt
        "chat:read" -> R.string.device_capability_chat_read
        "chat:send" -> R.string.device_capability_chat_send
        "task:create" -> R.string.device_capability_task_create
        "task:control" -> R.string.device_capability_task_control
        "task:dispatch" -> R.string.device_capability_task_dispatch
        "task:read" -> R.string.device_capability_task_read
        "artifact:read" -> R.string.device_capability_artifact_read
        "settings:read" -> R.string.device_capability_settings_read
        "settings:update" -> R.string.device_capability_settings_update
        "toolpack:browser_tools" -> R.string.device_toolpack_browser
        "toolpack:code_tools" -> R.string.device_toolpack_code
        "toolpack:delivery_tools" -> R.string.device_toolpack_delivery
        "toolpack:desktop_tools" -> R.string.device_toolpack_desktop
        "toolpack:entity_tools" -> R.string.device_toolpack_entity
        "toolpack:integration_tools" -> R.string.device_toolpack_integration
        "toolpack:knowledge_tools" -> R.string.device_toolpack_knowledge
        "toolpack:map_tools" -> R.string.device_toolpack_map
        "toolpack:memory_tools" -> R.string.device_toolpack_memory
        "toolpack:skill_tools" -> R.string.device_toolpack_skill
        "toolpack:subagent_tools" -> R.string.device_toolpack_subagent
        "toolpack:task_tools" -> R.string.device_toolpack_task
        else -> return context.getString(R.string.device_capability_unknown, capability)
    }
    return context.getString(stringId)
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
private fun PermissionRow(
    title: String,
    description: String,
    count: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
            Spacer(Modifier.width(8.dp))
            Text(
                "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var message by remember { mutableStateOf("") }
    val selectedProjectId = state.selectedProject?.optString("id").orEmpty()
    val selectedChatId = state.selectedChat?.optString("id").orEmpty()
    LaunchedEffect(selectedProjectId, selectedChatId) {
        if (selectedChatId.isBlank()) {
            message = ""
        } else if (state.creatingChat) {
            message = ""
        }
    }
    val project = state.selectedProject
    if (project == null) {
        CenterMessage(stringResource(R.string.project_select_first))
        return
    }
    if (state.selectedChat == null) {
        PendingChatDetail(
            message = message,
            onMessageChange = { message = it },
            projects = state.projects,
            selectedProject = project,
            onSelectProject = model::selectProject,
            permissionMode = state.permissionMode,
            onPermissionModeChange = model::setPermissionMode,
            busy = state.creatingChat,
            onModeMenuUnavailable = model::showLocalPermissionModeUnsupported,
            onSend = { attachments ->
                if (model.sendNewChatMessage(message, state.permissionMode, attachments)) {
                    message = ""
                    true
                } else {
                    false
                }
            },
        )
    } else {
        ChatDetail(
            state = state,
            model = model,
            message = message,
            onMessageChange = { message = it },
            permissionMode = state.permissionMode,
            onPermissionModeChange = model::setPermissionMode,
        )
    }
}

@Composable
private fun PendingChatDetail(
    message: String,
    onMessageChange: (String) -> Unit,
    projects: List<JSONObject>,
    selectedProject: JSONObject,
    onSelectProject: (JSONObject) -> Unit,
    permissionMode: PermissionMode,
    onPermissionModeChange: (PermissionMode) -> Unit,
    busy: Boolean,
    onModeMenuUnavailable: () -> Unit,
    onSend: (List<PendingAttachment>) -> Boolean,
) {
    val context = LocalContext.current
    val isLocal = selectedProject.optString("id") == LOCAL_PROJECT_ID
    var pendingAttachments by remember(selectedProject.optString("id")) {
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
    Box(Modifier.fillMaxSize()) {
        NewChatWelcome(
            projects = projects,
            selectedProject = selectedProject,
            enabled = !busy,
            onSelectProject = onSelectProject,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 28.dp, end = 28.dp, bottom = 124.dp),
        )
        ChatComposer(
            message = message,
            onMessageChange = onMessageChange,
            permissionMode = permissionMode,
            onPermissionModeChange = onPermissionModeChange,
            running = false,
            busy = busy,
            attachments = pendingAttachments,
            onAddAttachment = { attachmentPicker.launch(arrayOf("*/*")) },
            onRemoveAttachment = { removed ->
                pendingAttachments = pendingAttachments.filterNot { it.uri == removed.uri }
            },
            onSend = {
                if (onSend(pendingAttachments)) pendingAttachments = emptyList()
            },
            onInterrupt = {},
            modeMenuEnabled = !isLocal,
            onModeMenuUnavailable = onModeMenuUnavailable,
            placeholderRes = if (isLocal) {
                R.string.local_agent_composer
            } else {
                R.string.chat_composer_desktop
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun ChatDetail(
    state: MobileUiState,
    model: MainViewModel,
    message: String,
    onMessageChange: (String) -> Unit,
    permissionMode: PermissionMode,
    onPermissionModeChange: (PermissionMode) -> Unit,
) {
    val chat = state.selectedChat ?: return
    val isLocal = state.selectedProject?.optString("id") == LOCAL_PROJECT_ID
    val context = LocalContext.current
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
    val pendingQuestion = pendingApprovalQuestion(chat, state.runEvents)
    Column(Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        val liveTimeline = withoutDurableDuplicates(
            liveConversationTimeline(state.runEvents, state.activeRunId != null),
            messages,
        )
        val runError = state.runEvents.lastOrNull { it.optString("type") == "error" }
        val retryContent = if (messages == null) "" else
            (messages.length() - 1 downTo 0).firstNotNullOfOrNull { index ->
                messages.optJSONObject(index)
                    ?.takeIf { it.optString("role") == "user" }
                    ?.optString("content")
                    ?.let(::displayMessageContent)
                    ?.takeIf(String::isNotBlank)
            }.orEmpty()
        val transcriptCount = (messages?.length() ?: 0) + liveTimeline.size +
            (if (runError != null) 1 else 0)
        val liveReplyLength = liveTimeline.lastOrNull { it.optBoolean("liveReply") }
            ?.optString("content")?.length ?: 0
        LaunchedEffect(transcriptCount, liveReplyLength) {
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
                            state = state,
                            model = model,
                            onEdit = {
                                onMessageChange(displayMessageContent(item.optString("content")))
                            },
                            onRetry = {
                                val content = displayMessageContent(item.optString("content"))
                                if (content.isNotBlank() && !state.busy && state.activeRunId == null) {
                                    model.sendMessage(content, permissionMode)
                                }
                            },
                        )
                    }
                }
                items(liveTimeline.size, key = { liveTimeline[it].optString("id", "live-$it") }) { index ->
                    val item = liveTimeline[index]
                    ConversationMessage(
                        message = item,
                        state = state,
                        model = model,
                        onEdit = {},
                        onRetry = {},
                    )
                }
                if (runError != null) {
                    item {
                        RunErrorMessage(
                            message = runError.optString("message")
                                .ifBlank { stringResource(R.string.chat_run_failed) },
                            retryEnabled = retryContent.isNotBlank() &&
                                !state.busy && state.activeRunId == null,
                            onRetry = { model.sendMessage(retryContent, permissionMode) },
                        )
                    }
                }
            }
            if (transcriptCount == 0) {
                NewChatWelcome(
                    projects = state.projects,
                    selectedProject = state.selectedProject ?: JSONObject(),
                    enabled = !state.busy,
                    onSelectProject = model::selectProject,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 28.dp, end = 28.dp, bottom = 124.dp),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 14.dp)
            ) {
                if (pendingQuestion != null) {
                    ApprovalQuestionCard(
                        question = pendingQuestion,
                        busy = state.busy,
                        onAnswer = { model.answerChat(pendingQuestion.id, it) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ChatComposer(
                    message = message,
                    onMessageChange = onMessageChange,
                    permissionMode = permissionMode,
                    onPermissionModeChange = onPermissionModeChange,
                    running = state.activeRunId != null,
                    busy = state.busy || state.creatingChat,
                    attachments = pendingAttachments,
                    onAddAttachment = { attachmentPicker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { attachment ->
                        pendingAttachments = pendingAttachments - attachment
                    },
                    onSend = {
                        if (state.activeRunId == null) {
                            model.sendMessage(message, permissionMode, pendingAttachments)
                            onMessageChange("")
                            pendingAttachments = emptyList()
                        }
                        else if (!isLocal) {
                            model.guideRun(message)
                            onMessageChange("")
                        }
                    },
                    onInterrupt = model::interruptRun,
                    modeMenuEnabled = !isLocal,
                    onModeMenuUnavailable = model::showLocalPermissionModeUnsupported,
                    inputEnabled = !(isLocal && state.activeRunId != null),
                    interruptWhileRunning = isLocal,
                    placeholderRes = if (isLocal) {
                        R.string.local_agent_composer
                    } else {
                        R.string.chat_composer_desktop
                    },
                )
            }
        }
    }
}

@Composable
private fun ApprovalQuestionCard(
    question: ApprovalQuestion,
    busy: Boolean,
    onAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var customAnswer by remember(question.id) { mutableStateOf("") }
    val isPermission = isPermissionQuestion(question.kind)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = .45f),
        ),
    ) {
        Column(
            Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                question.title.ifBlank {
                    stringResource(
                        if (isPermission) R.string.approval_permission_title
                        else R.string.approval_confirmation_title,
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                question.prompt.ifBlank {
                    stringResource(R.string.approval_prompt_fallback)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            question.options.forEachIndexed { index, option ->
                if (index == 0) {
                    Button(
                        onClick = { onAnswer(option.label) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(option.label) }
                } else {
                    OutlinedButton(
                        onClick = { onAnswer(option.label) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(option.label) }
                }
            }
            if (question.allowCustom || question.options.isEmpty()) {
                OutlinedTextField(
                    value = customAnswer,
                    onValueChange = { customAnswer = it },
                    label = { Text(stringResource(R.string.approval_custom_answer)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    minLines = 1,
                    maxLines = 3,
                )
                Button(
                    onClick = {
                        onAnswer(customAnswer.trim())
                        customAnswer = ""
                    },
                    enabled = customAnswer.isNotBlank() && !busy,
                ) { Text(stringResource(R.string.chat_submit_answer)) }
            }
        }
    }
}

@Composable
private fun NewChatWelcome(
    projects: List<JSONObject>,
    selectedProject: JSONObject,
    enabled: Boolean,
    onSelectProject: (JSONObject) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
            shape = RoundedCornerShape(15.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.chat_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.chat_welcome_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(10.dp))
        CurrentProjectSelector(
            projects = projects,
            selectedProject = selectedProject,
            enabled = enabled,
            onSelect = onSelectProject,
        )
    }
}

@Composable
private fun CurrentProjectSelector(
    projects: List<JSONObject>,
    selectedProject: JSONObject,
    enabled: Boolean,
    onSelect: (JSONObject) -> Unit,
    showCurrentLabel: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedId = selectedProject.optString("id")
    val selectedName = selectedProject.optString("name")
        .ifBlank { stringResource(R.string.project_unnamed) }
    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (showCurrentLabel) {
                        stringResource(R.string.chat_current_project, selectedName)
                    } else {
                        selectedName
                    },
                    modifier = Modifier.widthIn(
                        max = if (showCurrentLabel) 220.dp else 120.dp,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.project_choose),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            projects.forEach { project ->
                val projectId = project.optString("id")
                DropdownMenuItem(
                    text = {
                        Text(
                            project.optString("name").ifBlank {
                                stringResource(R.string.project_unnamed)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = if (projectId == selectedId) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        if (projectId != selectedId) onSelect(project)
                    },
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
    permissionMode: PermissionMode,
    onPermissionModeChange: (PermissionMode) -> Unit,
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
    planModeEnabled: Boolean = true,
    interruptWhileRunning: Boolean = false,
    attachmentsEnabled: Boolean = true,
    onAttachmentsUnavailable: () -> Unit = {},
    onModeMenuUnavailable: () -> Unit = {},
    inputEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var modeMenuOpen by remember { mutableStateOf(false) }
    val submit = {
        when {
            running && (message.isBlank() || interruptWhileRunning) -> onInterrupt()
            (message.isNotBlank() || attachments.isNotEmpty()) && !busy -> onSend()
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
                enabled = !busy && inputEnabled,
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
                    onClick = {
                        if (attachmentsEnabled) onAddAttachment()
                        else onAttachmentsUnavailable()
                    },
                    enabled = !running && !busy,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = stringResource(R.string.chat_add_attachment),
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (attachmentsEnabled) 1f else .45f,
                        ),
                    )
                }
                Box {
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(enabled = !running) {
                                if (modeMenuEnabled) modeMenuOpen = true
                                else onModeMenuUnavailable()
                            },
                        color = if (permissionMode != PermissionMode.AUTO) {
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
                                tint = if (permissionMode != PermissionMode.AUTO) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(
                                    when (permissionMode) {
                                        PermissionMode.AUTO -> R.string.chat_mode_auto
                                        PermissionMode.DEFAULT -> R.string.chat_mode_default
                                        PermissionMode.PLAN -> R.string.chat_mode_plan
                                    },
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (permissionMode != PermissionMode.AUTO) MaterialTheme.colorScheme.primary
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
                                    Text(stringResource(R.string.chat_mode_auto))
                                    Text(
                                        stringResource(R.string.chat_mode_auto_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onPermissionModeChange(PermissionMode.AUTO)
                                modeMenuOpen = false
                            },
                            trailingIcon = if (permissionMode == PermissionMode.AUTO) {
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
                            } else null,
                        )
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
                                onPermissionModeChange(PermissionMode.DEFAULT)
                                modeMenuOpen = false
                            },
                            trailingIcon = if (permissionMode == PermissionMode.DEFAULT) {
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
                            } else null,
                        )
                        if (planModeEnabled) {
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
                                    onPermissionModeChange(PermissionMode.PLAN)
                                    modeMenuOpen = false
                                },
                                trailingIcon = if (permissionMode == PermissionMode.PLAN) {
                                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = submit,
                    enabled = (running || message.isNotBlank() || attachments.isNotEmpty()) && !busy,
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
        val projectId = state.selectedProject?.optString("id").orEmpty()
        var title by remember(projectId) { mutableStateOf("") }
        var goal by remember(projectId) { mutableStateOf("") }
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCard(
                    title = stringResource(R.string.task_create_title),
                    headerAction = {
                        CurrentProjectSelector(
                            projects = state.projects,
                            selectedProject = state.selectedProject,
                            enabled = !state.busy,
                            onSelect = model::selectProject,
                            showCurrentLabel = false,
                        )
                    },
                ) {
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
        val question = parseApprovalQuestion(task?.optJSONObject("pending_question"))
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
                        ApprovalQuestionCard(
                            question = question,
                            busy = state.busy,
                            onAnswer = { model.answerTask(question.id, it) },
                        )
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
                permissionMode = if (state.permissionMode == PermissionMode.AUTO) {
                    PermissionMode.AUTO
                } else {
                    PermissionMode.DEFAULT
                },
                onPermissionModeChange = model::setPermissionMode,
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
                planModeEnabled = false,
                interruptWhileRunning = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun TerminalScreen(state: MobileUiState, model: MainViewModel) {
    var input by remember { mutableStateOf("") }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var history by remember(state.selectedProject?.optString("id")) {
        mutableStateOf<List<String>>(emptyList())
    }
    var historyIndex by remember(state.selectedProject?.optString("id")) {
        mutableIntStateOf(0)
    }
    var historyDraft by remember(state.selectedProject?.optString("id")) {
        mutableStateOf("")
    }
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val terminalBackground = Color(0xFF10141D)
    val terminalChrome = Color(0xFF171C27)
    val terminalDivider = Color.White.copy(alpha = .12f)
    val terminalText = Color.White
    val terminalMuted = Color.White.copy(alpha = .72f)
    val terminalAccent = Color(0xFF73E2A7)
    val terminalDanger = Color(0xFFFF8A80)
    val terminalReady = state.terminalSessionStatus == "running"
    val projectName = state.selectedProject?.optString("name").orEmpty()
    val terminalProjects = state.projects.filterNot {
        it.optString("id") == LOCAL_PROJECT_ID
    }
    val promptTransformation = remember(state.terminalPrompt) {
        TerminalPromptVisualTransformation("${state.terminalPrompt} ")
    }
    fun focusTerminalInput() {
        if (!terminalReady) return
        focusRequester.requestFocus()
        scope.launch {
            delay(50)
            keyboardController?.show()
        }
    }
    LaunchedEffect(
        state.peer?.deviceId,
        state.selectedProject?.optString("id"),
    ) {
        model.ensureTerminalShell()
    }
    LaunchedEffect(state.terminalLines.size, input, terminalReady) {
        if (terminalReady) {
            listState.scrollToItem(state.terminalLines.size)
        }
    }
    LaunchedEffect(state.terminalSessionStatus) {
        if (terminalReady) {
            delay(120)
            focusTerminalInput()
        }
    }
    fun submit() {
        val command = input.trimEnd()
        if (
            command.isNotBlank() &&
            !state.terminalBusy &&
            terminalReady
        ) {
            model.sendTerminalCommand(command)
            history = (history.filterNot { it == command } + command).takeLast(100)
            historyIndex = history.size
            historyDraft = ""
            input = ""
        }
    }

    fun previousCommand() {
        if (history.isEmpty()) return
        if (historyIndex >= history.size) historyDraft = input
        historyIndex = (historyIndex - 1).coerceAtLeast(0)
        input = history[historyIndex]
        focusTerminalInput()
    }

    fun nextCommand() {
        if (history.isEmpty()) return
        if (historyIndex < history.lastIndex) {
            historyIndex += 1
            input = history[historyIndex]
        } else {
            historyIndex = history.size
            input = historyDraft
        }
        focusTerminalInput()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(terminalBackground)
            .imePadding()
            .clickable(
                enabled = terminalReady,
                onClick = ::focusTerminalInput,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(terminalChrome)
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        when (state.terminalSessionStatus) {
                            "running" -> terminalAccent
                            "connecting" -> Color(0xFFFFD166)
                            else -> terminalDanger
                        },
                        CircleShape,
                    )
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .clickable(
                            enabled = terminalProjects.isNotEmpty() && !state.terminalBusy,
                            onClick = { projectMenuExpanded = true },
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        projectName.ifBlank { stringResource(R.string.terminal_title) },
                        color = terminalText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.project_choose),
                        tint = terminalMuted,
                        modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(
                    expanded = projectMenuExpanded,
                    onDismissRequest = { projectMenuExpanded = false },
                ) {
                    terminalProjects.forEach { project ->
                        val projectId = project.optString("id")
                        val selected = projectId == state.selectedProject?.optString("id")
                        DropdownMenuItem(
                            text = {
                                Text(
                                    project.optString("name").ifBlank {
                                        stringResource(R.string.project_unnamed)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = if (selected) {
                                { Icon(Icons.Outlined.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            onClick = {
                                projectMenuExpanded = false
                                if (!selected) model.selectProject(project)
                            },
                        )
                    }
                }
            }
            if (state.terminalBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = terminalAccent,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(state.terminalLines.joinToString("\n")))
                },
                enabled = state.terminalLines.isNotEmpty(),
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.terminal_copy_output),
                    tint = terminalMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(
                onClick = model::clearTerminalOutput,
                enabled = state.terminalLines.isNotEmpty(),
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.terminal_clear_output),
                    tint = terminalMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = { model.ensureTerminalShell(force = true) },
                enabled = state.selectedProject != null && !state.terminalBusy,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.terminal_new_session),
                    tint = terminalMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        HorizontalDivider(color = terminalDivider)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (state.terminalSessionStatus == "connecting") {
                item {
                    Text(
                        stringResource(R.string.terminal_connecting),
                        color = terminalMuted,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(state.terminalLines) { line ->
                SelectionContainer {
                    Text(
                        line,
                        modifier = Modifier.horizontalScroll(horizontalScrollState),
                        color = terminalText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        softWrap = false,
                    )
                }
            }
            if (
                state.selectedProject == null ||
                (
                    !terminalReady &&
                        state.terminalSessionStatus != "connecting"
                    )
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
            if (terminalReady) {
                item(key = "terminal-input") {
                    BasicTextField(
                        value = input,
                        onValueChange = {
                            input = it.replace("\n", "")
                            if (historyIndex != history.size) {
                                historyIndex = history.size
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                val keyEvent = event.nativeKeyEvent
                                if (keyEvent.action != AndroidKeyEvent.ACTION_DOWN) {
                                    false
                                } else {
                                    when {
                                        keyEvent.isCtrlPressed &&
                                            keyEvent.keyCode == AndroidKeyEvent.KEYCODE_C -> {
                                            model.interruptTerminal()
                                            true
                                        }
                                        keyEvent.isCtrlPressed &&
                                            keyEvent.keyCode == AndroidKeyEvent.KEYCODE_L -> {
                                            model.clearTerminalOutput()
                                            true
                                        }
                                        keyEvent.isCtrlPressed &&
                                            keyEvent.keyCode == AndroidKeyEvent.KEYCODE_D -> {
                                            if (!state.terminalBusy) {
                                                model.sendTerminalCommand("exit")
                                            }
                                            true
                                        }
                                        keyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                            previousCommand()
                                            true
                                        }
                                        keyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                            nextCommand()
                                            true
                                        }
                                        keyEvent.keyCode == AndroidKeyEvent.KEYCODE_TAB -> {
                                            input += "\t"
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            },
                        singleLine = false,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = terminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Send,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        cursorBrush = SolidColor(terminalText),
                        visualTransformation = promptTransformation,
                    )
                }
            }
        }
        if (terminalReady) {
            HorizontalDivider(color = terminalDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(terminalChrome)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TerminalKeyButton("Ctrl-C", terminalDanger) {
                    model.interruptTerminal()
                    focusTerminalInput()
                }
                TerminalKeyButton("Ctrl-L", terminalMuted) {
                    model.clearTerminalOutput()
                    focusTerminalInput()
                }
                TerminalKeyButton("Tab", terminalMuted) {
                    input += "\t"
                    focusTerminalInput()
                }
                TerminalKeyButton(
                    label = "↑",
                    color = terminalMuted,
                    contentDescription = stringResource(R.string.terminal_previous_command),
                    onClick = ::previousCommand,
                )
                TerminalKeyButton(
                    label = "↓",
                    color = terminalMuted,
                    contentDescription = stringResource(R.string.terminal_next_command),
                    onClick = ::nextCommand,
                )
                TerminalKeyButton(
                    label = stringResource(R.string.terminal_paste),
                    color = terminalMuted,
                ) {
                    clipboard.getText()?.text?.let { input += it }
                    focusTerminalInput()
                }
                TerminalKeyButton("Ctrl-D", terminalMuted) {
                    if (!state.terminalBusy) {
                        model.sendTerminalCommand("exit")
                    }
                }
            }
        }
    }
}

private class TerminalPromptVisualTransformation(
    private val prompt: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = AnnotatedString(prompt + text.text)
        val promptLength = prompt.length
        return TransformedText(
            transformedText,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    (offset + promptLength).coerceAtMost(transformedText.length)

                override fun transformedToOriginal(offset: Int): Int =
                    (offset - promptLength).coerceIn(0, text.length)
            },
        )
    }
}

@Composable
private fun TerminalKeyButton(
    label: String,
    color: Color,
    contentDescription: String = label,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(34.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = .07f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = color,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun settingsPageTitle(pageId: String?, schema: JSONObject?): String {
    if (pageId == null) return stringResource(R.string.nav_settings)
    if (pageId == "mobile") return stringResource(R.string.settings_mobile_section)
    if (pageId == "about") return stringResource(R.string.settings_about_title)
    return jsonObjects(schema?.optJSONArray("sections"))
        .firstOrNull { it.optString("id") == pageId }
        ?.let { localizedSettingText(it, "label") }
        ?.takeIf(String::isNotBlank)
        ?: if (pageId == "models") {
            stringResource(R.string.settings_models_title)
        } else {
            stringResource(R.string.nav_settings)
        }
}

@Composable
private fun SettingsScreen(
    state: MobileUiState,
    model: MainViewModel,
    pageId: String?,
    onOpenPage: (String) -> Unit,
) {
    val context = LocalContext.current
    val desktop = state.desktopSettings
    val schema = state.desktopSettingsSchema
    var editingField by remember { mutableStateOf<JSONObject?>(null) }
    var editorValue by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf(false) }
    val currentVersion = remember(context) { installedVersionName(context) }
    LaunchedEffect(state.peer?.deviceId, pageId) {
        model.loadDesktopSettings()
        if (pageId == "models") {
            model.loadDesktopOpenAiOAuth()
        }
    }
    LaunchedEffect(state.desktopOpenAiOAuthAuthUrl) {
        val authUrl = state.desktopOpenAiOAuthAuthUrl
        if (authUrl.isNullOrBlank()) return@LaunchedEffect
        val uri = runCatching { Uri.parse(authUrl) }.getOrNull()
        if (uri?.let { parsed ->
                parsed.scheme in setOf("https", "http") && !parsed.host.isNullOrBlank()
            } == true) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
        model.clearDesktopOpenAiOAuthAuthUrl()
    }

    state.desktopOpenAiOAuthUserCode?.let { code ->
        AlertDialog(
            onDismissRequest = model::cancelMobileOpenAiOAuthLogin,
            title = { Text(stringResource(R.string.settings_model_oauth_login)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_model_oauth_device_instructions))
                    SelectionContainer {
                        Text(
                            code,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.settings_model_oauth_waiting))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = model::cancelMobileOpenAiOAuthLogin) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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
    val visibleSections = sections.filter { section ->
        val sectionId = section.optString("id")
        sectionId !in setOf("execution", "discussion", "models", "channels", "updates") && (
            sectionId == "skills" ||
                fields.any { it.optString("section") == sectionId }
            )
    }

    when (pageId) {
        null -> {
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SectionCard(stringResource(R.string.settings_mobile_section)) {
                        SettingsMenuRow(
                            title = stringResource(R.string.settings_mobile_menu_title),
                            subtitle = stringResource(R.string.settings_mobile_menu_description),
                            onClick = { onOpenPage("mobile") },
                        )
                        HorizontalDivider(Modifier.padding(vertical = 3.dp))
                        SettingsMenuRow(
                            title = stringResource(R.string.settings_update_title),
                            subtitle = stringResource(
                                R.string.settings_update_description,
                                currentVersion,
                            ),
                            onClick = { onOpenPage("about") },
                        )
                    }
                }
                item {
                    SectionCard(stringResource(R.string.settings_models_section)) {
                        SettingsMenuRow(
                            title = stringResource(R.string.settings_models_title),
                            subtitle = stringResource(R.string.settings_models_description),
                            onClick = { onOpenPage("models") },
                        )
                        }
                }
                item {
                    SectionCard(stringResource(R.string.settings_desktop_section)) {
                        if (desktop == null) {
                            Text(
                                stringResource(R.string.settings_desktop_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = model::loadDesktopSettings) {
                                Text(stringResource(R.string.action_refresh))
                            }
                        } else {
                            visibleSections.forEachIndexed { index, section ->
                                val sectionId = section.optString("id")
                                SettingsMenuRow(
                                    title = localizedSettingText(section, "label"),
                                    subtitle = settingsSectionDescription(sectionId),
                                    onClick = { onOpenPage(sectionId) },
                                )
                                if (index < visibleSections.lastIndex) {
                                    HorizontalDivider(Modifier.padding(vertical = 3.dp))
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.settings_security_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        "mobile" -> {
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SectionCard(stringResource(R.string.settings_appearance)) {
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
                    }
                }
                item {
                    SectionCard(stringResource(R.string.settings_language)) {
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
                    SectionCard(stringResource(R.string.settings_permission_mode)) {
                        ChoiceRow(
                            stringResource(R.string.chat_mode_auto),
                            state.permissionMode == PermissionMode.AUTO,
                        ) { model.setPermissionMode(PermissionMode.AUTO) }
                        ChoiceRow(
                            stringResource(R.string.chat_mode_default),
                            state.permissionMode == PermissionMode.DEFAULT,
                        ) { model.setPermissionMode(PermissionMode.DEFAULT) }
                        ChoiceRow(
                            stringResource(R.string.chat_mode_plan),
                            state.permissionMode == PermissionMode.PLAN,
                        ) { model.setPermissionMode(PermissionMode.PLAN) }
                        Text(
                            stringResource(R.string.settings_permission_mode_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        "about" -> AboutUpdateScreen(currentVersion)
        "models" -> {
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ModelSettingsCard(
                            models = state.desktopModels ?: JSONObject()
                                .put("source", "custom")
                                .put("custom_models", JSONArray())
                                .put("vision_models", JSONArray()),
                            busy = state.busy,
                            oauth = state.desktopOpenAiOAuth,
                            oauthBusy = state.desktopOpenAiOAuthLoading,
                            model = model,
                            showTitle = false,
                        )
                }
            }
        }
        else -> {
            val sectionFields = fields.filter { it.optString("section") == pageId }
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SectionCard(null) {
                        if (desktop == null) {
                            Text(
                                stringResource(R.string.settings_desktop_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = model::loadDesktopSettings) {
                                Text(stringResource(R.string.action_refresh))
                            }
                        } else if (sectionFields.isEmpty() && pageId == "skills") {
                            Text(
                                stringResource(R.string.settings_no_installed_skills),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = model::loadDesktopSettings) {
                                Text(stringResource(R.string.action_refresh))
                            }
                        } else if (sectionFields.isEmpty()) {
                            Text(
                                stringResource(R.string.settings_desktop_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
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
                item {
                    Text(
                        stringResource(R.string.settings_security_note),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutUpdateScreen(currentVersion: String) {
    val runtimePackageName = "ai.cyrene.mobile.runtime"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var checkFailed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }
    var downloadedMainApk by remember { mutableStateOf<File?>(null) }
    var downloadedRuntimeApk by remember { mutableStateOf<File?>(null) }
    var pendingMainApk by remember { mutableStateOf<File?>(null) }
    var pendingRuntimeApk by remember { mutableStateOf<File?>(null) }
    var actionError by remember { mutableStateOf<Int?>(null) }

    val mainInstallerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        pendingMainApk = null
        pendingRuntimeApk = null
    }

    val runtimeInstallerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val runtimeApk = pendingRuntimeApk
        val mainApk = pendingMainApk
        if (runtimeApk != null && isApkVersionInstalled(context, runtimeApk, runtimePackageName)) {
            pendingRuntimeApk = null
            if (mainApk != null) {
                runCatching { mainInstallerLauncher.launch(apkInstallerIntent(context, mainApk)) }
                    .onFailure { actionError = R.string.settings_update_install_failed }
            }
        } else if (runtimeApk != null) {
            pendingMainApk = null
            pendingRuntimeApk = null
            actionError = R.string.settings_update_runtime_install_cancelled
        }
    }

    val launchPendingInstallation: () -> Unit = {
        val mainApk = pendingMainApk
        val runtimeApk = pendingRuntimeApk
        when {
            mainApk == null -> Unit
            runtimeApk != null && !isApkVersionInstalled(context, runtimeApk, runtimePackageName) ->
                runCatching { runtimeInstallerLauncher.launch(apkInstallerIntent(context, runtimeApk)) }
                    .onFailure { actionError = R.string.settings_update_install_failed }
            else -> runCatching { mainInstallerLauncher.launch(apkInstallerIntent(context, mainApk)) }
                .onFailure { actionError = R.string.settings_update_install_failed }
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (canInstallPackages(context)) {
            launchPendingInstallation()
        } else if (pendingMainApk != null) {
            actionError = R.string.settings_update_install_permission
        }
    }

    val requestInstallation: (File, File?) -> Unit = { mainApk, runtimeApk ->
        actionError = null
        when {
            !isExpectedApk(context, mainApk, context.packageName) ||
                (runtimeApk != null && !isExpectedApk(context, runtimeApk, runtimePackageName)) -> {
                actionError = R.string.settings_update_invalid_apk
            }
            else -> {
                pendingMainApk = mainApk
                pendingRuntimeApk = runtimeApk
                if (canInstallPackages(context)) {
                    if (runtimeApk != null &&
                        !isApkVersionInstalled(context, runtimeApk, runtimePackageName)
                    ) {
                        runCatching {
                            runtimeInstallerLauncher.launch(apkInstallerIntent(context, runtimeApk))
                        }.onFailure { actionError = R.string.settings_update_install_failed }
                    } else {
                        runCatching {
                            mainInstallerLauncher.launch(apkInstallerIntent(context, mainApk))
                        }.onFailure { actionError = R.string.settings_update_install_failed }
                    }
                } else {
                    actionError = R.string.settings_update_install_permission
                    val permissionIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { installPermissionLauncher.launch(permissionIntent) }
                        .onFailure { actionError = R.string.settings_update_install_failed }
                }
            }
        }
    }

    val availableRelease = (result as? UpdateCheckResult.UpdateAvailable)?.release
    val latestVersion = when (val currentResult = result) {
        is UpdateCheckResult.UpdateAvailable -> currentResult.release.version
        is UpdateCheckResult.UpToDate -> currentResult.latestVersion
        else -> currentVersion
    }
    val progress = if (totalBytes > 0L) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    val statusLabel = when {
        checking -> stringResource(R.string.settings_update_checking)
        downloading -> progress?.let {
            stringResource(R.string.settings_update_downloading, (it * 100).roundToInt())
        } ?: stringResource(R.string.settings_update_downloading_unknown)
        downloadedMainApk != null -> stringResource(
            if (downloadedRuntimeApk != null) R.string.settings_update_both_downloaded
            else R.string.settings_update_downloaded,
        )
        checkFailed -> stringResource(R.string.settings_update_failed)
        result is UpdateCheckResult.UpdateAvailable ->
            stringResource(R.string.settings_update_available)
        result is UpdateCheckResult.UpToDate -> stringResource(R.string.settings_update_latest)
        result is UpdateCheckResult.NoReleases ->
            stringResource(R.string.settings_update_no_releases)
        else -> "—"
    }

    val startCheck: () -> Unit = {
        if (!checking && !downloading) {
            checking = true
            checkFailed = false
            actionError = null
            result = null
            downloadedMainApk = null
            downloadedRuntimeApk = null
            scope.launch {
                runCatching { GithubUpdateService.check(currentVersion) }
                    .onSuccess { result = it }
                    .onFailure { checkFailed = true }
                checking = false
            }
        }
    }

    val primaryAction: () -> Unit = {
        when {
            downloadedMainApk != null -> requestInstallation(
                requireNotNull(downloadedMainApk),
                downloadedRuntimeApk,
            )
            availableRelease?.apkUrl != null -> {
                downloading = true
                downloadedBytes = 0L
                totalBytes = 0L
                actionError = null
                scope.launch {
                    runCatching {
                        val assets = listOfNotNull(
                            availableRelease.runtimeApk,
                            availableRelease.mainApk,
                        )
                        var runtimeFile: File? = null
                        var mainFile: File? = null
                        assets.forEachIndexed { index, asset ->
                            val file = ApkUpdateDownloader.download(
                                context,
                                availableRelease,
                                asset,
                            ) { downloadProgress ->
                                if (downloadProgress.totalBytes > 0L) {
                                    val assetProgress = downloadProgress.downloadedBytes.toDouble() /
                                        downloadProgress.totalBytes.toDouble()
                                    downloadedBytes = ((index + assetProgress) * 1_000_000L).toLong()
                                    totalBytes = assets.size * 1_000_000L
                                } else {
                                    downloadedBytes = 0L
                                    totalBytes = 0L
                                }
                            }
                            if (asset == availableRelease.runtimeApk) runtimeFile = file
                            if (asset == availableRelease.mainApk) mainFile = file
                        }
                        requireNotNull(mainFile) to runtimeFile
                    }.onSuccess { (mainApk, runtimeApk) ->
                        downloadedMainApk = mainApk
                        downloadedRuntimeApk = runtimeApk
                        downloading = false
                        requestInstallation(mainApk, runtimeApk)
                    }.onFailure {
                        downloading = false
                        actionError = R.string.settings_update_download_failed
                    }
                }
            }
            availableRelease != null -> {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(availableRelease.releaseUrl)),
                    )
                }.onFailure {
                    actionError = R.string.settings_update_install_failed
                }
            }
            else -> startCheck()
        }
    }

    val primaryLabel = when {
        downloadedMainApk != null -> stringResource(
            if (downloadedRuntimeApk != null) R.string.settings_update_install_both
            else R.string.settings_update_install,
        )
        checking -> stringResource(R.string.settings_update_checking)
        downloading -> progress?.let {
            stringResource(R.string.settings_update_downloading, (it * 100).roundToInt())
        } ?: stringResource(R.string.settings_update_downloading_unknown)
        availableRelease?.apkUrl != null -> stringResource(
            if (availableRelease.runtimeApk != null) R.string.settings_update_download_both_version
            else R.string.settings_update_download_version,
            availableRelease.version,
        )
        availableRelease != null -> stringResource(R.string.settings_update_open_release)
        else -> stringResource(R.string.settings_update_check_action)
    }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_full),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Text(
                                    "Cyrene",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        currentVersion,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.settings_about_product_copy),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Button(
                        onClick = primaryAction,
                        enabled = !checking && !downloading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(primaryLabel)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.settings_update_settings),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                        ) {
                            Text(
                                statusLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth()) {
                        AboutVersionValue(
                            stringResource(R.string.settings_update_current_version),
                            currentVersion,
                            Modifier.weight(1f),
                        )
                        AboutVersionValue(
                            stringResource(R.string.settings_update_latest_version),
                            latestVersion,
                            Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        AboutVersionValue(
                            stringResource(R.string.settings_update_channel),
                            stringResource(R.string.settings_update_channel_github),
                            Modifier.weight(1f),
                        )
                        AboutVersionValue(
                            stringResource(R.string.settings_update_published),
                            availableRelease?.publishedAt?.substringBefore('T')
                                ?.takeIf(String::isNotBlank) ?: "—",
                            Modifier.weight(1f),
                        )
                    }
                    if (downloading) {
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            )
                        }
                    }
                    when {
                        actionError != null -> Text(
                            stringResource(requireNotNull(actionError)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        checkFailed -> Text(
                            stringResource(R.string.settings_update_failed_detail),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        result is UpdateCheckResult.NoReleases -> Text(
                            stringResource(R.string.settings_update_no_releases_detail),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        availableRelease != null && availableRelease.apkUrl == null -> Text(
                            stringResource(R.string.settings_update_no_apk),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    availableRelease?.notes
                        ?.takeIf(String::isNotBlank)
                        ?.let { notes ->
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(
                                    stringResource(R.string.settings_update_release_notes),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Box(Modifier.fillMaxWidth().padding(13.dp)) {
                                        MarkdownMessage(notes)
                                    }
                                }
                            }
                        }
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.settings_update_related_links)) {
                SettingsMenuRow(
                    title = stringResource(R.string.settings_update_github_repository),
                    subtitle = stringResource(R.string.settings_update_github_repository_hint),
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Yongchu-Yitao/Cyrene-mobile"),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutVersionValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun canInstallPackages(context: Context): Boolean =
    context.packageManager.canRequestPackageInstalls()

@Suppress("DEPRECATION")
private fun isExpectedApk(context: Context, apk: File, expectedPackageName: String): Boolean {
    val archiveInfo = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
    return apk.isFile && apk.length() > 0L && archiveInfo?.packageName == expectedPackageName
}

@Suppress("DEPRECATION")
private fun isApkVersionInstalled(context: Context, apk: File, packageName: String): Boolean {
    val archiveInfo = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0) ?: return false
    if (archiveInfo.packageName != packageName) return false
    val installedInfo = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            context.packageManager.getPackageInfo(packageName, 0)
        }
    }.getOrNull() ?: return false
    return installedInfo.longVersionCode >= archiveInfo.longVersionCode
}

private fun apkInstallerIntent(context: Context, apk: File): Intent {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.update-files",
        apk,
    )
    return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_RETURN_RESULT, true)
    }
}

@Suppress("DEPRECATION")
private fun installedVersionName(context: Context): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return packageInfo.versionName.orEmpty().ifBlank { "—" }
}

@Composable
private fun settingsSectionDescription(sectionId: String): String = stringResource(
    when (sectionId) {
        "general" -> R.string.settings_section_general_description
        "agent" -> R.string.settings_section_agent_description
        "context" -> R.string.settings_section_context_description
        "models" -> R.string.settings_section_models_description
        "skills" -> R.string.settings_section_skills_description
        "channels" -> R.string.settings_section_channels_description
        "updates" -> R.string.settings_section_updates_description
        "budget" -> R.string.settings_section_budget_description
        "tool_packs" -> R.string.settings_section_tool_packs_description
        else -> R.string.settings_section_default_description
    },
)

@Composable
private fun SettingsMenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "›",
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ModelEditTarget(val role: String, val index: Int)

private fun oauthModelId(candidate: JSONObject): String = candidate.optString("model")
    .ifBlank { candidate.optString("id") }
    .ifBlank { candidate.optString("slug") }
    .trim()

private fun oauthModelReasoning(candidate: JSONObject?): String = oauthReasoningEffort(
    candidate?.opt("defaultReasoningEffort")
        ?: candidate?.opt("default_reasoning_effort"),
)

private fun oauthReasoningEffort(value: Any?): String = when (value) {
    is JSONObject -> value.optString("reasoningEffort")
        .ifBlank { value.optString("reasoning_effort") }
        .ifBlank { value.optString("value") }
        .ifBlank { value.optString("id") }
    null, JSONObject.NULL -> ""
    else -> value.toString()
}.trim()

@Composable
private fun ModelSettingsCard(
    models: JSONObject,
    busy: Boolean,
    oauth: JSONObject?,
    oauthBusy: Boolean,
    model: MainViewModel,
    showTitle: Boolean = true,
) {
    val customModels = models.optJSONArray("custom_models") ?: JSONArray()
    val visionModels = models.optJSONArray("vision_models") ?: JSONArray()
    val codexModel = models.optJSONObject("codex_model")
    val secondaryModel = models.optJSONObject("secondary_model")
    val oauthModels = jsonObjects(oauth?.optJSONArray("models"))
    val oauthConnected = oauth?.optBoolean("connected") == true
    val oauthAvailable = oauth?.optBoolean("available", true) != false
    val oauthAccountLabel = oauth?.optJSONObject("account")?.let { account ->
        account.optString("email")
            .ifBlank { account.optString("planType") }
            .ifBlank { account.optString("plan_type") }
    }.orEmpty()
    val selectedSavedOAuthModel = codexModel?.optString("model").orEmpty()
    val selectedOAuthModel = oauthModels.firstOrNull {
        oauthModelId(it) == selectedSavedOAuthModel
    } ?: oauthModels.firstOrNull {
        it.optBoolean("isDefault") || it.optBoolean("is_default")
    } ?: oauthModels.firstOrNull()
    var oauthSelection by remember(models.toString(), oauth?.toString()) {
        mutableStateOf(selectedSavedOAuthModel.ifBlank { selectedOAuthModel?.let(::oauthModelId).orEmpty() })
    }
    var oauthReasoning by remember(models.toString(), oauth?.toString()) {
        mutableStateOf(
            codexModel?.optString("reasoning_effort").orEmpty().ifBlank {
                oauthModelReasoning(selectedOAuthModel)
            },
        )
    }
    var selectedSource by remember(models.toString()) {
        mutableStateOf(
            if (models.optString("source", "custom") == "codex") "codex" else "custom",
        )
    }
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
                        model.updateLocalModels(next)
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
                                model.updateLocalModels(next)
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

    SectionCard(
        if (showTitle) stringResource(R.string.settings_models_title) else null,
    ) {
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
            selectedSource == "custom",
            enabled = !busy && !oauthBusy,
        ) {
            selectedSource = "custom"
            model.updateLocalModels(
                JSONObject(models.toString()).put("source", "custom"),
            )
        }
        ChoiceRow(
            stringResource(R.string.settings_model_openai_oauth_title),
            selectedSource == "codex",
            enabled = !busy && !oauthBusy,
        ) {
            selectedSource = "codex"
            model.updateLocalModels(
                JSONObject(models.toString()).put("source", "codex"),
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        if (selectedSource == "codex") {
            Text(
                stringResource(R.string.settings_model_openai_oauth_title),
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (oauthConnected && oauthAccountLabel.isNotBlank()) {
                    oauthAccountLabel
                } else if (oauthConnected) {
                    stringResource(R.string.settings_model_oauth_connected)
                } else {
                    stringResource(R.string.settings_model_oauth_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!oauthConnected) {
            OutlinedButton(
                onClick = model::startDesktopOpenAiOAuthLogin,
                enabled = !busy && !oauthBusy && oauthAvailable,
            ) {
                if (oauthBusy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.settings_model_oauth_login))
                }
            }
            oauth?.optString("error")?.takeIf(String::isNotBlank)?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            } else if (oauthModels.isNotEmpty()) {
            val selected = oauthModels.firstOrNull { oauthModelId(it) == oauthSelection }
            val effortOptions = buildList {
                selected?.optJSONArray("supportedReasoningEfforts")?.let { values ->
                    (0 until values.length()).forEach { index ->
                        oauthReasoningEffort(values.opt(index))
                            .takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                selected?.optJSONArray("supported_reasoning_efforts")?.let { values ->
                    (0 until values.length()).forEach { index ->
                        oauthReasoningEffort(values.opt(index))
                            .takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                oauthReasoning.takeIf(String::isNotBlank)?.let(::add)
                selected?.let(::oauthModelReasoning)?.takeIf(String::isNotBlank)?.let(::add)
            }.distinct()
            val effectiveOAuthReasoning = oauthReasoning.ifBlank { effortOptions.firstOrNull().orEmpty() }
            if (selected != null) {
                Text(
                    stringResource(R.string.settings_model_oauth_model),
                    style = MaterialTheme.typography.labelMedium,
                )
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = selected.optString("displayName")
                            .ifBlank { selected.optString("display_name") }
                            .ifBlank { oauthSelection },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy && !oauthBusy) { menuExpanded = true },
                        trailingIcon = { Text("▾") },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        oauthModels.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.optString("displayName")
                                            .ifBlank { option.optString("display_name") }
                                            .ifBlank { oauthModelId(option) },
                                    )
                                },
                                onClick = {
                                    oauthSelection = oauthModelId(option)
                                    oauthReasoning = oauthModelReasoning(option)
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (effortOptions.isNotEmpty()) {
                    Text(
                        stringResource(R.string.settings_model_reasoning),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        effortOptions.forEach { effort ->
                            FilterChip(
                                selected = effectiveOAuthReasoning == effort,
                                onClick = { oauthReasoning = effort },
                                label = { Text(effort) },
                                enabled = !busy && !oauthBusy,
                            )
                            }
                    }
                }
                Button(
                    onClick = {
                        val candidate = JSONObject()
                            .put("id", "codex-$oauthSelection")
                            .put("name", oauthSelection)
                            .put("model", oauthSelection)
                            .put("provider", "codex_oauth")
                            .put("base_url", "codex://oauth")
                            .put("reasoning_effort", effectiveOAuthReasoning)
                            .put("description", "OpenAI OAuth")
                        model.updateLocalModels(
                            JSONObject(models.toString())
                                .put("codex_model", candidate)
                                .put("source", "codex"),
                        )
                    },
                    enabled = oauthSelection.isNotBlank() && !busy && !oauthBusy,
                ) { Text(stringResource(R.string.settings_model_oauth_use)) }
            }
            }
            if (oauthConnected) {
                OutlinedButton(
                    onClick = model::logoutDesktopOpenAiOAuth,
                    enabled = !busy && !oauthBusy,
                ) { Text(stringResource(R.string.settings_model_oauth_logout)) }
            }
        } else {
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
private fun SectionCard(
    title: String?,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        modifier = if (headerAction != null) Modifier.weight(1f) else Modifier,
                        fontWeight = FontWeight.SemiBold,
                    )
                    headerAction?.invoke()
                }
            }
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
    state: MobileUiState,
    model: MainViewModel,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
) {
    val role = message.optString("role")
    val content = displayMessageContent(message.optString("content"))
    val timestamp = formatChatTime(
        message.optString("createdAt").ifBlank { message.optString("created_at") }
    )
    if (role == "user") {
        UserMessage(
            content = content,
            attachments = message.optJSONArray("attachments"),
            state = state,
            model = model,
            timestamp = timestamp,
            deliveryState = message.optString("deliveryState"),
            onEdit = onEdit,
            onRetry = onRetry,
        )
        return
    }
    val trace = message.optJSONArray("trace")
    val isActivity = message.optBoolean("activityCard")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if ((trace != null && trace.length() > 0) || isActivity) {
            ExecutionCard(
                entries = List(trace?.length() ?: 0) { trace?.optJSONObject(it) ?: JSONObject() },
                running = message.optBoolean("runtimeActivityActive"),
                reasoning = message.optString("reasoning"),
                provider = message.optString("provider"),
            )
        }
        if (content.isNotBlank()) {
            AssistantMessage(
                content = content,
                timestamp = timestamp,
                running = false,
                attachments = message.optJSONArray("attachments"),
                state = state,
                model = model,
            )
        } else if ((message.optJSONArray("attachments")?.length() ?: 0) > 0) {
            AssistantMessage(
                content = "",
                timestamp = timestamp,
                running = false,
                attachments = message.optJSONArray("attachments"),
                state = state,
                model = model,
            )
        }
    }
}

@Composable
private fun UserMessage(
    content: String,
    attachments: JSONArray?,
    state: MobileUiState,
    model: MainViewModel,
    timestamp: String,
    deliveryState: String,
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
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(13.dp, 13.dp, 4.dp, 13.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.secondary.copy(alpha = .55f),
                ),
                modifier = Modifier.widthIn(max = 310.dp),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                    if (content.isNotBlank()) {
                        Text(
                            content,
                            modifier = Modifier.padding(horizontal = 3.dp),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    InlineMessageAttachments(
                        attachments = attachments,
                        state = state,
                        model = model,
                        modifier = Modifier.padding(top = if (content.isBlank()) 0.dp else 8.dp),
                    )
                }
            }
        }
        when (deliveryState) {
            "sending" -> Text(
                stringResource(R.string.chat_message_sending),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            "failed" -> Text(
                stringResource(R.string.chat_message_send_failed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
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
private fun RunErrorMessage(
    message: String,
    retryEnabled: Boolean,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(13.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.chat_run_failed),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(message, fontSize = 14.sp, lineHeight = 21.sp)
            OutlinedButton(
                onClick = onRetry,
                enabled = retryEnabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.chat_retry_message))
            }
        }
    }
}

@Composable
private fun AssistantMessage(
    content: String,
    timestamp: String,
    running: Boolean,
    attachments: JSONArray? = null,
    state: MobileUiState? = null,
    model: MainViewModel? = null,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (content.isNotBlank()) {
            MarkdownMessage(content)
        }
        if (state != null && model != null) {
            InlineMessageAttachments(
                attachments = attachments,
                state = state,
                model = model,
            )
        }
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
private fun InlineMessageAttachments(
    attachments: JSONArray?,
    state: MobileUiState,
    model: MainViewModel,
    modifier: Modifier = Modifier,
) {
    if (attachments == null || attachments.length() == 0) return
    val files = remember(attachments.toString()) {
        (0 until attachments.length()).mapNotNull(attachments::optJSONObject)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        files.forEach { file ->
            if (isImageAttachment(file)) {
                val attachmentKey =
                    "${state.selectedChat?.optString("id").orEmpty()}::${file.optString("id")}"
                InlineChatImage(
                    file = file,
                    thumbnailPath = state.inlineAttachmentPaths[attachmentKey],
                    thumbnailLoading =
                        attachmentKey in state.inlineAttachmentLoading,
                    thumbnailFailed =
                        attachmentKey in state.inlineAttachmentErrors,
                    fullImagePath = state.fullImagePaths[attachmentKey],
                    fullImageLoading = attachmentKey in state.fullImageLoading,
                    fullImageFailed = attachmentKey in state.fullImageErrors,
                    onLoadThumbnail = { model.loadInlineChatImage(file) },
                    onLoadFullImage = { model.loadFullChatImage(file) },
                )
            } else {
                InlineFileAttachment(file = file, onOpen = { model.previewChatAttachment(file) })
            }
        }
    }
}

@Composable
private fun InlineChatImage(
    file: JSONObject,
    thumbnailPath: String?,
    thumbnailLoading: Boolean,
    thumbnailFailed: Boolean,
    fullImagePath: String?,
    fullImageLoading: Boolean,
    fullImageFailed: Boolean,
    onLoadThumbnail: () -> Unit,
    onLoadFullImage: () -> Unit,
) {
    val name = file.optString("name", stringResource(R.string.chat_image_attachment))
    var thumbnailBitmap by remember(thumbnailPath) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    var fullImageBitmap by remember(fullImagePath) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    var previewOpen by remember { mutableStateOf(false) }
    LaunchedEffect(file.optString("id"), thumbnailPath) {
        if (thumbnailPath == null) {
            onLoadThumbnail()
        } else {
            thumbnailBitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(thumbnailPath, maxDimension = 1100)?.asImageBitmap()
            }
        }
    }
    LaunchedEffect(previewOpen, fullImagePath) {
        if (previewOpen) {
            if (fullImagePath == null) {
                onLoadFullImage()
            } else {
                fullImageBitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(fullImagePath, maxDimension = 2600)?.asImageBitmap()
                }
            }
        }
    }
    val width = file.optInt("width")
    val height = file.optInt("height")
    val ratio = if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(.65f, 1.8f)
    } else {
        1f
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = thumbnailBitmap != null) { previewOpen = true },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f),
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val loadedBitmap = thumbnailBitmap
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else if (thumbnailFailed) {
                OutlinedButton(onClick = onLoadThumbnail) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.chat_image_retry))
                }
            } else if (thumbnailLoading || thumbnailPath == null) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            } else {
                Text(
                    stringResource(R.string.chat_image_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (previewOpen && thumbnailBitmap != null) {
        Dialog(
            onDismissRequest = { previewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = Color.Black.copy(alpha = .96f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val previewBitmap = fullImageBitmap ?: requireNotNull(thumbnailBitmap)
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                    if (fullImageLoading || (fullImagePath != null && fullImageBitmap == null)) {
                        Surface(
                            color = Color.Black.copy(alpha = .62f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    stringResource(R.string.chat_image_loading_original),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else if (fullImageFailed) {
                        OutlinedButton(
                            onClick = onLoadFullImage,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.chat_image_retry_original))
                        }
                    }
                    IconButton(
                        onClick = { previewOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = .55f), CircleShape),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineFileAttachment(file: JSONObject, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                file.optString("name", stringResource(R.string.right_sidebar_unknown_file_type)),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun isImageAttachment(file: JSONObject): Boolean {
    val mediaType = file.optString("content_type")
        .ifBlank { file.optString("media_type") }
        .substringBefore(';')
        .trim()
        .lowercase()
    val name = file.optString("name").lowercase()
    return file.optString("kind").equals("image", ignoreCase = true) ||
        mediaType.startsWith("image/") ||
        name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
        name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp")
}

private fun decodeSampledBitmap(path: String, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (
        bounds.outWidth / (sampleSize * 2) >= maxDimension ||
        bounds.outHeight / (sampleSize * 2) >= maxDimension
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

@Composable
private fun MarkdownMessage(content: String) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val tableCellPadding = with(density) { 8.dp.roundToPx() }
    val tableBorderWidth = with(density) { 1.dp.roundToPx() }
    val tableBorderColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val tableHeaderColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val tableOddRowColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f).toArgb()
    val markwon = remember(
        context,
        tableCellPadding,
        tableBorderWidth,
        tableBorderColor,
        tableHeaderColor,
        tableOddRowColor,
    ) {
        val tableTheme = TableTheme.buildWithDefaults(context)
            .tableCellPadding(tableCellPadding)
            .tableBorderWidth(tableBorderWidth)
            .tableBorderColor(tableBorderColor)
            .tableHeaderRowBackgroundColor(tableHeaderColor)
            .tableOddRowBackgroundColor(tableOddRowColor)
            .build()
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(tableTheme))
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            .build()
    }
    AndroidView(
        factory = {
            TextView(it).apply {
                setTextIsSelectable(true)
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
private fun ExecutionCard(
    entries: List<JSONObject>,
    running: Boolean,
    reasoning: String = "",
    provider: String = "",
) {
    val normalized = normalizeTraceEntries(entries)
    val canShowReasoning = reasoning.isNotBlank() && provider != "codex_oauth"
    val canExpand = normalized.isNotEmpty() || canShowReasoning
    // Local sessions refresh while a run is observed. The card remains at the
    // same composition position, so changing trace snapshots must not collapse
    // a user-opened card on every poll.
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canExpand) Modifier.clickable { expanded = !expanded }
                else Modifier,
            ),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .72f),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = .52f),
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.chat_execution),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (expanded && canShowReasoning) {
                Text(
                    reasoning,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .82f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (normalized.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            stringResource(
                                if (running) R.string.chat_thinking else R.string.chat_execution_complete
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
                            fontSize = 14.sp,
                        )
                    }
            } else {
                val visibleEntries = if (expanded) normalized else normalized.takeLast(3)
                visibleEntries.forEach { entry ->
                    val localizedLabel = localizedTraceLabel(entry.label)
                    Row(verticalAlignment = Alignment.Top) {
                        if (entry.running) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            Text(
                                if (entry.failed) "×" else "✓",
                                color = if (entry.failed) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            localizedLabel,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
                if (!expanded && normalized.size > visibleEntries.size) {
                    Text(
                        stringResource(
                            R.string.chat_execution_more,
                            normalized.size - visibleEntries.size,
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .72f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun localizedTraceLabel(label: String): String {
    val tool = label.substringBefore("（")
    val suffix = label.removePrefix(tool)
    val resource = when (tool) {
        "Bash", "bash", "exec", "exec_command", "执行命令" -> R.string.tool_name_bash
        "Read", "read", "read_file", "读取文件" -> R.string.tool_name_read
        "Write", "write", "write_file" -> R.string.tool_name_write
        "Edit", "edit", "edit_file" -> R.string.tool_name_edit
        "Glob", "glob" -> R.string.tool_name_glob
        "Grep", "grep" -> R.string.tool_name_grep
        "PublishFile", "publish_file" -> R.string.tool_name_publish_file
        "WebFetch", "web_fetch" -> R.string.tool_name_web_fetch
        "WebSearch", "web_search", "search_web", "网络搜索" -> R.string.tool_name_web_search
        "browser_tools" -> R.string.tool_name_browser_tools
        "code_tools" -> R.string.tool_name_code_tools
        "delivery_tools" -> R.string.tool_name_delivery_tools
        "desktop_tools" -> R.string.tool_name_desktop_tools
        "entity_tools" -> R.string.tool_name_entity_tools
        "integration_tools" -> R.string.tool_name_integration_tools
        "knowledge_tools" -> R.string.tool_name_knowledge_tools
        "map_tools" -> R.string.tool_name_map_tools
        "memory_tools" -> R.string.tool_name_memory_tools
        "remote_tools" -> R.string.tool_name_remote_tools
        "skill_tools" -> R.string.tool_name_skill_tools
        "subagent_tools" -> R.string.tool_name_subagent_tools
        "task_tools" -> R.string.tool_name_task_tools
        else -> null
    }
    val localizedSuffix = when (suffix.removePrefix("（").removeSuffix("）")) {
        "File published for download" ->
            "（${stringResource(R.string.tool_result_file_published)}）"
        else -> suffix
    }
    return (resource?.let { stringResource(it) } ?: tool) + localizedSuffix
}

private data class TraceDisplay(
    val label: String,
    val running: Boolean,
    val failed: Boolean,
)

private fun normalizeTraceEntries(entries: List<JSONObject>): List<TraceDisplay> {
    val byId = linkedMapOf<String, JSONObject>()
    entries.forEachIndexed { index, item ->
        val hiddenTool = item.optString("tool") in setOf(
            "use_tools", "quit", "send_message", "update_plan_progress",
        )
        if (hiddenTool) return@forEachIndexed
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
        val type = item.optString("type")
        val tool = item.optString("text").ifBlank {
            item.optString("tool").ifBlank { item.optString("name") }
        }
        val preview = if (type == "phase_transition") "" else item.optString("preview").ifBlank {
            item.optString("label").ifBlank {
                item.optString("query").ifBlank {
                    item.optString("result_preview").ifBlank {
                        item.optString("message").ifBlank {
                            item.optString("task").ifBlank { traceArgsPreview(item.optJSONObject("args")) }
                        }
                    }
                }
            }
        }
        val status = item.optString("status")
        val displayTool = when (type) {
            "phase_transition" -> item.optString("detail").ifBlank {
                listOf(item.optString("from"), item.optString("to"))
                    .filter(String::isNotBlank).joinToString(" → ")
            }.ifBlank { "阶段切换" }
            "plan" -> "执行计划已${if (status == "rejected") "拒绝" else "确认"}"
            "plan_progress" -> "计划步骤 ${item.optInt("step")}：${status.ifBlank { "更新" }}"
            "auto_review", "permission_decision" -> if (item.optBoolean("approved")) {
                "权限审查已通过"
            } else {
                "权限审查未通过"
            }
            "subagent_update" -> "子 Agent ${item.optString("agent_id")}：${status.ifBlank { "更新" }}"
            else -> when (tool.lowercase()) {
            "websearch", "web_search", "search_web" -> "网络搜索"
            "read", "read_file" -> "读取文件"
            "bash", "exec", "exec_command" -> "执行命令"
            else -> tool.ifBlank { "工具调用" }
            }
        }
        TraceDisplay(
            label = if (preview.isBlank()) displayTool else "$displayTool（$preview）",
            running = status == "running" || type.endsWith("started") || type.endsWith("progress"),
            failed = item.optBoolean("failed") || status == "failed" || type.endsWith("failed"),
        )
    }
}

private fun traceArgsPreview(args: JSONObject?): String {
    if (args == null) return ""
    return args.keys().asSequence().take(3).mapNotNull { key ->
        val value = args.opt(key)?.toString()?.replace(Regex("\\s+"), " ")?.take(80).orEmpty()
        value.takeIf(String::isNotBlank)?.let { "$key: $it" }
    }.joinToString(" · ").take(220)
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

private fun formatChatListTimestamp(raw: String): String {
    if (raw.isBlank()) return ""
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw
    val zone = ZoneId.systemDefault()
    val localDate = instant.atZone(zone).toLocalDate()
    val today = java.time.LocalDate.now(zone)
    val pattern = when {
        localDate == today -> "HH:mm"
        localDate.year == today.year -> "MM-dd HH:mm"
        else -> "yyyy-MM-dd"
    }
    return DateTimeFormatter.ofPattern(pattern)
        .withLocale(Locale.getDefault())
        .withZone(zone)
        .format(instant)
}

@Composable
private fun localizedStatus(raw: String): String = when (raw.lowercase()) {
    "local" -> stringResource(R.string.local_agent_device_only)
    "ready" -> stringResource(R.string.local_agent_ready)
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp))
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
