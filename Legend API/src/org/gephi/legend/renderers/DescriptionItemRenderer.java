/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.legend.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import org.gephi.legend.api.LegendItem;
import org.gephi.legend.api.LegendItem.Alignment;
import org.gephi.legend.api.LegendManager;
import org.gephi.legend.builders.DescriptionItemBuilder;
import org.gephi.legend.items.DescriptionItem;
import org.gephi.legend.properties.DescriptionProperty;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author edubecks
 */
@ServiceProvider(service = Renderer.class, position = 504)
public class DescriptionItemRenderer extends LegendItemRenderer {

    @Override
    public void renderToGraphics(Graphics2D graphics2D, AffineTransform origin, Integer width, Integer height) {

        if (numElements > 0) {
            graphics2D.setTransform(origin);
            int elementHeight = height / numElements;

            int padding = 5;
            FontMetrics keyFontMetrics = graphics2D.getFontMetrics(keyFont);
            FontMetrics valueFontMetrics = graphics2D.getFontMetrics(valueFont);

            int maxKeyLabelWidth = Integer.MIN_VALUE;
            for (String key : keys) {
                maxKeyLabelWidth = Math.max(maxKeyLabelWidth, keyFontMetrics.stringWidth(key));
            }

            for (int i = 0; i < numElements; i++) {
                String key = keys.get(i);
                String value = values.get(i);
                int xKey = 0;
                int yKey = i * elementHeight;
                int xValue = 0;
                int yValue = i * elementHeight;
                if (isFlowLayout) {
                    xValue += padding + keyFontMetrics.stringWidth(key);
                }
                else {
                    xValue += padding + maxKeyLabelWidth;
                }

                legendDrawText(graphics2D, key, keyFont, keyFontColor, xKey, yKey, xValue - xKey, elementHeight, keyAlignment);
                legendDrawText(graphics2D, value, valueFont, valueFontColor, xValue, yValue, width - xValue, elementHeight, valueAlignment);
            }
        }

    }

    @Override
    public void readOwnPropertiesAndValues(Item item, PreviewProperties previewProperties) {
        if (item != null) {

            PreviewProperty[] itemProperties = item.getData(LegendItem.PROPERTIES);

            Integer itemIndex = item.getData(LegendItem.ITEM_INDEX);



            keyFont = previewProperties.getFontValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_KEY_FONT));
            keyFontColor = previewProperties.getColorValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_KEY_FONT_COLOR));
            keyAlignment = (Alignment) previewProperties.getValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_KEY_FONT_ALIGNMENT));

            valueFont = previewProperties.getFontValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_VALUE_FONT));
            valueFontColor = previewProperties.getColorValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_VALUE_FONT_COLOR));
            valueAlignment = (Alignment) previewProperties.getValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_VALUE_FONT_ALIGNMENT));

            isFlowLayout = previewProperties.getBooleanValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_IS_FLOW_LAYOUT));

            // reading keys
            Integer numberOfItems = previewProperties.getIntValue(LegendManager.getProperty(DescriptionProperty.OWN_PROPERTIES, itemIndex, DescriptionProperty.DESCRIPTION_NUMBER_OF_ITEMS));
            System.out.println("@Var: numberOfItems: " + numberOfItems);
            if (DescriptionItemBuilder.updatePreviewProperty(item, numberOfItems)) {
                System.out.printf("Refresh property sheet\n");
                LegendManager legendManager = previewProperties.getValue(LegendManager.LEGEND_PROPERTIES);
                legendManager.refreshActiveLegendsComboBox();
            }
            keys = new ArrayList<String>();
            values = new ArrayList<String>();
            for (int i = 0; i < numberOfItems; i++) {
                String key = previewProperties.getStringValue(LegendManager.getDynamicProperty(DescriptionProperty.OWN_PROPERTIES[DescriptionProperty.DESCRIPTION_KEY], itemIndex, i));
                System.out.println("@Var: key: " + key);
                String value = previewProperties.getStringValue(LegendManager.getDynamicProperty(DescriptionProperty.OWN_PROPERTIES[DescriptionProperty.DESCRIPTION_VALUE], itemIndex, i));
                System.out.println("@Var: value: " + value);
                keys.add(key);
                values.add(value);
            }
            numElements = keys.size();
            LegendManager legendManager = previewProperties.getValue(LegendManager.LEGEND_PROPERTIES);
            legendManager.refreshDynamicPreviewProperties();
        }
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(DescriptionItemRenderer.class, "DescriptionItemRenderer.name");
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return item instanceof DescriptionItem;
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof DescriptionItemBuilder;
    }

    private ArrayList<String> keys;
    private ArrayList<String> values;
    private Font keyFont;
    private Alignment keyAlignment;
    private Color keyFontColor;
    private Font valueFont;
    private Color valueFontColor;
    private Alignment valueAlignment;
    private Boolean isFlowLayout;
    private Integer numElements;
}
