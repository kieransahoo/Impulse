package com.impulse.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.impulse.ui.theme.Primary

@Composable
fun NewCollectionAction(
    active: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        if (!active) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Primary
            )
            Spacer(Modifier.size(4.dp))
        }
        Text(if (active) "Cancel" else "New collection", color = Primary)
    }
}
