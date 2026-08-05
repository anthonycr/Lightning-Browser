package acr.browser.lightning.browser.compose

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.compose.sheets.BookmarksBottomSheet
import acr.browser.lightning.browser.tab.TabModel
import acr.browser.lightning.preview.TopCropTransformation
import acr.browser.lightning.search.SuggestionsModel
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabs(
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
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
            useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
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
                        model = when (tab.preview) {
                            is TabModel.Preview.Image -> ImageRequest.Builder(LocalContext.current)
                                .data(tab.preview.path)
                                .memoryCacheKey("${tab.preview.time}-${tab.preview.path}")
                                .transformations(transformations)
                                .build()

                            TabModel.Preview.None -> null
                        },
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
