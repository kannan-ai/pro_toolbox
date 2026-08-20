package com.example.utilityhub.features.ai

import com.example.utilityhub.features.media.ProductCompare

object PriceAnalysisEngine {
    
    /**
     * Calculates the unit value (price per unit) for each product and marks the one with the lowest value.
     */
    fun calculateBestValue(products: List<ProductCompare>): List<ProductCompare> {
        if (products.isEmpty()) return products
        
        // Step 1: Calculate unit values
        val analyzedProducts = products.map { product ->
            val unitValue = if (product.quantity > 0) product.price / product.quantity else Double.MAX_VALUE
            product.copy(unitValue = unitValue)
        }
        
        // Step 2: Find the minimum unit value
        val minUnitValue = analyzedProducts.minOfOrNull { it.unitValue } ?: Double.MAX_VALUE
        
        // Step 3: Mark the best value products (there could be ties)
        return analyzedProducts.map { 
            it.copy(isBestValue = it.unitValue == minUnitValue && it.unitValue < Double.MAX_VALUE) 
        }
    }

    /**
     * Simulation of Entity Extraction parsing. 
     * In a full implementation, this would use com.google.mlkit.nl.entityextraction.
     */
    fun extractProductDetails(ocrText: String): ProductCompare? {
        // Basic Regex fallback for price and quantity if Entity Extraction is still downloading models
        val priceRegex = """(\d+(\.\d{1,2})?)""".toRegex()
        val quantityRegex = """(\d+)\s*(g|ml|kg|l|units)""".toRegex(RegexOption.IGNORE_CASE)
        
        val priceMatch = priceRegex.find(ocrText)
        val quantityMatch = quantityRegex.find(ocrText)
        
        if (priceMatch != null && quantityMatch != null) {
            return ProductCompare(
                name = "Scanned Product",
                price = priceMatch.value.toDouble(),
                quantity = quantityMatch.groupValues[1].toDouble(),
                unit = quantityMatch.groupValues[2],
                specs = ocrText.take(50)
            )
        }
        return null
    }
}
