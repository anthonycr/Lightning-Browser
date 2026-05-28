package acr.browser.lightning.settings

sealed interface SettingsOption

data class SettingsClickableState(
    val enabled: Boolean = true,
    val title: String,
    val summary: String? = null
) : SettingsOption

data class SettingsToggleState(
    val enabled: Boolean = true,
    val title: String,
    val summary: String? = null,
    val isChecked: Boolean
) : SettingsOption

data class SettingsSnackBarState(
    val message: String
)

data class SettingsBottomSheetChooserState(
    val title: String,
    val values: List<String>,
    val selected: Int
)

data class SettingsBottomSheetInputState(
    val title: String,
    val hint: String,
    val currentValue: String = ""
)

data class SettingsDialogConfirmationState(
    val title: String,
    val message: String,
    val positiveAction: String,
    val negativeAction: String
)

data class SettingsUiState(
    val title: String,
    val content: Content,
    val ephemeral: SettingsSnackBarState? = null,
    val chooseFile: String? = null,
    val createFile: String? = null,
    val webLink: String? = null,
) {
    sealed interface Content {
        data object Loading : Content

        data class Actual(
            val entries: List<SettingsOption>,
            val bottomSheetChooser: SettingsBottomSheetChooserState? = null,
            val bottomSheetInput: SettingsBottomSheetInputState? = null,
            val dialogConfirmation: SettingsDialogConfirmationState? = null,
        ) : Content
    }
}
