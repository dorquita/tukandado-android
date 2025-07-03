package com.tukandado.tukandadov2.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Bienvenido a Tukandado 🚀", style = MaterialTheme.typography.headlineMedium)
    }
}