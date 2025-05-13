package com.example.demograph.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class GraphToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val graphPanel = GraphPanel()
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(graphPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}