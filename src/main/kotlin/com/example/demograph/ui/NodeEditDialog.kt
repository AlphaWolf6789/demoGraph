package com.example.demograph.ui

import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities

class NodeEditDialog(private val owner: JPanel, private val nodeId: String, private val currentContent: String, private val existingIds: Set<String>) {
    fun show(): String? {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(owner), "Edit Node Content", JDialog.DEFAULT_MODALITY_TYPE)
        dialog.layout = BorderLayout()
        dialog.setSize(400, 200)
        dialog.setLocationRelativeTo(owner)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        panel.add(JLabel("Node ID: $nodeId (cannot be changed)"))
        val contentField = JTextField(currentContent, 20)
        panel.add(JLabel("Content:"))
        panel.add(contentField)

        val buttonPanel = JPanel()
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")

        var result: String? = null
        okButton.addActionListener {
            val content = contentField.text.trim()
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Content cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            result = content
            dialog.dispose()
        }
        cancelButton.addActionListener { dialog.dispose() }

        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)
        dialog.add(panel, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)
        dialog.isVisible = true

        return result
    }
}