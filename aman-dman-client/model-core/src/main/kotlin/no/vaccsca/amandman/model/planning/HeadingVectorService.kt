package no.vaccsca.amandman.model.planning

import no.vaccsca.amandman.model.airport.RunwayThreshold
import no.vaccsca.amandman.model.navigation.LatLng
import no.vaccsca.amandman.model.navigation.Waypoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estimates a route for an aircraft currently flying an ATC-assigned heading rather than
 * tracking toward its next filed waypoint. TMA vectoring is almost always used to set up an
 * intercept of the final approach course, so this projects the assigned heading forward and
 * finds where it crosses the extended runway centerline, rather than assuming the aircraft
 * will fly (a straight line to) the rest of its filed/overridden route.
 */
object HeadingVectorService {

    private const val NM_PER_DEGREE_LAT = 60.0
    private const val MAX_INTERCEPT_DISTANCE_NM = 60.0
    private const val MAX_HEADING_DISTANCE_NM = 60.0

    /**
     * Returns a single-waypoint route to where [assignedHeadingDeg], flown from
     * [currentPosition], intercepts the extended final approach course for [runway] - or null
     * if no sane intercept exists (e.g. the heading points away from or roughly parallel to the
     * course), in which case the caller should fall back to the filed/overridden route.
     */
    fun projectOntoFinalApproachCourse(
        currentPosition: LatLng,
        assignedHeadingDeg: Int,
        runway: RunwayThreshold,
    ): List<Waypoint>? {
        val origin = runway.latLng
        val meanLatRad = Math.toRadians((origin.lat + currentPosition.lat) / 2.0)
        val lonToNm = NM_PER_DEGREE_LAT * cos(meanLatRad)
        if (lonToNm == 0.0) return null

        val aircraftX = (currentPosition.lon - origin.lon) * lonToNm
        val aircraftY = (currentPosition.lat - origin.lat) * NM_PER_DEGREE_LAT

        // The extended final approach course points outward from the threshold, opposite the
        // inbound course aircraft fly once established (runway.trueHeading).
        val courseRad = Math.toRadians((runway.trueHeading + 180.0) % 360.0)
        val courseX = sin(courseRad)
        val courseY = cos(courseRad)

        val headingRad = Math.toRadians(assignedHeadingDeg.toDouble())
        val headingX = sin(headingRad)
        val headingY = cos(headingRad)

        // Solve d*courseVec - s*headingVec = aircraftXY (origin is the runway threshold, so
        // runwayXY is (0,0)) for d (distance from the runway along the course) and s (distance
        // from the aircraft along its heading).
        val det = -courseX * headingY + headingX * courseY
        if (det == 0.0) return null // heading is parallel to the approach course

        val d = (-aircraftX * headingY + headingX * aircraftY) / det
        val s = (courseX * aircraftY - courseY * aircraftX) / det

        if (d <= 0.0 || d > MAX_INTERCEPT_DISTANCE_NM) return null
        if (s <= 0.0 || s > MAX_HEADING_DISTANCE_NM) return null

        val interceptPoint = LatLng(
            lat = origin.lat + (d * courseY) / NM_PER_DEGREE_LAT,
            lon = origin.lon + (d * courseX) / lonToNm,
        )

        return listOf(Waypoint(id = "VECTOR_INTERCEPT", latLng = interceptPoint))
    }
}
