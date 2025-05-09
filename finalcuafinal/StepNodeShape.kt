package com.example.finalcuafinal

import com.mxgraph.canvas.mxGraphics2DCanvas
import com.mxgraph.shape.mxRectangleShape
import com.mxgraph.view.mxCellState
import java.awt.Color

/**
 * Custom shape for step nodes with a green dot in the top-left corner
 */
class StepNodeShape : mxRectangleShape() {
    @Override
    override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
        super.paintShape(canvas, state) // Draw the rectangle first
        
        // Draw the green dot in the top-left corner
        val g = canvas.graphics
        g.color = Color(74, 222, 128) // Green color #4ade80
        
        // Calculate position for the dot
        val x = state.x.toInt() + 10
        val y = state.y.toInt() + 13
        val size = 24 // Size of the dot
        
        // Draw the circle
        g.fillOval(x, y, size, size)
    }
} 