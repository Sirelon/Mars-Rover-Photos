package com.sirelon.marsroverphotos.feature.mission

import com.sirelon.marsroverphotos.feature.rovers.CuriosityId
import com.sirelon.marsroverphotos.feature.rovers.OpportunityId
import com.sirelon.marsroverphotos.feature.rovers.SpiritId
import com.sirelon.marsroverphotos.feature.rovers.InsightId
import com.sirelon.marsroverphotos.feature.rovers.PerserveranceId

/**
 * Data class representing a rover camera specification.
 */
data class CameraSpec(
    val name: String,
    val fullName: String,
    val description: String
)

/**
 * Static data for rover cameras and mission information.
 */
object RoverMissionData {

    /**
     * Get camera specifications for a specific rover.
     * @param roverId The ID of the rover
     * @return List of camera specifications
     */
    fun getCamerasForRover(roverId: Long): List<CameraSpec> {
        return when (roverId) {
            CuriosityId -> curiosityCameras
            OpportunityId -> opportunityCameras
            SpiritId -> spiritCameras
            PerserveranceId -> perseveranceCameras
            InsightId -> insightCameras
            else -> emptyList()
        }
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

    private val perseveranceCameras = listOf(
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
            name = "NAVCAM",
            fullName = "Navigation Camera",
            description = "Stereo cameras for 3D terrain mapping and navigation planning"
        ),
        CameraSpec(
            name = "MCZ",
            fullName = "Mastcam-Z",
            description = "Zoomable stereo color camera system on the mast"
        ),
        CameraSpec(
            name = "SHERLOC",
            fullName = "Scanning Habitable Environments with Raman & Luminescence for Organics & Chemicals",
            description = "Spectrometer and imaging system on the robotic arm"
        ),
        CameraSpec(
            name = "SUPERCAM",
            fullName = "SuperCam",
            description = "Remote sensing instrument with camera, laser, and spectrometers"
        ),
        CameraSpec(
            name = "EDL",
            fullName = "Entry, Descent, and Landing Cameras",
            description = "Multiple cameras captured the landing sequence"
        )
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
