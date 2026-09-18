package com.legalmetrology.inspector.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legalmetrology.inspector.domain.model.CommodityCategory
import com.legalmetrology.inspector.domain.model.PackageType
import com.legalmetrology.inspector.ui.theme.Cobalt600
import com.legalmetrology.inspector.ui.theme.Slate100
import com.legalmetrology.inspector.ui.theme.Slate300
import com.legalmetrology.inspector.ui.theme.Slate950
import com.legalmetrology.inspector.ui.theme.TextSecondary
import com.legalmetrology.inspector.ui.theme.TextTertiary

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
    onProceedToScan: (PackageType, CommodityCategory, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedPackageType by remember { mutableStateOf<PackageType?>(null) }
    var selectedCategory by remember { mutableStateOf<CommodityCategory?>(null) }
    var productName by remember { mutableStateOf("") }

    val canProceed = selectedPackageType != null && 
                     selectedCategory != null && 
                     productName.trim().isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "New Inspection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Section 1: Package type
            SectionHeader(
                title = "Package Type",
                subtitle = "What kind of packaging are you inspecting?"
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
            SectionHeader(
                title = "Commodity Category",
                subtitle = "Category determines which rules apply"
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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

            Spacer(Modifier.height(24.dp))

            // Section 3: Product Name
            SectionHeader(
                title = "Product Name",
                subtitle = "This will appear on the Legal Metrology inspection report"
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Commodity / Product Name") },
                placeholder = { Text("e.g., Lay's Classic Salted 50g, Fortune Sunflower Oil 1L") },
                trailingIcon = {
                    if (productName.isNotEmpty()) {
                        IconButton(onClick = { productName = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cobalt600,
                    unfocusedBorderColor = Slate300,
                    focusedLabelColor = Cobalt600,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Cobalt600
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Enter a clear product description including brand, variant, and net quantity",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Proceed button
            AnimatedVisibility(
                visible = canProceed,
                enter = fadeIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + 
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
                        )
            ) {
                Button(
                    onClick = {
                        val pt = selectedPackageType ?: return@Button
                        val cat = selectedCategory ?: return@Button
                        onProceedToScan(pt, cat, productName.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Cobalt600.copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cobalt600,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Start AR Scan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Slate950,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.15.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
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
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Cobalt600 else Slate300,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "borderColor"
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Cobalt600.copy(alpha = 0.06f) else Color.White,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "bgColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 4.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Cobalt600.copy(alpha = 0.1f)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
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
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Cobalt600 else Color.White,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "chipBgColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "chipTextColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Cobalt600 else Slate300,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "chipBorderColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Cobalt600.copy(alpha = 0.1f)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
