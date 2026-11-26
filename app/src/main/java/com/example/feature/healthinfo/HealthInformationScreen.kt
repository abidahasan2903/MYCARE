package com.example.feature.healthinfo

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.R
import com.example.core.ui.components.MyCareTopBar
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HealthInformationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        MyCareTopBar(stringResource(R.string.health_info_title), onBack)
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.health_info_intro), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.health_info_query)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (query.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.health_info_empty_query), Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(healthInformationSearchUrl(query)))
                        try { context.startActivity(intent) }
                        catch (_: ActivityNotFoundException) { Toast.makeText(context, context.getString(R.string.health_info_no_browser), Toast.LENGTH_SHORT).show() }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Text("  " + stringResource(R.string.health_info_search))
            }
            Text(stringResource(R.string.health_info_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun healthInformationSearchUrl(query: String): String =
    "https://www.google.com/search?q=" + URLEncoder.encode("(site:medlineplus.gov OR site:nhs.uk) ${query.trim()}", StandardCharsets.UTF_8.name())

