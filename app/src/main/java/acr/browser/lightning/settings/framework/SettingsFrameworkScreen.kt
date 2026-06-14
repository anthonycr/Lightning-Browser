package acr.browser.lightning.settings.framework

import acr.browser.lightning.settings.SettingsBottomSheetChooser
import acr.browser.lightning.settings.SettingsBottomSheetInput
import acr.browser.lightning.settings.SettingsClickable
import acr.browser.lightning.settings.SettingsClickableState
import acr.browser.lightning.settings.SettingsLoader
import acr.browser.lightning.settings.SettingsToggle
import acr.browser.lightning.settings.SettingsToggleState
import acr.browser.lightning.settings.SettingsUiState
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFrameworkScreen(
    presenter: SettingsFrameworkPresenter,
    onUp: () -> Unit
) {
    BackHandler { onUp() }
    val state by presenter.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    state.ephemeral?.let {
        LaunchedEffect(it.message) {
            when (snackbarHostState.showSnackbar(it.message)) {
                SnackbarResult.Dismissed -> presenter.onEvent(SettingsFrameworkUiEvent.SnackbarDismissed)
                SnackbarResult.ActionPerformed -> presenter.onEvent(SettingsFrameworkUiEvent.SnackbarDismissed)
            }
        }
    }
    state.chooseFile?.let { mimeType ->
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                presenter.onEvent(SettingsFrameworkUiEvent.FileChosen(it))
            }
        LaunchedEffect(null) {
            launcher.launch(mimeType)
        }
    }
    state.createFile?.let { mimeType ->
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
                presenter.onEvent(SettingsFrameworkUiEvent.FileChosen(it))
            }
        LaunchedEffect(null) {
            launcher.launch(mimeType)
        }
    }
    state.webLink?.let { webLink ->
        val current = LocalContext.current
        LaunchedEffect(webLink) {
            current.startActivity(Intent(Intent.ACTION_VIEW, webLink.toUri()))
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(state.title)
                }
            )
        }
    ) { innerPadding ->
        when (val content = state.content) {
            is SettingsUiState.Content.Actual -> {
                if (content.bottomSheetChooser != null) {
                    SettingsBottomSheetChooser(
                        innerPadding = innerPadding,
                        state = content.bottomSheetChooser,
                        onDismiss = {
                            presenter.onEvent(
                                SettingsFrameworkUiEvent.BottomSheetChoiceResult(null)
                            )
                        },
                        onSelected = {
                            presenter.onEvent(
                                SettingsFrameworkUiEvent.BottomSheetChoiceResult(it)
                            )
                        }
                    )
                }
                if (content.bottomSheetInput != null) {
                    SettingsBottomSheetInput(
                        innerPadding = innerPadding,
                        state = content.bottomSheetInput,
                        onDismiss = {
                            presenter.onEvent(
                                SettingsFrameworkUiEvent.BottomSheetInputResult(null)
                            )
                        },
                        onSelected = {
                            presenter.onEvent(
                                SettingsFrameworkUiEvent.BottomSheetInputResult(it.toString())
                            )
                        }
                    )
                }
                if (content.dialogConfirmation != null) {
                    BasicAlertDialog(onDismissRequest = {
                        presenter.onEvent(SettingsFrameworkUiEvent.DialogConfirmation(false))
                    }) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .wrapContentHeight(),
                            shape = AlertDialogDefaults.shape,
                            color = AlertDialogDefaults.containerColor
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = content.dialogConfirmation.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = AlertDialogDefaults.titleContentColor
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = content.dialogConfirmation.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = AlertDialogDefaults.textContentColor
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        presenter.onEvent(
                                            SettingsFrameworkUiEvent.DialogConfirmation(false)
                                        )
                                    }) {
                                        Text(content.dialogConfirmation.negativeAction)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    TextButton(onClick = {
                                        presenter.onEvent(
                                            SettingsFrameworkUiEvent.DialogConfirmation(true)
                                        )
                                    }) {
                                        Text(content.dialogConfirmation.positiveAction)
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    content.entries.forEachIndexed { index, option ->
                        when (option) {
                            is SettingsClickableState -> SettingsClickable(option) {
                                presenter.onEvent(SettingsFrameworkUiEvent.Click(index))
                            }

                            is SettingsToggleState -> SettingsToggle(option) {
                                presenter.onEvent(SettingsFrameworkUiEvent.Toggle(it, index))
                            }
                        }
                    }
                }
            }

            SettingsUiState.Content.Loading -> SettingsLoader(innerPadding)
        }
    }
}
