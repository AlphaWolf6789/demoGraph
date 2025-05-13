package com.example.demograph.service

import com.example.demograph.model.GraphData
import com.example.demograph.model.GraphModel
import com.example.demograph.model.NodeData
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mxgraph.io.mxCodec
import com.mxgraph.model.mxICell
import com.mxgraph.util.mxXmlUtils
import java.io.File
import javax.swing.JOptionPane

class StorageService(private val graphModel: GraphModel) {
    private val mapper = jacksonObjectMapper()

    fun saveGraphXml() {
        try {
            val encoder = mxCodec()
            val node = encoder.encode(graphModel.getGraph().model)
            val xml = mxXmlUtils.getXml(node)
            val file = File("graph.xml")
            file.writeText(xml)
            JOptionPane.showMessageDialog(null, "XML saved to ${file.absolutePath}")
            println("saveGraphXml: XML saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to save XML: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("saveGraphXml: Error - ${e.message}")
        }
    }

    fun loadGraphXml() {
        try {
            val file = File("graph.xml")
            if (file.exists()) {
                val xml = file.readText()
                val document = mxXmlUtils.parseXml(xml)
                val decoder = mxCodec(document)
                decoder.decode(document.documentElement, graphModel.getGraph().model)
                graphModel.getGraph().refresh()
                val nodeCount = graphModel.getGraph().model.getChildCount(graphModel.parent as mxICell)
                val edgeCount = graphModel.getGraph().model.getChildCount(graphModel.parent as mxICell) - nodeCount
                graphModel.setNodeCount(nodeCount)
                graphModel.setEdgeCount(edgeCount)
                JOptionPane.showMessageDialog(null, "XML loaded successfully")
                println("loadGraphXml: XML loaded, nodeCount: $nodeCount, edgeCount: $edgeCount")
            } else {
                JOptionPane.showMessageDialog(null, "graph.xml not found")
                println("loadGraphXml: graph.xml not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to load XML: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("loadGraphXml: Error - ${e.message}")
        }
    }

    fun saveGraphJson() {
        try {
            val nodes = mutableListOf<NodeData>()
            val edgeMap = mutableMapOf<String, MutableList<String>>()
            val prevEdgeMap = mutableMapOf<String, MutableList<String>>()
            graphModel.collectNodesAndEdges(nodes, edgeMap, prevEdgeMap)
            val finalNodes = nodes.map { node ->
                node.copy(
                    nextStep = edgeMap[node.id] ?: emptyList(),
                    prevStep = prevEdgeMap[node.id] ?: emptyList()
                )
            }
            val graphData = GraphData(finalNodes)
            val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(graphData)
            val file = File("graph.json")
            file.writeText(json)
            JOptionPane.showMessageDialog(null, "JSON saved to ${file.absolutePath}")
            println("saveGraphJson: JSON saved to ${file.absolutePath}")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to save JSON: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("saveGraphJson: Error - ${e.message}")
        }
    }

    fun loadGraphJson() {
        try {
            val file = File("graph.json")
            if (file.exists()) {
                val json = file.readText()
                val graphData: GraphData = mapper.readValue(json)
                graphModel.getGraph().model.beginUpdate()
                try {
                    graphModel.clear()
                    val nodeMap = mutableMapOf<String, Any>()
                    nodeMap["defaultParent"] = graphModel.parent

                    for (nodeData in graphData.nodes) {
                        val cell = graphModel.getGraph().insertVertex(
                            graphModel.parent, nodeData.id, nodeData.guideContent,
                            nodeData.xNode, nodeData.yNode, nodeData.width, nodeData.height
                        )
                        nodeMap[nodeData.id] = cell
                        println("loadGraphJson: Added node ${nodeData.id}")
                    }

                    for (nodeData in graphData.nodes) {
                        val source = nodeMap[nodeData.id] ?: continue
                        for (targetId in nodeData.nextStep) {
                            val target = nodeMap[targetId] ?: continue
                            val id = "edge${graphModel.getEdgeCount() + 1}"
                            graphModel.getGraph().insertEdge(graphModel.parent, id, "", source, target)
                            println("loadGraphJson: Added edge from ${nodeData.id} to $targetId")
                        }
                    }
                } finally {
                    graphModel.getGraph().model.endUpdate()
                }
                graphModel.getGraph().refresh()
                graphModel.setNodeCount(graphData.nodes.size)
                graphModel.setEdgeCount(graphData.nodes.sumOf { it.nextStep.size })
                JOptionPane.showMessageDialog(null, "JSON loaded successfully")
                println("loadGraphJson: Loaded ${graphModel.getNodeCount()} nodes, ${graphModel.getEdgeCount()} edges")
            } else {
                JOptionPane.showMessageDialog(null, "graph.json not found")
                println("loadGraphJson: graph.json not found")
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed to load JSON: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            println("loadGraphJson: Error - ${e.message}")
        }
    }
}