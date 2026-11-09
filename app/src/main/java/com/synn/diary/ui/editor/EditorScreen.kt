package com.synn.diary.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.BoxScope
import com.synn.diary.ui.LocalBackgroundFiles
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import java.io.File
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.data.EditorSettings
import com.synn.diary.data.media.FontStore
import com.synn.diary.data.PendingWrites
import com.synn.diary.data.body.ImageAlign
import com.synn.diary.data.db.TagEntity
import com.synn.diary.ui.LocalImageFiles
import com.synn.diary.ui.components.ImageIcon
import com.synn.diary.ui.components.PaletteIcon
import com.synn.diary.ui.components.RuledLinesIcon
import com.synn.diary.ui.components.SaveIcon
import com.synn.diary.ui.components.TextFieldsIcon
import com.synn.diary.ui.mood.Mood
import com.synn.diary.util.Dates
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    editorSettings: EditorSettings,
    fontStore: FontStore,
    onBack: () -> Unit,
    onExternalUiStart: () -> Unit = {},
    onExternalUiEnd: () -> Unit = {},
) {
    val imageFile = LocalImageFiles.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entryTags by viewModel.entryTags.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    var confirmDelete by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    // 字体家族解析一次，别每次重组都重新读文件
    val contentFamily = remember(state.fontId) {
        ContentFont.from(state.fontId, fontStore).family(fontStore)
    }

    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // 横线是"看习惯"的开关，全局记忆；纸张花纹/照片本身不画线，这里只是决定纯色纸上画不画
    var ruledLines by remember { mutableStateOf(editorSettings.ruledLines) }
    val paperSuppressesRules = (state.background as? EntryBackground.Preset)
        ?.pattern?.let { it != PaperPattern.NONE } ?: (state.background is EntryBackground.Custom)

    // 正文是"文本块 + 图片块"的列表：图片插在哪个位置，就等价于图片块在列表里的位置。
    val blocks = remember { mutableStateListOf<BodyBlock>() }
    var seeded by remember { mutableStateOf(false) }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var pendingFocusId by remember { mutableStateOf<String?>(null) }
    var selectedImageId by remember { mutableStateOf<String?>(null) }
    var popupAnchor by remember { mutableStateOf(Offset.Zero) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    fun syncBody() {
        viewModel.onBodyChange(serializeBlocks(blocks))
    }

    LaunchedEffect(state.loaded) {
        if (state.loaded && !seeded) {
            blocks.clear()
            blocks.addAll(parseBlocks(state.body))
            seeded = true
            // 打开已有日记：光标落在最后一个文本块末尾，接着写
            pendingFocusId = blocks.lastOrNull { it is BodyBlock.Text }?.id
        }
    }

    LaunchedEffect(pendingFocusId) {
        val id = pendingFocusId ?: return@LaunchedEffect
        focusRequesters[id]?.requestFocus()
        keyboard?.show()
        pendingFocusId = null
    }

    val backgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) viewModel.importBackground(uri)
    }

    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) {
            viewModel.importFont(uri) { fileName ->
                if (fileName == null) {
                    Toast.makeText(context, "这个文件不是字体（只支持 ttf / otf）", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) {
            viewModel.importImage(uri) { fileName ->
                if (fileName != null) {
                    // 插在光标所在的文本块中间：前半段留下，图片，后半段另起一块
                    val focusIndex = blocks.indexOfFirst { it.id == focusedBlockId }
                    val index = if (focusIndex >= 0) focusIndex
                    else blocks.indexOfLast { it is BodyBlock.Text }
                    val textBlock = blocks.getOrNull(index) as? BodyBlock.Text
                    if (textBlock != null) {
                        val selection = textBlock.value.selection.start
                            .coerceIn(0, textBlock.value.text.length)
                        val before = textBlock.value.text.substring(0, selection)
                        val after = textBlock.value.text.substring(selection)

                        blocks[index] = BodyBlock.Text(
                            textBlock.id,
                            TextFieldValue(before, TextRange(before.length)),
                        )
                        blocks.add(index + 1, BodyBlock.Image(newBlockId(), fileName))
                        val afterId = newBlockId()
                        blocks.add(index + 2, BodyBlock.Text(afterId, TextFieldValue(after, TextRange(0))))
                        pendingFocusId = afterId
                        syncBody()
                    }
                }
            }
        }
    }

    // 离开页面时落盘；同时登记到 PendingWrites，
    // 这样系统要杀进程（vivo 内存机制 KILL 广播）时也能先把这页刷掉
    DisposableEffect(Unit) {
        val flushHook: () -> Unit = { viewModel.flush() }
        PendingWrites.register(flushHook)
        onDispose {
            PendingWrites.unregister(flushHook)
            viewModel.flush()
        }
    }

    // 保存成功的轻反馈：先给一下"已收好"，再回列表
    var showSavedHint by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 纸铺满整页：状态栏、顶栏、正文是同一张纸，不再有一条颜色不同的顶栏
    PaperBackdrop(
        background = state.background,
        modifier = Modifier.fillMaxSize(),
    ) {
    Scaffold(
        // 整个页面跟着键盘上浮：如果只给底部工具栏加 imePadding，
        // Scaffold 会把键盘高度也算进内容区留白，正文会被压没。
        modifier = Modifier.fillMaxSize().imePadding(),
        // 让纸透上来；顶栏也透明，否则上方还是盖着一层底色
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !showSavedHint,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveNow {
                                showSavedHint = true
                                // 让"已收好"停留一小会儿再走，否则一闪就回列表看不见
                                scope.launch {
                                    delay(520)
                                    onBack()
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = SaveIcon,
                            contentDescription = "保存",
                        )
                    }
                },

            )
        },
        bottomBar = {
            EditorToolbar(
                onInsertImage = {
                    onExternalUiStart()
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onFont = { showFontPicker = true },
                linesOn = ruledLines,
                onToggleLines = {
                    val next = !ruledLines
                    ruledLines = next
                    editorSettings.ruledLines = next
                    if (next && paperSuppressesRules) {
                        Toast.makeText(
                            context,
                            "这张纸有花纹或是照片，本来就不画横线；换回纯色纸才看得到",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onBackground = { showBackgroundPicker = true },
                onDelete = { confirmDelete = true },
            )
        },
    ) { inner ->
        // 关掉开关、或者纸上有花纹/照片时不画行线：两种纹理叠在一起太吵
        val ruleColor = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (!ruledLines || paperSuppressesRules) 0f else 0.03f,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
                // 整页一起滚：分屏/小窗时可用高度很小，
                // 如果情绪、标签、标题固定占位，正文会被挤成 0 高，根本没法打字。
                .verticalScroll(rememberScrollState())
                // 极淡的横向行线，像横线本；纯装饰，不参与布局
                .drawBehind {
                    val step = 36.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                        y += step
                    }
                },
        ) {
            // 日期、天气、地点合并成一行，居中放在顶栏下面
            var showCaptureHint by remember { mutableStateOf(false) }
            LaunchedEffect(state.capturing) {
                showCaptureHint = state.capturing
                if (state.capturing) {
                    delay(4_000)
                    showCaptureHint = false
                }
            }
            val environmentLine = listOfNotNull(state.weatherText, state.locationText)
                .joinToString(" · ")
            val dateFull = if (state.entryDate != 0L) {
                buildString {
                    append(Dates.fullDate(state.entryDate))
                    append(' ')
                    append(Dates.weekday(state.entryDate))
                    if (state.createdAt > 0) {
                        append(' ')
                        append(Dates.time(state.createdAt))
                    }
                }
            } else {
                null
            }
            val dateNoWeekday = if (state.entryDate != 0L) {
                buildString {
                    append(Dates.fullDate(state.entryDate))
                    if (state.createdAt > 0) {
                        append(' ')
                        append(Dates.time(state.createdAt))
                    }
                }
            } else {
                null
            }
            val lunarPart = Dates.lunar(state.entryDate)
            val environmentPart = environmentLine.ifBlank {
                if (showCaptureHint) "正在记录天气…" else ""
            }.ifBlank { null }

            // 按可用宽度从"信息最全"往后退，保证始终单行。
            // 优先级：有天气时先舍星期（星期最不值钱）；没天气时就没有可舍的了
            val candidates = when {
                dateFull != null -> buildList {
                    // 农历排在天气前面：想省空间先省星期，再省天气地点，最后才省农历
                    if (lunarPart != null && environmentPart != null) {
                        // 先试单行；放不下就整段换行——别让自动折行把"小雨"劈成两半
                        add("$dateFull · $lunarPart · $environmentPart")
                        add("$dateNoWeekday · $lunarPart · $environmentPart")
                        add("$dateFull · $lunarPart\n$environmentPart")
                        add("$dateNoWeekday · $lunarPart\n$environmentPart")
                    }
                    if (lunarPart != null) {
                        add("$dateFull · $lunarPart")
                        add("$dateNoWeekday · $lunarPart")
                    }
                    if (environmentPart != null) {
                        add("$dateFull · $environmentPart")
                        add("$dateNoWeekday · $environmentPart")
                    }
                    add(dateFull)
                    dateNoWeekday?.let(::add)
                }
                environmentPart != null -> listOf(environmentPart)
                lunarPart != null -> listOf(lunarPart)
                else -> emptyList()
            }

            if (candidates.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val available = constraints.maxWidth
                    val headerStyle = MaterialTheme.typography.labelSmall
                    val measurer = rememberTextMeasurer()
                    // 允许折成两行：农历、天气、地点都尽量保留，别为了单行硬砍信息
                    val chosen = remember(candidates, available) {
                        candidates.firstOrNull { text ->
                            // 带换行的候选允许占两行；不带换行的必须真的只占一行，
                            // 否则自动折行会把"小雨"这类词劈开
                            val allowedLines = if ('\n' in text) HEADER_MAX_LINES else 1
                            measurer.measure(
                                text = text,
                                style = headerStyle,
                                constraints = androidx.compose.ui.unit.Constraints(maxWidth = available),
                            ).lineCount <= allowedLines
                        } ?: candidates.last()
                    }
                    Text(
                        text = chosen,
                        style = headerStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = HEADER_MAX_LINES,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp),
                    )
                }
            }

            // 日期行下的发丝线：页眉和正文之间一条安静的分界
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 0.6.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                )
            }

            MoodRow(selected = state.mood, onSelect = viewModel::toggleMood)

            TagRow(
                tags = entryTags,
                onRemove = viewModel::removeTag,
                onAddClick = { showTagDialog = true },
            )

            BasicTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = contentFamily,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (state.title.isEmpty()) {
                        Text(
                            text = "标题（可选）",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = contentFamily),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    innerTextField()
                },
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is BodyBlock.Text -> {
                            val requester = focusRequesters.getOrPut(block.id) { FocusRequester() }
                            BasicTextField(
                                value = block.value,
                                onValueChange = { updated ->
                                    blocks[index] = BodyBlock.Text(block.id, updated)
                                    syncBody()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 22.dp)
                                    .focusRequester(requester)
                                    .onFocusChanged { if (it.isFocused) focusedBlockId = block.id },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    // 17sp / 30sp：中文正文在这个行高下读着最不挤，和 36dp 的横线也还算协调
                                    fontSize = 17.sp,
                                    lineHeight = 30.sp,
                                    letterSpacing = 0.1.sp,
                                    fontFamily = contentFamily,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (block.value.text.isEmpty() && blocks.size == 1) {
                                        Text(
                                            text = "写点什么…",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = contentFamily,
                                            ),
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }

                        is BodyBlock.Image -> ImageBlock(
                            block = block,
                            file = imageFile(block.fileName),
                            onTap = { position ->
                                keyboard?.hide()
                                popupAnchor = position
                                selectedImageId = if (selectedImageId == block.id) null else block.id
                            },
                            onDelete = {
                                blocks.removeAt(index)
                                viewModel.removeImage(block.fileName)
                                syncBody()
                            },
                        )
                    }
                }

            }

        }

        val selectedImage = blocks.firstOrNull { it.id == selectedImageId } as? BodyBlock.Image
        if (selectedImage != null) {
            FloatingImageControls(
                anchor = popupAnchor,
                block = selectedImage,
                onResize = { percent ->
                    val index = blocks.indexOfFirst { it.id == selectedImage.id }
                    if (index >= 0) {
                        blocks[index] = selectedImage.copy(widthPercent = percent)
                        syncBody()
                    }
                },
                onAlign = { align ->
                    val index = blocks.indexOfFirst { it.id == selectedImage.id }
                    if (index >= 0) {
                        blocks[index] = selectedImage.copy(align = align)
                        syncBody()
                    }
                },
                onDismiss = { selectedImageId = null },
            )
        }
    }

    }

    // 保存成功的轻提示。用 Popup 是为了不依赖父布局的 BoxScope，
    // 也不会把编辑器内容重新布局一次。
    if (showSavedHint) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = {},
            properties = PopupProperties(focusable = false),
        ) {
            // 出现时从 0.85 弹到 1，走一点点 spring；消失就直接淡出
            val visible = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(130)) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
                ),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120)),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "已收好",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
            }
        }
    }

    if (showFontPicker) {
        FontPickerDialog(
            currentId = state.fontId,
            store = fontStore,
            onPick = { id ->
                viewModel.setFont(id)
                showFontPicker = false
            },
            onImport = {
                showFontPicker = false
                onExternalUiStart()
                fontPicker.launch(arrayOf("*/*"))
            },
            onDismiss = { showFontPicker = false },
        )
    }

    if (showBackgroundPicker) {
        BackgroundPicker(
            current = state.background,
            onPick = {
                viewModel.setBackground(it)
                showBackgroundPicker = false
            },
            onPickCustom = {
                showBackgroundPicker = false
                onExternalUiStart()
                backgroundPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDismiss = { showBackgroundPicker = false },
        )
    }

    if (showTagDialog) {
        TagDialog(
            allTags = allTags,
            attachedIds = entryTags.map { it.id }.toSet(),
            onToggle = { tag, attached ->
                if (attached) viewModel.removeTag(tag.id) else viewModel.addTag(tag.name)
            },
            onCreate = { name ->
                viewModel.addTag(name)
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条记录？") },
            text = { Text("会先进回收站，暂时还能找回。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(onBack)
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodRow(selected: String?, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Mood.entries.forEach { mood ->
            val isSelected = mood.emoji == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) mood.color.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(1.5.dp, mood.color, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(mood.emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = mood.emoji, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun TagRow(
    tags: List<TagEntity>,
    onRemove: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        items(tags, key = { it.id }) { tag ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onRemove(tag.id) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = tag.name, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Text(text = "×", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item(key = "add") {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAddClick),
            ) {
                Text(
                    text = "＋ 标签",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TagDialog(
    allTags: List<TagEntity>,
    attachedIds: Set<Long>,
    onToggle: (TagEntity, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标签") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    label = { Text("新标签") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "已有标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allTags, key = { it.id }) { tag ->
                            val attached = tag.id in attachedIds
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (attached) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onToggle(tag, attached) },
                            ) {
                                Text(
                                    text = if (attached) "✓ ${tag.name}" else tag.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (input.isNotBlank()) onCreate(input.trim()) else onDismiss() },
            ) { Text(if (input.isNotBlank()) "添加" else "完成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun EditorToolbar(
    onInsertImage: () -> Unit,
    onFont: () -> Unit,
    linesOn: Boolean,
    onToggleLines: () -> Unit,
    onBackground: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
        ) {
            ToolbarItem("图片", ImageIcon, onInsertImage, Modifier.weight(1f))
            ToolbarItem(
                label = "横线",
                icon = RuledLinesIcon,
                onClick = onToggleLines,
                modifier = Modifier.weight(1f),
                // 关掉时用灰色；打开时用主色，一眼能看出状态
                muted = !linesOn,
                active = linesOn,
            )
            ToolbarItem("字体", TextFieldsIcon, onFont, Modifier.weight(1f))
            ToolbarItem("背景", PaletteIcon, onBackground, Modifier.weight(1f))
            ToolbarItem("删除", Icons.Default.Delete, onDelete, Modifier.weight(1f), error = true)
        }
    }
}

@Composable
private fun ToolbarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    muted: Boolean = false,
    active: Boolean = false,
) {
    val tint = when {
        error -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.primary
        muted -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
        )
    }
}

@Composable
private fun FontPickerDialog(
    currentId: String,
    store: FontStore,
    onPick: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val customFiles = remember(store) { store.list() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("正文字体") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "只改这条日记的正文；界面本身仍用手机系统字体。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(8.dp))

                ContentFont.presets.forEach { font ->
                    FontRow(
                        label = font.label,
                        family = font.family(store),
                        selected = currentId == font.id,
                        onClick = { onPick(font.id) },
                    )
                }

                if (customFiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "我的字体",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    customFiles.forEach { file ->
                        val font = ContentFont.custom(file.name, store)
                        FontRow(
                            label = store.displayName(file.name),
                            family = font.family(store),
                            selected = currentId == font.id,
                            onClick = { onPick(font.id) },
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onImport) { Text("＋ 导入字体（ttf / otf）") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun FontRow(
    label: String,
    family: androidx.compose.ui.text.font.FontFamily?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "永和九年，岁在癸丑 Aa",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = family),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BackgroundPicker(
    current: EntryBackground,
    onPick: (EntryBackground) -> Unit,
    onPickCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val previewPattern = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("纸张") },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EntryBackground.presets.forEach { style ->
                        val selected = style.id == current.id
                        PaperSwatch(
                            label = style.label,
                            selected = selected,
                            onClick = { onPick(style) },
                        ) {
                            if (style.isDefault) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.surface),
                                )
                            } else {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(style.color(isDark))
                                        .drawBehind {
                                            // 预览块只有 54dp，把花纹缩小来看得到两三个图案
                                            scale(0.55f, pivot = Offset.Zero) {
                                                drawPaperPattern(style.pattern, previewPattern)
                                            }
                                        },
                                )
                            }
                        }
                    }

                    // 自定义：从相册选一张，直接用成纸张
                    val custom = current as? EntryBackground.Custom
                    PaperSwatch(
                        label = if (custom == null) "自定义" else "换一张",
                        selected = custom != null,
                        onClick = onPickCustom,
                    ) {
                        if (custom != null) {
                            AsyncImage(
                                model = LocalBackgroundFiles.current(custom.fileName),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                        } else {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("＋", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "自定义会铺一层淡色蒙版，保证字能看清",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

@Composable
private fun PaperSwatch(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                ),
            content = content,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImageBlock(
    block: BodyBlock.Image,
    file: File,
    onTap: (Offset) -> Unit,
    onDelete: () -> Unit,
) {
    val fraction = (block.widthPercent / 100f).coerceIn(0.2f, 1f)
    val boxAlign = when (block.align) {
        ImageAlign.LEFT -> Alignment.CenterStart
        ImageAlign.CENTER -> Alignment.Center
        ImageAlign.RIGHT -> Alignment.CenterEnd
    }
    var boxPosition by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        // 整行都可点，并且把点击位置换算成窗口坐标，控制条就弹在手指底下
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boxPosition = it.positionInRoot() }
                .pointerInput(Unit) {
                    detectTapGestures { tap -> onTap(boxPosition + tap) }
                },
            contentAlignment = boxAlign,
        ) {
            Box(modifier = Modifier.fillMaxWidth(fraction)) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.FillWidth,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "×", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 跟手浮层：在点击处弹出，点外面收起。 */
@Composable
private fun FloatingImageControls(
    anchor: Offset,
    block: BodyBlock.Image,
    onResize: (Int) -> Unit,
    onAlign: (ImageAlign) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    // 用「窗口容器尺寸」而不是物理屏幕尺寸：小窗/分屏下窗口比屏幕小得多，
    // 按屏幕算会把浮层放到窗口外面去。
    val container = LocalWindowInfo.current.containerSize
    val barWidth = with(density) { 260.dp.toPx() }
    val barHeight = with(density) { 170.dp.toPx() }
    val screenWidth = container.width.toFloat()
    val screenHeight = container.height.toFloat()
    val margin = with(density) { 12.dp.toPx() }

    val x = anchor.x.coerceIn(margin, (screenWidth - barWidth - margin).coerceAtLeast(margin))
    // 优先落在点击点下方；下面放不下就翻到上方
    val below = anchor.y + margin
    val y = if (below + barHeight + margin < screenHeight) {
        below
    } else {
        (anchor.y - barHeight - margin).coerceAtLeast(margin)
    }

    Popup(
        offset = IntOffset(x.roundToInt(), y.roundToInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "宽度 ${block.widthPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = block.widthPercent.toFloat(),
                    onValueChange = { value -> onResize(value.roundToInt()) },
                    valueRange = 20f..100f,
                    // 20..100 每 5% 一档
                    steps = 15,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ImageAlign.entries.forEach { align ->
                        ControlChip(
                            label = when (align) {
                                ImageAlign.LEFT -> "左"
                                ImageAlign.CENTER -> "中"
                                ImageAlign.RIGHT -> "右"
                            },
                            selected = block.align == align,
                            onClick = { onAlign(align) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private const val HEADER_MAX_LINES = 2
