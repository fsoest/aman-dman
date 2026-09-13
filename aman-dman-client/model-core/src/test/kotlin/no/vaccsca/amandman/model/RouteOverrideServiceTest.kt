package no.vaccsca.amandman.model

import no.vaccsca.amandman.model.airport.RouteOverride
import no.vaccsca.amandman.model.atc.ExtractedRoutePoint
import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.navigation.Waypoint
import no.vaccsca.amandman.model.planning.RouteOverrideService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteOverrideServiceTest {

    private val override = RouteOverride(afterFix = "ANEKI", waypointNames = listOf("FINAL"))

    @Test
    fun `splices override waypoints after afterFix when it is still ahead`() {
        val remainingWaypoints = listOf(
            Waypoint("ANEKI", LatLng(50.0, 8.0)),
            Waypoint("VEC1", LatLng(50.1, 8.1)),
            Waypoint("VEC2", LatLng(50.2, 8.2)),
        )
        val extractedRoute = listOf(
            ExtractedRoutePoint("ANEKI", LatLng(50.0, 8.0), isActive = true),
            ExtractedRoutePoint("VEC1", LatLng(50.1, 8.1), isActive = true),
            ExtractedRoutePoint("VEC2", LatLng(50.2, 8.2), isActive = true),
            ExtractedRoutePoint("FINAL", LatLng(50.3, 8.3), isActive = false),
        )

        val result = RouteOverrideService.apply(remainingWaypoints, extractedRoute, override)

        assertEquals(listOf("ANEKI", "FINAL"), result.map { it.id })
        assertEquals(LatLng(50.3, 8.3), result.last().latLng)
    }

    @Test
    fun `replaces the whole route when afterFix has already been crossed`() {
        val remainingWaypoints = listOf(
            Waypoint("VEC1", LatLng(50.1, 8.1)),
            Waypoint("VEC2", LatLng(50.2, 8.2)),
        )
        val extractedRoute = listOf(
            ExtractedRoutePoint("ANEKI", LatLng(50.0, 8.0), isActive = false),
            ExtractedRoutePoint("VEC1", LatLng(50.1, 8.1), isActive = true),
            ExtractedRoutePoint("VEC2", LatLng(50.2, 8.2), isActive = true),
            ExtractedRoutePoint("FINAL", LatLng(50.3, 8.3), isActive = true),
        )

        val result = RouteOverrideService.apply(remainingWaypoints, extractedRoute, override)

        assertEquals(listOf("FINAL"), result.map { it.id })
    }

    @Test
    fun `leaves the route unchanged when afterFix is not on the route at all`() {
        val remainingWaypoints = listOf(
            Waypoint("OTHER1", LatLng(50.1, 8.1)),
            Waypoint("OTHER2", LatLng(50.2, 8.2)),
        )
        val extractedRoute = listOf(
            ExtractedRoutePoint("OTHER1", LatLng(50.1, 8.1), isActive = true),
            ExtractedRoutePoint("OTHER2", LatLng(50.2, 8.2), isActive = true),
        )

        val result = RouteOverrideService.apply(remainingWaypoints, extractedRoute, override)

        assertEquals(remainingWaypoints, result)
    }

    @Test
    fun `leaves the route unchanged when no override waypoint can be resolved`() {
        val remainingWaypoints = listOf(
            Waypoint("ANEKI", LatLng(50.0, 8.0)),
            Waypoint("VEC1", LatLng(50.1, 8.1)),
        )
        val extractedRoute = listOf(
            ExtractedRoutePoint("ANEKI", LatLng(50.0, 8.0), isActive = true),
            ExtractedRoutePoint("VEC1", LatLng(50.1, 8.1), isActive = true),
        )

        val result = RouteOverrideService.apply(remainingWaypoints, extractedRoute, override)

        assertEquals(remainingWaypoints, result)
    }

    @Test
    fun `hasCrossed is true only for fixes bypassed before the first active point`() {
        val extractedRoute = listOf(
            ExtractedRoutePoint("ANEKI", LatLng(50.0, 8.0), isActive = false),
            ExtractedRoutePoint("VEC1", LatLng(50.1, 8.1), isActive = true),
            ExtractedRoutePoint("VEC2", LatLng(50.2, 8.2), isActive = true),
        )

        assertTrue(RouteOverrideService.hasCrossed("ANEKI", extractedRoute))
        assertFalse(RouteOverrideService.hasCrossed("VEC1", extractedRoute))
        assertFalse(RouteOverrideService.hasCrossed("UNKNOWN", extractedRoute))
    }
}
