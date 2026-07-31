package acr.browser.lightning.browser

import acr.browser.lightning.R
import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.compose.CustomView
import acr.browser.lightning.browser.compose.DesktopTabShape
import acr.browser.lightning.browser.compose.TabCountButton
import acr.browser.lightning.browser.compose.sheets.BookmarkAddOrEditSheet
import acr.browser.lightning.browser.compose.sheets.BookmarkFolderRenameSheet
import acr.browser.lightning.browser.compose.sheets.BookmarksBottomSheet
import acr.browser.lightning.browser.compose.sheets.CloseBrowserSheet
import acr.browser.lightning.browser.compose.sheets.DownloadOptionsSheet
import acr.browser.lightning.browser.compose.sheets.LocalFileBlockedSheet
import acr.browser.lightning.browser.compose.sheets.LongPressBookmarkLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressFolderLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressHistoryLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressImageLinkSheet
import acr.browser.lightning.browser.compose.sheets.LongPressLinkSheet
import acr.browser.lightning.browser.compose.sheets.PageToolsSheet
import acr.browser.lightning.browser.compose.sheets.SslInfoSheet
import acr.browser.lightning.browser.menu.MenuSelection
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.compose.BrowserTheme
import acr.browser.lightning.concurrency.StateProvider
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.preview.TopCropTransformation
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.ssl.SslState
import android.widget.FrameLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import kotlinx.coroutines.launch

@Composable
fun ThemableActivity.BrowserScreen(
    tabConfigurationStateProvider: StateProvider<TabConfiguration>,
    blackStatusStateProvider: StateProvider<Boolean>,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    browserFrameLayout: FrameLayout,
    customFrameLayout: FrameLayout,
    suggestionsModel: SuggestionsModel,
) {
    BrowserTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        browserViewState.ephemeral?.let {
            LaunchedEffect(it.message) {
                when (snackbarHostState.showSnackbar(
                    message = it.message,
                    actionLabel = it.actionLabel,
                    duration = SnackbarDuration.Short,
                )) {
                    SnackbarResult.Dismissed -> presenter.onSnackbarDismissed()
                    SnackbarResult.ActionPerformed -> presenter.onSnackbarActionPerformed()
                }
            }
        }
        if (browserViewState.showCustomView) {
            CustomView(
                blackStatusStateProvider,
                browserViewState,
                customFrameLayout,
                snackbarHostState
            )
        } else {
            val tabConfiguration = tabConfigurationStateProvider.state.collectAsState()
            when (tabConfiguration.value) {
                TabConfiguration.DESKTOP -> DesktopTabs(
                    blackStatusStateProvider,
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel,
                    snackbarHostState
                )

                TabConfiguration.DRAWER_SIDE -> DrawerTabs(
                    blackStatusStateProvider,
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel,
                    snackbarHostState
                )

                TabConfiguration.DRAWER_BOTTOM -> BottomTabs(
                    blackStatusStateProvider,
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel,
                    snackbarHostState
                )

                null -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabs(
    blackStatusStateProvider: StateProvider<Boolean>,
    frameLayout: FrameLayout,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(56.dp)
            )
        }
    ) { innerPadding ->
        BrowserStatusBar(
            browserComposeState = browserViewState,
            blackStatusStateProvider = blackStatusStateProvider,
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BookmarksBottomSheet(browserViewState, presenter)
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .weight(1f, false),
            )
            BrowserFindInPage(browserViewState, presenter)
            BottomTabNavigationBar(browserViewState, presenter, suggestionsModel)
            TabsBottomSheet(browserViewState, presenter)
            BrowserDialogs(browserViewState, presenter)
        }
    }
}

@Composable
fun DesktopTabs(
    blackStatusStateProvider: StateProvider<Boolean>,
    frameLayout: FrameLayout,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BrowserStatusBar(
            browserComposeState = browserViewState,
            blackStatusStateProvider = blackStatusStateProvider,
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val heightDp = remember { 92.dp }
            val height = with(LocalDensity.current) {
                remember { heightDp.roundToPx() }
            }

            BookmarksBottomSheet(browserViewState, presenter)
            if (browserViewState.toolbarVisibility == BrowserViewState.ToolbarVisibility.FIXED) {
                TopTabDesktopNavigationBar(
                    height = heightDp,
                    offset = remember { mutableIntStateOf(0) },
                    browserViewState = browserViewState,
                    presenter = presenter,
                    suggestionsModel = suggestionsModel
                )
                BrowserFindInPage(browserViewState, presenter)
                AndroidView(
                    factory = { frameLayout },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceDim)
                        .weight(1f, false),
                )
            } else {
                val currentOffset = remember { mutableIntStateOf(0) }
                AnimateMutableOffset(
                    shouldShow = browserViewState.toolbarVisibility == BrowserViewState.ToolbarVisibility.SHOW,
                    mutableOffset = currentOffset,
                    maxOffset = height
                )
                Box(modifier = Modifier.weight(1f, false)) {
                    TabContainer(
                        offsetDp = heightDp,
                        frameLayout = frameLayout,
                        fixedOffset = height,
                        mutableOffset = currentOffset,
                    )
                    Column {
                        TopTabDesktopNavigationBar(
                            heightDp,
                            currentOffset,
                            browserViewState,
                            presenter,
                            suggestionsModel
                        )
                        BrowserFindInPage(browserViewState, presenter)
                    }
                }
            }

            BrowserDialogs(browserViewState, presenter)
        }
    }
}

@Composable
fun DrawerTabs(
    blackStatusStateProvider: StateProvider<Boolean>,
    frameLayout: FrameLayout,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
    snackbarHostState: SnackbarHostState,
) {
    val lazyListState = rememberLazyListState()
    if (browserViewState.scrollToTab != -1) {
        LaunchedEffect(browserViewState.scrollToTab) {
            lazyListState.scrollToItem(browserViewState.scrollToTab)
            presenter.onTabScroll()
        }
    }

    val desiredDrawerState = if (browserViewState.openTabs) {
        DrawerValue.Open
    } else {
        DrawerValue.Closed
    }
    val drawerState = rememberDrawerState(
        initialValue = desiredDrawerState,
        confirmStateChange = {
            if (it == DrawerValue.Closed) {
                presenter.onTabDrawerMoved(isOpen = false)
            }
            true
        }
    )
    LaunchedEffect(desiredDrawerState) {
        if (drawerState.currentValue != desiredDrawerState &&
            drawerState.targetValue != desiredDrawerState
        ) {
            if (desiredDrawerState == DrawerValue.Open) {
                drawerState.open()
            } else {
                drawerState.close()
            }
        }
    }
    ModalNavigationDrawer(
        gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(horizontal = 16.dp),
                        onClick = { presenter.onTabMenuClick() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_tabs),
                            contentDescription = "more"
                        )
                    }
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.tabs),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f, false),
                    state = lazyListState
                ) {
                    itemsIndexed(
                        items = browserViewState.tabs,
                        key = { _, item -> item.id },
                        contentType = { _, item -> item.isSelected },
                    ) { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null
                                )
                                .combinedClickable(
                                    onClick = { presenter.onTabClick(index) },
                                    onLongClick = { presenter.onTabLongClick(index) }
                                )
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (tab.icon) {
                                TabModel.Favicon.Frozen -> Icon(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(horizontal = 16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    painter = painterResource(R.drawable.ic_frozen),
                                    contentDescription = "test"
                                )

                                is TabModel.Favicon.Icon -> Image(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(horizontal = 16.dp),
                                    bitmap = tab.icon.bitmap,
                                    contentDescription = "test"
                                )

                                TabModel.Favicon.None -> Icon(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(horizontal = 16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    painter = painterResource(R.drawable.ic_webpage),
                                    contentDescription = "test"
                                )
                            }
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .weight(1f, false),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (tab.isSelected) {
                                    FontWeight.Bold
                                } else {
                                    null
                                },
                                text = tab.title
                            )
                            IconButton(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp),
                                onClick = { presenter.onTabClose(index) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.ic_action_delete),
                                    contentDescription = stringResource(R.string.close_tab)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .height(56.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        enabled = browserViewState.isBackEnabled,
                        onClick = { presenter.onBackClick() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_back),
                            contentDescription = ""
                        )
                    }
                    IconButton(
                        enabled = browserViewState.isForwardEnabled,
                        onClick = { presenter.onForwardClick() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_forward),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = { presenter.onHomeClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_home),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = { presenter.onToolsClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_page_tools),
                            contentDescription = ""
                        )
                    }
                    IconButton(
                        enabled = browserViewState.isBookmarkEnabled,
                        onClick = { presenter.onStarClick() }
                    ) {
                        BookmarkIcon(browserViewState.isBookmarked)
                    }
                    IconButton(onClick = { presenter.onNewTabClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_plus),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            BrowserStatusBar(
                browserComposeState = browserViewState,
                blackStatusStateProvider = blackStatusStateProvider,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val heightDp = remember { 56.dp }
                val height = with(LocalDensity.current) {
                    remember { heightDp.roundToPx() }
                }

                BookmarksBottomSheet(browserViewState, presenter)
                if (browserViewState.toolbarVisibility == BrowserViewState.ToolbarVisibility.FIXED) {
                    TopTabNavigationBar(
                        height = heightDp,
                        offset = remember { mutableIntStateOf(0) },
                        browserViewState = browserViewState,
                        presenter = presenter,
                        suggestionsModel = suggestionsModel
                    )
                    BrowserFindInPage(browserViewState, presenter)
                    AndroidView(
                        factory = { frameLayout },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceDim)
                            .weight(1f, false),
                    )
                } else {
                    val currentOffset = remember { mutableIntStateOf(0) }
                    AnimateMutableOffset(
                        shouldShow = browserViewState.toolbarVisibility == BrowserViewState.ToolbarVisibility.SHOW,
                        mutableOffset = currentOffset,
                        maxOffset = height
                    )
                    Box(modifier = Modifier.weight(1f, false)) {
                        TabContainer(
                            offsetDp = heightDp,
                            frameLayout = frameLayout,
                            fixedOffset = height,
                            mutableOffset = currentOffset,
                        )
                        Column {
                            TopTabNavigationBar(
                                heightDp,
                                currentOffset,
                                browserViewState,
                                presenter,
                                suggestionsModel
                            )
                            BrowserFindInPage(browserViewState, presenter)
                        }
                    }
                }
                BrowserDialogs(browserViewState, presenter)
            }
        }
    }
}

@Composable
fun BrowserStatusBar(
    browserComposeState: BrowserComposeState,
    blackStatusStateProvider: StateProvider<Boolean>,
) {
    StatusBar(
        paintSurfaceColor = browserComposeState.toolbarVisibility != BrowserViewState.ToolbarVisibility.FIXED,
        blackStatusStateProvider = blackStatusStateProvider,
    )
}

@Composable
fun StatusBar(
    paintSurfaceColor: Boolean,
    blackStatusStateProvider: StateProvider<Boolean>,
) {
    val blackStatus by blackStatusStateProvider.state.collectAsState()
    if (paintSurfaceColor || blackStatus == true) {
        val topInset = with(LocalDensity.current) {
            val statusBars = WindowInsets.statusBars
            statusBars.getTop(this).toDp()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topInset)
                .zIndex(1F)
                .background(
                    if (blackStatus == true) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
        )
    }
}

@Composable
fun TabContainer(
    frameLayout: FrameLayout,
    offsetDp: Dp,
    fixedOffset: Int,
    mutableOffset: MutableIntState,
) {
    AndroidView(
        factory = { frameLayout },
        modifier = Modifier
            .padding(top = offsetDp * (fixedOffset + mutableOffset.intValue) / fixedOffset.toFloat())
            // TODO: Offset is smoother, but incorrectly includes window insets
            // .offset {
            //     IntOffset(0, fixedOffset + mutableOffset.intValue)
            // }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim),
    )
}

@Composable
fun AnimateMutableOffset(
    shouldShow: Boolean,
    mutableOffset: MutableIntState,
    maxOffset: Int
) {
    LaunchedEffect(shouldShow) {
        if (shouldShow && mutableOffset.intValue != 0) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                mutableOffset.intValue = ((maxOffset * value) + -maxOffset).toInt()
            }
        } else if (!shouldShow && mutableOffset.intValue == 0) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                mutableOffset.intValue = (value * -maxOffset).toInt()
            }
        }
    }
}

@Composable
fun BrowserDialogs(
    browserViewState: BrowserComposeState,
    browserPresenter: BrowserPresenter,
) {
    when (val dialog = browserViewState.dialog) {
        is BrowserViewState.Dialogs.AddBookmark -> BookmarkAddOrEditSheet(
            edit = false,
            title = dialog.title,
            url = dialog.url,
            folder = "",
            folders = dialog.folders,
            presenter = browserPresenter,
            onConfirmed = { title, url, folder ->
                browserPresenter.onBookmarkConfirmed(title, url, folder)
            }
        )

        is BrowserViewState.Dialogs.BookmarkOptions -> LongPressBookmarkLinkSheet(
            browserViewState = browserViewState,
            presenter = browserPresenter,
            onClick = { browserPresenter.onBookmarkOptionClick(dialog.bookmarkOptionsDialog, it) }
        )

        is BrowserViewState.Dialogs.CloseBrowser -> CloseBrowserSheet(
            presenter = browserPresenter,
            onClick = { browserPresenter.onCloseBrowserEvent(dialog.selectedTab, it) }
        )

        is BrowserViewState.Dialogs.DownloadOptions -> DownloadOptionsSheet(
            presenter = browserPresenter,
            onClick = { browserPresenter.onDownloadOptionClick(dialog.downloadOptionsDialog, it) }
        )

        is BrowserViewState.Dialogs.EditBookmark -> BookmarkAddOrEditSheet(
            edit = true,
            title = dialog.title,
            url = dialog.url,
            folder = dialog.folder,
            folders = dialog.folders,
            presenter = browserPresenter,
            onConfirmed = { title, url, folder ->
                browserPresenter.onBookmarkEditConfirmed(title, url, folder)
            }
        )

        is BrowserViewState.Dialogs.EditFolder -> BookmarkFolderRenameSheet(
            oldTitle = dialog.title,
            presenter = browserPresenter,
            onSelected = {
                browserPresenter.onBookmarkFolderRenameConfirmed(
                    dialog.title,
                    it.toString()
                )
            }
        )

        is BrowserViewState.Dialogs.FolderOptions -> LongPressFolderLinkSheet(
            presenter = browserPresenter,
            onClick = { browserPresenter.onFolderOptionClick(dialog.folderOptionsDialog, it) }
        )

        is BrowserViewState.Dialogs.HistoryOptions -> LongPressHistoryLinkSheet(
            browserViewState = browserViewState,
            presenter = browserPresenter,
            onClick = { browserPresenter.onHistoryOptionClick(dialog.historyOptionsDialog, it) }
        )

        is BrowserViewState.Dialogs.ImageLongPress -> LongPressImageLinkSheet(
            browserViewState = browserViewState,
            longPress = dialog.imageLongPressDialog,
            presenter = browserPresenter,
            onClick = { browserPresenter.onImageLongPressEvent(dialog.imageLongPressDialog, it) }
        )

        is BrowserViewState.Dialogs.LinkLongPress -> LongPressLinkSheet(
            browserViewState = browserViewState,
            longPress = dialog.linkLongPressDialog,
            presenter = browserPresenter,
            onClick = { browserPresenter.onLinkLongPressEvent(dialog.linkLongPressDialog, it) }
        )

        BrowserViewState.Dialogs.LocalFileBlocked -> LocalFileBlockedSheet {
            browserPresenter.onConfirmOpenLocalFile(it)
        }

        is BrowserViewState.Dialogs.PageTools -> PageToolsSheet(
            areAdsAllowed = dialog.areAdsAllowed,
            shouldShowAdBlockOption = dialog.shouldShowAdBlockOption,
            presenter = browserPresenter,
        )

        is BrowserViewState.Dialogs.SslInfo -> SslInfoSheet(dialog.sslDialog, browserPresenter)
        null -> Unit // No dialog
    }
}

@Composable
fun BookmarkIcon(
    isBookmarked: Boolean,
) {
    if (isBookmarked) {
        Icon(
            painter = painterResource(R.drawable.ic_bookmark),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = ""
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_action_star),
            contentDescription = ""
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabNavigationBar(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Column(
        modifier = Modifier.height(56.dp)
    ) {
        Box(
            contentAlignment = Alignment.TopCenter
        ) {
            HorizontalDivider()
            BrowserProgressIndicator(browserViewState)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserSearchBar(browserViewState, presenter, suggestionsModel)
            TabCountButton(browserViewState) {
                presenter.onTabCountViewClick()
            }
            BrowserOverflowMenu(presenter, browserViewState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTabNavigationBar(
    height: Dp,
    offset: MutableIntState,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Column(
        modifier = Modifier
            .height(height)
            .offset {
                IntOffset(0, offset.intValue)
            }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabCountButton(browserViewState) {
                presenter.onTabCountViewClick()
            }
            BrowserSearchBar(browserViewState, presenter, suggestionsModel)
            BrowserOverflowMenu(presenter, browserViewState)
        }
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            BrowserProgressIndicator(browserViewState)
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTabDesktopNavigationBar(
    height: Dp,
    offset: MutableIntState,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    val lazyListState = rememberLazyListState()
    if (browserViewState.scrollToTab != -1) {
        LaunchedEffect(browserViewState.scrollToTab) {
            lazyListState.scrollToItem(browserViewState.scrollToTab)
            presenter.onTabScroll()
        }
    }
    Column(
        modifier = Modifier
            .height(height)
            .offset {
                IntOffset(0, offset.intValue)
            }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.scrim, RectangleShape),
            state = lazyListState,
            verticalAlignment = Alignment.CenterVertically,
            overscrollEffect = null,
            horizontalArrangement = Arrangement.spacedBy((-16).dp)
        ) {
            itemsIndexed(
                items = browserViewState.tabs,
                key = { _, item -> item.id },
                contentType = { _, item -> item.isSelected },
            ) { index, tab ->
                Row(
                    modifier = Modifier
                        .width(175.dp)
                        .height(36.dp)
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null
                        )
                        .combinedClickable(
                            onClick = { presenter.onTabClick(index) },
                            onLongClick = { presenter.onTabLongClick(index) }
                        )
                        .zIndex(
                            if (tab.isSelected) {
                                1f
                            } else {
                                0f
                            }
                        )
                        .background(
                            color = if (tab.isSelected) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = DesktopTabShape
                        )
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (tab.icon) {
                        TabModel.Favicon.Frozen -> Icon(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(horizontal = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                            painter = painterResource(R.drawable.ic_frozen),
                            contentDescription = "test"
                        )

                        is TabModel.Favicon.Icon -> Image(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(horizontal = 4.dp),
                            bitmap = tab.icon.bitmap,
                            contentDescription = "test"
                        )

                        TabModel.Favicon.None -> Icon(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(horizontal = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                            painter = painterResource(R.drawable.ic_webpage),
                            contentDescription = "test"
                        )
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, false),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = tab.title
                    )
                    IconButton(
                        modifier = Modifier
                            .size(30.dp),
                        onClick = { presenter.onTabClose(index) }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.ic_action_delete),
                            contentDescription = stringResource(R.string.close_tab)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { presenter.onHomeClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_home),
                    contentDescription = "test"
                )
            }
            BrowserSearchBar(browserViewState, presenter, suggestionsModel)
            BrowserOverflowMenu(presenter, browserViewState)
        }
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            BrowserProgressIndicator(browserViewState)
            HorizontalDivider()
        }
    }
}

@Composable
fun BrowserFindInPage(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
) {
    val findInPage = browserViewState.findInPage ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { presenter.onFindDismiss() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_delete),
                    contentDescription = "test"
                )
            }
            var text by remember { mutableStateOf(findInPage) }
            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .weight(1f, false)
                    .indicatorLine(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource
                    ),
                value = text,
                onValueChange = {
                    text = it
                    presenter.onFindInPage(it)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.action_find),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            IconButton(onClick = { presenter.onFindPrevious() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_collapse),
                    contentDescription = "test"
                )
            }
            IconButton(onClick = { presenter.onFindNext() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_expand),
                    contentDescription = "test"
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun BrowserProgressIndicator(browserViewState: BrowserComposeState) {
    if (browserViewState.progress == 100) {
        Spacer(modifier = Modifier.height(4.dp))
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            trackColor = Color(0x00000000),
            drawStopIndicator = {},
            progress = { browserViewState.progress / 100f }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchSuggestions(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
    searchBarState: SearchBarState,
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(browserViewState.isSearchBarExpanded) {
        if (searchBarState.currentValue == SearchBarValue.Expanded &&
            !browserViewState.isSearchBarExpanded
        ) {
            searchBarState.animateToCollapsed()
            presenter.onSearchBarExpandedOrCollapsed(false)
        } else if (searchBarState.currentValue == SearchBarValue.Collapsed &&
            browserViewState.isSearchBarExpanded
        ) {
            searchBarState.animateToExpanded()
            presenter.onSearchBarExpandedOrCollapsed(true)
        }
    }
    ExpandedFullScreenSearchBar(
        collapsedShape = MaterialTheme.shapes.small,
        state = searchBarState,
        inputField = {
            var state by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = browserViewState.searchQuery,
                        selection = TextRange(
                            0,
                            browserViewState.searchQuery.length
                        ),
                    )
                )
            }
            // Workaround (?) for the on value change not triggering on the initial value
            // Wouldn't be needed if suggestions were piped through presenter state
            LaunchedEffect(null) {
                suggestionsModel.updateQuery(state.text)
            }
            state = state.copy(
                text = browserViewState.searchQuery,
                selection = TextRange(
                    browserViewState.searchQuerySelection.first,
                    browserViewState.searchQuerySelection.second
                )
            )
            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(),
                value = state,
                onValueChange = {
                    // Updating the state mid-animation can cause cursor selection bugs
                    if (searchBarState.targetValue != SearchBarValue.Collapsed) {
                        state = it
                        suggestionsModel.updateQuery(it.text)
                        presenter.onSearchQueryChanged(it.text, it.selection.min, it.selection.max)
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    coroutineScope.launch {
                        searchBarState.animateToCollapsed()
                        presenter.onSearchBarExpandedOrCollapsed(false)
                    }
                    presenter.onSearch(state.text)
                }),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = {
                    Box {
                        if (state.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        it()
                    }
                }
            )
        }
    ) {
        val suggestions = suggestionsModel.results().collectAsState(emptyList())
        suggestions.value.forEach {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        presenter.onSearchSuggestionClicked(it)
                        coroutineScope.launch {
                            searchBarState.animateToCollapsed()
                            presenter.onSearchBarExpandedOrCollapsed(false)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val resource = when (it) {
                        is Bookmark -> R.drawable.ic_bookmark
                        is HistoryEntry -> R.drawable.ic_history
                        is SearchSuggestion -> R.drawable.ic_search
                    }
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(resource),
                        contentDescription = "test"
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                            .weight(1f, false)
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.titleMedium,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Text(
                            text = it.url,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = {
                        presenter.onSearchSuggestionInsertClicked(it)
                    }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_insert),
                            contentDescription = "test"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.BrowserSearchBar(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    val searchBarState = rememberSearchBarState()
    SearchBar(
        shape = MaterialTheme.shapes.small,
        state = searchBarState,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(40.dp)
            .weight(1f, false),
        inputField = {
            BrowserSearchBarInputField(browserViewState, presenter, searchBarState)
        }
    )
    BrowserSearchSuggestions(browserViewState, presenter, suggestionsModel, searchBarState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchBarInputField(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    searchBarState: SearchBarState,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    searchBarState.animateToExpanded()
                    presenter.onSearchBarExpandedOrCollapsed(true)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (browserViewState.sslState) {
            is SslState.Invalid -> IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { presenter.onSslIconClick() }
            ) {
                Icon(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp),
                    tint = null,
                    painter = painterResource(R.drawable.ic_unsecured),
                    contentDescription = "SSL Cert is Invalid"
                )
            }

            SslState.None -> if (browserViewState.displayUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.padding(start = 16.dp))
            }

            SslState.Valid -> IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { presenter.onSslIconClick() }
            ) {
                Icon(
                    modifier = Modifier.padding(6.dp),
                    tint = null,
                    painter = painterResource(R.drawable.ic_secured),
                    contentDescription = "SSL Cert is Valid"
                )
            }
        }
        if (browserViewState.displayUrl.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f, false),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                text = browserViewState.displayUrl,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(onClick = { presenter.onRefreshOrStopClick() }) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = when (browserViewState.isRefresh) {
                    true -> painterResource(R.drawable.ic_action_refresh)
                    false -> painterResource(R.drawable.ic_action_delete)
                },
                contentDescription = "refresh"
            )
        }
    }
}

@Composable
fun BrowserOverflowMenu(presenter: BrowserPresenter, browserViewState: BrowserComposeState) {
    Box {
        var dropDownExpanded by remember { mutableStateOf(false) }
        IconButton(onClick = {
            dropDownExpanded = !dropDownExpanded
        }) {
            Icon(
                painter = painterResource(R.drawable.more),
                contentDescription = "more"
            )
        }
        DropdownMenu(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .defaultMinSize(minWidth = 175.dp),
            shape = MaterialTheme.shapes.small,
            properties = PopupProperties(focusable = true, clippingEnabled = false),
            expanded = dropDownExpanded,
            onDismissRequest = { dropDownExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_tab)) },
                onClick = {
                    presenter.onMenuClick(MenuSelection.NEW_TAB)
                    dropDownExpanded = false
                }
            )
            if (!browserViewState.isIncognito) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_incognito)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.NEW_INCOGNITO_TAB)
                        dropDownExpanded = false
                    }
                )
            }
            if (browserViewState.enableFullMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.SHARE)
                        dropDownExpanded = false
                    }
                )
            }
            if (!browserViewState.isIncognito) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_history)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.HISTORY)
                        dropDownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_downloads)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.DOWNLOADS)
                        dropDownExpanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_find)) },
                onClick = {
                    presenter.onMenuClick(MenuSelection.FIND)
                    dropDownExpanded = false
                }
            )
            if (browserViewState.enableFullMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.COPY_LINK)
                        dropDownExpanded = false
                    }
                )
                if (!browserViewState.isIncognito) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_homescreen)) },
                        onClick = {
                            presenter.onMenuClick(MenuSelection.ADD_TO_HOME)
                            dropDownExpanded = false
                        }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_bookmarks)) },
                onClick = {
                    presenter.onMenuClick(MenuSelection.BOOKMARKS)
                    dropDownExpanded = false
                }
            )
            if (browserViewState.enableFullMenu && !browserViewState.isIncognito) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_bookmark)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.ADD_BOOKMARK)
                        dropDownExpanded = false
                    }
                )
            }
            if (!browserViewState.isIncognito) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.SETTINGS)
                        dropDownExpanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsBottomSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
) {
    val lazyListState = rememberLazyListState()
    if (browserViewState.scrollToTab != -1) {
        LaunchedEffect(browserViewState.scrollToTab) {
            lazyListState.scrollToItem(browserViewState.scrollToTab)
            presenter.onTabScroll()
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(browserViewState.openTabs) }
    if (showBottomSheet != browserViewState.openTabs) {
        if (showBottomSheet) {
            LaunchedEffect(null) {
                sheetState.hide()
                showBottomSheet = false
            }
        } else {
            showBottomSheet = true
            LaunchedEffect(null) {
                sheetState.show()
            }
        }
    }
    if (!showBottomSheet) return
    ModalBottomSheet(
        dragHandle = {},
        sheetState = sheetState,
        onDismissRequest = { presenter.onTabDrawerMoved(false) }
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                enabled = browserViewState.isBackEnabled,
                onClick = { presenter.onBackClick() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_back),
                    contentDescription = ""
                )
            }
            IconButton(
                enabled = browserViewState.isForwardEnabled,
                onClick = { presenter.onForwardClick() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_forward),
                    contentDescription = ""
                )
            }
            IconButton(onClick = { presenter.onHomeClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_home),
                    contentDescription = ""
                )
            }
            IconButton(onClick = { presenter.onToolsClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_page_tools),
                    contentDescription = ""
                )
            }
            IconButton(
                enabled = browserViewState.isBookmarkEnabled,
                onClick = { presenter.onStarClick() }
            ) {
                BookmarkIcon(browserViewState.isBookmarked)
            }
            IconButton(onClick = { presenter.onNewTabClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_plus),
                    contentDescription = ""
                )
            }
        }
        LazyRow(
            modifier = Modifier.height(200.dp),
            state = lazyListState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = browserViewState.tabs,
                key = { _, item -> item.id },
                contentType = { _, item -> item.isSelected },
            ) { index, tab ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .combinedClickable(
                            onClick = { presenter.onTabClick(index) },
                            onLongClick = { presenter.onTabLongClick(index) }
                        )
                        .optionalBorder(tab.isSelected)
                        .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (tab.icon) {
                            TabModel.Favicon.Frozen -> Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                painter = painterResource(R.drawable.ic_frozen),
                                contentDescription = "test"
                            )

                            is TabModel.Favicon.Icon -> Image(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 4.dp),
                                bitmap = tab.icon.bitmap,
                                contentDescription = "test"
                            )

                            TabModel.Favicon.None -> Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                painter = painterResource(R.drawable.ic_webpage),
                                contentDescription = "test"
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, false),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = tab.title
                        )
                        IconButton(
                            modifier = Modifier
                                .size(30.dp),
                            onClick = { presenter.onTabClose(index) }) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.ic_action_delete),
                                contentDescription = stringResource(R.string.close_tab)
                            )
                        }
                    }
                    val transformations = with(LocalDensity.current) {
                        remember {
                            listOf(
                                TopCropTransformation,
                                RoundedCornersTransformation(8.dp.toPx())
                            )
                        }
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(tab.preview.first)
                            .memoryCacheKey("${tab.preview.second}-${tab.preview.first}")
                            .transformations(transformations)
                            .build(),
                        contentDescription = "test",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .weight(1f, false),
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.optionalBorder(apply: Boolean): Modifier {
    if (apply) {
        return border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium
        )
    }
    return this
}


