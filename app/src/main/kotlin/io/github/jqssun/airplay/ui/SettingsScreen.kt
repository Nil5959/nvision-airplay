package io.github.jqssun.airplay.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.appSettingsIntent
import io.github.jqssun.airplay.viewmodel.MainViewModel
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.jqssun.airplay.ui.theme.AirPlayRing
import io.github.jqssun.airplay.ui.theme.Inter
import io.github.jqssun.airplay.ui.theme.TvFocusSurface
import io.github.jqssun.airplay.ui.theme.TvMuted
import io.github.jqssun.airplay.ui.theme.TvText
import kotlinx.coroutines.launch

// tvOS-style settings: short list on one side, AirPlay ring + version on the other.
// Everything else lives under "Advanced" (AdvancedSettings below).
@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenLogs: () -> Unit = {}) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var backFromAdvanced by remember { mutableStateOf(false) }
    BackHandler(enabled = advanced) { advanced = false; backFromAdvanced = true }
    val ctx = LocalContext.current
    val version = remember(ctx) {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val listWeight = if (advanced) 0.6f else 0.42f
    Row(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(modifier = Modifier.weight(listWeight).fillMaxHeight()) {
            if (advanced) {
                val first = remember { FocusRequester() }
                LaunchedEffect(Unit) { first.requestFocusUntilLanded() }
                TransparentSurface { AdvancedSettings(viewModel, onOpenLogs, first) }
            } else {
                MainSettingsList(viewModel, focusAdvanced = backFromAdvanced, onAdvanced = { advanced = true })
            }
        }
        Column(
            modifier = Modifier.weight(1f - listWeight).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AirPlayRing(size = 150.dp, breathe = false)
            Spacer(Modifier.height(24.dp))
            Text("AirPlay", fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 40.sp, color = TvText)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.settings_version, version), fontSize = 16.sp, color = TvMuted)
        }
    }
}

private enum class Editor { NAME, RESOLUTION, AUDIO_DELAY }

@Composable
private fun MainSettingsList(viewModel: MainViewModel, focusAdvanced: Boolean, onAdvanced: () -> Unit) {
    val serverName by viewModel.pref(Prefs.SERVER_NAME).collectAsState()
    val bootAutoStart by viewModel.pref(Prefs.BOOT_AUTO_START).collectAsState()
    val requirePin by viewModel.pref(Prefs.REQUIRE_PIN).collectAsState()
    val resolution by viewModel.pref(Prefs.RESOLUTION).collectAsState()
    val audioLatencyMs by viewModel.pref(Prefs.AUDIO_LATENCY_MS).collectAsState()
    var editor by remember { mutableStateOf<Editor?>(null) }
    val nameFocus = remember { FocusRequester() }
    val resFocus = remember { FocusRequester() }
    val delayFocus = remember { FocusRequester() }
    val advancedFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { (if (focusAdvanced) advancedFocus else nameFocus).requestFocusUntilLanded() }
    fun closeEditor() {
        val back = when (editor) {
            Editor.NAME -> nameFocus
            Editor.RESOLUTION -> resFocus
            Editor.AUDIO_DELAY -> delayFocus
            null -> null
        }
        editor = null
        back?.let { scope.launch { it.requestFocusUntilLanded() } }
    }
    val on = stringResource(R.string.chip_on)
    val off = stringResource(R.string.chip_off)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // room for the focus scale + shadow, which the scroll container would clip
            .padding(start = 32.dp, end = 32.dp, top = 56.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(stringResource(R.string.settings_group_general))
        TvRow(stringResource(R.string.settings_room_name), serverName, Modifier.focusRequester(nameFocus)) {
            editor = Editor.NAME
        }
        TvRow(stringResource(R.string.settings_start_with_tv), if (bootAutoStart) on else off) {
            viewModel.set(Prefs.BOOT_AUTO_START, !bootAutoStart)
        }
        TvRow(stringResource(R.string.settings_pin_on_screen), if (requirePin) on else off) {
            viewModel.set(Prefs.REQUIRE_PIN, !requirePin)
        }
        SectionHeader(stringResource(R.string.settings_group_picture_sound))
        val resolutionLabel = when (resolution) {
            Prefs.AUTO -> stringResource(R.string.chip_auto)
            Prefs.PORTRAIT -> stringResource(R.string.chip_device_portrait)
            Prefs.LANDSCAPE -> stringResource(R.string.chip_device_landscape)
            else -> resolution
        }
        TvRow(stringResource(R.string.setting_resolution), resolutionLabel, Modifier.focusRequester(resFocus)) {
            editor = Editor.RESOLUTION
        }
        TvRow(
            stringResource(R.string.settings_audio_delay),
            if (audioLatencyMs < 0) stringResource(R.string.chip_auto)
            else stringResource(R.string.audio_delay_value, audioLatencyMs),
            Modifier.focusRequester(delayFocus)
        ) { editor = Editor.AUDIO_DELAY }
        Spacer(Modifier.height(10.dp))
        TvRow(stringResource(R.string.settings_advanced), "›", Modifier.focusRequester(advancedFocus), onClick = onAdvanced)
    }

    when (editor) {
        Editor.NAME -> {
            var text by remember { mutableStateOf(serverName) }
            val commit = {
                if (text.isNotBlank() && text != serverName) viewModel.set(Prefs.SERVER_NAME, text.trim())
                closeEditor()
            }
            EditorDialog(stringResource(R.string.settings_room_name), ::closeEditor, onOk = commit) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = doneAndHide { commit() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Editor.RESOLUTION -> EditorDialog(stringResource(R.string.setting_resolution), ::closeEditor) {
            SettingResolution(value = resolution, onValueChange = { viewModel.set(Prefs.RESOLUTION, it) })
        }
        Editor.AUDIO_DELAY -> EditorDialog(stringResource(R.string.settings_audio_delay), ::closeEditor) {
            Column { AudioDelayEditor(viewModel, audioLatencyMs) }
        }
        null -> {}
    }
}

@Composable
private fun EditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onOk: () -> Unit = onDismiss,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TransparentSurface(content) },
        confirmButton = {
            TextButton(onClick = onOk, modifier = Modifier.dpadFocus()) { Text(stringResource(R.string.btn_ok)) }
        }
    )
}

// existing Material rows paint colorScheme.surface; let the backdrop show through instead
@Composable
private fun TransparentSurface(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent), content = content)
}

/** tvOS settings row: label on one side, current value in grey on the other; white when focused. */
@Composable
private fun TvRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TvFocusSurface(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth(), focusedScale = 1.04f) { focused ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            Text(
                value, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (focused) Color(0xFF636366) else TvMuted
            )
        }
    }
}

@Composable
private fun AudioDelayEditor(viewModel: MainViewModel, audioLatencyMs: Int) {
    SettingSwitch(
        title = stringResource(R.string.setting_audio_delay),
        description = stringResource(R.string.setting_audio_delay_desc),
        checked = audioLatencyMs >= 0,
        onCheckedChange = { viewModel.set(Prefs.AUDIO_LATENCY_MS, if (it) 250 else -1) }
    )

    if (audioLatencyMs >= 0) {
        var sliderVal by remember(audioLatencyMs) { mutableFloatStateOf(audioLatencyMs.toFloat()) }
        ListItem(
            headlineContent = {
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = { viewModel.set(Prefs.AUDIO_LATENCY_MS, sliderVal.roundToInt()) },
                    valueRange = 0f..1000f,
                    steps = 19,
                    modifier = Modifier.dpadFocus().dpadAdjust(
                        onLeft = { viewModel.set(Prefs.AUDIO_LATENCY_MS, (audioLatencyMs - 50).coerceIn(0, 1000)) },
                        onRight = { viewModel.set(Prefs.AUDIO_LATENCY_MS, (audioLatencyMs + 50).coerceIn(0, 1000)) }
                    )
                )
            },
            trailingContent = { Text(stringResource(R.string.audio_delay_value, sliderVal.roundToInt())) }
        )
    }
}

@Composable
private fun AdvancedSettings(viewModel: MainViewModel, onOpenLogs: () -> Unit, firstFocus: FocusRequester) {
    val h265 by viewModel.pref(Prefs.H265).collectAsState()
    val enforceSdr by viewModel.pref(Prefs.ENFORCE_SDR).collectAsState()
    val alacEnabled by viewModel.pref(Prefs.ALAC_ENABLED).collectAsState()
    val aacEnabled by viewModel.pref(Prefs.AAC_ENABLED).collectAsState()
    val idlePreview by viewModel.pref(Prefs.IDLE_PREVIEW).collectAsState()
    val autoFullscreen by viewModel.pref(Prefs.AUTO_FULLSCREEN).collectAsState()
    val keepScreenOn by viewModel.pref(Prefs.KEEP_SCREEN_ON).collectAsState()
    val advertiseVideo by viewModel.pref(Prefs.ADVERTISE_VIDEO).collectAsState()
    val advertiseMirroring by viewModel.pref(Prefs.ADVERTISE_MIRRORING).collectAsState()
    val advertiseAudio by viewModel.pref(Prefs.ADVERTISE_AUDIO).collectAsState()
    val launchOnConnect by viewModel.pref(Prefs.LAUNCH_ON_CONNECT).collectAsState()
    val maxFps by viewModel.pref(Prefs.MAX_FPS).collectAsState()
    val overscanned by viewModel.pref(Prefs.OVERSCANNED).collectAsState()
    val allowNewConn by viewModel.pref(Prefs.ALLOW_NEW_CONN).collectAsState()
    val usbOnly by viewModel.pref(Prefs.USB_ONLY).collectAsState()
    val autoStart by viewModel.pref(Prefs.AUTO_START).collectAsState()
    val runInBackground by viewModel.pref(Prefs.RUN_IN_BACKGROUND).collectAsState()
    val serverPort by viewModel.pref(Prefs.SERVER_PORT).collectAsState()
    val forceSwAlac by viewModel.pref(Prefs.FORCE_SW_ALAC).collectAsState()
    val forceSwAac by viewModel.pref(Prefs.FORCE_SW_AAC).collectAsState()
    val syncClientVolume by viewModel.pref(Prefs.SYNC_CLIENT_VOLUME).collectAsState()
    val debugEnabled by viewModel.pref(Prefs.DEBUG_ENABLED).collectAsState()
    val developerOptions by viewModel.pref(Prefs.DEVELOPER_OPTIONS).collectAsState()
    val keyAllowFrameDrop by viewModel.pref(Prefs.KEY_ALLOW_FRAME_DROP).collectAsState()
    val realtimeDecoderPriority by viewModel.pref(Prefs.KEY_PRIORITY).collectAsState()
    val lowLatency by viewModel.pref(Prefs.LOW_LATENCY).collectAsState()
    val operatingRate by viewModel.pref(Prefs.OPERATING_RATE).collectAsState()
    val scheduledOutputBufferRelease by viewModel.pref(Prefs.SCHEDULED_OUTPUT_BUFFER_RELEASE).collectAsState()
    val benchmarkLog by viewModel.pref(Prefs.BENCHMARK_LOG).collectAsState()
    val audioAutoBuffer by viewModel.pref(Prefs.AUDIO_AUTO_BUFFER).collectAsState()
    val audioCushionMs by viewModel.pref(Prefs.AUDIO_CUSHION_MS).collectAsState()
    val audioAdaptiveStep by viewModel.pref(Prefs.AUDIO_ADAPTIVE_STEP).collectAsState()
    val oboeBufferFrames by viewModel.pref(Prefs.OBOE_BUFFER_FRAMES).collectAsState()
    val tri = listOf(
        Prefs.AUTO to stringResource(R.string.chip_auto),
        Prefs.ON to stringResource(R.string.chip_on),
        Prefs.OFF to stringResource(R.string.chip_off),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 32.dp)
    ) {
        SectionHeader(stringResource(R.string.settings_advanced))

        TvRow(
            label = stringResource(R.string.tab_logs),
            value = "›",
            modifier = Modifier.focusRequester(firstFocus),
            onClick = onOpenLogs
        )

        SectionHeader(stringResource(R.string.section_server))

        SettingTextField(
            label = stringResource(R.string.setting_server_port),
            value = serverPort.toString(),
            onCommit = { viewModel.set(Prefs.SERVER_PORT, it.toInt()) },
            range = Prefs.SERVER_PORT.range
        )

        SettingSwitch(
            title = stringResource(R.string.setting_run_in_background),
            description = stringResource(R.string.setting_run_in_background_desc),
            checked = runInBackground,
            onCheckedChange = { viewModel.set(Prefs.RUN_IN_BACKGROUND, it) }
        )

        SectionHeader(stringResource(R.string.section_connection))

        SettingSegmented(
            title = stringResource(R.string.setting_discovery),
            summary = stringResource(if (usbOnly) R.string.setting_discovery_usb_desc else R.string.setting_discovery_all_desc),
            value = usbOnly,
            options = listOf(
                false to stringResource(R.string.chip_all_networks),
                true to stringResource(R.string.chip_usb_only),
            ),
            onValueChange = { viewModel.set(Prefs.USB_ONLY, it) }
        )

        SettingSwitch(
            title = stringResource(R.string.setting_allow_new_conn),
            description = stringResource(R.string.setting_allow_new_conn_desc),
            checked = allowNewConn,
            onCheckedChange = { viewModel.set(Prefs.ALLOW_NEW_CONN, it) }
        )

        val ctx = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var hasOverlayPermission by remember { mutableStateOf(canAutoLaunch(ctx)) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) hasOverlayPermission = canAutoLaunch(ctx)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val needsOverlayPermission = launchOnConnect && !hasOverlayPermission
        val autoLaunch = launchOnConnect && hasOverlayPermission
        ListItem(
            modifier = Modifier
                .dpadFocus(RectangleShape)
                .toggleable(
                    value = autoLaunch,
                    role = Role.Switch,
                    onValueChange = {
                        viewModel.set(Prefs.LAUNCH_ON_CONNECT, it)
                        if (it && !canAutoLaunch(ctx)) _openOverlaySettings(ctx)
                    }
                ),
            headlineContent = { Text(stringResource(R.string.setting_launch_on_connect)) },
            supportingContent = {
                Text(stringResource(
                    if (needsOverlayPermission) R.string.setting_launch_on_connect_no_permission
                    else R.string.setting_launch_on_connect_desc
                ))
            },
            trailingContent = {
                Switch(checked = autoLaunch, onCheckedChange = null)
            }
        )

        SectionHeader(stringResource(R.string.section_display))

        SettingSwitch(
            title = stringResource(R.string.setting_auto_fullscreen),
            description = stringResource(R.string.setting_auto_fullscreen_desc),
            checked = autoFullscreen,
            onCheckedChange = { viewModel.set(Prefs.AUTO_FULLSCREEN, it) }
        )

        SettingChipField(
            title = stringResource(R.string.setting_max_fps),
            description = stringResource(R.string.setting_max_fps_desc),
            value = maxFps.toString(),
            presets = listOf("24" to "24", "30" to "30", "60" to "60", "120" to "120"),
            placeholder = stringResource(R.string.setting_max_fps_placeholder),
            keyboardType = KeyboardType.Number,
            onValueChange = { it.toIntOrNull()?.let { v -> viewModel.set(Prefs.MAX_FPS, v) } }
        )

        SettingSwitch(
            title = stringResource(R.string.setting_overscanned),
            description = stringResource(R.string.setting_overscanned_desc),
            checked = overscanned,
            onCheckedChange = { viewModel.set(Prefs.OVERSCANNED, it) }
        )

        SectionHeader(stringResource(R.string.section_decode))

        SettingChips(
            title = stringResource(R.string.setting_h265),
            description = stringResource(R.string.setting_h265_desc),
            value = h265,
            options = tri,
            onValueChange = { viewModel.set(Prefs.H265, it) }
        )

        SettingSwitch(
            title = stringResource(R.string.setting_sw_alac),
            description = stringResource(R.string.setting_sw_alac_desc),
            checked = forceSwAlac,
            onCheckedChange = { viewModel.set(Prefs.FORCE_SW_ALAC, it) }
        )

        SettingSwitch(
            title = stringResource(R.string.setting_sw_aac),
            description = stringResource(R.string.setting_sw_aac_desc),
            checked = forceSwAac,
            onCheckedChange = { viewModel.set(Prefs.FORCE_SW_AAC, it) }
        )

        SettingSwitch(
            title = stringResource(R.string.setting_sync_client_volume),
            description = stringResource(R.string.setting_sync_client_volume_desc),
            checked = syncClientVolume,
            onCheckedChange = { viewModel.set(Prefs.SYNC_CLIENT_VOLUME, it) }
        )

        SectionHeader(stringResource(R.string.section_developer))

        SettingSwitch(
            title = stringResource(R.string.setting_developer_options),
            description = stringResource(R.string.setting_developer_options_desc),
            checked = developerOptions,
            onCheckedChange = { viewModel.set(Prefs.DEVELOPER_OPTIONS, it) }
        )

        if (developerOptions) {
            SettingSwitch(
                title = stringResource(R.string.setting_auto_start),
                description = stringResource(R.string.setting_auto_start_desc),
                checked = autoStart,
                onCheckedChange = { viewModel.set(Prefs.AUTO_START, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_keep_screen_on),
                description = stringResource(R.string.setting_keep_screen_on_desc),
                checked = keepScreenOn,
                onCheckedChange = { viewModel.set(Prefs.KEEP_SCREEN_ON, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_idle_preview),
                description = stringResource(R.string.setting_idle_preview_desc),
                checked = idlePreview,
                onCheckedChange = { viewModel.set(Prefs.IDLE_PREVIEW, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_advertise_video),
                description = stringResource(R.string.setting_advertise_video_desc),
                checked = advertiseVideo,
                onCheckedChange = { viewModel.set(Prefs.ADVERTISE_VIDEO, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_advertise_mirroring),
                description = stringResource(R.string.setting_advertise_mirroring_desc),
                checked = advertiseMirroring,
                onCheckedChange = { viewModel.set(Prefs.ADVERTISE_MIRRORING, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_advertise_audio),
                description = stringResource(R.string.setting_advertise_audio_desc),
                checked = advertiseAudio,
                onCheckedChange = { viewModel.set(Prefs.ADVERTISE_AUDIO, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_alac),
                description = stringResource(R.string.setting_alac_desc),
                checked = alacEnabled,
                onCheckedChange = { viewModel.set(Prefs.ALAC_ENABLED, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_aac),
                description = stringResource(R.string.setting_aac_desc),
                checked = aacEnabled,
                onCheckedChange = { viewModel.set(Prefs.AAC_ENABLED, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_key_allow_frame_drop),
                description = stringResource(R.string.setting_key_allow_frame_drop_desc),
                checked = keyAllowFrameDrop,
                onCheckedChange = { viewModel.set(Prefs.KEY_ALLOW_FRAME_DROP, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_enforce_sdr),
                description = stringResource(R.string.setting_enforce_sdr_desc),
                checked = enforceSdr,
                onCheckedChange = { viewModel.set(Prefs.ENFORCE_SDR, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_realtime_decoder_priority),
                description = stringResource(R.string.setting_realtime_decoder_priority_desc),
                checked = realtimeDecoderPriority,
                onCheckedChange = { viewModel.set(Prefs.KEY_PRIORITY, it) }
            )

            SettingChips(
                title = stringResource(R.string.setting_operating_rate),
                description = stringResource(R.string.setting_operating_rate_desc),
                value = operatingRate,
                options = tri,
                onValueChange = { viewModel.set(Prefs.OPERATING_RATE, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_low_latency),
                description = stringResource(R.string.setting_low_latency_desc),
                checked = lowLatency,
                onCheckedChange = { viewModel.set(Prefs.LOW_LATENCY, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_scheduled_output_buffer_release),
                description = stringResource(R.string.setting_scheduled_output_buffer_release_desc),
                checked = scheduledOutputBufferRelease,
                onCheckedChange = { viewModel.set(Prefs.SCHEDULED_OUTPUT_BUFFER_RELEASE, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_audio_auto_buffer),
                description = stringResource(R.string.setting_audio_auto_buffer_desc),
                checked = audioAutoBuffer,
                onCheckedChange = { viewModel.set(Prefs.AUDIO_AUTO_BUFFER, it) }
            )

            if (audioAutoBuffer) {
                val maxStep = Prefs.AUDIO_ADAPTIVE_STEP.range.last
                var stepVal by remember(audioAdaptiveStep) { mutableFloatStateOf(audioAdaptiveStep.toFloat()) }
                ListItem(
                    headlineContent = {
                        Slider(
                            value = stepVal,
                            onValueChange = { stepVal = it },
                            onValueChangeFinished = { viewModel.set(Prefs.AUDIO_ADAPTIVE_STEP, stepVal.roundToInt()) },
                            valueRange = 0f..maxStep.toFloat(),
                            steps = maxStep - 1,
                            modifier = Modifier.dpadFocus().dpadAdjust(
                                onLeft = { viewModel.set(Prefs.AUDIO_ADAPTIVE_STEP, (audioAdaptiveStep - 1).coerceIn(0, maxStep)) },
                                onRight = { viewModel.set(Prefs.AUDIO_ADAPTIVE_STEP, (audioAdaptiveStep + 1).coerceIn(0, maxStep)) }
                            )
                        )
                    },
                    trailingContent = {
                        val step = stepVal.roundToInt().coerceIn(0, maxStep)
                        Text(stringResource(R.string.audio_adaptive_value,
                            Prefs.ADAPTIVE_PERCENTILES[step],
                            stringArrayResource(R.array.audio_adaptive_step_names)[step]))
                    }
                )
            } else {
                SettingTextField(
                    label = stringResource(R.string.setting_audio_cushion_ms),
                    value = audioCushionMs.toString(),
                    onCommit = { viewModel.set(Prefs.AUDIO_CUSHION_MS, it.toInt()) },
                    description = stringResource(R.string.setting_audio_cushion_ms_desc),
                    range = Prefs.AUDIO_CUSHION_MS.range
                )
            }

            SettingTextField(
                label = stringResource(R.string.setting_oboe_buffer_frames),
                value = oboeBufferFrames.toString(),
                onCommit = { viewModel.set(Prefs.OBOE_BUFFER_FRAMES, it.toInt()) },
                description = stringResource(R.string.setting_oboe_buffer_frames_desc),
                range = Prefs.OBOE_BUFFER_FRAMES.range
            )


            SettingSwitch(
                title = stringResource(R.string.setting_debug_overlay),
                description = stringResource(R.string.setting_debug_overlay_desc),
                checked = debugEnabled,
                onCheckedChange = { viewModel.set(Prefs.DEBUG_ENABLED, it) }
            )

            SettingSwitch(
                title = stringResource(R.string.setting_benchmark_log),
                description = stringResource(R.string.setting_benchmark_log_desc),
                checked = benchmarkLog,
                onCheckedChange = { viewModel.set(Prefs.BENCHMARK_LOG, it) }
            )
        }
    }
}

private fun canAutoLaunch(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(ctx)

// overlay settings screen may not exist on some builds
private fun _openOverlaySettings(ctx: Context) {
    val overlay = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
    try {
        ctx.startActivity(overlay.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        ctx.startActivity(ctx.appSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        color = TvMuted,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingResolution(
    value: String,
    onValueChange: (String) -> Unit
) {
    val presets = listOf(
        Prefs.AUTO to stringResource(R.string.chip_auto),
        "1280x720" to "1280x720",
        "1920x1080" to "1920x1080",
        "3840x2160" to "3840x2160"
    )
    val devicePresets = listOf(
        Prefs.PORTRAIT to stringResource(R.string.chip_device_portrait),
        Prefs.LANDSCAPE to stringResource(R.string.chip_device_landscape)
    )
    val isPreset = (presets + devicePresets).any { it.first == value }
    var editing by remember { mutableStateOf(false) }
    val parts = if (!isPreset && value.contains("x")) value.split("x", limit = 2) else listOf("", "")
    var width by remember(value) { mutableStateOf(if (isPreset) "" else parts[0]) }
    var height by remember(value) { mutableStateOf(if (isPreset) "" else parts.getOrElse(1) { "" }) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.setting_resolution)) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.setting_resolution_desc))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    @Composable
                    fun presetChip(key: String, label: String) = FilterChip(
                        selected = value == key && !editing,
                        onClick = {
                            editing = false
                            width = ""; height = ""
                            onValueChange(key)
                        },
                        label = { Text(label) },
                        modifier = Modifier.dpadFocus()
                    )
                    presets.forEach { (key, label) -> presetChip(key, label) }
                    FilterChip(
                        selected = !isPreset || editing,
                        onClick = { editing = true },
                        label = { Text(stringResource(R.string.chip_custom)) },
                        modifier = Modifier.dpadFocus()
                    )
                    devicePresets.forEach { (key, label) -> presetChip(key, label) }
                }
                if (editing || !isPreset) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it.filter { c -> c.isDigit() }.take(5) },
                            singleLine = true,
                            label = { Text(stringResource(R.string.setting_resolution_width)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter { c -> c.isDigit() }.take(5) },
                            singleLine = true,
                            label = { Text(stringResource(R.string.setting_resolution_height)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = doneAndHide {
                                val w = width.toIntOrNull()
                                val h = height.toIntOrNull()
                                if (w != null && w > 0 && h != null && h > 0) {
                                    onValueChange("${w}x${h}")
                                    editing = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun doneAndHide(commit: () -> Unit): KeyboardActions {
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(onDone = { commit(); keyboard?.hide() })
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    description: String? = null,
    range: IntRange? = null
) {
    var text by remember(value) { mutableStateOf(value) }
    val valid = range == null || text.toIntOrNull()?.let { it in range } == true
    fun save() {
        if (valid && text != value) onCommit(text)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = if (range == null) it else it.filter { c -> c.isDigit() }.take(6) },
        label = { Text(label) },
        supportingText = description?.let { { Text(it) } },
        singleLine = true,
        isError = !valid,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (range == null) KeyboardType.Text else KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = doneAndHide(::save),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .onFocusChanged { if (!it.isFocused) save() }
    )
}

@Composable
private fun SettingChipField(
    title: String,
    description: String,
    value: String,
    presets: List<Pair<String, String>>,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    val isPreset = presets.any { it.first == value }
    var editing by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(if (isPreset) "" else value) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                Spacer(Modifier.height(8.dp))
                ChipRow(if (editing) "" else value, presets, { editing = false; text = ""; onValueChange(it) }) {
                    FilterChip(
                        selected = !isPreset || editing,
                        onClick = { editing = true },
                        label = { Text(stringResource(R.string.chip_custom)) },
                        modifier = Modifier.dpadFocus()
                    )
                }
                if (editing || !isPreset) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        label = { Text(placeholder) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = doneAndHide {
                            if (text.isNotBlank()) {
                                onValueChange(text)
                                editing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = value == key,
                onClick = { onSelect(key) },
                label = { Text(label) },
                modifier = Modifier.dpadFocus()
            )
        }
        trailing()
    }
}

@Composable
private fun SettingChips(
    title: String,
    description: String,
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                Spacer(Modifier.height(8.dp))
                ChipRow(value, options, onValueChange)
            }
        }
    )
}

@Composable
private fun <T> SettingSegmented(
    title: String,
    summary: String,
    value: T,
    options: List<Pair<T, String>>,
    onValueChange: (T) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (key, label) ->
                        val shape = SegmentedButtonDefaults.itemShape(i, options.size)
                        SegmentedButton(
                            selected = value == key,
                            onClick = { onValueChange(key) },
                            shape = shape,
                            modifier = Modifier.dpadFocus(shape),
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(summary)
            }
        }
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier
            .dpadFocus(RectangleShape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) }
    )
}
