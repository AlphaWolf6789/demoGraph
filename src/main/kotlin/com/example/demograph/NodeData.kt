package com.example.demograph

import java.io.Serializable

/**
 * Data class for storing node information
 */
data class NodeData(
    val id: String,
    val name: String,
    val previousStepId: String? = null,
    val nextStepId: String? = null
) : Serializable {
    override fun toString(): String {
        // Return a string representation for display in the graph
        return name
    }
} 