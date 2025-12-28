package com.sirelon.marsroverphotos.firebase.mission

internal fun mapMissionFacts(
    roverId: Long,
    data: Map<String, Any?>
): RoverMissionFacts {
    return RoverMissionFacts(
        roverId = (data["roverId"] as? Number)?.toLong() ?: roverId,
        roverName = data["roverName"] as? String ?: "",
        objectives = parseStringList(data["objectives"]),
        funFacts = parseStringList(data["funFacts"])
    )
}

private fun parseStringList(value: Any?): List<String> {
    return when (value) {
        is List<*> -> value.filterIsInstance<String>()
        else -> emptyList()
    }
}
