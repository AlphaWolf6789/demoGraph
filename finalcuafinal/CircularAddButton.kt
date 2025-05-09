package com.example.finalcuafinal

import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import java.util.HashMap

/**
 * Helper class for configuring the circular add button style
 */
class CircularAddButton {
    companion object {
        /**
         * Configures the add button style in the graph stylesheet
         */
        fun configureStyle(graph: mxGraph) {
            val stylesheet = graph.stylesheet
            
            // Add button style
            val addButtonStyle = HashMap<String, Any>()
            addButtonStyle[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_ELLIPSE
            addButtonStyle[mxConstants.STYLE_PERIMETER] = mxConstants.PERIMETER_ELLIPSE
            addButtonStyle[mxConstants.STYLE_FILLCOLOR] = "none" // Transparent fill
            addButtonStyle[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple border
            addButtonStyle[mxConstants.STYLE_STROKEWIDTH] = "2"
            addButtonStyle[mxConstants.STYLE_FONTCOLOR] = "#6366F1" // Purple text
            addButtonStyle[mxConstants.STYLE_FONTSIZE] = "16"
            addButtonStyle[mxConstants.STYLE_FONTSTYLE] = mxConstants.FONT_BOLD
            addButtonStyle[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_MIDDLE
            addButtonStyle[mxConstants.STYLE_ALIGN] = mxConstants.ALIGN_CENTER
            
            stylesheet.putCellStyle("addButton", addButtonStyle)
        }
    }
} 