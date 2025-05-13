package com.example.demograph.model

data class NodeData(
    val id: String,
    val guideContent: String,
    val xNode: Double,
    val yNode: Double,
    val width: Double = 280.0,
    val height: Double = 110.0,
    val nextStep: List<String> = emptyList(),
    val prevStep: List<String> = emptyList()
)