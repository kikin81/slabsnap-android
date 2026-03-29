package us.kikinsoft.slabsnap.ui.scanner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapToViewCoordinatesTest {

    companion object {
        private const val DELTA = 0.01f
    }

    @Test
    fun `GIVEN image matches view size WHEN rotation is 0 THEN rect is unchanged`() {
        // Given / When
        val result = mapToViewCoordinates(
            rectLeft = 100f,
            rectTop = 200f,
            rectRight = 300f,
            rectBottom = 400f,
            imageWidth = 1080,
            imageHeight = 1920,
            rotationDegrees = 0,
            viewWidth = 1080f,
            viewHeight = 1920f,
        )

        // Then
        assertEquals(100f, result.left, DELTA)
        assertEquals(200f, result.top, DELTA)
        assertEquals(300f, result.right, DELTA)
        assertEquals(400f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN portrait rotation 90 WHEN image dims are swapped THEN source dims are corrected`() {
        // Given — ImageProxy reports 1920x1080 (landscape buffer), rotation 90
        // After swap: sourceW=1080, sourceH=1920 — matches view exactly

        // When
        val result = mapToViewCoordinates(
            rectLeft = 50f,
            rectTop = 100f,
            rectRight = 150f,
            rectBottom = 300f,
            imageWidth = 1920,
            imageHeight = 1080,
            rotationDegrees = 90,
            viewWidth = 1080f,
            viewHeight = 1920f,
        )

        // Then — 1:1 scale, no offset
        assertEquals(50f, result.left, DELTA)
        assertEquals(100f, result.top, DELTA)
        assertEquals(150f, result.right, DELTA)
        assertEquals(300f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN rotation 270 WHEN image dims are swapped THEN source dims are corrected`() {
        // Given / When
        val result = mapToViewCoordinates(
            rectLeft = 0f,
            rectTop = 0f,
            rectRight = 100f,
            rectBottom = 100f,
            imageWidth = 1920,
            imageHeight = 1080,
            rotationDegrees = 270,
            viewWidth = 1080f,
            viewHeight = 1920f,
        )

        // Then — 1:1 scale, no offset
        assertEquals(0f, result.left, DELTA)
        assertEquals(0f, result.top, DELTA)
        assertEquals(100f, result.right, DELTA)
        assertEquals(100f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN rotation 180 WHEN no swap needed THEN dims used as-is`() {
        // Given / When
        val result = mapToViewCoordinates(
            rectLeft = 50f,
            rectTop = 50f,
            rectRight = 150f,
            rectBottom = 150f,
            imageWidth = 1080,
            imageHeight = 1920,
            rotationDegrees = 180,
            viewWidth = 1080f,
            viewHeight = 1920f,
        )

        // Then — 1:1 scale, no offset
        assertEquals(50f, result.left, DELTA)
        assertEquals(50f, result.top, DELTA)
        assertEquals(150f, result.right, DELTA)
        assertEquals(150f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN FILL_CENTER wider view WHEN scaling THEN horizontal offset centers`() {
        // Given — source 1000x1000 (square), view 2000x1000 (wide)
        // scale = max(2000/1000, 1000/1000) = 2.0
        // dx = (2000 - 1000*2) / 2 = 0, dy = (1000 - 1000*2) / 2 = -500

        // When
        val result = mapToViewCoordinates(
            rectLeft = 100f,
            rectTop = 100f,
            rectRight = 200f,
            rectBottom = 200f,
            imageWidth = 1000,
            imageHeight = 1000,
            rotationDegrees = 0,
            viewWidth = 2000f,
            viewHeight = 1000f,
        )

        // Then — scale=2, dx=0, dy=-500
        assertEquals(200f, result.left, DELTA)
        assertEquals(-300f, result.top, DELTA)
        assertEquals(400f, result.right, DELTA)
        assertEquals(-100f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN FILL_CENTER taller view WHEN scaling THEN vertical offset centers`() {
        // Given — source 1000x1000 (square), view 1000x2000 (tall)
        // scale = max(1000/1000, 2000/1000) = 2.0
        // dx = (1000 - 1000*2) / 2 = -500, dy = (2000 - 1000*2) / 2 = 0

        // When
        val result = mapToViewCoordinates(
            rectLeft = 100f,
            rectTop = 100f,
            rectRight = 200f,
            rectBottom = 200f,
            imageWidth = 1000,
            imageHeight = 1000,
            rotationDegrees = 0,
            viewWidth = 1000f,
            viewHeight = 2000f,
        )

        // Then — scale=2, dx=-500, dy=0
        assertEquals(-300f, result.left, DELTA)
        assertEquals(200f, result.top, DELTA)
        assertEquals(-100f, result.right, DELTA)
        assertEquals(400f, result.bottom, DELTA)
    }

    @Test
    fun `GIVEN typical phone portrait WHEN 90 rotation with mismatched aspect THEN correct`() {
        // Given — ImageProxy: 1920x1080 (landscape buffer), rotation 90
        // After swap: sourceW=1080, sourceH=1920
        // View: 1080x2400 (taller phone)
        // scale = max(1080/1080, 2400/1920) = max(1.0, 1.25) = 1.25
        // dx = (1080 - 1080*1.25) / 2 = -135, dy = (2400 - 1920*1.25) / 2 = 0

        // When
        val result = mapToViewCoordinates(
            rectLeft = 200f,
            rectTop = 400f,
            rectRight = 600f,
            rectBottom = 800f,
            imageWidth = 1920,
            imageHeight = 1080,
            rotationDegrees = 90,
            viewWidth = 1080f,
            viewHeight = 2400f,
        )

        // Then — scale=1.25, dx=-135, dy=0
        assertEquals(115f, result.left, DELTA)
        assertEquals(500f, result.top, DELTA)
        assertEquals(615f, result.right, DELTA)
        assertEquals(1000f, result.bottom, DELTA)
    }
}
