package acr.browser.lightning.extensions

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import android.webkit.PermissionRequest

/**
 * Returns the permissions retrieved from [Manifest.permission] which are required by the requested
 * resources. If none of the resources require a permission, the list will be empty.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun PermissionRequest.requiredPermissions(): Set<String> {
    return resources.flatMap {
        when (it) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> listOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
            )
            PermissionRequest.RESOURCE_MIDI_SYSEX -> emptyList()
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> emptyList()
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> listOf(
                    Manifest.permission.CAMERA
            )
            else -> emptyList()
        }
    }.toHashSet()
}