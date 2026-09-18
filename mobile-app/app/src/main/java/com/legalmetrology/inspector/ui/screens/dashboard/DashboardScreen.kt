package com.legalmetrology.inspector.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legalmetrology.inspector.ui.theme.Amber600
import com.legalmetrology.inspector.ui.theme.Cobalt600
import com.legalmetrology.inspector.ui.theme.Crimson600
import com.legalmetrology.inspector.ui.theme.Emerald600
import com.legalmetrology.inspector.ui.theme.Slate100
import com.legalmetrology.inspector.ui.theme.Slate950
import com.legalmetrology.inspector.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * DashboardScreen — Officer home screen with stats and quick actions.
 *
 * Role-aware:
 * - OFFICER: own inspections, district stats
 * - CONTROLLER: district overview
 * - DIRECTOR: national overview
 *
 * TODO — BACKEND INTEGRATION:
 * Load real stats from ViewModel/Room/API using the logged-in user's role.
 * Currently all stats are hardcoded mock values.
 */
@Composable
fun DashboardScreen(
    onStartNewInspection: () -> Unit,
    onViewHistory: () -> Unit,
    onECommerceInspection: () -> Unit,
    onLogout: () -> Unit
) {
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Slate950, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Good morning,",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            "Rajesh Kumar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "OFFICER · South Delhi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cobalt600,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Default.ExitToApp,
                        "Logout",
                        tint = TextSecondary
                    )
                }
            }

            // Stats row (animated count-up)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                item { AnimatedStatCard("Total Scans", 247, Cobalt600) }
                item { AnimatedStatCard("Violations Found", 31, Crimson600) }
                item { AnimatedStatCard("Compliant", 198, Emerald600) }
                item { AnimatedStatCard("Pending Review", 18, Amber600) }
            }

            Spacer(Modifier.height(32.dp))

            // Quick actions
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            // Primary CTA
            Button(
                onClick = onStartNewInspection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Cobalt600.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cobalt600
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "New Physical Inspection",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "AR camera + field detection",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.9f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Secondary actions grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = Icons.Default.ScreenShare,
                    title = "E-Commerce",
                    subtitle = "Check online listing",
                    color = Amber600,
                    modifier = Modifier.weight(1f),
                    onClick = onECommerceInspection
                )
                ActionCard(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "Past inspections",
                    color = Emerald600,
                    modifier = Modifier.weight(1f),
                    onClick = onViewHistory
                )
            }

            Spacer(Modifier.height(32.dp))

            // Recent violations
            Text(
                "Recent Violations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("Lays Classic 26g · MRP font 1.8mm < 2.5mm", "2 hours ago", Crimson600),
                Triple("Dove Shampoo · Missing consumer care contact", "Yesterday", Crimson600),
                Triple("XYZ Biscuits · Non-standard pack size 347g", "2 days ago", Amber600),
            ).forEach { (text, time, color) ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                time,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnimatedStatCard(label: String, targetValue: Int, color: Color) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(targetValue.toFloat(), tween(1200))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(120.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.1f)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "${animatedValue.value.roundToInt()}",
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.1f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(0.8f)
                )
            }
        }
    }
}
