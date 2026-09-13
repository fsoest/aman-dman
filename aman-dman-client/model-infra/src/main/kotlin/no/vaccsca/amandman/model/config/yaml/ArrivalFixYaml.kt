package no.vaccsca.amandman.model.config.yaml

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import jakarta.validation.constraints.NotNull

data class ArrivalProfileYaml(
    @field:NotNull
    @field:JsonPropertyDescription("Arrival name or procedure name. One or multiple wildcards (*) can be used to match multiple arrival names")
    val arrivalName: String,

    @field:JsonPropertyDescription("Optional airport area id that makes matching arrivals frozen for sequencing while inside the area")
    val frozenSequenceArea: String? = null,

    @field:JsonPropertyDescription("Optional airport area id. Aircraft inside this area are included in the active sequencing window regardless of sequencingHorizon.")
    val sequencingArea: String? = null,

    @field:NotNull
    val fixes: List<ArrivalProfileFixYaml> = emptyList(),

    @field:JsonPropertyDescription("Optional override of the route flown after a given fix, for STARs that are rarely flown in full (e.g. downwind procedures routinely cut short by vectoring)")
    val routeOverride: RouteOverrideYaml? = null,
)

data class RouteOverrideYaml(
    @field:NotNull
    @field:JsonPropertyDescription("Fix after which the filed route is unreliable and should be replaced by 'waypoints'")
    val afterFix: String,

    @field:NotNull
    @field:JsonPropertyDescription("The fixes actually flown after afterFix, in order. Must be real fixes already present on the STAR.")
    val waypoints: List<String> = emptyList(),
)

data class ArrivalProfileFixYaml(
    @field:NotNull
    val fix: String,

    val role: ArrivalFixRoleYaml? = null,

    @field:JsonPropertyDescription("Expected altitude at the fix in feet AMSL")
    val altitude: Int? = null,

    @field:JsonPropertyDescription("Expected indicated airspeed in knots (IAS)")
    val speed: Int? = null,
)

enum class ArrivalFixRoleYaml {
    IF,
    IAF,
}
