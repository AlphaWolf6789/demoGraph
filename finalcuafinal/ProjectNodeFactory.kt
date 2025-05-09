package com.example.finalcuafinal

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ProjectNodeFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val nodeGraph = NodeGraph()
        
        // Create a new project node structure when tool window is opened
        SwingUtilities.invokeLater {
            nodeGraph.createNewProjectNode()
        }
        
        // Add content to the tool window
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(nodeGraph, "Project Nodes", false)
        toolWindow.contentManager.addContent(content)
    }
} 