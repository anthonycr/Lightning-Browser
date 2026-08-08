package acr.browser.lightning.browser.compose

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.browser.compose.sheets.BookmarksBottomSheet
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.search.SuggestionsModel
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DesktopTabs(
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
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
            useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
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
                    AnimatableTabContainer(
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
            presenter.onEvent(BrowserUiEvent.TabScroll)
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
                            onClick = { presenter.onEvent(BrowserUiEvent.TabClick(index)) },
                            onLongClick = { presenter.onEvent(BrowserUiEvent.TabLongClick(index)) }
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
                        onClick = { presenter.onEvent(BrowserUiEvent.TabClose(index)) }
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
            IconButton(onClick = { presenter.onEvent(BrowserUiEvent.HomeClick) }) {
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
