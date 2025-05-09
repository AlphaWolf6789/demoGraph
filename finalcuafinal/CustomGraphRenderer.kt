package com.example.finalcuafinal

import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph

/**
 * Configures basic graph styling
 */
class CustomGraphRenderer {
    companion object {
        /**
         * Registers styles with the graph
         */
        fun register(graph: mxGraph) {
            // Configure visual styles for nodes and edges
            val stylesheet = graph.stylesheet
            
            // Configure vertex style
            val style = stylesheet.getDefaultVertexStyle()
            style[mxConstants.STYLE_SHAPE] = NodeGraph.SHAPE_NODEWITH_DOT
            style[mxConstants.STYLE_ROUNDED] = "1"
            style[mxConstants.STYLE_FILLCOLOR] = "#2D2D2D"
            style[mxConstants.STYLE_STROKECOLOR] = "#6366F1" // Purple border
            style[mxConstants.STYLE_STROKEWIDTH] = "2"
        }
    }
} 