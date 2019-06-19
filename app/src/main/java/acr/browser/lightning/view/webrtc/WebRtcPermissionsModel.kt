package acr.browser.lightning.view.webrtc

import acr.browser.lightning.extensions.requiredPermissions
import android.annotation.TargetApi
import android.os.Build
import android.webkit.PermissionRequest
import javax.inject.Inject
import javax.inject.Singleton

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@Singleton
class WebRtcPermissionsModel @Inject constructor() {

    private val resourceGrantMap = mutableMapOf<String, HashSet<String>>()

    /**
     * Request a permission from the user to use certain device resources. Will call either
     * [PermissionRequest.grant] or [PermissionRequest.deny] based on the response received from the
     * user.
     *
     * @param permissionRequest the request being made.
     * @param view the view that will delegate requesting permissions or resources from the user.
     */
    fun requestPermission(permissionRequest: PermissionRequest, view: WebRtcPermissionsView) {
        val host = permissionRequest.origin.host ?: ""
        val requiredResources = permissionRequest.resources
        val requiredPermissions = permissionRequest.requiredPermissions()

        if (resourceGrantMap[host]?.containsAll(requiredResources.asList()) == true) {
            view.requestPermissions(requiredPermissions) { permissionsGranted ->
                if (permissionsGranted) {
                    permissionRequest.grant(requiredResources)
                } else {
                    permissionRequest.deny()
                }
            }
        } else {
            view.requestResources(host, requiredResources) { resourceGranted ->
                if (resourceGranted) {
                    view.requestPermissions(requiredPermissions) { permissionsGranted ->
                        if (permissionsGranted) {
                            resourceGrantMap[host]?.addAll(requiredResources)
                                ?: resourceGrantMap.put(host, requiredResources.toHashSet())
                            permissionRequest.grant(requiredResources)
                        } else {
                            permissionRequest.deny()
                        }
                    }
                } else {
                    permissionRequest.deny()
                }
            }
        }
    }

}
