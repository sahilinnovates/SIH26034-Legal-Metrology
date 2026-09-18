package com.legalmetrology.inspector.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * All navigation routes in the app.
 * Using sealed class for type safety and compile-time route verification.
 */
sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object Login        : Screen("login")
    object Onboarding   : Screen("onboarding")
    object Scan         : Screen("scan/{packageType}/{category}/{productName}") {
        fun createRoute(packageType: String, category: String, productName: String): String {
            val encodedName = URLEncoder.encode(productName, StandardCharsets.UTF_8.toString())
            return "scan/$packageType/$category/$encodedName"
        }
    }
    object Review       : Screen("review/{inspectionId}") {
        fun createRoute(inspectionId: String) = "review/$inspectionId"
    }
    object Report       : Screen("report/{inspectionId}") {
        fun createRoute(inspectionId: String) = "report/$inspectionId"
    }
    object History      : Screen("history")
    object Dashboard    : Screen("dashboard")
    object ECommerce    : Screen("ecommerce")
}
