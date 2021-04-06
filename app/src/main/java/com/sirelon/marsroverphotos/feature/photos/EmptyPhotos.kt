package com.sirelon.marsroverphotos.feature.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.ui.CenteredColumn

/**
 * Created on 06.04.2021 21:43 for Mars-Rover-Photos.
 */
@Composable
fun EmptyPhotos(title: String, btnTitle: String, callback: () -> Unit) {
    CenteredColumn(
        modifier = Modifier
            .clickable(onClick = callback)
            .padding(32.dp)
    ) {
        Image(painter = painterResource(R.drawable.alien_icon), contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = title, style = MaterialTheme.typography.h5, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = btnTitle,
            style = MaterialTheme.typography.h6
        )
    }
}