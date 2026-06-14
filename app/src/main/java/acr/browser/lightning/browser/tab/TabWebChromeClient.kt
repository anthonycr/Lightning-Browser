package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import acr.browser.lightning.browser.view.CustomViewCoordinator
import acr.browser.lightning.browser.webrtc.WebRtcPermissionsModel
import acr.browser.lightning.browser.webrtc.WebRtcPermissionsView
import acr.browser.lightning.concurrency.TabCoroutineScope
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.utils.Utils
import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.palette.graphics.Palette
import com.permissionx.guolindev.PermissionX
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * A [WebChromeClient] that supports the tab adaptation.
 */
class TabWebChromeClient @AssistedInject constructor(
    private val activity: FragmentActivity,
    private val faviconModel: FaviconModel,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val webRtcPermissionsModel: WebRtcPermissionsModel,
    @Assisted private val tabCoroutineScope: TabCoroutineScope,
    private val customViewCoordinator: CustomViewCoordinator,
) : WebChromeClient(), WebRtcPermissionsView {

    @AssistedFactory
    interface Factory {
        fun create(tabCoroutineScope: TabCoroutineScope): TabWebChromeClient
    }

    private val defaultColor = activity.color(R.color.primary_color)
    private val geoLocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Emits changes to the page loading progress.
     */
    val progressSharedFlow: MutableSharedFlow<Int> = MutableSharedFlow()

    /**
     * Emits changes to the page title.
     */
    val titleShareFlow: MutableSharedFlow<String> = MutableSharedFlow()

    /**
     * Emits changes to the page favicon. Always emits the last emitted favicon.
     */
    val faviconStateFlow: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    /**
     * Emits create window requests.
     */
    val createWindowSharedFlow: MutableSharedFlow<TabInitializer> = MutableSharedFlow()

    /**
     * Emits close window requests.
     */
    val closeWindowSharedFlow: MutableSharedFlow<Unit> = MutableSharedFlow()

    /**
     * Emits changes to the thematic color of the current page.
     */
    val colorChangeStateFlow: MutableStateFlow<Int> = MutableStateFlow(defaultColor)

    /**
     * Emits requests to open the file chooser for upload.
     */
    val fileChooserSharedFlow: MutableSharedFlow<Intent> = MutableSharedFlow()

    /**
     * Emits requests to show a custom view (i.e. full screen video).
     */
    val showCustomViewSharedFlow: MutableSharedFlow<Unit> = MutableSharedFlow()

    /**
     * Emits requests to hide the custom view that was shown prior.
     */
    val hideCustomViewObservable: MutableSharedFlow<Unit> = MutableSharedFlow()

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var customViewCallback: CustomViewCallback? = null


    /**
     * Handle the [activityResult] that was returned by the file chooser.
     */
    fun onResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val intent = activityResult.data
        val result = FileChooserParams.parseResult(resultCode, intent)

        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    /**
     * Notify the client that we have manually hidden the custom view.
     */
    fun hideCustomView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        tabCoroutineScope.launch {
            createWindowSharedFlow.emit(ResultMessageInitializer(resultMsg))
        }
        return true
    }

    override fun onCloseWindow(window: WebView) {
        tabCoroutineScope.launch {
            closeWindowSharedFlow.emit(Unit)
        }
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        tabCoroutineScope.launch {
            progressSharedFlow.emit(newProgress)
        }
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        tabCoroutineScope.launch {
            titleShareFlow.emit(title)
            faviconStateFlow.emit(null)
            generateColorAndPropagate(null)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        tabCoroutineScope.launch {
            faviconStateFlow.emit(icon)
            val url = view.url ?: return@launch
            tabCoroutineScope.launch {
                faviconModel.cacheFaviconForUrl(icon, url)
            }
            generateColorAndPropagate(icon)
        }
    }

    private suspend fun generateColorAndPropagate(favicon: Bitmap?) {
        val icon = favicon ?: return run {
            colorChangeStateFlow.emit(defaultColor)
        }
        Palette.from(icon).generate { palette ->
            // OR with opaque black to remove transparency glitches
            val color = Color.BLACK or (palette?.getDominantColor(defaultColor) ?: defaultColor)

            // Lighten up the dark color if it is too dark
            val finalColor = if (Utils.isColorTooDark(color)) {
                Utils.mixTwoColors(defaultColor, color, 0.25f)
            } else {
                color
            }
            tabCoroutineScope.launch {
                colorChangeStateFlow.emit(finalColor)
            }
        }
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        // Ensure that previously set callbacks are resolved.
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = null

        this.filePathCallback = filePathCallback
        tabCoroutineScope.launch {
            fileChooserSharedFlow.emit(fileChooserParams.createIntent())
        }
        return true
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        customViewCallback = callback
        tabCoroutineScope.launch {
            showCustomViewSharedFlow.emit(Unit)
        }
        customViewCoordinator.showCustomView(view)
    }

    override fun onHideCustomView() {
        tabCoroutineScope.launch {
            hideCustomViewObservable.emit(Unit)
        }
        customViewCoordinator.hideCustomView()
        customViewCallback = null
    }

    override fun requestPermissions(permissions: Set<String>, onGrant: (Boolean) -> Unit) {
        val missingPermissions = permissions
            .filter { !PermissionX.isGranted(activity, it) }

        if (missingPermissions.isEmpty()) {
            onGrant(true)
        } else {
            PermissionX.init(activity).permissions(missingPermissions)
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        onGrant(true)
                    } else {
                        onGrant(false)
                    }
                }
        }
    }

    override fun requestResources(
        source: String,
        resources: Array<String>,
        onGrant: (Boolean) -> Unit
    ) {
        activity.runOnUiThread {
            val resourcesString = resources.joinToString(separator = "\n")
            BrowserDialog.showPositiveNegativeDialog(
                activity = activity,
                title = R.string.title_permission_request,
                message = R.string.message_permission_request,
                messageArguments = arrayOf(source, resourcesString),
                positiveButton = DialogItem(title = R.string.action_allow) { onGrant(true) },
                negativeButton = DialogItem(title = R.string.action_dont_allow) { onGrant(false) },
                onCancel = { onGrant(false) }
            )
        }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        tabCoroutineScope.launch {
            if (userPreferencesDataStore.webRtcEnabled.get()) {
                webRtcPermissionsModel.requestPermission(request, this@TabWebChromeClient)
            } else {
                request.deny()
            }
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        PermissionX.init(activity).permissions(geoLocationPermissions.toList())
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val remember = true
                    AlertDialog.Builder(activity).apply {
                        setTitle(activity.getString(R.string.location))
                        val org = if (origin.length > 50) {
                            "${origin.subSequence(0, 50)}..."
                        } else {
                            origin
                        }
                        setMessage(org + activity.getString(R.string.message_location))
                        setCancelable(true)
                        setPositiveButton(activity.getString(R.string.action_allow)) { _, _ ->
                            callback.invoke(origin, true, remember)
                        }
                        setNegativeButton(activity.getString(R.string.action_dont_allow)) { _, _ ->
                            callback.invoke(origin, false, remember)
                        }
                    }.resizeAndShow()
                } else {
                    //TODO show message and/or turn off setting
                }
            }
    }
}
