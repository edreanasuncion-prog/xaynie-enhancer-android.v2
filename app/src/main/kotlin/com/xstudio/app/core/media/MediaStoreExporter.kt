
package com.xstudio.app

import com.xstudio.app.core.media.MediaStoreExporter
import com.xstudio.app.history.HistoryFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HistoryAndExportTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun photoTtlIsExactly72Hours() {
        assertEquals(72L * 60 * 60 * 1000, HistoryFiles.PHOTO_TTL_MS)
        assertEquals(1_000L + 259_200_000L, HistoryFiles.photoExpiryFor(1_000L))
    }

    @Test
    fun expiryBoundary() {
        val exp = HistoryFiles.photoExpiryFor(0L)

        assertFalse(HistoryFiles.isExpired(exp, exp - 1))
        assertTrue(HistoryFiles.isExpired(exp, exp))
        assertFalse(HistoryFiles.isExpired(null, Long.MAX_VALUE))
    }

    @Test
    fun pathGuard() {
        val root = tmp.newFolder("app")

        val inside = File(root, "enhanced_photos/a.jpg").apply {
            parentFile!!.mkdirs()
            writeText("x")
        }

        val outside = tmp.newFile("other.jpg")
        val traversal = File(root, "../other.jpg")

        assertTrue(HistoryFiles.isStrictlyInside(root, inside))
        assertFalse(HistoryFiles.isStrictlyInside(root, outside))
        assertFalse(HistoryFiles.isStrictlyInside(root, traversal))
        assertFalse(HistoryFiles.isStrictlyInside(root, root))
    }

    @Test
    fun mimeTypesFollowExtension() {
        val png = requireNotNull(MediaStoreExporter.specFor("a.PNG")) {
            "PNG extension should be supported"
        }
        val jpg = requireNotNull(MediaStoreExporter.specFor("a.jpg")) {
            "JPG extension should be supported"
        }
        val mp4 = requireNotNull(MediaStoreExporter.specFor("a.mp4")) {
            "MP4 extension should be supported"
        }
        val webp = requireNotNull(MediaStoreExporter.specFor("a.webp")) {
            "WEBP extension should be supported"
        }

        assertEquals("image/png", png.mimeType)
        assertEquals("image/jpeg", jpg.mimeType)
        assertEquals("video/mp4", mp4.mimeType)
        assertEquals(MediaStoreExporter.Kind.VIDEO, mp4.kind)
        assertEquals("image/webp", webp.mimeType)
        assertEquals(MediaStoreExporter.Kind.IMAGE, webp.kind)

        assertNull(
            "EXE files must not be supported",
            MediaStoreExporter.specFor("a.exe")
        )
    }
}
