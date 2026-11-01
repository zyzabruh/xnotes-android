package com.xnotes.ui.theme

import com.xnotes.core.model.Rgba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPaletteTest {

    private val seed = Rgba(0, 230, 118) // the default green accent

    private fun luminance(c: Rgba): Double =
        (0.299 * c.r + 0.587 * c.g + 0.114 * c.b) / 255.0

    @Test fun contentAccentTurnsToReadOnTheNotesPaper() {
        // A dark theme's pale accent over white paper: this is the selection and lasso the
        // reporter could not see (issue #325).
        val white = Rgba(255, 255, 255, 255)
        val pale = Palette.DEFAULT.copy(accent = Rgba(200, 220, 255, 255))
        val ink = pale.accentOn(white)
        assertTrue(ColorMath.luminance(ink) < ColorMath.luminance(white) - 0.3)
        // ...and it stays a blue rather than collapsing to black.
        assertTrue(ink.b > ink.r)
        assertEquals(255, ink.a)

        // On black paper the same accent has to go the other way.
        val dark = Palette.DEFAULT.copy(accent = Rgba(30, 40, 50, 255))
        assertTrue(ColorMath.luminance(dark.accentOn(Rgba(0, 0, 0, 255))) > 0.3)

        // An accent that already reads is left exactly as it is.
        val black = Palette.DEFAULT.copy(accent = Rgba(0, 0, 0, 255))
        assertEquals(Rgba(0, 0, 0, 255), black.accentOn(white))
    }

    @Test fun contentAccentClearsTheGapOnAnyPaper() {
        val papers = listOf(
            Rgba(255, 255, 255, 255), Rgba(250, 248, 240, 255),
            Rgba(0, 0, 0, 255), Rgba(40, 44, 52, 255), Rgba(128, 128, 128, 255),
        )
        for (paper in papers) {
            val ink = Palette.DEFAULT.accentOn(paper)
            assertTrue(
                "accent on ${paper.r},${paper.g},${paper.b} stays too close to the paper",
                Math.abs(ColorMath.luminance(ink) - ColorMath.luminance(paper)) >= 0.3,
            )
        }
    }

    @Test fun materialAppearanceMapping() {
        val dark = MaterialColors.seeded(seed, dark = true)
        val light = MaterialColors.seeded(seed, dark = false)
        assertTrue(Palette.material("dark", dark).isDark)
        assertTrue(Palette.material("oled", dark).isDark)
        assertFalse(Palette.material("light", light).isDark)
    }

    @Test fun seededLightSurfacesStepDownFromWhitePaper() {
        val m = MaterialColors.seeded(seed, dark = false)
        assertEquals(Rgba(255, 255, 255, 255), m.surfaceContainerLowest)
        val p = Palette.materialLight(m)
        assertEquals(m.surfaceContainerLowest, p.paper)
        assertTrue(luminance(p.paper) > luminance(p.menuBg))
        assertTrue(luminance(p.menuBg) > luminance(p.panel))
        assertTrue(luminance(p.panel) > luminance(p.bg))
        assertTrue(luminance(p.bg) > luminance(p.surface))
        assertTrue(luminance(p.surface) > luminance(p.surfaceHi))
        assertTrue(luminance(p.text) < 0.3)
        // The backstage background must sit visibly below white paper, not wash out.
        assertTrue(luminance(p.menuBg) < 0.95)
    }

    @Test fun seededDarkSurfacesStayDarkWithPastelPrimary() {
        val m = MaterialColors.seeded(seed, dark = true)
        val p = Palette.materialDark(m)
        assertEquals(m.surfaceContainerLow, p.paper)
        assertTrue(luminance(p.bg) < 0.1)
        assertTrue(luminance(p.paper) < 0.2)
        assertTrue(luminance(p.accent) > 0.5)
        assertTrue(luminance(p.text) > 0.7)
    }

    @Test fun materialOledBigSurfacesArePitchBlack() {
        val p = Palette.materialOled(MaterialColors.seeded(seed, dark = true))
        val black = Rgba(0, 0, 0, 255)
        assertEquals(black, p.bg)
        assertEquals(black, p.panel)
        assertEquals(black, p.paper)
        assertEquals(black, p.menuBg)
        assertTrue(p.isDark)
        assertTrue(luminance(p.surface) > 0.0)
    }

    @Test fun materialBordersSitCloserToTheChromeThanOutlineVariant() {
        val light = MaterialColors.seeded(seed, dark = false)
        val lp = Palette.materialLight(light)
        assertTrue(
            kotlin.math.abs(luminance(lp.border) - luminance(lp.menuBg)) <
                kotlin.math.abs(luminance(light.outlineVariant) - luminance(lp.menuBg)),
        )
        val dark = MaterialColors.seeded(seed, dark = true)
        val dp = Palette.materialDark(dark)
        assertTrue(
            kotlin.math.abs(luminance(dp.border) - luminance(dp.bg)) <
                kotlin.math.abs(luminance(dark.outlineVariant) - luminance(dp.bg)),
        )
    }

    @Test fun seededIsDeterministic() {
        assertEquals(MaterialColors.seeded(seed, true), MaterialColors.seeded(seed, true))
        assertEquals(MaterialColors.seeded(seed, false), MaterialColors.seeded(seed, false))
    }
}
