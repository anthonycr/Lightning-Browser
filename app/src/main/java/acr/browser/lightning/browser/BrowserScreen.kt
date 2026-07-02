package acr.browser.lightning.browser

import acr.browser.lightning.R
import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.menu.MenuSelection
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.view.targetUrl.LongPress
import acr.browser.lightning.compose.BrowserTheme
import acr.browser.lightning.compose.StateProvider
import acr.browser.lightning.constant.HTTP
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.preview.TopCropTransformation
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil3.Canvas
import coil3.Image
import coil3.compose.AsyncImage
import coil3.compose.ImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import kotlinx.coroutines.launch
import kotlin.math.tan

@Composable
fun ThemableActivity.BrowserScreen(
    tabConfigurationStateProvider: StateProvider<TabConfiguration>,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    browserFrameLayout: FrameLayout,
    customFrameLayout: FrameLayout,
    suggestionsModel: SuggestionsModel,
) {
    BrowserTheme(isIncognito = browserViewState.isIncognito) {
        if (browserViewState.showCustomView) {
            CustomView(customFrameLayout)
        } else {
            val tabConfiguration = tabConfigurationStateProvider.state.collectAsState()
            when (tabConfiguration.value) {
                TabConfiguration.DESKTOP -> DesktopTabs(
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel
                )

                TabConfiguration.DRAWER_SIDE -> DrawerTabs(
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel
                )

                TabConfiguration.DRAWER_BOTTOM -> BottomTabs(
                    browserFrameLayout,
                    browserViewState,
                    presenter,
                    suggestionsModel
                )

                null -> Unit
            }
        }
    }
}

@Composable
fun CustomView(
    frameLayout: FrameLayout
) {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabs(
    frameLayout: FrameLayout,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Scaffold { innerPadding ->
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
    frameLayout: FrameLayout,
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    suggestionsModel: SuggestionsModel,
) {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BookmarksBottomSheet(browserViewState, presenter)
            TopTabDesktopNavigationBar(browserViewState, presenter, suggestionsModel)
            BrowserFindInPage(browserViewState, presenter)
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .weight(1f, false),
            )
            BrowserDialogs(browserViewState, presenter)
        }
    }
}

@Composable
fun DrawerTabs(
    frameLayout: FrameLayout,
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
    val drawerState = rememberDrawerState(
        initialValue = if (browserViewState.openTabs) {
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
                    itemsIndexed(
                        items = browserViewState.tabs,
                        key = { _, item -> item.id },
                        contentType = { _, item -> item.isSelected },
                    ) { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .combinedClickable(
                                    onClick = { presenter.onTabClick(index) },
                                    onLongClick = { presenter.onTabLongClick(index) }
                                )
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tab.icon?.let {
                                Image(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(horizontal = 16.dp),
                                    bitmap = it,
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
        Scaffold { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                BookmarksBottomSheet(browserViewState, presenter)
                TopTabNavigationBar(browserViewState, drawerState, presenter, suggestionsModel)
                BrowserFindInPage(browserViewState, presenter)
                AndroidView(
                    factory = { frameLayout },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceDim)
                        .weight(1f, false),
                )
                BrowserDialogs(browserViewState, presenter)
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
        is BrowserViewState.Dialogs.AddBookmark -> TODO()
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

        is BrowserViewState.Dialogs.EditBookmark -> TODO()
        is BrowserViewState.Dialogs.EditFolder -> TODO()
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

        BrowserViewState.Dialogs.LocalFileBlocked -> TODO()
        is BrowserViewState.Dialogs.PageTools -> TODO()
        is BrowserViewState.Dialogs.SslInfo -> SslInfoSheet(dialog.sslDialog, browserPresenter)
        null -> Unit // No dialog
    }
}

@Composable
fun DownloadOptionsSheet(
    presenter: BrowserPresenter,
    onClick: (BrowserContract.DownloadOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_downloads),
        items = listOf(
            DialogItem(title = R.string.dialog_delete_all_downloads) {
                onClick(BrowserContract.DownloadOptionEvent.DELETE_ALL)
            },
            DialogItem(title = R.string.dialog_delete_download) {
                onClick(BrowserContract.DownloadOptionEvent.DELETE)
            },
        ),
        presenter = presenter
    )
}

@Composable
fun CloseBrowserSheet(
    presenter: BrowserPresenter,
    onClick: (BrowserContract.CloseTabEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.dialog_title_close_browser),
        items = listOf(
            DialogItem(title = R.string.close_tab) {
                onClick(BrowserContract.CloseTabEvent.CLOSE_CURRENT)
            },
            DialogItem(title = R.string.close_other_tabs) {
                onClick(BrowserContract.CloseTabEvent.CLOSE_OTHERS)
            },
            DialogItem(title = R.string.close_all_tabs) {
                onClick(BrowserContract.CloseTabEvent.CLOSE_ALL)
            },
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressImageLinkSheet(
    browserViewState: BrowserComposeState,
    longPress: LongPress,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.ImageLongPressEvent) -> Unit
) {
    ListItemSheet(
        title = longPress.targetUrl?.replace(HTTP, "").orEmpty(),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.ImageLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.ImageLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.ImageLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.ImageLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.ImageLongPressEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_download_image) {
                onClick(BrowserContract.ImageLongPressEvent.DOWNLOAD)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressLinkSheet(
    browserViewState: BrowserComposeState,
    longPress: LongPress,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.LinkLongPressEvent) -> Unit
) {
    ListItemSheet(
        title = longPress.targetUrl?.replace(HTTP, "").orEmpty(),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.LinkLongPressEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.LinkLongPressEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.LinkLongPressEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.LinkLongPressEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.LinkLongPressEvent.COPY_LINK)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressFolderLinkSheet(
    presenter: BrowserPresenter,
    onClick: (BrowserContract.FolderOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_folder),
        items = listOf(
            DialogItem(title = R.string.dialog_rename_folder) {
                onClick(BrowserContract.FolderOptionEvent.RENAME)
            },
            DialogItem(title = R.string.dialog_remove_folder) {
                onClick(BrowserContract.FolderOptionEvent.REMOVE)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressBookmarkLinkSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.BookmarkOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_bookmarks),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.BookmarkOptionEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.BookmarkOptionEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.BookmarkOptionEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.BookmarkOptionEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.BookmarkOptionEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_remove_bookmark) {
                onClick(BrowserContract.BookmarkOptionEvent.REMOVE)
            },
            DialogItem(title = R.string.dialog_edit_bookmark) {
                onClick(BrowserContract.BookmarkOptionEvent.EDIT)
            }
        ),
        presenter = presenter
    )
}

@Composable
fun LongPressHistoryLinkSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
    onClick: (BrowserContract.HistoryOptionEvent) -> Unit
) {
    ListItemSheet(
        title = stringResource(R.string.action_history),
        items = listOf(
            DialogItem(title = R.string.dialog_open_new_tab) {
                onClick(BrowserContract.HistoryOptionEvent.NEW_TAB)
            },
            DialogItem(title = R.string.dialog_open_background_tab) {
                onClick(BrowserContract.HistoryOptionEvent.BACKGROUND_TAB)
            },
            DialogItem(
                title = R.string.dialog_open_incognito_tab,
                isConditionMet = !browserViewState.isIncognito
            ) {
                onClick(BrowserContract.HistoryOptionEvent.INCOGNITO_TAB)
            },
            DialogItem(title = R.string.action_share) {
                onClick(BrowserContract.HistoryOptionEvent.SHARE)
            },
            DialogItem(title = R.string.dialog_copy_link) {
                onClick(BrowserContract.HistoryOptionEvent.COPY_LINK)
            },
            DialogItem(title = R.string.dialog_remove_from_history) {
                onClick(BrowserContract.HistoryOptionEvent.REMOVE)
            }
        ),
        presenter = presenter
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItemSheet(
    title: String,
    items: List<DialogItem>,
    presenter: BrowserPresenter,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { presenter.onDialogDismissed() }
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column {
            items.forEach { item ->
                if (item.isConditionMet) {
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth()
                            .clickable(onClick = { item.onClick() }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(16.dp),
                            text = stringResource(item.title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslInfoSheet(
    sslCertificateInfo: SslCertificateInfo,
    presenter: BrowserPresenter,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { presenter.onDialogDismissed() }
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = when (sslCertificateInfo.sslState) {
                        is SslState.Invalid -> painterResource(R.drawable.ic_unsecured)
                        SslState.None -> error("This icon shouldn't show")
                        SslState.Valid -> painterResource(R.drawable.ic_secured)
                    },
                    tint = null,
                    contentDescription = "test"
                )
            }
            Text(
                text = sslCertificateInfo.issuedToCommonName,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ssl_info_issued_by),
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = sslCertificateInfo.issuedByCommonName)

            Text(
                text = stringResource(R.string.ssl_info_issued_to),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = sslCertificateInfo.issuedToOrganizationName?.takeIf { it.isNotBlank() }
                    ?: sslCertificateInfo.issuedToCommonName
            )

            val dateFormat = with(LocalContext.current) {
                remember { DateFormat.getDateFormat(this) }
            }

            Text(
                text = stringResource(R.string.ssl_info_issued_on),
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = dateFormat.format(sslCertificateInfo.issueDate))

            Text(
                text = stringResource(R.string.ssl_info_expires_on),
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = dateFormat.format(sslCertificateInfo.expireDate))
        }
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
    browserViewState: BrowserComposeState,
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
            TabCountButton(browserViewState) {
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
            itemsIndexed(
                items = browserViewState.tabs,
                key = { _, item -> item.id },
                contentType = { _, item -> item.isSelected },
            ) { index, tab ->
                Row(
                    modifier = Modifier
                        .width(175.dp)
                        .height(36.dp)
                        .animateItem()
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
                            bitmap = it,
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
                    // Maybe TODO: selection switches to end when suggestions are dismissed
                    state = it
                    suggestionsModel.updateQuery(it.text)
                    presenter.onSearchQueryChanged(it.text, it.selection.min, it.selection.max)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    coroutineScope.launch {
                        searchBarState.animateToCollapsed()
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

@Composable
fun TabCountButton(browserViewState: BrowserComposeState, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        val color = MaterialTheme.colorScheme.onSurface
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = color,
            fontWeight = FontWeight.Bold
        )
        Canvas(Modifier.size(24.dp)) {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            val textLayout = textMeasurer.measure(
                style = textStyle,
                text = browserViewState.tabCountText
            )
            val textWidth = textLayout.size.width
            val textHeight = textLayout.size.height
            drawText(
                textMeasurer = textMeasurer,
                text = browserViewState.tabCountText,
                style = textStyle,
                topLeft = Offset(
                    12.dp.toPx() - textWidth / 2,
                    12.dp.toPx() - textHeight / 2
                )
            )
        }
    }
}

class LetterImage(
    private val textSize: Float,
    private val radius: Float,
    private val character: Char,
    override val width: Int,
    override val height: Int,
    private val color: Int,
) : Image {

    private val paint = Paint().apply {
        color = this@LetterImage.color
        val boldText = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        typeface = boldText
        textSize = this@LetterImage.textSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override val size: Long = 0
    override val shareable: Boolean = false

    override fun draw(canvas: Canvas) {
        val outer = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(outer, radius, radius, paint)

        val xPos = (width / 2)
        val yPos = ((height / 2) - ((paint.descent() + paint.ascent()) / 2)).toInt()

        paint.color = android.graphics.Color.WHITE
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawText(character.toString(), xPos.toFloat(), yPos.toFloat(), paint)
    }

    companion object {
        @Composable
        fun create(
            density: Density,
            character: Char,
            width: Int,
            height: Int,
        ) = LetterImage(
            textSize = with(density) { 14.sp.toPx() },
            radius = with(density) { 6.dp.toPx() },
            character = character,
            color = when (character.code % 4) {
                0 -> colorResource(R.color.bookmark_default_blue)
                1 -> colorResource(R.color.bookmark_default_green)
                2 -> colorResource(R.color.bookmark_default_red)
                3 -> colorResource(R.color.bookmark_default_orange)
                else -> error("Impossible result from modulus 4")
            }.toArgb(),
            width = width,
            height = height,
        )
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
                        .animateItem()
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
                        tab.icon?.let {
                            Image(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 4.dp),
                                bitmap = it,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    browserViewState: BrowserComposeState,
    presenter: BrowserPresenter,
) {
    if (!browserViewState.openBookmarks) return
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
            IconButton(
                modifier = Modifier
                    .size(56.dp),
                onClick = { presenter.onBookmarkMenuClick() }) {
                Icon(
                    painter = if (browserViewState.isRootFolder) {
                        painterResource(R.drawable.ic_action_star)
                    } else {
                        painterResource(R.drawable.ic_action_back)
                    },
                    contentDescription = "test"
                )
            }
            Text(
                text = stringResource(R.string.action_bookmarks),
                style = MaterialTheme.typography.titleLarge
            )
        }
        LazyColumn {
            itemsIndexed(
                items = browserViewState.bookmarks,
                contentType = { _, item -> item.icon is BrowserViewState.BookmarkListItem.Icon.Folder },
            ) { index, bookmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .combinedClickable(
                            onClick = { presenter.onBookmarkClick(index) },
                            onLongClick = { presenter.onBookmarkLongClick(index) }
                        )
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val icon = bookmark.icon) {
                        BrowserViewState.BookmarkListItem.Icon.Folder -> Icon(
                            modifier = Modifier
                                .size(56.dp)
                                .padding(horizontal = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = "test"
                        )

                        is BrowserViewState.BookmarkListItem.Icon.Image -> {
                            val placeholder = ImagePainter(
                                with(LocalDensity.current) {
                                    LetterImage.create(
                                        density = this,
                                        character = bookmark.title.first(),
                                        width = 24.dp.toPx().toInt(),
                                        height = 24.dp.toPx().toInt(),
                                    )
                                }
                            )
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(icon.path)
                                    .build(),
                                placeholder = placeholder,
                                fallback = placeholder,
                                error = placeholder,
                                contentDescription = "test",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
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

