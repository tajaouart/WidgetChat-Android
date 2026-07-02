package com.widgetchat.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.widgetchat.app.data.RichAction
import com.widgetchat.app.data.RichButton
import com.widgetchat.app.data.RichCard
import com.widgetchat.app.data.RichComponent
import com.widgetchat.app.data.RichContentResponse
import com.widgetchat.app.data.RichImage
import com.widgetchat.app.data.RichSwatch
import com.widgetchat.app.ui.theme.ChatColors
import com.widgetchat.app.ui.theme.parseHex

@Composable
fun RichContentView(response: RichContentResponse, colors: ChatColors, onAction: (RichAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        response.components.forEach { ComponentView(it, colors, onAction) }
    }
}

@Composable
fun ComponentView(component: RichComponent, colors: ChatColors, onAction: (RichAction) -> Unit) {
    when (component) {
        is RichComponent.TextComp -> MarkdownText(component.content, colors.botText)
        is RichComponent.CardComp -> CardView(component.card, colors, onAction)
        is RichComponent.Carousel -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(component.items.size) { i ->
                Box(Modifier.width(220.dp)) { ComponentView(component.items[i], colors, onAction) }
            }
        }
        is RichComponent.ProductList -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            component.items.forEach { ComponentView(it, colors, onAction) }
        }
        is RichComponent.ImageComp -> ImageView(component.image, onAction)
        is RichComponent.ButtonGroup -> {
            if (component.layout == "vertical") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    component.buttons.forEach { ButtonView(it, colors, onAction) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    component.buttons.forEach { Box(Modifier.weight(1f)) { ButtonView(it, colors, onAction) } }
                }
            }
        }
        is RichComponent.SwatchGrid -> SwatchGridView(component.swatches, colors, onAction)
        is RichComponent.HostSlot -> Text("Custom content: ${component.slotId}", color = Color.Gray, fontSize = 12.sp)
        is RichComponent.Unknown -> {}
    }
}

@Composable
private fun CardView(card: RichCard, colors: ChatColors, onAction: (RichAction) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.botBubble)
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        card.image?.let {
            AsyncImage(model = it.url, contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(it.ratio.coerceIn(0.5f, 2f)))
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            card.title?.let { Text(it, color = colors.botText, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            card.subtitle?.let { Text(it, color = Color.Gray, fontSize = 13.sp) }
            card.price?.let { Text(it, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            card.description?.let { Text(it, color = colors.botText.copy(alpha = 0.85f), fontSize = 13.sp) }
            card.actions.forEach { ActionButton(it.label ?: "Select", it.style, colors) { onAction(it) } }
        }
    }
}

@Composable
private fun ImageView(image: RichImage, onAction: (RichAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AsyncImage(model = image.url, contentDescription = image.alt,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(image.ratio.coerceIn(0.5f, 2f))
                .clip(RoundedCornerShape(14.dp))
                .clickable { image.actions.firstOrNull()?.let(onAction) })
        image.caption?.let { Text(it, color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
private fun ButtonView(button: RichButton, colors: ChatColors, onAction: (RichAction) -> Unit) {
    val action = button.action ?: return
    ActionButton(button.label, button.style, colors) { onAction(action) }
}

@Composable
fun ActionButton(label: String, style: String, colors: ChatColors, onClick: () -> Unit) {
    val (bg, fg) = colors.actionColors(style)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
private fun SwatchGridView(swatches: List<RichSwatch>, colors: ChatColors, onAction: (RichAction) -> Unit) {
    // Simple wrapping rows of 4.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { swatch ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { swatch.action?.let(onAction) }) {
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .background(parseHex(swatch.hex) ?: Color.Gray)
                            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape))
                        swatch.label?.let { Text(it, fontSize = 10.sp, color = Color.Gray) }
                    }
                }
            }
        }
    }
}
