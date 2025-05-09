package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import java.awt.Point
import java.awt.Rectangle
import java.util.HashMap

/**
 * A class to handle the creation and management of dot icons for nodes
 */
class DotManager(private val graph: mxGraph) {
    private val dotCells = mutableListOf<mxCell>() // Track dot cells
    private var currentHoverCell: Any? = null // Track current hovered cell
    
    init {
        // Initialize dot styles
        initDotStyles()
    }
    
    private fun initDotStyles() {
        // Create edit dot style
        val editDotStyle = HashMap<String, Any>()
        editDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        editDotStyle[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D" // Dark background
        editDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple
        editDotStyle[mxConstants.STYLE_STROKEWIDTH] = 1.5
        graph.stylesheet.putCellStyle("editDot", editDotStyle)
        
        // Create copy dot style
        val copyDotStyle = HashMap<String, Any>()
        copyDotStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
        copyDotStyle[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D" // Dark background
        copyDotStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple
        copyDotStyle[mxConstants.STYLE_STROKEWIDTH] = 1.5
        graph.stylesheet.putCellStyle("copyDot", copyDotStyle)
    }
    
    // Handle node hovering
    fun handleNodeHover(cell: Any?, e: Point) {
        // If hovering over a dot, keep the dots visible
        if (cell != null && dotCells.contains(cell)) {
            return
        }
        
        // Check if the cell is a node we should react to
        val isHoveringOverNode = cell != null && 
                              graph.model.isVertex(cell) && 
                              !isAddButton(cell as mxCell) &&
                              !dotCells.contains(cell)
        
        // Determine if we need to clear existing dots
        var shouldClearDots = false
        
        // If moving to a different cell or empty space
        if (cell != currentHoverCell) {
            // If we were hovering over a node before, check if we're still in the dot area
            if (currentHoverCell != null) {
                // Safely capture the current hover cell in a local variable
                val hoverCell = currentHoverCell
                
                // If the current position is not in a dot area, clear the dots
                val inDotArea = if (hoverCell != null) isInDotArea(hoverCell, e) else false
                if (!inDotArea && !dotCells.contains(cell)) {
                    shouldClearDots = true
                }
            } else {
                // We weren't hovering over anything before, so just clear
                shouldClearDots = true
            }
        }
        
        // Clear dots if needed
        if (shouldClearDots) {
            clearDragDots()
            currentHoverCell = null
        }
        
        // Create new dots if hovering over a node
        if (isHoveringOverNode && cell != currentHoverCell) {
            currentHoverCell = cell
            createDragDots(cell as mxCell)
        }
    }
    
    // Create dots for editing and copying
    private fun createDragDots(cell: mxCell) {
        graph.model.beginUpdate()
        try {
            val cellGeometry = graph.getCellGeometry(cell)
            val x = cellGeometry.x
            val y = cellGeometry.y + cellGeometry.height + 10 // Position dots below the node
            
            // Determine if this is a query node (with purple border)
            val iconColor = "#6366F1" // Purple
            
            // Create edit dot with pen icon
            createEditDot(cell, x, y, iconColor)
            
            // Create copy dot with copy icon
            createCopyDot(cell, x, y, iconColor)
        } finally {
            graph.model.endUpdate()
        }
    }
    
    // Create edit dot with a pen icon
    private fun createEditDot(cell: mxCell, x: Double, y: Double, iconColor: String) {
        // Create HTML content with enhanced styling for the edit icon
        val editIconHtml = """
            <div style="display:flex;justify-content:center;align-items:center;width:100%;height:100%;padding-bottom:10px;margin:0;">
                <span style="font-size:12px;font-weight:bold;color:${iconColor};line-height:1;">✎</span>
            </div>
        """.trimIndent()
        
        // Insert the edit dot
        val editDot = graph.insertVertex(
            graph.defaultParent, null, editIconHtml,
            x + 5, y, 20.0, 20.0, "editDot;html=1"
        )

        dotCells.add(editDot as mxCell)
    }
    
    // Create copy dot with a copy icon
    private fun createCopyDot(cell: mxCell, x: Double, y: Double, iconColor: String) {
        // Create HTML content with enhanced styling for the copy icon
        val copyIconHtml = """
            <div style="display:flex;justify-content:center;align-items:center;width:100%;height:100%;padding-bottom:10px;margin:0;">
                <span style="font-size:10px;font-weight:bold;color:${iconColor};line-height:1;">⧉</span>
            </div>
        """.trimIndent()
        
        // Insert the copy dot
        val copyDot = graph.insertVertex(
            graph.defaultParent, null, copyIconHtml,
            x + 35, y, 20.0, 20.0, "copyDot;html=1"
        )

        dotCells.add(copyDot as mxCell)
    }
    
    // Clear all drag dots
    fun clearDragDots() {
        if (dotCells.isNotEmpty()) {
            graph.removeCells(dotCells.toTypedArray())
            dotCells.clear()
        }
    }
    
    // Find the parent node for a dot
    fun findParentNodeForDot(dotCell: Any): mxCell? {
        // Get all vertices in the graph
        val vertices = graph.getChildVertices(graph.defaultParent)
        
        // Find the node that owns this dot (closest node above the dot)
        return vertices.filterIsInstance<mxCell>()
            .filter { it != dotCell && !dotCells.contains(it) }
            .minByOrNull { 
                val dotGeom = graph.getCellGeometry(dotCell)
                val cellGeom = graph.getCellGeometry(it)
                val dx = dotGeom.x - cellGeom.x
                val dy = dotGeom.y - (cellGeom.y + cellGeom.height)
                if (dx >= 0 && dy >= 0 && dx < cellGeom.width + 50) dy else Double.MAX_VALUE
            }
    }
    
    // Check if a point is in the dot area below a node
    private fun isInDotArea(node: Any, point: Point): Boolean {
        val cellGeometry = graph.getCellGeometry(node)
        if (cellGeometry != null) {
            // Get the transform from the graph view
            val transform = graph.view.getState(node)?.absoluteOffset
            val scale = graph.view.scale
            
            // Calculate the dot area in screen coordinates
            val x = (cellGeometry.x * scale).toInt()
            val y = ((cellGeometry.y + cellGeometry.height) * scale).toInt()
            val width = (cellGeometry.width * scale).toInt()
            val height = (40 * scale).toInt()  // Height of dot area
            
            // If we have a transform, apply it
            val dotArea = if (transform != null) {
                Rectangle(
                    x + transform.x.toInt(),
                    y + transform.y.toInt(),
                    width,
                    height
                )
            } else {
                Rectangle(x, y, width, height)
            }
            
            return dotArea.contains(point)
        }
        return false
    }
    
    // Check if a cell is an edit dot
    fun isEditDot(cell: mxCell): Boolean {
        return dotCells.contains(cell) && graph.getModel().getStyle(cell)?.contains("editDot") == true
    }
    
    // Check if a cell is a copy dot
    fun isCopyDot(cell: mxCell): Boolean {
        return dotCells.contains(cell) && graph.getModel().getStyle(cell)?.contains("copyDot") == true
    }
    
    // Helper method to check if a cell is the add button
    private fun isAddButton(cell: mxCell): Boolean {
        val style = graph.getCellStyle(cell)
        return style.getOrDefault(mxConstants.STYLE_SHAPE, "") == "addButton" || 
               (cell.value == "+" && cell.style?.contains("addButton") == true)
    }
    
    // Get the list of dot cells
    fun getDotCells(): List<mxCell> = dotCells
    
    // Get current hover cell
    fun getCurrentHoverCell(): Any? = currentHoverCell
    
    // Set current hover cell
    fun setCurrentHoverCell(cell: Any?) {
        currentHoverCell = cell
    }
} 