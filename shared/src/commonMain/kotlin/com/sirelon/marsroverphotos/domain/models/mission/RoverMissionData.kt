package com.sirelon.marsroverphotos.domain.models.mission

import com.sirelon.marsroverphotos.domain.models.*

/**
 * Static data for rover cameras and mission information.
 */
object RoverMissionData {

    /**
     * Get camera specifications for a specific rover.
     */
    fun getCamerasForRover(roverId: Long): List<CameraSpec> {
        return when (roverId) {
            CURIOSITY_ID -> curiosityCameras
            OPPORTUNITY_ID -> opportunityCameras
            SPIRIT_ID -> spiritCameras
            PERSEVERANCE_ID -> perseveranceCameras
            INSIGHT_ID -> insightCameras
            else -> emptyList()
        }
    }

    /** Landing site displayed in the mission hero and timeline (e.g. "Jezero Crater, Mars"). */
    fun getLandingLocation(roverId: Long): String = when (roverId) {
        CURIOSITY_ID    -> "Gale Crater, Mars"
        OPPORTUNITY_ID  -> "Meridiani Planum, Mars"
        SPIRIT_ID       -> "Gusev Crater, Mars"
        PERSEVERANCE_ID -> "Jezero Crater, Mars"
        INSIGHT_ID      -> "Elysium Planitia, Mars"
        else            -> "Mars"
    }

    /** One-sentence mission overview shown below the hero image. */
    fun getMissionDescription(roverId: Long): String = when (roverId) {
        CURIOSITY_ID    -> "Exploring the habitability of ancient Mars and searching for organic molecules in Gale Crater — a 154 km-wide impact crater that once held a lake."
        OPPORTUNITY_ID  -> "Discovered evidence of ancient water on Mars during a record-breaking 15-year roving mission across Meridiani Planum."
        SPIRIT_ID       -> "Explored Gusev Crater and uncovered evidence of past volcanic and hydrothermal activity over a six-year surface mission."
        PERSEVERANCE_ID -> "Hunting for signs of ancient microbial life and caching rock samples in Jezero Crater — a former lake bed that held liquid water billions of years ago."
        INSIGHT_ID      -> "Studying the deep interior structure of Mars using seismic instruments and a burrowing heat probe at Elysium Planitia."
        else            -> ""
    }

    private val curiosityCameras = listOf(
        CameraSpec(
            name = "FHAZ",
            fullName = "Front Hazard Avoidance Camera",
            description = "Mounted on the rover's lower front, detects hazards in the path ahead"
        ),
        CameraSpec(
            name = "RHAZ",
            fullName = "Rear Hazard Avoidance Camera",
            description = "Mounted on the rover's lower rear, monitors the area behind the rover"
        ),
        CameraSpec(
            name = "MAST",
            fullName = "Mast Camera",
            description = "High-resolution color imaging system mounted on the mast"
        ),
        CameraSpec(
            name = "CHEMCAM",
            fullName = "Chemistry and Camera Complex",
            description = "Fires laser pulses to vaporize rock samples and analyze their composition"
        ),
        CameraSpec(
            name = "MAHLI",
            fullName = "Mars Hand Lens Imager",
            description = "Close-up camera on the robotic arm for detailed rock and soil imaging"
        ),
        CameraSpec(
            name = "MARDI",
            fullName = "Mars Descent Imager",
            description = "Captured images during descent and landing"
        ),
        CameraSpec(
            name = "NAVCAM",
            fullName = "Navigation Camera",
            description = "Stereo cameras for 3D terrain mapping and navigation planning"
        )
    )

    private val opportunityCameras = listOf(
        CameraSpec(
            name = "FHAZ",
            fullName = "Front Hazard Avoidance Camera",
            description = "Black and white cameras for obstacle detection"
        ),
        CameraSpec(
            name = "RHAZ",
            fullName = "Rear Hazard Avoidance Camera",
            description = "Monitors the terrain behind the rover"
        ),
        CameraSpec(
            name = "NAVCAM",
            fullName = "Navigation Camera",
            description = "Stereo pair of cameras for navigation and panoramic imaging"
        ),
        CameraSpec(
            name = "PANCAM",
            fullName = "Panoramic Camera",
            description = "High-resolution color cameras mounted on the mast"
        ),
        CameraSpec(
            name = "MINITES",
            fullName = "Miniature Thermal Emission Spectrometer",
            description = "Identifies minerals in rocks and soils from a distance"
        )
    )

    private val spiritCameras = listOf(
        CameraSpec(
            name = "FHAZ",
            fullName = "Front Hazard Avoidance Camera",
            description = "Black and white cameras for obstacle detection"
        ),
        CameraSpec(
            name = "RHAZ",
            fullName = "Rear Hazard Avoidance Camera",
            description = "Monitors the terrain behind the rover"
        ),
        CameraSpec(
            name = "NAVCAM",
            fullName = "Navigation Camera",
            description = "Stereo pair of cameras for navigation and panoramic imaging"
        ),
        CameraSpec(
            name = "PANCAM",
            fullName = "Panoramic Camera",
            description = "High-resolution color cameras mounted on the mast"
        ),
        CameraSpec(
            name = "MINITES",
            fullName = "Miniature Thermal Emission Spectrometer",
            description = "Identifies minerals in rocks and soils from a distance"
        )
    )

    // fullName values are prefixes of the instrument identifiers the NASA Mars 2020 API
    // returns in the "instrument" field (e.g. "FRONT_HAZCAM_LEFT_A", "NAVCAM_LEFT").
    // filterByCameras uses startsWith matching against these prefixes.
    private val perseveranceCameras = listOf(
        CameraSpec(
            name = "FHAZ",
            fullName = "FRONT_HAZCAM",
            description = "Mounted on the rover's lower front, detects hazards in the path ahead"
        ),
        CameraSpec(
            name = "RHAZ",
            fullName = "REAR_HAZCAM",
            description = "Mounted on the rover's lower rear, monitors the area behind the rover"
        ),
        CameraSpec(
            name = "NAVCAM",
            fullName = "NAVCAM",
            description = "Stereo cameras for 3D terrain mapping and navigation planning"
        ),
        CameraSpec(
            name = "MCZ",
            fullName = "MCZ",
            description = "Zoomable stereo color camera system on the mast"
        ),
        CameraSpec(
            name = "SHERLOC",
            fullName = "SHERLOC",
            description = "Arm-mounted close-up imager for rock and soil imaging"
        ),
        CameraSpec(
            name = "SUPERCAM",
            fullName = "SUPERCAM",
            description = "Remote sensing instrument with camera, laser, and spectrometers"
        ),
        CameraSpec(
            name = "SKYCAM",
            fullName = "SKYCAM",
            description = "Sky-facing camera for atmospheric imaging"
        ),
    )

    private val insightCameras = listOf(
        CameraSpec(
            name = "ICC",
            fullName = "Instrument Context Camera",
            description = "Mounted on the robotic arm, provides images of instruments and workspace"
        ),
        CameraSpec(
            name = "IDC",
            fullName = "Instrument Deployment Camera",
            description = "Wide-angle camera on the robotic arm for deployment monitoring"
        )
    )
}
