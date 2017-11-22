package acr.browser.lightning.view.webrtc

/**
 * A view which specializes in requesting device permissions and resources from the user.
 */
interface WebRtcPermissionsView {
    /**
     * Request the provided permissions from the user, and call back to [onGrant] with true when all
     * the permissions have been granted, or false if one or more was denied.
     *
     * @param permissions the permissions to request.
     * @param onGrant the callback to invoke when the user indicates their intent to grant or deny.
     */
    fun requestPermissions(permissions: Set<String>, onGrant: (Boolean) -> Unit)

    /**
     * Request the provided device resources from the user, and call back to [onGrant] with true
     * when all the permissions have been granted, or false if one or more was denied.
     *
     * @param source the domain from which the request originated.
     * @param resources the device resources being requested.
     * @param onGrant the callback to invoke when the user indicates their intent to grant or deny.
     */
    fun requestResources(source: String, resources: Array<String>, onGrant: (Boolean) -> Unit)
}

