package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsDialogConfirmationState
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.inject.Inject

class BookmarkSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val bookmarkExporter: BookmarkExporter,
    private val bookmarkRepository: BookmarkRepository,
) {
    val key = "bookmark"

    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        title = resourceProvider.stringResource(R.string.bookmark_settings),
        content = listOf(
            ClickableState(
                title = resourceProvider.stringResource(R.string.export_bookmarks),
                onClick = ClickableOnClick.FileCreator(
                    fileName = "ExportedBookmarks.txt",
                    onCreated = {
                        if (it != null) {
                            ClickableOnClick.Snackbar {
                                val exportFile = bookmarkExporter.exportBookmarksToUri(it)
                                if (exportFile == null) {
                                    SettingsSnackBarState(
                                        resourceProvider.stringResource(R.string.bookmark_export_failure)
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
                                val imported = bookmarkExporter.importBookmarksFromUri(it)
                                if (imported == null) {
                                    SettingsSnackBarState(
                                        resourceProvider.stringResource(R.string.import_bookmark_error)
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
                    produceState = {
                        SettingsDialogConfirmationState(
                            title = resourceProvider.stringResource(R.string.action_delete),
                            message = resourceProvider.stringResource(R.string.action_delete_all_bookmarks),
                            negativeAction = resourceProvider.stringResource(R.string.no),
                            positiveAction = resourceProvider.stringResource(R.string.yes),
                        )
                    },
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

@Composable
fun BookmarkSettingsScreen(
    bookmarkSettingsScreen: BookmarkSettingsScreen,
    onUp: () -> Unit
) {
    SettingsFrameworkScreen(
        viewModel(
            key = bookmarkSettingsScreen.key,
            factory = SettingsFrameworkPresenter.Factory(
                settingsFrameworkState = {
                    bookmarkSettingsScreen.createSettingsFrameworkState()
                }
            )
        ),
        onUp
    )
}
