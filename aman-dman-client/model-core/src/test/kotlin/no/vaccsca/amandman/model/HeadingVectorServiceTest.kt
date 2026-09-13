package no.vaccsca.amandman.model

import no.vaccsca.amandman.model.airport.RunwayThreshold
import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.planning.HeadingVectorService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeadingVectorServiceTest {

    // Runway faces due north (trueHeading 0), so the extended final approach course runs
    // due south of the threshold - i.e. along local (east=0, north=-d) for d > 0.
    private val runway = RunwayThreshold(
        id = "01",
        latLng = LatLng(0.0, 0.0),
        elevation = 0f,
        trueHeading = 0f,
    )

    @Test
    fun `intercepts the final approach course when heading crosses it ahead of the aircraft`() {
        // 10 NM east, 20 NM south of the threshold, flying due west (270) toward the course.
        val position = LatLng(lat = -20.0 / 60.0, lon = 10.0 / 60.0)

        val result = HeadingVectorService.projectOntoFinalApproachCourse(
            currentPosition = position,
            assignedHeadingDeg = 270,
            runway = runway,
        )

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("VECTOR_INTERCEPT", result.single().id)
        assertEquals(0.0, result.single().latLng.lon, 1e-6)
        assertEquals(-20.0 / 60.0, result.single().latLng.lat, 1e-6)
    }

    @Test
    fun `returns null when the heading points away from the course`() {
        val position = LatLng(lat = -20.0 / 60.0, lon = 10.0 / 60.0)

        val result = HeadingVectorService.projectOntoFinalApproachCourse(
            currentPosition = position,
            assignedHeadingDeg = 90, // due east, away from the course at lon 0
            runway = runway,
        )

        assertNull(result)
    }

    @Test
    fun `returns null when the heading is parallel to the course`() {
        val position = LatLng(lat = -20.0 / 60.0, lon = 10.0 / 60.0)

        val result = HeadingVectorService.projectOntoFinalApproachCourse(
            currentPosition = position,
            assignedHeadingDeg = 0, // parallel to the north-south course, never crosses it
            runway = runway,
        )

        assertNull(result)
    }

    @Test
    fun `returns null when the intercept is behind the aircraft`() {
        // North of the threshold - behind the inbound course, which only extends south of it.
        val position = LatLng(lat = 10.0 / 60.0, lon = 10.0 / 60.0)

        val result = HeadingVectorService.projectOntoFinalApproachCourse(
            currentPosition = position,
            assignedHeadingDeg = 270,
            runway = runway,
        )

        assertNull(result)
    }
}
