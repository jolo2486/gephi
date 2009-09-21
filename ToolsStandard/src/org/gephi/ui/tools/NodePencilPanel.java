/*
Copyright 2008 WebAtlas
Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gephi.ui.tools;

import java.awt.Color;
import org.gephi.ui.components.JColorButton;

/**
 *
 * @author Mathieu Bastian
 */
public class NodePencilPanel extends javax.swing.JPanel {

    /** Creates new form NodePencilPanel */
    public NodePencilPanel() {
        initComponents();
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setColor(Color color) {
        ((JColorButton)colorButton).setColor(color);
    }

    public Color getColor() {
        return ((JColorButton)colorButton).getColor();
    }

    public float getNodeSize() {
        return (Float)sizeSpinner.getModel().getValue();
    }

    public void setNodeSize(float size) {
        sizeSpinner.getModel().setValue(new Float(size));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sizeSpinner = new javax.swing.JSpinner();
        labelSize = new javax.swing.JLabel();
        colorButton = new JColorButton(Color.BLACK);
        labelColor = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();

        sizeSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.0f), null, Float.valueOf(0.5f)));

        labelSize.setText(org.openide.util.NbBundle.getMessage(NodePencilPanel.class, "NodePencilPanel.labelSize.text")); // NOI18N

        colorButton.setText(org.openide.util.NbBundle.getMessage(NodePencilPanel.class, "NodePencilPanel.colorButton.text")); // NOI18N

        labelColor.setText(org.openide.util.NbBundle.getMessage(NodePencilPanel.class, "NodePencilPanel.labelColor.text")); // NOI18N

        statusLabel.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        statusLabel.setText(org.openide.util.NbBundle.getMessage(NodePencilPanel.class, "NodePencilPanel.statusLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(statusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 250, Short.MAX_VALUE)
                .addComponent(labelColor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17)
                .addComponent(labelSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sizeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(sizeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(labelSize)
                .addComponent(labelColor)
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(colorButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton colorButton;
    private javax.swing.JLabel labelColor;
    private javax.swing.JLabel labelSize;
    private javax.swing.JSpinner sizeSpinner;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables

}
