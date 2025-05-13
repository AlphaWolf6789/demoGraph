package com.example.demograph.service

import com.mxgraph.swing.mxGraphComponent

class ZoomService {
    private var currentZoom = 1.0
    private val zoomFactor = 1.2
    private val minZoom = 0.1
    private val maxZoom = 5.0

    fun zoomIn(graphComponent: mxGraphComponent) {
        if (currentZoom < maxZoom) {
            graphComponent.zoomIn()
            currentZoom *= zoomFactor
            println("Zoom in: $currentZoom")
        }
    }

    fun zoomOut(graphComponent: mxGraphComponent) {
        if (currentZoom > minZoom) {
            graphComponent.zoomOut()
            currentZoom /= zoomFactor
            println("Zoom out: $currentZoom")
        }
    }
}