package com.example.demograph.ui

import com.example.demograph.model.NodeData
import java.awt.BorderLayout
import java.awt.Dialog
import javax.swing.*
import javax.swing.BoxLayout

class NodeDialog(private val owner: JPanel, private val existingIds: Set<String>) {
    fun show(): NodeData? {
        return showInternal(null, null, null)
    }
    
    fun showWithPrevStep(prevStepId: String, xPos: Double, yPos: Double): NodeData? {
        return showInternal(prevStepId, xPos, yPos)
    }
    
    private fun showInternal(prevStepId: String?, xPos: Double?, yPos: Double?): NodeData? {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(owner), "Add Node", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()
        dialog.setSize(400, 350)
        dialog.setLocationRelativeTo(owner)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val idField = JTextField("node${existingIds.size + 1}", 20)
        val labelField = JTextField("Node ${existingIds.size + 1}", 20)
        val nextStepField = JTextField("", 20)
        val prevStepField = JTextField(prevStepId ?: "", 20)

        panel.add(JLabel("Node ID:"))
        panel.add(idField)
        panel.add(JLabel("Label:"))
        panel.add(labelField)
        panel.add(JLabel("Next Step (comma-separated IDs, optional):"))
        panel.add(nextStepField)
        panel.add(JLabel("Prev Step (comma-separated IDs, optional):"))
        panel.add(prevStepField)
        panel.add(JLabel("Existing IDs (${existingIds.size}): ${existingIds.joinToString(", ")}"))

        val buttonPanel = JPanel()
        val okButton = JButton("OK")
        val cancelButton = JButton("Cancel")

        var result: NodeData? = null
        okButton.addActionListener {
            val id = idField.text.trim()
            val label = labelField.text.trim()
            val nextStep = nextStepField.text.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val prevStep = prevStepField.text.trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "ID cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            if (existingIds.contains(id)) {
                JOptionPane.showMessageDialog(dialog, "ID '$id' already exists", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            if (label.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Label cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }

            if (existingIds.isNotEmpty()) {
                val invalidNext = nextStep.filter { it !in existingIds }
                val invalidPrev = prevStep.filter { it !in existingIds }
                if (invalidNext.isNotEmpty() || invalidPrev.isNotEmpty()) {
                    val message = buildString {
                        if (invalidNext.isNotEmpty()) append("Invalid nextStep IDs: ${invalidNext.joinToString(", ")}\n")
                        if (invalidPrev.isNotEmpty()) append("Invalid prevStep IDs: ${invalidPrev.joinToString(", ")}")
                    }
                    JOptionPane.showMessageDialog(dialog, message, "Warning", JOptionPane.WARNING_MESSAGE)
                }
            }

            // Sử dụng vị trí được chỉ định hoặc vị trí mặc định
            val nodeX = xPos ?: (40.0 + (existingIds.size % 5) * 100.0)
            val nodeY = yPos ?: (40.0 + (existingIds.size / 5) * 60.0)

            result = NodeData(
                id = id,
                guideContent = label,
                xNode = nodeX,
                yNode = nodeY,
//                width = 100.0,
//                height = 40.0,
                nextStep = nextStep.filter { existingIds.isEmpty() || it in existingIds },
                prevStep = prevStep.filter { existingIds.isEmpty() || it in existingIds }
            )
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