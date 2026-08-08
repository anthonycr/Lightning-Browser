package acr.browser.lightning.browser.compose.sheets

import acr.browser.lightning.BrowserUiEvent
import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserPresenter
import acr.browser.lightning.ssl.SslCertificateInfo
import acr.browser.lightning.ssl.SslState
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslInfoSheet(
    sslCertificateInfo: SslCertificateInfo,
    presenter: BrowserPresenter,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        dragHandle = {},
        onDismissRequest = { presenter.onEvent(BrowserUiEvent.DialogDismissed) }
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
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
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.ssl_info_issued_by),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = sslCertificateInfo.issuedByCommonName,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ssl_info_issued_to),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = sslCertificateInfo.issuedToOrganizationName?.takeIf { it.isNotBlank() }
                    ?: sslCertificateInfo.issuedToCommonName,
                style = MaterialTheme.typography.bodyLarge
            )

            val dateFormat = with(LocalContext.current) {
                remember { DateFormat.getDateFormat(this) }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ssl_info_issued_on),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = dateFormat.format(sslCertificateInfo.issueDate),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ssl_info_expires_on),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = dateFormat.format(sslCertificateInfo.expireDate),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
