package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserComposeState
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.browser.BrowserViewState
import acr.browser.lightning.browser.compose.LetterImage
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil3.compose.AsyncImage
import coil3.compose.ImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
        dragHandle = {},
        onDismissRequest = { presenter.onBookmarkDrawerMoved(false) }
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
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
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null
                        )
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
                                        size = 24.dp.toPx().toInt(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkAddOrEditSheet(
    edit: Boolean,
    title: String,
    url: String,
    folder: String,
    folders: List<String>,
    presenter: BrowserPresenter,
    onConfirmed: (title: String, url: String, folder: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { presenter.onDialogDismissed() },
        dragHandle = {},
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (edit) {
                    stringResource(R.string.title_edit_bookmark)
                } else {
                    stringResource(R.string.action_add_bookmark)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        val titleTextFieldState = rememberTextFieldState(title)
        TextField(
            state = titleTextFieldState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.hint_title)) },
            placeholder = { Text(stringResource(R.string.hint_title)) }
        )

        val urlTextFieldState = rememberTextFieldState(url)
        TextField(
            state = urlTextFieldState,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.hint_url)) },
            placeholder = { Text(stringResource(R.string.hint_url)) }
        )

        var expanded by remember { mutableStateOf(false) }
        var selectedFolder by remember { mutableStateOf(folder) }

        ExposedDropdownMenuBox(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            var folderState by remember { mutableStateOf(folders) }

            TextField(
                value = selectedFolder,
                onValueChange = { newValue ->
                    selectedFolder = newValue
                    folderState = folders.filter { it.startsWith(selectedFolder) }.take(5)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                label = { Text(stringResource(R.string.folder)) },
                placeholder = { Text(stringResource(R.string.folder)) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
            )

            if (folderState.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    folderState.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                selectedFolder = option
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!edit) {
                Button(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    onClick = {
                        scope.launch {
                            delay(500.milliseconds)
                            sheetState.hide()
                            presenter.onDialogDismissed()
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
            Button(
                modifier = Modifier
                    .padding(end = 16.dp),
                onClick = {
                    scope.launch {
                        delay(500.milliseconds)
                        sheetState.hide()
                        onConfirmed(
                            titleTextFieldState.text.toString(),
                            urlTextFieldState.text.toString(),
                            selectedFolder
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkFolderRenameSheet(
    oldTitle: String,
    presenter: BrowserPresenter,
    onSelected: (CharSequence) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { presenter.onDialogDismissed() },
        dragHandle = {},
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.title_rename_folder),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        val textFieldState = rememberTextFieldState(oldTitle)
        TextField(
            textFieldState,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.hint_title)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier
                    .padding(end = 16.dp),
                onClick = {
                    scope.launch {
                        delay(500.milliseconds)
                        sheetState.hide()
                        onSelected(textFieldState.text)
                    }
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        }
    }
}
