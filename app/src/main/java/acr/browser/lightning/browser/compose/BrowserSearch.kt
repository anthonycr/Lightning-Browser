package acr.browser.lightning.browser.compose

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.database.Bookmark
import acr.browser.lightning.database.HistoryEntry
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.search.SuggestionsModel
import acr.browser.lightning.ssl.SslState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
            presenter.onEvent(BrowserUiEvent.SearchBarExpandedOrCollapsed(expanded = false))
        } else if (searchBarState.currentValue == SearchBarValue.Collapsed &&
            browserViewState.isSearchBarExpanded
        ) {
            searchBarState.animateToExpanded()
            presenter.onEvent(BrowserUiEvent.SearchBarExpandedOrCollapsed(expanded = true))
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
                        presenter.onEvent(
                            BrowserUiEvent.SearchQueryChanged(
                                query = it.text,
                                selectionStart = it.selection.min,
                                selectionEnd = it.selection.max
                            )
                        )
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
                        presenter.onEvent(BrowserUiEvent.SearchBarExpandedOrCollapsed(expanded = false))
                    }
                    presenter.onEvent(BrowserUiEvent.SearchConfirmed(state.text))
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
                        presenter.onEvent(BrowserUiEvent.SearchSuggestionClick(it))
                        coroutineScope.launch {
                            searchBarState.animateToCollapsed()
                            presenter.onEvent(BrowserUiEvent.SearchBarExpandedOrCollapsed(expanded = false))
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
                        presenter.onEvent(BrowserUiEvent.SearchSuggestionInsertClick(it))
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
                    presenter.onEvent(BrowserUiEvent.SearchBarExpandedOrCollapsed(expanded = true))
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (browserViewState.sslState) {
            is SslState.Invalid -> IconButton(
                modifier = Modifier.size(36.dp),
                onClick = { presenter.onEvent(BrowserUiEvent.SslIconClick) }
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
                onClick = { presenter.onEvent(BrowserUiEvent.SslIconClick) }
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
        IconButton(onClick = { presenter.onEvent(BrowserUiEvent.RefreshOrStopClick) }) {
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
