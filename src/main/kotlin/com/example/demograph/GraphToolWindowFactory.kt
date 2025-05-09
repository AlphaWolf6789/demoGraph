package com.example.demograph

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel

class GraphToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val graphPanel = GraphPanel()
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(graphPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class GraphPanel : JPanel(BorderLayout()) {
    private val graphManager = GraphManager()
    
    init {
        // Add the toolbar and graph component to the panel
        add(graphManager.createToolbar(), BorderLayout.NORTH)
        add(graphManager.graphComponent, BorderLayout.CENTER)
    }
}