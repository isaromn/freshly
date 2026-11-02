package com.ecoalacena.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/** Opens the native iOS share sheet (`UIActivityViewController`) over the screen on top. */
actual class ShareService {

    @OptIn(ExperimentalForeignApi::class)
    actual fun shareText(text: String): ShareResult {
        val presenter = topViewController() ?: return ShareResult.FAILED
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)

        // On iPad the sheet is a popover and needs an anchor: the middle of the screen.
        sheet.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            popover.sourceRect = presenter.view.bounds
        }
        presenter.presentViewController(sheet, animated = true, completion = null)
        return ShareResult.SHARED
    }

    @Suppress("DEPRECATION")
    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }
}
