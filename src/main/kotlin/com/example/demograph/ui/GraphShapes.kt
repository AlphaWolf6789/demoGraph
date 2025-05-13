package com.example.demograph.ui

import com.mxgraph.canvas.mxGraphics2DCanvas
import com.mxgraph.shape.mxRectangleShape
import com.mxgraph.view.mxCellState
import com.mxgraph.util.mxConstants
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import com.mxgraph.util.mxUtils
import java.awt.BasicStroke

/**
 * Contains all custom shapes for the JGraphX graph
 */
object GraphShapes {
    
    /**
     * Register all custom shapes with the mxGraphics2DCanvas
     */
    fun registerShapes() {
        // Register custom shape for user query nodes
        mxGraphics2DCanvas.putShape(
            "userQueryShape", 
            UserQueryNodeShape()
        )
        
        // Register custom shape for step nodes
        mxGraphics2DCanvas.putShape(
            "stepNodeShape", 
            StepNodeShape()
        )
        
        // Register custom shape for add button (circle with plus)
        mxGraphics2DCanvas.putShape(
            "addButtonShape", 
            AddButtonShape()
        )
        
        // Register custom shape for connection dot
        mxGraphics2DCanvas.putShape(
            "connectionDotShape", 
            ConnectionDotShape()
        )
    }
    
    /**
     * Custom shape for utterance/user query nodes with a purple dot in the top-left corner
     */
    class UserQueryNodeShape : mxRectangleShape() {
        override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
            // Get the graphics context and enable anti-aliasing
            val g = canvas.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // First paint the rectangle with rounded corners
            super.paintShape(canvas, state)
            
            // Then add the dot icon in the top-left
            g.color = Color(146, 119, 255)  // purple #9277FF
            
            // Calculate position for the dot
            val x = state.x.toInt() + 16
            val y = state.y.toInt() + 13
            val size = 24 // Size of the dot
            
            // Draw the circle
            g.fillOval(x, y, size, size)
        }
    }
    
    /**
     * Custom shape for step nodes with a green dot in the top-left corner
     */
    class StepNodeShape : mxRectangleShape() {
        override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
            // Get the graphics context and enable anti-aliasing
            val g = canvas.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // First paint the rectangle with rounded corners
            super.paintShape(canvas, state)
            
            // Then add the dot icon in the top-left
            g.color = Color(74, 222, 128)  // green #4ADE80
            
            // Calculate position for the dot
            val x = state.x.toInt() + 16
            val y = state.y.toInt() + 13
            val size = 24 // Size of the dot
            
            // Draw the circle
            g.fillOval(x, y, size, size)
        }
    }
    
    /**
     * Custom shape for add button with a plus sign
     */
    class AddButtonShape : mxRectangleShape() {
        override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
            // Get the graphics context and enable anti-aliasing
            val g = canvas.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // Draw a circle instead of using rectangle shape's method
            val x = state.x.toInt()
            val y = state.y.toInt()
            val w = state.width.toInt()
            val h = state.height.toInt()
            
            // Get stroke color from style
            val strokeColor = mxUtils.getColor(state.style, mxConstants.STYLE_STROKECOLOR, Color(146, 119, 255))
            g.color = strokeColor
            
            // Draw circle outline with thicker stroke
            val borderThickness = 3.0f  // Increased from default
            g.setStroke(BasicStroke(borderThickness))
            g.drawOval(x, y, w, h)
            
            // Then add the plus sign - now smaller
            g.color = Color(146, 119, 255)  // purple #9277FF
            
            // Calculate center of the button
            val cx = state.x.toInt() + state.width.toInt() / 2
            val cy = state.y.toInt() + state.height.toInt() / 2
            
            // Draw plus sign with thicker lines but smaller size
            val plusSize = 7  // Reduced from 10 to 7
            val lineThickness = 2
            g.setStroke(BasicStroke(lineThickness.toFloat()))
            g.drawLine(cx - plusSize, cy, cx + plusSize, cy)
            g.drawLine(cx, cy - plusSize, cx, cy + plusSize)
        }
    }
    
    /**
     * Custom shape for connection dot (shown on edge hover)
     */
    class ConnectionDotShape : mxRectangleShape() {
        override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
            val g = canvas.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g.color = Color(146, 119, 255)  // purple #9277FF
            
            // Draw the circle
            val x = state.x.toInt()
            val y = state.y.toInt()
            val w = state.width.toInt()
            val h = state.height.toInt()
            
            g.fillOval(x, y, w, h)
        }
    }
} 