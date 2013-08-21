/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.legend.plugin.groups;

import java.awt.Color;
import java.util.ArrayList;
import org.gephi.legend.spi.LegendItem;
import org.gephi.preview.api.Item;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author mvvijesh, edubecks
 */
@ServiceProvider(service = CustomGroupsItemBuilder.class, position = 1)
public class Default implements CustomGroupsItemBuilder {

    @Override
    public String getDescription() {
        return DEFAULT_DESCRIPTION;
    }

    @Override
    public String getTitle() {
        return DEFAULT_TITLE;
    }

    @Override
    public boolean isAvailableToBuild() {
        return true;
    }

    @Override
    public String stepsNeededToBuild() {
        return NONE_NEEDED;
    }

    @Override
    public void retrieveData(Item item, ArrayList<GroupElement> groups) {
        String[] labels = {"Group 1", "Group 2", "Group 3", "Group 4", "Group 5"};
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK};
        Float[] values = {1f, 2f, 1.5f, 1f, 0.75f};

        for (int i = 0; i < labels.length; i++) {
            GroupElement groupElement = new GroupElement(item, labels[i], GroupElement.labelFont, GroupElement.labelColor, GroupElement.labelAlignment, GroupElement.labelPosition, values[i], GroupElement.shape, colors[i], true);
            groups.add(groupElement);
        }
    }
}