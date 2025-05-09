package com.example.demograph

import com.mxgraph.model.mxCell
import com.mxgraph.view.mxGraph

class CustomGraph : mxGraph() {
    override fun isPort(cell: Any?): Boolean {
        return cell is mxCell && model.getStyle(cell)?.contains("portStyle") == true
    }
}