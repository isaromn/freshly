package com.ecoalacena.app.presentation.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.core.formatDate
import com.ecoalacena.app.presentation.i18n.LocalAppStrings

/**
 * Warns that the scanned receipt is old: the expiration dates were counted from the day of the
 * purchase, so some of its food may have gone off already ([expiredCount] of them did).
 */
@Composable
internal fun OldReceiptWarning(
    receiptDate: Long,
    ageDays: Int,
    expiredCount: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.oldReceiptTitle(formatDate(receiptDate, strings.language), ageDays),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = strings.oldReceiptBody(expiredCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
