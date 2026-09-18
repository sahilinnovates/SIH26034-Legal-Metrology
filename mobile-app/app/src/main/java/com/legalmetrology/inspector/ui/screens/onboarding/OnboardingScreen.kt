package com.legalmetrology.inspector.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legalmetrology.inspector.domain.model.CommodityCategory
import com.legalmetrology.inspector.domain.model.PackageType
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy800
import com.legalmetrology.inspector.ui.theme.Navy900

private val packageTypeData = listOf(
    Triple(PackageType.RETAIL, "🛒", "Full 9-field check\nincl. font size measurement"),
    Triple(PackageType.WHOLESALE, "📦", "3 fields only:\nManufacturer, Net Qty, MRP"),
    Triple(PackageType.COMBINATION, "🎁", "Gift sets, multi-piece kits\nOuter + inner checks")
)

private val categoryData = listOf(
    Triple(CommodityCategory.FOOD, "🍪", "FSSAI rules apply"),
    Triple(CommodityCategory.COSMETIC, "💄", "D&C Rules apply"),
    Triple(CommodityCategory.ALCOHOL, "🥂", "State Excise MRP"),
    Triple(CommodityCategory.SEED, "🌱", "No mfg. date req."),
    Triple(CommodityCategory.MEDICAL_DEVICE, "💊", "Skips font size check"),
    Triple(CommodityCategory.GENERAL, "📋", "Standard full check")
)

@Composable
fun OnboardingScreen(
    onProceedToScan: (PackageType, CommodityCategory) -> Unit,
    onBack: () -> Unit
) {
    var selectedPackageType by remember { mutableStateOf<PackageType?>(null) }
    var selectedCategory by remember { mutableStateOf<CommodityCategory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Navy900, Navy800))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(
                    "New Inspection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Section 1: Package type
            Text(
                "Package Type",
                style = MaterialTheme.typography.titleMedium,
                color = Indigo500,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "What kind of packaging are you inspecting?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            packageTypeData.forEach { (type, emoji, desc) ->
                SelectionCard(
                    emoji = emoji,
                    title = type.displayName,
                    subtitle = desc,
                    isSelected = selectedPackageType == type,
                    onClick = { selectedPackageType = type }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Section 2: Commodity category
            Text(
                "Commodity Category",
                style = MaterialTheme.typography.titleMedium,
                color = Indigo500,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Category determines which rules apply",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(categoryData) { (cat, emoji, note) ->
                    CategoryChip(
                        emoji = emoji,
                        label = cat.displayName.split(" ").first(),
                        note = note,
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Proceed button
            AnimatedVisibility(
                visible = selectedPackageType != null && selectedCategory != null,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring()
                )
            ) {
                Button(
                    onClick = {
                        val pt = selectedPackageType ?: return@Button
                        val cat = selectedCategory ?: return@Button
                        onProceedToScan(pt, cat)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Text("Start AR Scan", style = MaterialTheme.typography.labelLarge)
                    Icon(
                        Icons.Default.ArrowForward,
                        null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectionCard(
    emoji: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Indigo500 else Color.Transparent
    val bgColor = if (isSelected) Indigo500.copy(alpha = 0.12f) else Navy800

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    emoji: String,
    label: String,
    note: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Indigo500 else Navy800
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}
