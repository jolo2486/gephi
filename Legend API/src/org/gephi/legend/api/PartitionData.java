/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.legend.api;

import java.awt.Color;
import java.util.ArrayList;
import org.gephi.graph.api.Node;
import org.gephi.partition.api.NodePartition;
import org.gephi.partition.api.Part;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.api.PartitionModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

/**
 *
 * @author eduBecKs
 */
public class PartitionData {

    ArrayList<String> labels;
    ArrayList<Color> colors;
    ArrayList<Float> values;

    public PartitionData() {
    }

    /**
     * Retrieves the current labels used in each partition
     *
     * @return
     */
    public ArrayList<String> getLabels() {
        if (labels == null) {
            retrieveData();
        }
        return labels;
    }

    /**
     * Retrieves the current colors used in each partition
     *
     * @return
     */
    public ArrayList<Color> getColors() {
        if (colors == null) {
            retrieveData();
        }
        return colors;
    }

    /**
     * Retrieves the current values used in each partition
     *
     * @return
     */
    public ArrayList<Float> getValues() {
        if (values == null) {
            retrieveData();
        }
        return values;
    }

    private void retrieveData() {

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        Workspace workspace = pc.getCurrentWorkspace();


        PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        PartitionModel partitionModel = partitionController.getModel();

        this.labels = new ArrayList<String>();
        this.colors = new ArrayList<Color>();
        this.values = new ArrayList<Float>();

        if (partitionModel.getSelectedPartition() != null && partitionModel.getSelectedPartitioning() == PartitionModel.NODE_PARTITIONING) {
            NodePartition nodesPartition = (NodePartition) partitionModel.getSelectedPartition();
            for (Part<Node> part : nodesPartition.getParts()) {
                values.add(part.getPercentage());
                colors.add(part.getColor());
                labels.add(part.getValue().toString());
            }
        }
    }

}
