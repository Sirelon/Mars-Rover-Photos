package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.sirelon.marsroverphotos.domain.models.CURIOSITY_ID
import com.sirelon.marsroverphotos.domain.models.INSIGHT_ID
import com.sirelon.marsroverphotos.domain.models.OPPORTUNITY_ID
import com.sirelon.marsroverphotos.domain.models.PERSEVERANCE_ID
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.domain.models.SPIRIT_ID
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.img_curiosity
import com.sirelon.marsroverphotos.shared.resources.img_insight
import com.sirelon.marsroverphotos.shared.resources.img_opportunity
import com.sirelon.marsroverphotos.shared.resources.img_perseverance
import com.sirelon.marsroverphotos.shared.resources.img_placeholder
import com.sirelon.marsroverphotos.shared.resources.img_spirit
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
        else -> Res.drawable.img_placeholder
    }
}
