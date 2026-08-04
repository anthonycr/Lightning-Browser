package acr.browser.lightning.browser.webrtc

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.Manifest
import android.net.Uri
import android.webkit.PermissionRequest
import androidx.core.net.toUri
import com.anthonycr.mockingbird.core.fake
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test for [WebRtcPermissionsModel].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class WebRtcPermissionsModelTest {

    private val permissionRequest = fake<PermissionRequest>()

    private class FakePermissionRequest(
        private val delegate: PermissionRequest,
        private val origin: Uri,
        private val resources: Array<String>
    ) : PermissionRequest() {

        override fun deny() = delegate.deny()

        override fun getOrigin(): Uri = origin

        override fun getResources(): Array<String> = resources

        override fun grant(p0: Array<String>) = delegate.grant(p0)

    }

    private class FakeWebRtcPermissionsView(
        private val allowedPermissions: Set<String>,
        private val allowedResources: Set<String>,
    ) : WebRtcPermissionsView {
        override fun requestPermissions(
            permissions: Set<String>,
            onGrant: (Boolean) -> Unit
        ) {
            onGrant(allowedPermissions.containsAll(permissions))
        }

        override fun requestResources(
            source: String,
            resources: Array<String>,
            onGrant: (Boolean) -> Unit
        ) {
            onGrant(allowedResources.containsAll(resources.toList()))
        }

    }

    @Test
    fun `requestPermission with resource denial denies request`() {
        val model = WebRtcPermissionsModel()
        val fakePermissionsView = FakeWebRtcPermissionsView(
            allowedPermissions = emptySet(),
            allowedResources = emptySet(),
        )
        val fakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        )

        model.requestPermission(fakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.deny()
        }
    }

    @Test
    fun `requestPermission with resource grant and permission denial denies request`() {
        val model = WebRtcPermissionsModel()
        val fakePermissionsView = FakeWebRtcPermissionsView(
            allowedPermissions = emptySet(),
            allowedResources = setOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE),
        )
        val fakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        )

        model.requestPermission(fakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.deny()
        }
    }

    @Test
    fun `requestPermission with resource and permission grant grants request`() {
        val model = WebRtcPermissionsModel()
        val fakePermissionsView = FakeWebRtcPermissionsView(
            allowedPermissions = setOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            ),
            allowedResources = setOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE),
        )
        val resources = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val fakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = resources
        )

        model.requestPermission(fakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.grant(resources)
        }
    }

    @Test
    fun `requestPermission with pre-granted resources and permission grant grants request`() {
        val model = WebRtcPermissionsModel()
        val allowedResources = mutableSetOf(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_MIDI_SYSEX
        )
        val fakePermissionsView = FakeWebRtcPermissionsView(
            allowedPermissions = setOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            ),
            allowedResources = allowedResources,
        )
        val firstResources = arrayOf(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_MIDI_SYSEX
        )
        val firstFakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = firstResources
        )
        val secondResources = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val secondFakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = secondResources
        )

        model.requestPermission(firstFakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.grant(firstResources)
        }

        // We don't need to ask the user again because they already approved the resource usage.
        // If we ask the user again, clearing `allowedResources` will fail the test.
        allowedResources.clear()

        model.requestPermission(secondFakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.grant(secondResources)
        }
    }

    @Test
    fun `requestPermission with pre-granted resources and permission denial denies request`() {
        val model = WebRtcPermissionsModel()
        val allowedPermissions = mutableSetOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        val fakePermissionsView = FakeWebRtcPermissionsView(
            allowedPermissions = allowedPermissions,
            allowedResources = setOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE),
        )
        val firstResources = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val firstFakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = firstResources
        )

        val secondResource = arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        val secondFakePermissionRequest = FakePermissionRequest(
            delegate = permissionRequest,
            origin = "https://test.com".toUri(),
            resources = secondResource
        )

        model.requestPermission(firstFakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.grant(firstResources)
        }

        // Simulate revoking permissions outside the app
        allowedPermissions.clear()

        model.requestPermission(secondFakePermissionRequest, fakePermissionsView)

        com.anthonycr.mockingbird.core.verify(permissionRequest) {
            permissionRequest.deny()
        }
    }
}
