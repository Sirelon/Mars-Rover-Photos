package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.rover_blurb_curiosity
import com.sirelon.marsroverphotos.shared.resources.rover_blurb_insight
import com.sirelon.marsroverphotos.shared.resources.rover_blurb_opportunity
import com.sirelon.marsroverphotos.shared.resources.rover_blurb_perseverance
import com.sirelon.marsroverphotos.shared.resources.rover_blurb_spirit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Returns the localized mission blurb string for this rover, or empty for unknown ids. */
@Composable
fun Rover.blurb(): String = blurbResource()?.let { stringResource(it) } ?: ""

fun Rover.blurbResource(): StringResource? = when (id) {
    CURIOSITY_ID -> Res.string.rover_blurb_curiosity
    OPPORTUNITY_ID -> Res.string.rover_blurb_opportunity
    SPIRIT_ID -> Res.string.rover_blurb_spirit
    INSIGHT_ID -> Res.string.rover_blurb_insight
    PERSEVERANCE_ID -> Res.string.rover_blurb_perseverance
    else -> null
}
