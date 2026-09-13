package no.vaccsca.amandman.model.planning

import no.vaccsca.amandman.model.airport.RouteOverride
import no.vaccsca.amandman.model.atc.ExtractedRoutePoint
import no.vaccsca.amandman.model.navigation.Waypoint
import org.slf4j.LoggerFactory

/**
 * Replaces the tail of a filed route with a curated set of waypoints for STARs that are rarely
 * flown in full (e.g. downwind procedures routinely cut short by vectoring), so ETA/trajectory
 * calculations reflect how the approach is actually flown rather than the published procedure.
 */
object RouteOverrideService {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Applies [override] to [remainingWaypoints]. If the aircraft hasn't yet reached
     * [RouteOverride.afterFix], everything past it is replaced by the resolved override
     * waypoints. If the aircraft has already flown past it, the entire remaining route is
     * replaced. If [RouteOverride.afterFix] isn't found on this route at all (active or
     * already passed), [remainingWaypoints] is returned unchanged.
     */
    fun apply(
        remainingWaypoints: List<Waypoint>,
        extractedRoute: List<ExtractedRoutePoint>,
        override: RouteOverride,
    ): List<Waypoint> {
        val afterFixIndex = remainingWaypoints.indexOfFirst { it.id.equals(override.afterFix, ignoreCase = true) }
        val crossed = afterFixIndex == -1 && hasCrossed(override.afterFix, extractedRoute)

        if (afterFixIndex == -1 && !crossed) {
            return remainingWaypoints
        }

        val overrideWaypoints = resolveWaypoints(override, extractedRoute)
        if (overrideWaypoints.isEmpty()) {
            return remainingWaypoints
        }

        return if (crossed) {
            overrideWaypoints
        } else {
            remainingWaypoints.subList(0, afterFixIndex + 1) + overrideWaypoints
        }
    }

    /**
     * Whether [fixId] appears among the already passed/skipped points of [extractedRoute],
     * i.e. the aircraft has flown past it.
     */
    fun hasCrossed(fixId: String, extractedRoute: List<ExtractedRoutePoint>): Boolean {
        val firstActiveIndex = extractedRoute.indexOfFirst { it.isActive }
        val bypassedPoints = if (firstActiveIndex == -1) extractedRoute else extractedRoute.take(firstActiveIndex)
        return bypassedPoints.any { !it.isActive && it.id.equals(fixId, ignoreCase = true) }
    }

    private fun resolveWaypoints(override: RouteOverride, extractedRoute: List<ExtractedRoutePoint>): List<Waypoint> {
        val byId = extractedRoute.associateBy { it.id.uppercase() }
        return override.waypointNames.mapNotNull { name ->
            val point = byId[name.uppercase()]
            if (point == null) {
                logger.warn(
                    "Route override waypoint '$name' (after ${override.afterFix}) not found on the extracted route; skipping it."
                )
                null
            } else {
                Waypoint(id = point.id, latLng = point.latLng)
            }
        }
    }
}
