package com.sirelon.marsroverphotos.domain.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * @author romanishin
 * @since 31.10.16 on 11:19
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MarsPhoto(
    @SerialName(value = "id")
    @Serializable(with = StringOrNumberAsStringSerializer::class)
    val id: String,

    @SerialName(value = "sol")
    val sol: Long,

    @JsonNames("name", "title")
    val name: String?,

    @JsonNames("img_src", "url")
    val imageUrl: String,

    @JsonNames("earth_date", "created_at")
    val earthDate: String,

    @SerialName(value = "camera")
    val camera: RoverCamera? = null,

    // Flat instrument identifier used by mars.nasa.gov raw-image feeds (e.g. "MAST_RIGHT").
    // Absent in the old mars-photos API responses; defaults to null for backward compatibility.
    @SerialName(value = "instrument")
    val instrument: String? = null,

    // When the photo was taken, as the MSL raw-image feed reports it. That feed has no
    // `earth_date`, so [earthDate] falls back to `created_at` there — the time NASA stored the
    // record, which can be years after the sol. Prefer this field when it is present.
    @SerialName(value = "date_taken")
    val dateTaken: String? = null,
)

private object StringOrNumberAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumberAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return jsonDecoder.decodeJsonElement().jsonPrimitive.content
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
