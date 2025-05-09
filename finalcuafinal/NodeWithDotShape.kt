package com.example.finalcuafinal

import com.mxgraph.canvas.mxGraphics2DCanvas
import com.mxgraph.shape.mxRectangleShape
import com.mxgraph.view.mxCellState
import java.awt.Color

class NodeWithDotShape : mxRectangleShape() {
    @Override
    override fun paintShape(canvas: mxGraphics2DCanvas, state: mxCellState) {
        super.paintShape(canvas, state) // Vẽ node chính
        
        // Vẽ chấm tròn ở góc trái trên
        val g = canvas.graphics
        g.color = Color(99, 102, 241) // Màu tím #6366F1
        
        // Lấy vị trí và tính toán tọa độ cho chấm tròn
        val x = state.x.toInt() + 10
        val y = state.y.toInt() + 13
        val size = 24 // Kích thước chấm tròn lớn hơn (tăng từ 16 lên 24)
        
        // Vẽ hình tròn
        g.fillOval(x, y, size, size)
    }
} 