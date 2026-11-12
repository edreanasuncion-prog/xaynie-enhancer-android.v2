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
    @get:Rule val tmp = TemporaryFolder()

    @Test fun photoTtlIsExactly72Hours() {
        assertEquals(72L * 60 * 60 * 1000, HistoryFiles.PHOTO_TTL_MS)
        assertEquals(1_000L + 259_200_000L, HistoryFiles.photoExpiryFor(1_000L))
    }

    @Test fun expiryBoundary() {
        val exp = HistoryFiles.photoExpiryFor(0L)
        assertFalse(HistoryFiles.isExpired(exp, exp - 1))
        assertTrue(HistoryFiles.isExpired(exp, exp))
        assertFalse(HistoryFiles.isExpired(null, Long.MAX_VALUE)) // videos never expire
    }

    @Test fun pathGuard() {
        val root = tmp.newFolder("app")
        val inside = File(root, "enhanced_photos/a.jpg").apply { parentFile!!.mkdirs(); writeText("x") }
        val outside = tmp.newFile("other.jpg")
        val traversal = File(root, "../other.jpg")
        assertTrue(HistoryFiles.isStrictlyInside(root, inside))
        assertFalse(HistoryFiles.isStrictlyInside(root, outside))
        assertFalse(HistoryFiles.isStrictlyInside(root, traversal))
        assertFalse(HistoryFiles.isStrictlyInside(root, root))
    }

    @Test fun mimeTypesFollowExtension() {
        assertEquals("image/png", MediaStoreExporter.specFor("a.PNG")!!.mimeType)
        assertEquals("image/jpeg", MediaStoreExporter.specFor("a.jpg")!!.mimeType)
        assertEquals("video/mp4", MediaStoreExporter.specFor("a.mp4")!!.mimeType)
        assertEquals(MediaStoreExporter.Kind.VIDEO, MediaStoreExporter.specFor("a.mp4")!!.kind)
        assertNull(MediaStoreExporter.specFor("a.exe"))
        assertNotNull(MediaStoreExporter.specFor("a.webp"))
    }
}
