package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.database.bookmark.NewBookmarkExporter
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BookmarkSettingsScreen(
    resourceProvider: ResourceProvider,
    newBookmarkExporter: NewBookmarkExporter,
    bookmarkRepository: BookmarkRepository,
    onUp: () -> Unit
) {
    val presenter: SettingsFrameworkPresenter = viewModel(
        key = "bookmark",
        factory = SettingsFrameworkPresenter.Factory(
            settingsFrameworkState = {
                SettingsFrameworkState(
                    title = resourceProvider.stringResource(R.string.bookmark_settings),
                    content = listOf(
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.export_bookmarks),
                            onClick = ClickableOnClick.FileCreator(
                                fileName = "ExportedBookmarks.txt",
                                onCreated = {
                                    if (it != null) {
                                        ClickableOnClick.Snackbar {
                                            val exportFile =
                                                newBookmarkExporter.exportBookmarksToUri(it)
                                            if (exportFile == null) {
                                                SettingsSnackBarState(
                                                    resourceProvider.stringResource(
                                                        R.string.bookmark_export_failure
                                                    )
                                                )
                                            } else {
                                                SettingsSnackBarState(
                                                    resourceProvider.stringResource(
                                                        R.string.bookmark_export_path
                                                    ) + " $exportFile"
                                                )
                                            }
                                        }
                                    } else {
                                        ClickableOnClick.Snackbar {
                                            SettingsSnackBarState(resourceProvider.stringResource(R.string.action_message_canceled))
                                        }
                                    }
                                }
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.import_backup),
                            onClick = ClickableOnClick.FileChooser(
                                mimeType = "text/*",
                                onSelected = {
                                    if (it != null) {
                                        ClickableOnClick.Snackbar {
                                            val imported =
                                                newBookmarkExporter.importBookmarksFromUri(it)
                                            if (imported == null) {
                                                SettingsSnackBarState(
                                                    resourceProvider.stringResource(
                                                        R.string.import_bookmark_error
                                                    )
                                                )
                                            } else {
                                                SettingsSnackBarState(
                                                    "${imported.size} " + resourceProvider.stringResource(
                                                        R.string.message_import
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        ClickableOnClick.Snackbar {
                                            SettingsSnackBarState(resourceProvider.stringResource(R.string.action_message_canceled))
                                        }
                                    }
                                }
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.action_delete_all_bookmarks),
                            onClick = ClickableOnClick.Confirmation(
                                title = resourceProvider.stringResource(R.string.action_delete),
                                message = resourceProvider.stringResource(R.string.action_delete_all_bookmarks),
                                negativeAction = resourceProvider.stringResource(R.string.no),
                                positiveAction = resourceProvider.stringResource(R.string.yes),
                                onConfirmed = {
                                    ClickableOnClick.Action {
                                        if (it) {
                                            bookmarkRepository.deleteAllBookmarks()
                                        }
                                    }
                                }
                            )
                        )
                    )
                )
            }
        )
    )
    SettingsFrameworkScreen(presenter, onUp)
}
