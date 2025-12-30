package com.example.keyboardnew.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KeyboardLayout() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .height(300.dp)
            .background(Color.Green)
    ) {
        Text(text = "Hello from Keyboard")
    }
}