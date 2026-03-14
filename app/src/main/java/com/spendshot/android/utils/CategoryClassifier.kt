package com.spendshot.android.utils

// Enum removed


object CategoryClassifier {
    // Defines lists of keywords for each category
    private val categoryKeywords = mapOf(
        "Food" to listOf(
            "swiggy", "zomato", "eats", "food", "restaurant", "cafe", "coffee", "starbucks",
            "mcdonalds", "kfc", "burger", "pizza", "dominos", "subway", "barbeque", "briyani",
            "chai", "tea", "baker", "cake", "sweet", "snack", "tiffin", "mess", "dining"
        ),
        "Groceries" to listOf(
            "grocery", "mart", "supermarket", "blinkit", "zepto", "instamart", "bigbasket",
            "store", "market", "vegetable", "fruit", "dairy", "milk", "kirana", "general store"
        ),
        "Travel" to listOf(
            "uber", "ola", "rapido", "taxi", "cab", "auto", "metro", "train", "rail", "irctc",
            "bus", "transport", "fuel", "petrol", "diesel", "pump", "shell", "hp", "indian oil",
            "flight", "airline", "air", "indigo", "vistara", "toll", "parking", "fasttag", "travel"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "shop", "mall", "clothing",
            "fashion", "retail", "store", "ikea", "decathlon", "zudio", "trends", "max"
        ),
        "Entertainment" to listOf(
            "netflix", "prime", "hotstar", "disney", "spotify", "music", "movie", "cinema",
            "theatre", "bookmyshow", "pvr", "imax", "inox", "entertainment", "game", "steam",
            "playstation", "xbox", "youtube"
        ),
        "Health" to listOf(
            "pharmacy", "medical", "doctor", "clinic", "hospital", "health", "medicine", "drug",
            "lab", "diagnostic", "apollo", "medplus", "1mg", "pharmeasy"
        ),
        "Bills" to listOf(
            "bill", "recharge", "jio", "airtel", "vi", "vodafone", "bsnl", "electricity",
            "power", "water", "gas", "internet", "wifi", "broadband", "dth", "tatasky",
            "rent", "maintenance"
        )
    )

    fun classify(merchant: String, note: String = "", detectedApp: String? = null): String? {
        val searchTerms = listOfNotNull(merchant, note, detectedApp).joinToString(" ").lowercase()

        // 1. Direct priority check for specific apps if detected
        if (detectedApp != null) {
            val appLower = detectedApp.lowercase()
            for ((category, keywords) in categoryKeywords) {
                 // If the detected app name itself contains a keyword (e.g., "Swiggy" contains "swiggy")
                if (keywords.any { appLower.contains(it) }) {
                    return category
                }
            }
        }

        // 2. Keyword matching in merchant and note
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { searchTerms.contains(it) }) {
                return category
            }
        }

        return null // Return null if no match found, caller can handle default
    }
}