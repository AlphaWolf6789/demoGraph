package com.example.demograph.ui

import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import com.mxgraph.view.mxStylesheet
import java.util.HashMap

/**
 * Utility class for configuring and managing all styles for the graph
 */
object CustomGraphRenderer {

    /**
     * Apply all styles to the graph
     * 
     * @param graph The mxGraph instance to apply styles to
     */
    fun applyStyles(graph: mxGraph) {
        // Register custom shapes
        GraphShapes.registerShapes()
        
        // Configure the graph stylesheet
        val stylesheet = graph.stylesheet
        
        // Apply common styles
        configureCommonStyles(stylesheet)
        
        // Apply node styles
        configureNodeStyles(stylesheet)
        
        // Apply edge styles
        configureEdgeStyles(stylesheet)
        
        // Apply special element styles
        configureSpecialStyles(stylesheet)
        
        // Configure global settings for the graph
        graph.setGridEnabled(false) // Don't draw our own grid
        graph.setCellsLocked(false) // Allow cell interaction
    }
    
    /**
     * Configure common styles for all elements
     */
    private fun configureCommonStyles(stylesheet: mxStylesheet) {
        // Default vertex style
        val defaultStyle = stylesheet.getDefaultVertexStyle()
        defaultStyle[mxConstants.STYLE_FONTCOLOR] = "#FFFFFF"
        defaultStyle[mxConstants.STYLE_FONTSTYLE] = 0
        defaultStyle[mxConstants.STYLE_FONTSIZE] = 13
        defaultStyle[mxConstants.STYLE_FONTFAMILY] = "Arial"
        defaultStyle[mxConstants.STYLE_SHADOW] = false
        defaultStyle[mxConstants.STYLE_FILLCOLOR] = "none" // Transparent fill by default
        defaultStyle[mxConstants.STYLE_OPACITY] = 100
        defaultStyle[mxConstants.STYLE_ROUNDED] = true
        defaultStyle[mxConstants.STYLE_GLASS] = false
    }
    
    /**
     * Configure styles for all node types
     */
    private fun configureNodeStyles(stylesheet: mxStylesheet) {
        // User query node style
        val userQueryStyle = stylesheet.getDefaultVertexStyle()

        userQueryStyle[mxConstants.STYLE_FILLCOLOR] = "none" // Dark fill
        userQueryStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple border
        userQueryStyle[mxConstants.STYLE_STROKEWIDTH] = "3"
        userQueryStyle[mxConstants.STYLE_FONTCOLOR] = "#FFFFFF"
        userQueryStyle[mxConstants.STYLE_FONTFAMILY] = "Arial"
        userQueryStyle[mxConstants.STYLE_FONTSIZE] = "12"
        userQueryStyle[mxConstants.STYLE_SHADOW] = "0"
        userQueryStyle[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_LEFT
        userQueryStyle[mxConstants.STYLE_SHAPE] = "userQueryShape" // Use custom shape
        stylesheet.putCellStyle("userQueryNode", userQueryStyle)
        
        // Step node style 
        val stepStyle = HashMap<String, Any>()
        stepStyle[mxConstants.STYLE_SHAPE] = "stepNodeShape"
        stepStyle[mxConstants.STYLE_ROUNDED] = true
        stepStyle[mxConstants.STYLE_FILLCOLOR] = "none"
        stepStyle[mxConstants.STYLE_STROKECOLOR] = "#4ADE80" // Green border
        stepStyle[mxConstants.STYLE_STROKEWIDTH] = 3
        stepStyle[mxConstants.STYLE_FONTCOLOR] = "#FFFFFF"
        stepStyle[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_LEFT
        stylesheet.putCellStyle("stepNode", stepStyle)
    }
    
    /**
     * Configure styles for edges
     */
    private fun configureEdgeStyles(stylesheet: mxStylesheet) {
        // Default edge style (dotted line with arrow) - Purple for user queries
        val edgeStyle = stylesheet.getDefaultEdgeStyle()
        edgeStyle[mxConstants.STYLE_STROKECOLOR] = "#9277FF" // Purple edge
        edgeStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
        edgeStyle[mxConstants.STYLE_ENDARROW] = mxConstants.ARROW_CLASSIC
        edgeStyle[mxConstants.STYLE_STARTARROW] = mxConstants.NONE
        edgeStyle[mxConstants.STYLE_EDGE] = mxConstants.EDGESTYLE_ORTHOGONAL
        edgeStyle[mxConstants.STYLE_DASHED] = "1" // Dashed line
        edgeStyle[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
        edgeStyle[mxConstants.STYLE_EXIT_X] = "1.0"
        edgeStyle[mxConstants.STYLE_EXIT_Y] = "0.5"
        edgeStyle[mxConstants.STYLE_ENTRY_X] = "0.0"
        edgeStyle[mxConstants.STYLE_ENTRY_Y] = "0.5"
        edgeStyle[mxConstants.STYLE_DASH_PATTERN] = "8 4" // 8px dash, 4px space
         // Tạo style riêng cho edge mặc định
        val standardEdgeStyle = HashMap<String, Any>()
        edgeStyle.forEach { (key, value) ->
            if (value != null) {
                standardEdgeStyle[key] = value
            }
        }
        stylesheet.putCellStyle("defaultEdge", standardEdgeStyle)
        
        // Green edge style for step nodes
        val greenEdgeStyle = HashMap<String, Any>(standardEdgeStyle)
        greenEdgeStyle[mxConstants.STYLE_STROKECOLOR] = "#4ADE80" // Green edge for step nodes
        stylesheet.putCellStyle("stepEdge", greenEdgeStyle)
        
        // Highlighted edge style for hover
        val highlightedEdgeStyle = HashMap<String, Any>(edgeStyle)
        highlightedEdgeStyle[mxConstants.STYLE_STROKECOLOR] = "#B69EFF" // Lighter purple
        highlightedEdgeStyle[mxConstants.STYLE_STROKEWIDTH] = 3
        stylesheet.putCellStyle("highlightedEdge", highlightedEdgeStyle)
    }
    
    /**
     * Configure styles for special elements like add buttons, connection dots
     */
    private fun configureSpecialStyles(stylesheet: mxStylesheet) {
        // Add button style (circular with plus) - Purple for default
        val addButtonStyle = HashMap<String, Any>()
        addButtonStyle[mxConstants.STYLE_SHAPE] = "addButtonShape"
        addButtonStyle[mxConstants.STYLE_PERIMETER] = mxConstants.PERIMETER_ELLIPSE
        addButtonStyle[mxConstants.STYLE_FILLCOLOR] = "none" // Transparent fill
        addButtonStyle[mxConstants.STYLE_STROKECOLOR] = "#9277FF" // Purple outline
        addButtonStyle[mxConstants.STYLE_STROKEWIDTH] = 3
        addButtonStyle[mxConstants.STYLE_FONTCOLOR] = "#9277FF"
        addButtonStyle[mxConstants.STYLE_FONTSTYLE] = mxConstants.FONT_BOLD
        
        // Add these to enforce circular shape
        addButtonStyle[mxConstants.STYLE_ROUNDED] = true
        addButtonStyle[mxConstants.STYLE_ARCSIZE] = 100 // 100% arc size = circle
        
        stylesheet.putCellStyle("addButton", addButtonStyle)
        
        // Add button style - Green for step nodes
        val greenAddButtonStyle = HashMap<String, Any>(addButtonStyle)
        greenAddButtonStyle[mxConstants.STYLE_STROKECOLOR] = "#4ADE80" // Green outline
        greenAddButtonStyle[mxConstants.STYLE_FONTCOLOR] = "#4ADE80"
        stylesheet.putCellStyle("stepAddButton", greenAddButtonStyle)
        
        // Connection dot style (dot at connection point)
        val connectionDotStyle = HashMap<String, Any>()
        connectionDotStyle[mxConstants.STYLE_SHAPE] = "connectionDotShape"
        connectionDotStyle[mxConstants.STYLE_FILLCOLOR] = "#9277FF" // Purple fill
        connectionDotStyle[mxConstants.STYLE_STROKECOLOR] = "#9277FF"
        connectionDotStyle[mxConstants.STYLE_STROKEWIDTH] = 0
        stylesheet.putCellStyle("connectionDot", connectionDotStyle)
    }
} 