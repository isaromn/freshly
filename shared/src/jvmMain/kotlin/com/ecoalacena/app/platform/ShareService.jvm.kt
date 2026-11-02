package com.ecoalacena.app.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Desktop has no share sheet: the text goes to the system clipboard, ready to paste anywhere. */
actual class ShareService {

    actual fun shareText(text: String): ShareResult =
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            ShareResult.COPIED_TO_CLIPBOARD
        } catch (error: Exception) {
            ShareResult.FAILED
        }
}
