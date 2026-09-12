package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.AquagendaConstants
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.AquaGreen
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaYellow

@Composable
fun AquagendaLogo(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    logoUrl: String = AquagendaConstants.URL_AQUAGENDA_LOGO
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(logoUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Aquagenda Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .testTag("aquagenda_logo"),
        loading = {
            AquagendaLogoVectorFallback(height = height)
        },
        error = {
            AquagendaLogoVectorFallback(height = height)
        }
    )
}

/**
 * High-fidelity fallback vector representation of the Aquagenda brand splash
 * in case of network unavailability or during asset transition.
 */
@Composable
fun AquagendaLogoVectorFallback(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp
) {
    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Splash Calendar Icon (4-color quadrants with calendar dots)
        Box(
            modifier = Modifier
                .size(height * 0.85f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            // 4 colored quadrants
            Box(
                modifier = Modifier
                    .size((height * 0.85f) / 2)
                    .align(Alignment.TopStart)
                    .background(AquaMagenta)
            )
            Box(
                modifier = Modifier
                    .size((height * 0.85f) / 2)
                    .align(Alignment.TopEnd)
                    .background(AquaCyan)
            )
            Box(
                modifier = Modifier
                    .size((height * 0.85f) / 2)
                    .align(Alignment.BottomStart)
                    .background(AquaYellow)
            )
            Box(
                modifier = Modifier
                    .size((height * 0.85f) / 2)
                    .align(Alignment.BottomEnd)
                    .background(AquaGreen)
            )
            // Center mini cutouts mimicking calendar squares
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Brand Typography "AQUAGENDA"
        Text(
            text = "AQUAGENDA",
            color = AquaPrimary,
            fontSize = (height.value * 0.52f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
    }
}
