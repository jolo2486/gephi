/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.legend.renderers;

import com.itextpdf.text.pdf.PdfContentByte;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import org.apache.batik.svggen.SVGGraphics2D;
import org.gephi.legend.builders.TextItemBuilder;
import org.gephi.legend.items.TextItem;
import org.gephi.preview.api.*;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.w3c.dom.Node;
import processing.core.PGraphicsJava2D;

/**
 *
 * @author edubecks
 */
@ServiceProvider(service = Renderer.class, position = 400)
public class TextItemRenderer extends LegendItemRenderer {

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(TextItemRenderer.class, "TextItemRenderer.name");
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
    }

    //body
    private String body;
    private Font bodyFont;
    private Color bodyFontColor;
    
    @Override
    protected void readOwnPropertiesAndValues(Item item, PreviewProperties properties) {
        
        body = item.getData(TextItem.BODY);
        bodyFont = properties.getFontValue(PreviewProperty.LEGEND_TEXT_BODY_FONT);
        bodyFontColor = properties.getColorValue(PreviewProperty.LEGEND_TEXT_BODY_FONT_COLOR);
    }
    

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {

//        readPropertiesAndValues(item, properties);
        readLegendPropertiesAndValues(item, properties);
        readOwnPropertiesAndValues(item, properties);

        if (target instanceof ProcessingTarget) {
            renderProcessing((ProcessingTarget) target);
        }
        else if (target instanceof SVGTarget) {
            renderSVG((SVGTarget) target);
        }
        else if (target instanceof PDFTarget) {
            renderPDF((PDFTarget) target);

        }

    }

    
    // default values
    
    protected final Font defaultBodyFont = new Font("Arial", Font.PLAIN, 20);
    protected final Color defaultBodyFontColor = Color.BLACK;

    @Override
    public PreviewProperty[] getProperties() {
        
        PreviewProperty[] legendProperties = createLegendProperties();
        PreviewProperty[] properties = {PreviewProperty.createProperty(this,
                                                   PreviewProperty.LEGEND_TEXT_BODY_FONT,
                                                   Font.class,
                                                   NbBundle.getMessage(TextItemRenderer.class, "TextItemRenderer.property.body.font.displayName"),
                                                   NbBundle.getMessage(TextItemRenderer.class, "TextItemRenderer.property.body.font.description"),
                                                   PreviewProperty.CATEGORY_LEGENDS).setValue(defaultBodyFont),
                    PreviewProperty.createProperty(this,
                                                   PreviewProperty.LEGEND_TEXT_BODY_FONT_COLOR,
                                                   Color.class,
                                                   NbBundle.getMessage(TextItemRenderer.class, "TextItemRenderer.property.body.font.color.displayName"),
                                                   NbBundle.getMessage(TextItemRenderer.class, "TextItemRenderer.property.body.font.color.description"),
                                                   PreviewProperty.CATEGORY_LEGENDS).setValue(defaultBodyFontColor)};
        
        
        PreviewProperty[] previewProperties = new PreviewProperty[legendProperties.length+properties.length];
        System.arraycopy(legendProperties, 0, previewProperties, 0, legendProperties.length);
        System.arraycopy(properties, 0, previewProperties, legendProperties.length, properties.length);
        
        return previewProperties;
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return item instanceof TextItem;
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof TextItemBuilder;
    }
    

    @Override
    public void renderToGraphics(Graphics2D graphics2D, AffineTransform origin, int width, int height) {

        graphics2D.setTransform(origin);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics bodyFontMetrics = graphics2D.getFontMetrics(bodyFont);
        int bodyFontHeight = bodyFontMetrics.getHeight();

        FontMetrics titleFontMetrics = graphics2D.getFontMetrics(titleFont);
        int titleFontHeight = titleFontMetrics.getHeight();

        FontMetrics descriptionFontMetrics = graphics2D.getFontMetrics(descriptionFont);
        int descriptionFontHeight = descriptionFontMetrics.getHeight();

        int actualLine = 0;

        // title 
        if (isDisplayingTitle) {
            actualLine += titleFontHeight;
            graphics2D.setColor(titleFontColor);
            graphics2D.setFont(titleFont);
            graphics2D.drawString(title, 0, actualLine);
        }

        actualLine+= bodyFontHeight;
        graphics2D.setColor(bodyFontColor);
        graphics2D.setFont(bodyFont);
        graphics2D.drawString(body, 0, actualLine);
        System.out.println("@Var: body: " + body);
        
        
        // description
        if (isDisplayingDescription) {
            actualLine += descriptionFontHeight;
            graphics2D.setColor(descriptionFontColor);
            graphics2D.setFont(descriptionFont);
            graphics2D.drawString(description, 0, actualLine);
        }


    }

}
