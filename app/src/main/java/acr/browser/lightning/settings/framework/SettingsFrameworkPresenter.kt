package acr.browser.lightning.settings.framework

import acr.browser.lightning.settings.SettingsBottomSheetChooserState
import acr.browser.lightning.settings.SettingsBottomSheetInputState
import acr.browser.lightning.settings.SettingsClickableState
import acr.browser.lightning.settings.SettingsDialogConfirmationState
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.SettingsToggleState
import acr.browser.lightning.settings.SettingsUiState
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsFrameworkState(
    val title: String,
    val content: List<SettingsFrameworkOption>,
)

sealed interface SettingsFrameworkOption

class ToggleState(
    val enabled: suspend () -> Boolean = { true },
    val title: String,
    val summary: (suspend () -> String)? = null,
    val isChecked: suspend () -> Boolean,
    val onToggle: suspend (Boolean) -> SettingsSnackBarState?
) : SettingsFrameworkOption

class ClickableState(
    val enabled: suspend () -> Boolean = { true },
    val title: String,
    val summary: (suspend () -> String)? = null,
    val onClick: ClickableOnClick
) : SettingsFrameworkOption

sealed interface ClickableOnClick {

    data object None : ClickableOnClick

    class Action(val action: suspend () -> Unit) : ClickableOnClick

    class ItemSelector(
        val title: String,
        val values: List<String>,
        val selected: suspend () -> Int,
        val onSelected: suspend (Int) -> ClickableOnClick,
    ) : ClickableOnClick

    class Input(
        val title: String,
        val hint: String,
        val currentValue: suspend () -> String = { "" },
        val onValueUpdated: suspend (String) -> ClickableOnClick
    ) : ClickableOnClick

    class Confirmation(
        val title: String,
        val message: String,
        val positiveAction: String,
        val negativeAction: String,
        val onConfirmed: suspend (Boolean) -> ClickableOnClick
    ) : ClickableOnClick

    class FileChooser(
        val mimeType: String,
        val onSelected: suspend (Uri?) -> ClickableOnClick
    ) : ClickableOnClick

    class FileCreator(
        val fileName: String,
        val onCreated: suspend (Uri?) -> ClickableOnClick
    ) : ClickableOnClick

    class WebLink(
        val url: String
    ) : ClickableOnClick

    class Snackbar(
        val produceState: suspend () -> SettingsSnackBarState
    ) : ClickableOnClick
}

sealed interface SettingsFrameworkUiEvent {
    data object Initialize : SettingsFrameworkUiEvent

    data class Toggle(val value: Boolean, val index: Int) : SettingsFrameworkUiEvent

    data class Click(val index: Int) : SettingsFrameworkUiEvent

    data class BottomSheetChoiceResult(val chosen: Int?) : SettingsFrameworkUiEvent

    data class BottomSheetInputResult(val input: String?) : SettingsFrameworkUiEvent

    data class DialogConfirmation(val confirmed: Boolean) : SettingsFrameworkUiEvent

    data class FileChosen(val uri: Uri?) : SettingsFrameworkUiEvent

    data object SnackbarDismissed : SettingsFrameworkUiEvent
}

class SettingsFrameworkPresenter(
    private val settingsFrameworkState: SettingsFrameworkState
) : ViewModel() {

    class Factory(
        private val settingsFrameworkState: () -> SettingsFrameworkState
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == SettingsFrameworkPresenter::class.java)
            return SettingsFrameworkPresenter(
                settingsFrameworkState()
            ) as T
        }
    }

    val state = MutableStateFlow(
        SettingsUiState(
            title = settingsFrameworkState.title,
            content = SettingsUiState.Content.Loading
        )
    )

    private val pendingActionState = MutableStateFlow<ClickableOnClick>(ClickableOnClick.None)

    private val eventFlow = MutableStateFlow<SettingsFrameworkUiEvent>(
        SettingsFrameworkUiEvent.Initialize
    )

    private suspend fun SettingsFrameworkState.asUiState(
        bottomSheetChooser: SettingsBottomSheetChooserState? = null,
        bottomSheetInput: SettingsBottomSheetInputState? = null,
        dialogConfirmation: SettingsDialogConfirmationState? = null,
        ephemeral: SettingsSnackBarState? = null,
        chooseFile: String? = null,
        createFile: String? = null,
        webLink: String? = null,
    ): SettingsUiState = state.value.copy(
        content = SettingsUiState.Content.Actual(
            entries = content.map {
                when (it) {
                    is ClickableState -> SettingsClickableState(
                        enabled = it.enabled(),
                        title = it.title,
                        summary = it.summary?.invoke()
                    )

                    is ToggleState -> SettingsToggleState(
                        enabled = it.enabled(),
                        title = it.title,
                        summary = it.summary?.invoke(),
                        isChecked = it.isChecked()
                    )
                }
            },
            bottomSheetChooser = bottomSheetChooser,
            bottomSheetInput = bottomSheetInput,
            dialogConfirmation = dialogConfirmation,
        ),
        ephemeral = ephemeral,
        chooseFile = chooseFile,
        createFile = createFile,
        webLink = webLink,
    )

    private suspend fun ClickableOnClick.asUiState(): SettingsUiState {
        return when (this) {
            is ClickableOnClick.Action -> {
                this.action()
                pendingActionState.emit(ClickableOnClick.None)
                settingsFrameworkState.asUiState()
            }

            is ClickableOnClick.ItemSelector -> {
                pendingActionState.emit(this)
                settingsFrameworkState.asUiState(
                    bottomSheetChooser = SettingsBottomSheetChooserState(
                        title = this.title,
                        this.values,
                        selected = this.selected()
                    )
                )
            }

            is ClickableOnClick.Input -> {
                pendingActionState.emit(this)
                settingsFrameworkState.asUiState(
                    bottomSheetInput = SettingsBottomSheetInputState(
                        title = this.title,
                        hint = this.hint,
                        currentValue = this.currentValue()
                    )
                )
            }

            ClickableOnClick.None -> {
                error("Element is not clickable, set enabled=false to disable click events")
            }

            is ClickableOnClick.FileChooser -> {
                pendingActionState.emit(this)
                settingsFrameworkState.asUiState(chooseFile = this.mimeType)
            }

            is ClickableOnClick.Snackbar -> settingsFrameworkState.asUiState(ephemeral = this.produceState())
            is ClickableOnClick.Confirmation -> {
                pendingActionState.emit(this)
                settingsFrameworkState.asUiState(
                    dialogConfirmation = SettingsDialogConfirmationState(
                        title = this.title,
                        message = this.message,
                        negativeAction = this.negativeAction,
                        positiveAction = this.positiveAction
                    )
                )
            }

            is ClickableOnClick.FileCreator -> {
                pendingActionState.emit(this)
                settingsFrameworkState.asUiState(createFile = this.fileName)
            }

            is ClickableOnClick.WebLink -> settingsFrameworkState.asUiState(webLink = this.url)
        }
    }

    init {
        viewModelScope.launch {
            eventFlow.collectLatest { event ->
                val transformedState = when (event) {
                    is SettingsFrameworkUiEvent.BottomSheetChoiceResult -> {
                        val pendingAction = pendingActionState.value
                        require(pendingAction is ClickableOnClick.ItemSelector)
                        if (event.chosen != null) {
                            pendingAction.onSelected(event.chosen).asUiState()
                        } else {
                            settingsFrameworkState.asUiState()
                        }
                    }

                    is SettingsFrameworkUiEvent.BottomSheetInputResult -> {
                        val pendingAction = pendingActionState.value
                        require(pendingAction is ClickableOnClick.Input)
                        if (event.input != null) {
                            pendingAction.onValueUpdated(event.input).asUiState()
                        } else {
                            settingsFrameworkState.asUiState()
                        }
                    }

                    is SettingsFrameworkUiEvent.Click -> {
                        val actual = settingsFrameworkState.content[event.index]
                        require(actual is ClickableState)
                        actual.onClick.asUiState()
                    }

                    SettingsFrameworkUiEvent.Initialize -> settingsFrameworkState.asUiState()
                    SettingsFrameworkUiEvent.SnackbarDismissed -> state.value.copy(ephemeral = null)

                    is SettingsFrameworkUiEvent.Toggle -> {
                        val actual = settingsFrameworkState.content[event.index]
                        require(actual is ToggleState)
                        val snackbarState = actual.onToggle(event.value)
                        settingsFrameworkState.asUiState(ephemeral = snackbarState)
                    }

                    is SettingsFrameworkUiEvent.FileChosen -> {
                        when (val pendingAction = pendingActionState.value) {
                            is ClickableOnClick.FileChooser -> pendingAction.onSelected(event.uri)
                                .asUiState()

                            is ClickableOnClick.FileCreator -> pendingAction.onCreated(event.uri)
                                .asUiState()

                            else -> error("Only file actions can produce a FileChosen event")
                        }
                    }

                    is SettingsFrameworkUiEvent.DialogConfirmation -> {
                        val pendingAction = pendingActionState.value
                        require(pendingAction is ClickableOnClick.Confirmation)
                        pendingAction.onConfirmed(event.confirmed).asUiState()
                    }
                }
                state.emit(transformedState)
            }
        }
    }

    fun onEvent(event: SettingsFrameworkUiEvent) {
        viewModelScope.launch {
            eventFlow.emit(event)
        }
    }
}
