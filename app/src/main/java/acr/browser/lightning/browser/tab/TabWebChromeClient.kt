package acr.browser.lightning.browser.tab

import acr.browser.lightning.R
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
import android.webkit.*
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.palette.graphics.Palette
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by anthonycr on 9/12/20.
 */
class TabWebChromeClient(
    private val activity: Activity,
    private val faviconModel: FaviconModel,
    private val diskScheduler: Scheduler,
    private val userPreferences: UserPreferences,
    private val webRtcPermissionsModel: WebRtcPermissionsModel
) : WebChromeClient(), WebRtcPermissionsView {

    private val defaultColor = activity.color(R.color.primary_color)
    private val geoLocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    val progressObservable: PublishSubject<Int> = PublishSubject.create()
    val titleObservable: PublishSubject<String> = PublishSubject.create()
    val faviconObservable: BehaviorSubject<Option<Bitmap>> = BehaviorSubject.create()
    val createWindowObservable: PublishSubject<TabInitializer> = PublishSubject.create()
    val closeWindowObservable: PublishSubject<Unit> = PublishSubject.create()
    val colorChangeObservable: BehaviorSubject<Int> = BehaviorSubject.createDefault(defaultColor)
    val fileChooserObservable: PublishSubject<Intent> = PublishSubject.create()
    val showCustomViewObservable: PublishSubject<View> = PublishSubject.create()
    val hideCustomViewObservable: PublishSubject<Unit> = PublishSubject.create()

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var customViewCallback: CustomViewCallback? = null

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
        super.onProgressChanged(view, newProgress)
        progressObservable.onNext(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        titleObservable.onNext(title)
        faviconObservable.onNext(Option.None)
        generateColorAndPropagate(null)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
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

    fun onResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val intent = activityResult.data
        val result = FileChooserParams.parseResult(resultCode, intent)

        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    fun hideCustomView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
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
