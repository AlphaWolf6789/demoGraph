package com.example.finalcuafinal

import com.mxgraph.model.mxCell
import com.mxgraph.view.mxGraph
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder

/**
 * A utility class for handling the edit dialog for nodes in the graph
 */
class NodeEditDialog {
    companion object {
        /**
         * Shows an edit dialog for the specified cell
         * 
         * @param parent The parent component (NodeGraph) that contains the cell
         * @param cell The cell to edit
         * @param graph The graph containing the cell
         */
        fun showEditDialog(parent: JPanel, cell: mxCell, graph: mxGraph) {
            val content = graph.getModel().getValue(cell).toString()
            // Extract the query text from the HTML content
            val queryText = extractQueryTextFromHtml(content)
            
            // Create a dialog for editing
            val dialog = JDialog(SwingUtilities.getWindowAncestor(parent) as Frame, "EDIT USER QUERY", true)
            dialog.layout = BorderLayout()
            dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            
            val panel = JPanel(BorderLayout())
            panel.background = Color(33, 33, 33)
            panel.border = EmptyBorder(20, 20, 20, 20)
            
            val headerLabel = JLabel("EDIT USER QUERY")
            headerLabel.foreground = Color.WHITE
            headerLabel.font = Font("Arial", Font.BOLD, 16)
            panel.add(headerLabel, BorderLayout.NORTH)
            
            val textArea = JTextArea(queryText)
            textArea.preferredSize = Dimension(300, 100)
            textArea.background = Color(59, 59, 59)
            textArea.foreground = Color.WHITE
            textArea.caretColor = Color.WHITE
            textArea.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
            panel.add(JScrollPane(textArea), BorderLayout.CENTER)
            
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            buttonPanel.background = Color(33, 33, 33)
            
            val updateButton = JButton("Update")
            updateButton.background = Color(100, 100, 100)
            updateButton.foreground = Color.WHITE
            updateButton.addActionListener {
                // Update the node content with edited text
                graph.model.setValue(cell, createQueryNodeContent(textArea.text))
                dialog.dispose()
            }
            
            val closeButton = JButton("Close")
            closeButton.background = Color(100, 100, 100)
            closeButton.foreground = Color.WHITE
            closeButton.addActionListener { dialog.dispose() }
            
            buttonPanel.add(updateButton)
            buttonPanel.add(closeButton)
            panel.add(buttonPanel, BorderLayout.SOUTH)
            
            dialog.contentPane = panel
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
        }
        
        /**
         * Creates HTML content for a query node
         */
        fun createQueryNodeContent(queryText: String): String {
            return """
            <table style="width:150px; color:#fff; font-family:Arial; font-size:9px;margin-top:30px;">
                <tr><td>${queryText.escapeHtml()}</td></tr> 
                <tr><td style="font-weight:bold; opacity:0.7;">User Query</td></tr>
            </table>
            """.trimIndent()
        }
        
        /**
         * Creates HTML content for a step node
         */
        fun createStepNodeContent(stepName: String, guideText: String): String {
            return """
               <table style="width:150px; color:#fff; font-family:Arial; font-size:9px;margin-top:30px;">
                  <tr><td>${guideText.escapeHtml()}</td></tr>
                  <tr><td style="font-weight:bold; opacity:0.7;">${stepName.escapeHtml()}</td></tr>
                </table>
            """.trimIndent()
        }
        
        /**
         * Extract query text from HTML content
         */
        private fun extractQueryTextFromHtml(htmlContent: String): String {
            // Simple regex to extract text between <td> and </td> tags from the first row
            val pattern = "<tr><td>(.*?)</td></tr>".toRegex()
            val matchResult = pattern.find(htmlContent)
            return matchResult?.groupValues?.get(1) ?: ""
        }
        
        /**
         * Extension function to escape HTML special characters
         */
        fun String.escapeHtml(): String {
            return this.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }
    }
} 