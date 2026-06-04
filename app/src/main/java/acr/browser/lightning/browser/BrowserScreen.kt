package acr.browser.lightning.browser

import acr.browser.lightning.BrowserScreenState
import acr.browser.lightning.R
import acr.browser.lightning.browser.menu.MenuSelection
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.compose.AppTheme
import acr.browser.lightning.compose.StateProvider
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.ssl.SslState
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DrawerState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.tan

@Composable
fun BrowserScreen(
    tabConfigurationStateProvider: StateProvider<TabConfiguration>,
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    frameLayout: FrameLayout,
    suggestionsModel: SuggestionsModel,
) {
    AppTheme {
        val tabConfiguration = tabConfigurationStateProvider.state.collectAsState()
        when (tabConfiguration.value) {
            TabConfiguration.DESKTOP -> DesktopTabs(
                frameLayout,
                browserScreenState,
                presenter,
                suggestionsModel
            )

            TabConfiguration.DRAWER_SIDE -> DrawerTabs(
                frameLayout,
                browserScreenState,
                presenter,
                suggestionsModel
            )

            TabConfiguration.DRAWER_BOTTOM -> BottomTabs(
                frameLayout,
                browserScreenState,
                presenter,
                suggestionsModel
            )

            null -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabs(
    frameLayout: FrameLayout,
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BookmarksBottomSheet(browserScreenState, presenter)
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .weight(1f, false),
            )
            BrowserFindInPage(browserScreenState, presenter)
            BottomTabNavigationBar(browserScreenState, presenter, suggestionsModel)
            TabsBottomSheet(browserScreenState, presenter)
        }
    }
}

@Composable
fun DesktopTabs(
    frameLayout: FrameLayout,
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BookmarksBottomSheet(browserScreenState, presenter)
            TopTabDesktopNavigationBar(browserScreenState, presenter, suggestionsModel)
            BrowserFindInPage(browserScreenState, presenter)
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .weight(1f, false),
            )
        }
    }
}

@Composable
fun DrawerTabs(
    frameLayout: FrameLayout,
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    val lazyListState = rememberLazyListState()
    val drawerState = rememberDrawerState(
        initialValue = if (browserScreenState.openTabs) {
            DrawerValue.Open
        } else {
            DrawerValue.Closed
        }
    )
    ModalNavigationDrawer(
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    itemsIndexed(browserScreenState.tabState) { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { presenter.onTabClick(index) }
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tab.icon?.let {
                                Image(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(horizontal = 16.dp),
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "test"
                                )
                            } ?: Icon(
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(horizontal = 16.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                painter = painterResource(R.drawable.ic_webpage),
                                contentDescription = "test"
                            )
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .height(56.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { presenter.onBackClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_back),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = { presenter.onForwardClick() }) {
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
                    IconButton(onClick = { presenter.onStarClick() }) {
                        BookmarkIcon(browserScreenState.browserViewState.isBookmarked)
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
        Scaffold { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                BookmarksBottomSheet(browserScreenState, presenter)
                TopTabNavigationBar(browserScreenState, drawerState, presenter, suggestionsModel)
                BrowserFindInPage(browserScreenState, presenter)
                AndroidView(
                    factory = { frameLayout },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceDim)
                        .weight(1f, false),
                )
            }
        }
    }
}

@Composable
fun BookmarkIcon(isBookmarked: Boolean) {
    Icon(
        painter = painterResource(
            if (isBookmarked) {
                R.drawable.ic_bookmark
            } else {
                R.drawable.ic_action_star
            }
        ),
        tint = if (isBookmarked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        contentDescription = ""
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabNavigationBar(
    browserScreenState: BrowserScreenState,
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
            BrowserProgressIndicator(browserScreenState)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserSearchBar(browserScreenState, presenter, suggestionsModel)
            TabCountButton(browserScreenState.tabState.size) {
                presenter.onTabCountViewClick()
            }
            BrowserOverflowMenu(presenter, browserScreenState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTabNavigationBar(
    browserScreenState: BrowserScreenState,
    drawerState: DrawerState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Column(
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coroutineScope = rememberCoroutineScope()
            TabCountButton(browserScreenState.tabState.size) {
                if (drawerState.isAnimationRunning) return@TabCountButton
                // TODO: Figure out how to do this more like bottom sheet modal
                coroutineScope.launch {
                    if (drawerState.isOpen) {
                        drawerState.close()
                    } else {
                        drawerState.open()
                    }
                    presenter.onTabCountViewClick()
                }
            }
            BrowserSearchBar(browserScreenState, presenter, suggestionsModel)
            BrowserOverflowMenu(presenter, browserScreenState)
        }
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            BrowserProgressIndicator(browserScreenState)
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTabDesktopNavigationBar(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    val lazyListState = rememberLazyListState()
    Column(
        modifier = Modifier.height(92.dp)
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
            itemsIndexed(browserScreenState.tabState) { index, tab ->
                Row(
                    modifier = Modifier
                        .width(175.dp)
                        .height(36.dp)
                        .clickable { presenter.onTabClick(index) }
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
                            shape = TabBackground
                        )
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tab.icon?.let {
                        Image(
                            modifier = Modifier
                                .size(28.dp)
                                .padding(horizontal = 4.dp),
                            bitmap = it.asImageBitmap(),
                            contentDescription = "test"
                        )
                    } ?: Icon(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(horizontal = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                        painter = painterResource(R.drawable.ic_webpage),
                        contentDescription = "test"
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, false),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // TODO: Condensed
                        fontFamily = FontFamily.SansSerif,
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
            BrowserSearchBar(browserScreenState, presenter, suggestionsModel)
            BrowserOverflowMenu(presenter, browserScreenState)
        }
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            BrowserProgressIndicator(browserScreenState)
            HorizontalDivider()
        }
    }
}

@Composable
fun BrowserFindInPage(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
) {
    val findInPage = browserScreenState.browserViewState.findInPage ?: return
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
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                interactionSource = interactionSource,
                decorationBox = {
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.action_find),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        it()
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
fun BrowserProgressIndicator(browserScreenState: BrowserScreenState) {
    if (browserScreenState.browserViewState.progress == 100) {
        Spacer(modifier = Modifier.height(4.dp))
    } else {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            trackColor = Color(0x00000000),
            drawStopIndicator = {},
            progress = { browserScreenState.browserViewState.progress / 100f }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchSuggestions(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
    searchBarState: SearchBarState,
) {
    val coroutineScope = rememberCoroutineScope()
    ExpandedFullScreenSearchBar(
        collapsedShape = MaterialTheme.shapes.small,
        state = searchBarState,
        inputField = {
            var state by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = browserScreenState.browserViewState.displayUrl,
                        selection = TextRange(
                            0,
                            browserScreenState.browserViewState.displayUrl.length
                        ),
                    )
                )
            }
            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(),
                value = state,
                onValueChange = {
                    state = it
                    suggestionsModel.updateQuery(it.text)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    coroutineScope.launch {
                        searchBarState.animateToCollapsed()
                    }
                    presenter.onSearch(state.text)
                }),
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
                        // TODO: insert
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
    browserScreenState: BrowserScreenState,
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
            BrowserSearchBarInputField(browserScreenState, presenter, searchBarState)
        }
    )
    BrowserSearchSuggestions(browserScreenState, presenter, suggestionsModel, searchBarState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchBarInputField(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
    searchBarState: SearchBarState,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    println("test")
                    searchBarState.animateToExpanded()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (browserScreenState.browserViewState.sslState) {
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

            SslState.None -> Unit
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
        if (browserScreenState.browserViewState.displayUrl.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f, false),
                maxLines = 1,
                overflow = TextOverflow.Clip,
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
                text = browserScreenState.browserViewState.displayUrl,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(onClick = { presenter.onRefreshOrStopClick() }) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = when (browserScreenState.browserViewState.isRefresh) {
                    true -> painterResource(R.drawable.ic_action_refresh)
                    false -> painterResource(R.drawable.ic_action_delete)
                },
                contentDescription = "refresh"
            )
        }
    }
}

@Composable
fun BrowserOverflowMenu(presenter: BrowserPresenter, browserScreenState: BrowserScreenState) {
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
            modifier = Modifier.align(Alignment.BottomEnd),
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_incognito)) },
                onClick = {
                    presenter.onMenuClick(MenuSelection.NEW_INCOGNITO_TAB)
                    dropDownExpanded = false
                }
            )
            if (browserScreenState.browserViewState.enableFullMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.SHARE)
                        dropDownExpanded = false
                    }
                )
            }
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_find)) },
                onClick = {
                    presenter.onMenuClick(MenuSelection.FIND)
                    dropDownExpanded = false
                }
            )
            if (browserScreenState.browserViewState.enableFullMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.COPY_LINK)
                        dropDownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_to_homescreen)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.ADD_TO_HOME)
                        dropDownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_bookmarks)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.BOOKMARKS)
                        dropDownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_bookmark)) },
                    onClick = {
                        presenter.onMenuClick(MenuSelection.ADD_BOOKMARK)
                        dropDownExpanded = false
                    }
                )
            }
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

@Composable
fun TabCountButton(count: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        val color = MaterialTheme.colorScheme.onSurface
        val text: String = if (count > 99) {
            stringResource(R.string.infinity)
        } else {
            // TODO: Preformat using NumberFormat
            count.toString()
        }
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold
        )
        Canvas(Modifier.size(24.dp)) {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            val textLayout = textMeasurer.measure(style = textStyle, text = text)
            val textWidth = textLayout.size.width
            val textHeight = textLayout.size.height
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = textStyle,
                topLeft = Offset(
                    12.dp.toPx() - textWidth / 2,
                    12.dp.toPx() - textHeight / 2
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsBottomSheet(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
) {
    val lazyListState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(browserScreenState.openTabs) }
    if (showBottomSheet != browserScreenState.openTabs) {
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
                .height(56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { presenter.onBackClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_back),
                    contentDescription = ""
                )
            }
            IconButton(onClick = { presenter.onForwardClick() }) {
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
            IconButton(onClick = { presenter.onStarClick() }) {
                BookmarkIcon(browserScreenState.browserViewState.isBookmarked)
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
            itemsIndexed(browserScreenState.tabState) { index, tab ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { presenter.onTabClick(index) }
                        .optionalBorder(tab.isSelected)
                        .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tab.icon?.let {
                            Image(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 4.dp),
                                bitmap = it.asImageBitmap(),
                                contentDescription = "test"
                            )
                        } ?: Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(horizontal = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                            painter = painterResource(R.drawable.ic_webpage),
                            contentDescription = "test"
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, false),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // TODO: Condensed
                            fontFamily = FontFamily.SansSerif,
                            text = tab.title
                        )
                        IconButton(
                            modifier = Modifier
                                .size(30.dp),
                            onClick = { presenter.onTabClose(index) }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.ic_action_delete),
                                contentDescription = stringResource(R.string.close_tab)
                            )
                        }
                    }
                    // TODO: Get image to reload on tab updates
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(tab.preview.first)
                            .build(),
                        placeholder = null,
                        contentDescription = "test",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxSize()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    browserScreenState: BrowserScreenState,
    presenter: BrowserPresenter,
) {
    if (!browserScreenState.openBookmarks) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { presenter.onBookmarkDrawerMoved(false) }
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.action_bookmarks),
                style = MaterialTheme.typography.titleLarge
            )
        }
        LazyColumn {
            itemsIndexed(browserScreenState.browserViewState.bookmarks) { index, bookmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { presenter.onBookmarkClick(index) }
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TODO: Use real icon
                    Icon(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(horizontal = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                        painter = painterResource(R.drawable.ic_webpage),
                        contentDescription = "test"
                    )
                    Text(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .fillMaxWidth()
                            .weight(1f, false),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = bookmark.title
                    )
                }
            }
        }
    }
}

val TabBackground: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radians = Math.PI / 3
        val base = (size.height / tan(radians)).toInt()

        return Outline.Generic(
            Path().apply {
                reset()
                moveTo(0f, size.height)
                lineTo(size.width, size.height)
                lineTo((size.width - base), 0f)
                lineTo(base.toFloat(), 0f)
                close()
            }
        )
    }

}

