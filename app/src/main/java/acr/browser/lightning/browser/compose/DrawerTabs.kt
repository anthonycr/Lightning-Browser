package acr.browser.lightning.browser.compose

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.browser.compose.sheets.BookmarksBottomSheet
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.tab.TabModel
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DrawerTabs(
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
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
                useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
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
                        AnimatableTabContainer(
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
