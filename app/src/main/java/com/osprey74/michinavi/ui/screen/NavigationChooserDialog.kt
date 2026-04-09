package com.osprey74.michinavi.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationChooserDialog(
    lat: Double,
    lng: Double,
    name: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "ナビアプリを選択",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            // Google Maps
            ListItem(
                headlineContent = { Text("Google Maps") },
                leadingContent = {
                    Icon(
                        Icons.Default.Map, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.clickable {
                    val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val browserUri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng",
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                    onDismiss()
                },
            )

            // Yahoo!カーナビ
            ListItem(
                headlineContent = { Text("Yahoo!カーナビ") },
                leadingContent = {
                    Icon(
                        Icons.Default.DirectionsCar, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.clickable {
                    val encoded = URLEncoder.encode(name, "UTF-8")
                    val uri = Uri.parse("yjcarnavi://navi/select?lat=$lat&lon=$lng&name=$encoded")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    onDismiss()
                },
            )

            // Waze
            ListItem(
                headlineContent = { Text("Waze") },
                leadingContent = {
                    Icon(
                        Icons.Default.Public, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.clickable {
                    val uri = Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

fun shareLocation(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    name: String,
) {
    val googleMapsUrl = "https://www.google.com/maps?q=$lat,$lng"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$name\n$googleMapsUrl")
        putExtra(Intent.EXTRA_SUBJECT, name)
    }
    context.startActivity(Intent.createChooser(intent, "共有"))
}
