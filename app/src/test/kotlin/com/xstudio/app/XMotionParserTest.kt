package com.xstudio.app

import com.xstudio.app.xmotion.MotionLayer
import com.xstudio.app.xmotion.XMotionProject
import com.xstudio.app.xmotion.parseAlightMotionXml
import com.xstudio.app.xmotion.visibleLayerCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XMotionParserTest {
    private val xml = """
        <scene width="1080" height="1920" fps="30" title="Demo">
          <media id="m1"/>
          <shape id="a" label="Box" startTime="0" endTime="3000" s="round">
            <transform>
              <location value="540,960"/><scale value="2,2"/><rotation value="15"/><opacity value="0.5"/><skew value="1"/>
            </transform>
            <fillColor value="#FF0000FF"/>
            <effect id="e"/>
          </shape>
          <audio id="s1"/>
        </scene>
    """.trimIndent()

    @Test fun parsesSupportedAndReportsUnsupported() {
        val p = parseAlightMotionXml(xml, "x")
        assertEquals("Demo", p.name)
        assertEquals(1, p.layers.size)
        val l = p.layers[0]
        assertTrue(l.rounded)
        assertEquals(2f, l.scale, 0.001f)
        assertEquals(15f, l.rotationDeg, 0.001f)
        assertEquals(0.5f, l.opacity, 0.001f)
        assertEquals(260f, l.offsetX, 0.5f)
        assertEquals(xml, p.sourceXml)
        assertEquals(1, p.unsupportedElements["media"])
        assertEquals(1, p.unsupportedElements["audio"])
        assertEquals(1, p.unsupportedElements["shape/effect"])
        assertEquals(1, p.unsupportedElements["shape/transform/skew"])
        assertFalse(p.xmlNotice!!.contains("preserved in XML metadata"))
        assertTrue(p.xmlNotice!!.contains("NOT imported"))
    }

    @Test(expected = IllegalStateException::class) fun rejectsDoctype() {
        parseAlightMotionXml("<!DOCTYPE x><scene/>", "x")
    }

    @Test fun visibleLayerCountUsesTimeRange() {
        val p = XMotionProject(layers = mutableListOf(MotionLayer("1", "a", startMs = 1000, endMs = 2000)))
        assertEquals(0, visibleLayerCount(p, 500f))
        assertEquals(1, visibleLayerCount(p, 1500f))
    }
}
