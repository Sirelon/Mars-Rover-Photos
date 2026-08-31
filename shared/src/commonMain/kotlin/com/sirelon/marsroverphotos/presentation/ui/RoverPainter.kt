package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_1_ID
import com.sirelon.marsroverphotos.domain.models.VIKING_2_ID
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_curiosity
import com.sirelon.marsroverphotos.shared.resources.img_insight
import com.sirelon.marsroverphotos.shared.resources.img_opportunity
import com.sirelon.marsroverphotos.shared.resources.img_perseverance
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import com.sirelon.marsroverphotos.shared.resources.img_spirit
import com.sirelon.marsroverphotos.shared.resources.img_viking1
import com.sirelon.marsroverphotos.shared.resources.img_viking2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
public fun Rover.painter(): Painter {
    return painterResource(drawableResource())
}

private fun Rover.drawableResource(): DrawableResource {
    return when (id) {
        PERSEVERANCE_ID -> Res.drawable.img_perseverance
        INSIGHT_ID -> Res.drawable.img_insight
        CURIOSITY_ID -> Res.drawable.img_curiosity
        OPPORTUNITY_ID -> Res.drawable.img_opportunity
        SPIRIT_ID -> Res.drawable.img_spirit
        // The two landers were the same spacecraft flown twice, and neither could photograph itself:
        // both cameras were bolted to the deck and only ever framed one instrument at a time. So
        // both cards show the craft as built rather than as it saw itself — Viking 1 in NASA's
        // Mars-surface diorama, Viking 2 as the museum lander. Keep every asset here roughly square:
        // the rovers list crops it to a portrait thumbnail while MissionInfoSections crops the same
        // file to a 16:9 hero, so a tall source survives the thumbnail but loses most of its height
        // in the hero.
        VIKING_1_ID -> Res.drawable.img_viking1
        VIKING_2_ID -> Res.drawable.img_viking2
        else -> Res.drawable.img_placeholder
    }
}
