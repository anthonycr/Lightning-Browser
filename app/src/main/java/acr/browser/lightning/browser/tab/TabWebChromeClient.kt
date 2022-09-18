package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
import acr.browser.lightning.browser.di.DiskScheduler
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.color
import acr.browser.lightning.extensions.resizeAndShow
import acr.browser.lightning.favicon.FaviconModel
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.Option
import acr.browser.lightning.utils.Utils
import acr.browser.lightning.browser.webrtc.WebRtcPermissionsModel
import acr.browser.lightning.browser.webrtc.WebRtcPermissionsView
import android.Manifest
import android.app.Activity
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
import androidx.palette.graphics.Palette
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * A [WebChromeClient] that supports the tab adaptation.
 */
class TabWebChromeClient @Inject constructor(
    private val activity: Activity,
    private val faviconModel: FaviconModel,
    @DiskScheduler private val diskScheduler: Scheduler,
    private val userPreferences: UserPreferences,
    private val webRtcPermissionsModel: WebRtcPermissionsModel
) : WebChromeClient(), WebRtcPermissionsView {

    private val defaultColor = activity.color(R.color.primary_color)
    private val geoLocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Emits changes to the page loading progress.
     */
    val progressObservable: PublishSubject<Int> = PublishSubject.create()

    /**
     * Emits changes to the page title.
     */
    val titleObservable: PublishSubject<String> = PublishSubject.create()

    /**
     * Emits changes to the page favicon. Always emits the last emitted favicon.
     */
    val faviconObservable: BehaviorSubject<Option<Bitmap>> = BehaviorSubject.create()

    /**
     * Emits create window requests.
     */
    val createWindowObservable: PublishSubject<TabInitializer> = PublishSubject.create()

    /**
     * Emits close window requests.
     */
    val closeWindowObservable: PublishSubject<Unit> = PublishSubject.create()

    /**
     * Emits changes to the thematic color of the current page.
     */
    val colorChangeObservable: BehaviorSubject<Int> = BehaviorSubject.createDefault(defaultColor)

    /**
     * Emits requests to open the file chooser for upload.
     */
    val fileChooserObservable: PublishSubject<Intent> = PublishSubject.create()

    /**
     * Emits requests to show a custom view (i.e. full screen video).
     */
    val showCustomViewObservable: PublishSubject<View> = PublishSubject.create()

    /**
     * Emits requests to hide the custom view that was shown prior.
     */
    val hideCustomViewObservable: PublishSubject<Unit> = PublishSubject.create()

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
        createWindowObservable.onNext(ResultMessageInitializer(resultMsg))
        return true
    }

    override fun onCloseWindow(window: WebView) {
        closeWindowObservable.onNext(Unit)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        progressObservable.onNext(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        titleObservable.onNext(title)
        faviconObservable.onNext(Option.None)
        generateColorAndPropagate(null)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        faviconObservable.onNext(Option.Some(icon))
        val url = view.url ?: return
        faviconModel.cacheFaviconForUrl(icon, url)
            .subscribeOn(diskScheduler)
            .subscribe()
        generateColorAndPropagate(icon)
    }

    private fun generateColorAndPropagate(favicon: Bitmap?) {
        val icon = favicon ?: return run {
            colorChangeObservable.onNext(defaultColor)
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
            colorChangeObservable.onNext(finalColor)
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
        fileChooserParams.createIntent().let(fileChooserObservable::onNext)
        return true
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        customViewCallback = callback
        showCustomViewObservable.onNext(view)
    }

    override fun onHideCustomView() {
        hideCustomViewObservable.onNext(Unit)
        customViewCallback = null
    }

    override fun requestPermissions(permissions: Set<String>, onGrant: (Boolean) -> Unit) {
        val missingPermissions = permissions
            .filter { PermissionsManager.getInstance().hasPermission(activity, it) }

        if (missingPermissions.isEmpty()) {
            onGrant(true)
        } else {
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
                activity,
                missingPermissions.toTypedArray(),
                object : PermissionsResultAction() {
                    override fun onGranted() = onGrant(true)

                    override fun onDenied(permission: String?) = onGrant(false)
                }
            )
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
        if (userPreferences.webRtcEnabled) {
            webRtcPermissionsModel.requestPermission(request, this)
        } else {
            request.deny()
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) = PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
        activity,
        geoLocationPermissions,
        object : PermissionsResultAction() {
            override fun onGranted() {
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
            }

            //TODO show message and/or turn off setting
            override fun onDenied(permission: String) = Unit
        })
}
