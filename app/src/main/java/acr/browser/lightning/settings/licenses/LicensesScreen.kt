package acr.browser.lightning.settings.licenses

import acr.browser.lightning.R
import acr.browser.lightning.compose.StatusBar
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders all licenses on the screen in a nested hierarchy:
 *
 * ```
 * License Name
 * | group1
 * | | artifact1:version
 * | | artifact2:version
 * | group2
 * | | artifact1:version
 * | | artifact2:version
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
    licensesScreenPresenter: LicensesScreenPresenter,
    onClickUrl: (String) -> Unit,
    onUp: () -> Unit
) {
    BackHandler { onUp() }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            StatusBar(
                paintSurfaceColor = false,
                useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
            )
            TopAppBar(
                title = {
                    Text(stringResource(R.string.licenses))
                }
            )
        }
    ) { innerPadding ->
        val state by licensesScreenPresenter.state.collectAsState()
        when (state) {
            is LicensesScreenState.Data -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                val data = (state as LicensesScreenState.Data).list
                items(items = data) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        when (it) {
                            is LicensesScreenState.Data.Items.Entry -> {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable(onClickLabel = it.url) { onClickUrl(it.url) }
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(8.dp))
                                    VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        thickness = 3.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        thickness = 3.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = it.text,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            is LicensesScreenState.Data.Items.Header -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClickLabel = it.url) { onClickUrl(it.url) }
                                        .padding(horizontal = 16.dp),
                                ) {
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = it.text,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = it.url,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            }

                            is LicensesScreenState.Data.Items.SubHeader -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(8.dp))
                                    VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        thickness = 3.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = it.text,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LicensesScreenState.Loading -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
