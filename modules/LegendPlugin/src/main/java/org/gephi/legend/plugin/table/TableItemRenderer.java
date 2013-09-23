package org.gephi.legend.plugin.table;

import com.bric.swing.ColorPicker;
import com.connectina.swing.fontchooser.JFontChooser;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.gephi.graph.api.Graph;
import org.gephi.legend.api.AbstractLegendItemRenderer;
import org.gephi.legend.api.BlockNode;
import org.gephi.legend.api.LegendController;
import org.gephi.legend.api.LegendModel;
import org.gephi.legend.inplaceeditor.Column;
import org.gephi.legend.inplaceeditor.InplaceClickResponse;
import org.gephi.legend.inplaceeditor.InplaceEditor;
import org.gephi.legend.inplaceeditor.InplaceItemBuilder;
import org.gephi.legend.inplaceeditor.InplaceItemRenderer;
import org.gephi.legend.inplaceeditor.Row;
import org.gephi.legend.inplaceelements.BaseElement;
import org.gephi.legend.inplaceelements.ElementCheckbox;
import org.gephi.legend.inplaceelements.ElementColor;
import org.gephi.legend.inplaceelements.ElementFile;
import org.gephi.legend.inplaceelements.ElementFont;
import org.gephi.legend.inplaceelements.ElementFunction;
import org.gephi.legend.inplaceelements.ElementImage;
import org.gephi.legend.inplaceelements.ElementLabel;
import org.gephi.legend.inplaceelements.ElementNumber;
import org.gephi.legend.inplaceelements.ElementText;
import org.gephi.legend.spi.LegendItem;
import org.gephi.legend.spi.LegendItem.Alignment;
import org.gephi.legend.spi.LegendItem.Shape;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.spi.ItemBuilder;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * this is the renderer for table item.
 *
 * This renderer does not provide a service. Hence, it should be registered with
 * the legend model when the corresponding items are built. Since, it doesnt
 * provide a service, this class should be made singleton. Note that this
 * approach is subjected to change.
 *
 * The table item consists of a two dimensional array of cells. A 'cell' is an
 * object which has a collection of preview properties for that cell. The table
 * legend three modes of rendering: text, shape and image. Each cell can be
 * configured to one mode at a time. The inplace editors associated with the
 * cell nodes change according to the type of cell. When the type of a cell is
 * shape, its value is normalized across the cells in the shape mode and then
 * rendered.
 *
 * @author mvvijesh, edubecks
 */
public class TableItemRenderer extends AbstractLegendItemRenderer {

    public static String TABLENODE = "table node";
    public static String CELLNODE = "cell node";
    public static String CELLNODE_ROW_NUMBER = "cell node row number";
    public static String CELLNODE_COL_NUMBER = "cell node column number";
    public static String TABLE_PROPERTIES_ADDED = "table properties added";
    // own properties
    private Font tableFont;
    private Color tableFontColor;
    private Alignment tableFontAlignment;
    private int tableCellSpacing;
    private int tableCellPadding;
    private int tableCellBorderSize;
    private Color tableCellBorderColor;
    private Color tableBackgroundColor;
    private Boolean tableIsOccupyingFullWidth;
    private int tableNumberOfRows;
    private int tableNumberOfColumns;
    private ArrayList<ArrayList<Cell>> table;
    private Boolean structureChanged;
    private float colWidthTolerance = 0.05f;
    private int cellSpacingLowerLimit = 5;
    private int cellPaddingLowerLimit = 5;
    private int cellBorderLowerLimit = 5;
    private Boolean insufficientEmptySpace;
    // instance
    private static TableItemRenderer instance = null;

    private TableItemRenderer() {
        // private constructor is required to ensure singleton class
    }

    public static TableItemRenderer getInstance() {
        if (instance == null) {
            instance = new TableItemRenderer();
        }

        return instance;
    }

    @Override
    public boolean isAnAvailableRenderer(Item item) {
        return item instanceof TableItem;
    }

    /**
     *
     * @param item - table item being rendered
     * @param properties - PreviewProperty for the PreviewModel
     */
    @Override
    protected void readOwnPropertiesAndValues(Item item, PreviewProperties properties) {
        PreviewProperty[] tableItemPreviewProperties = item.getData(LegendItem.OWN_PROPERTIES);

        tableFont = tableItemPreviewProperties[TableProperty.TABLE_FONT].getValue();
        tableFontColor = tableItemPreviewProperties[TableProperty.TABLE_FONT_COLOR].getValue();
        tableFontAlignment = tableItemPreviewProperties[TableProperty.TABLE_FONT_ALIGNMENT].getValue();
        tableCellSpacing = tableItemPreviewProperties[TableProperty.TABLE_CELL_SPACING].getValue();
        tableCellPadding = tableItemPreviewProperties[TableProperty.TABLE_CELL_PADDING].getValue();
        tableCellBorderSize = tableItemPreviewProperties[TableProperty.TABLE_BORDER_SIZE].getValue();
        tableCellBorderColor = tableItemPreviewProperties[TableProperty.TABLE_BORDER_COLOR].getValue();
        tableBackgroundColor = tableItemPreviewProperties[TableProperty.TABLE_BACKGROUND_COLOR].getValue();
        tableIsOccupyingFullWidth = tableItemPreviewProperties[TableProperty.TABLE_WIDTH_FULL].getValue();

        tableNumberOfRows = ((TableItem) item).getNumberOfRows();
        tableNumberOfColumns = ((TableItem) item).getNumberOfColumns();

        table = ((TableItem) item).getTable();
        structureChanged = ((TableItem) item).getStructureChanged();
        insufficientEmptySpace = tableCellSpacing < cellSpacingLowerLimit && tableCellPadding < cellPaddingLowerLimit && tableCellBorderSize < cellBorderLowerLimit;
        // this condition is used to transfer the table node's inplace editors onto cell node's inplace editors when the intercellular space becomes too small to click on.
    }

    /**
     *
     * @param graphics2D - graphics object for the target
     * @param target - the target onto which the item should be rendered - SVG,
     * PDF or G2D
     * @param legendNode - BlockNode onto which the legend content will be
     * rendered.
     */
    @Override
    protected void renderToGraphics(Graphics2D graphics2D, RenderTarget target, BlockNode legendNode) {
        // get the legendNode Geometry
        int blockOriginX = (int) (legendNode.getOriginX());
        int blockOriginY = (int) (legendNode.getOriginY());
        int blockWidth = (int) legendNode.getBlockWidth();
        int blockHeight = (int) legendNode.getBlockHeight();

        // compute the mean column width and the row height
        int meanColWidth = (blockWidth - (tableNumberOfColumns + 1) * tableCellSpacing - 2 * tableNumberOfColumns * tableCellPadding - 2 * tableNumberOfColumns * tableCellBorderSize) / tableNumberOfColumns;
        int rowHeight = (blockHeight - (tableNumberOfRows + 1) * tableCellSpacing - 2 * tableNumberOfRows * tableCellPadding - 2 * tableNumberOfRows * tableCellBorderSize) / tableNumberOfRows;

        // determine the width to the columns
        TableItem item = (TableItem) legendNode.getItem();
        Integer[] tableColumnWidths = new Integer[tableNumberOfColumns];

        if (tableIsOccupyingFullWidth) {
            // the table should occupy the entire width
            for (int i = 0; i < tableNumberOfColumns; i++) {
                tableColumnWidths[i] = meanColWidth;
            }
        } else {
            // the table shouldnt occupy the entire width
            // Hence, compute and fill up the column widths appropriately
            Cell cell;
            int cellActiveWidth;
            int extraSpace = 0;
            for (int col = 0; col < tableNumberOfColumns; col++) {
                int maxRowWidthInColumn = 0;
                for (int row = 0; row < tableNumberOfRows; row++) {
                    cell = table.get(row).get(col);
                    cellActiveWidth = cell.getActiveWidth(graphics2D);

                    if (cellActiveWidth > maxRowWidthInColumn) {
                        maxRowWidthInColumn = cellActiveWidth;
                    }
                }

                maxRowWidthInColumn = (int) ((1 + colWidthTolerance) * maxRowWidthInColumn);
                // the column width should be exactly the same as the width of the longest string in the column, since it causes rendering problems.
                if (maxRowWidthInColumn < meanColWidth + extraSpace) {
                    tableColumnWidths[col] = maxRowWidthInColumn;
                    extraSpace += (meanColWidth - maxRowWidthInColumn);
                } else {
                    tableColumnWidths[col] = meanColWidth;
                }

                for (int row = 0; row < tableNumberOfRows; row++) {
                    table.get(row).get(col).updateWidthsHeights(tableColumnWidths[col], rowHeight);
                }
            }
        }

        int sumOfWidths = 0;
        for (int col = 0; col < tableNumberOfColumns; col++) {
            sumOfWidths += tableColumnWidths[col];
        }

        // compute table node dimensions
        int tableWidth = sumOfWidths + (tableNumberOfColumns + 1) * tableCellSpacing + 2 * tableNumberOfColumns * tableCellPadding + 2 * tableNumberOfColumns * tableCellBorderSize;
        int tableHeight = blockHeight;
        int tableOriginX = blockOriginX + blockWidth / 2 - tableWidth / 2;
        int tableOriginY = blockOriginY + blockHeight / 2 - tableHeight / 2;

        // if an table node is already created within a legend node, we need not create it again and build the inplace editors for it.
        BlockNode tableNode = legendNode.getChild(TABLENODE);
        if (tableNode == null) {
            // the table node has not been added. Hence, add create and add it.
            tableNode = legendNode.addChild(tableOriginX, tableOriginY, tableWidth, tableHeight, TABLENODE);

            // build and associate an inplace editor with the table node
            buildInplaceTable(tableNode, item, graphics2D, target);
        }

        // irrespective of whether a table node is newly built, update the origin and dimensions of the table legend
        tableNode.updateGeometry(tableOriginX, tableOriginY, tableWidth, tableHeight);

        // render background
        graphics2D.setColor(tableBackgroundColor);
        graphics2D.fillRect((int) (tableOriginX - currentRealOriginX), (int) (tableOriginY - currentRealOriginY), tableWidth, tableHeight);

        // cells are re-constructed only when the structure (i.e, number of rows and columns) of the table is changed
        if (structureChanged) {
            buildCellNodes(tableNode, item, rowHeight, tableColumnWidths, graphics2D, target);
            // once the cells are built, change the flag to false to indicate that the block nodes are built.
            item.setRowChanged(false);
            item.setColumnChanged(false);
            item.setInplaceEditorChanged(false);
            tableNode.getInplaceEditor().setData(TABLE_PROPERTIES_ADDED, false);
        }

        if (insufficientEmptySpace) {
            // The table properties must be added only once, to avoid the chain of properties.
            // check if the table properties has been added.
            if (!(Boolean) tableNode.getInplaceEditor().getData(TABLE_PROPERTIES_ADDED)) {
                addTablePropertiesToCells(tableNode, item, graphics2D, target);
                tableNode.getInplaceEditor().setData(TABLE_PROPERTIES_ADDED, true);
            }
        }

        // update the geometry of the cell nodes - this is redundant only when the table structure changes
        updateCellGeometry(tableNode, rowHeight, tableColumnWidths);

        // render the cells
        renderCells(graphics2D, tableNode, (TableItem) item);
    }

    /**
     * associate an inplace renderer with the table node.
     *
     * @param tableNode - BlockNode containing the table content
     * @param item - the legend table item
     * @param graphics2D - the graphics object for the target
     * @param target - the target onto which the item should be rendered - SVG,
     * PDF or G2D
     */
    private void buildInplaceTable(BlockNode tableNode, Item item, Graphics2D graphics2d, RenderTarget target) {
        Graph graph = null;
        InplaceItemBuilder ipbuilder = Lookup.getDefault().lookup(InplaceItemBuilder.class);
        InplaceEditor ipeditor = ipbuilder.createInplaceEditor(graph, tableNode);
        int itemIndex = item.getData(LegendItem.ITEM_INDEX);
        PreviewProperty[] tablePreviewProperties = item.getData(LegendItem.OWN_PROPERTIES);

        buildTableProperties(ipeditor, itemIndex, tablePreviewProperties, graphics2d, target);

        tableNode.setInplaceEditor(ipeditor);
    }

    /**
     *
     * @param tableNode - BlockNode containing the table content
     * @param item - the legend table item
     * @param rowHeight
     * @param colWidths - precomputed array of column widths
     * @param graphics2D - the graphics object for the target
     * @param target - the target onto which the item should be rendered - SVG,
     * PDF or G2D
     */
    private void buildCellNodes(BlockNode tableNode, Item item, int rowHeight, Integer[] colWidths, Graphics2D graphics2d, RenderTarget target) {
        // the legend model will still contain the reference to the old inplace editor, not the updated one. Hence, update it.
        LegendController legendController = LegendController.getInstance();
        LegendModel legendModel = legendController.getLegendModel();
        InplaceEditor currentInplaceEditor = legendModel.getInplaceEditor();
        Integer activeCellRow = null;
        Integer activeCellColumn = null;
        // if the active inplace editor belongs to  a cell blocknode, then store its row and column.
        // the row and column are later on used to reset the legend model's inplace editor
        if (currentInplaceEditor != null) {
            BlockNode currentBlockNode = currentInplaceEditor.getData(InplaceEditor.BLOCKNODE);
            if (currentBlockNode.getTag().equals(CELLNODE)) {
                activeCellRow = currentBlockNode.getData(CELLNODE_ROW_NUMBER);
                activeCellColumn = currentBlockNode.getData(CELLNODE_COL_NUMBER);
            }
        }

        // remove all the cell nodes and rebuild them
        tableNode.removeAllChildren();

        // associate inplace editors with the cellNodes
        Graph graph = null;
        InplaceItemBuilder ipbuilder = Lookup.getDefault().lookup(InplaceItemBuilder.class);
        PreviewProperty[] cellPreviewProperties = null;
        Cell cell = null;
        Row r;
        Column col;
        int itemIndex = item.getData(LegendItem.ITEM_INDEX);

        int tableOriginX = (int) tableNode.getOriginX();
        int tableOriginY = (int) tableNode.getOriginY();

        //precompute the relative positions of the columns from the table legend's origin.
        int[] columnOrigins = new int[colWidths.length];
        columnOrigins[0] = 0;
        int sum = 0;
        for (int i = 0; i < colWidths.length; i++) {
            columnOrigins[i] = sum;
            sum += colWidths[i];
        }

        // create blocknodes for cells and associate inplace editors with them.
        for (int rowNumber = 0; rowNumber < tableNumberOfRows; rowNumber++) {
            for (int colNumber = 0; colNumber < tableNumberOfColumns; colNumber++) {
                int tableCellOriginX = tableOriginX + columnOrigins[colNumber] + (colNumber + 1) * tableCellSpacing + (2 * colNumber + 1) * tableCellPadding + (2 * colNumber + 1) * tableCellBorderSize;
                int tableCellOriginY = tableOriginY + rowNumber * rowHeight + (rowNumber + 1) * tableCellSpacing + (2 * rowNumber + 1) * tableCellPadding + (2 * rowNumber + 1) * tableCellBorderSize;
                int tableCellWidth = colWidths[colNumber];
                int tableCellHeight = rowHeight;

                BlockNode cellNode = tableNode.addChild(tableCellOriginX, tableCellOriginY, tableCellWidth, tableCellHeight, CELLNODE);
                // setting optional data - to identify which cell this is
                // this can be used to reset the inplace editor in the legend model
                cellNode.setData(CELLNODE_ROW_NUMBER, rowNumber);
                cellNode.setData(CELLNODE_COL_NUMBER, colNumber);

                cell = table.get(rowNumber).get(colNumber);
                cellPreviewProperties = cell.getPreviewProperties();

                InplaceEditor ipeditor = ipbuilder.createInplaceEditor(graph, cellNode);
                Map<String, Object> data;
                BaseElement addedElement;
                // modify inplace editors

                // Cell Properties
                r = ipeditor.addRow();
                // see ElementLabel.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementLabel.LABEL_TEXT, "Cell: ");
                data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // see ElementColor.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementColor.COLOR_MARGIN, InplaceItemRenderer.COLOR_MARGIN);
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.COLOR, itemIndex, cellPreviewProperties[Cell.BACKGROUND_COLOR], data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // see ElementColor.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementColor.COLOR_MARGIN, InplaceItemRenderer.COLOR_MARGIN);
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.COLOR, itemIndex, cellPreviewProperties[Cell.BORDER_COLOR], data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                switch ((Integer) cellPreviewProperties[Cell.CELL_TYPE].getValue()) {
                    case Cell.TYPE_TEXT:
                        r = ipeditor.addRow();
                        // see ElementImage.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementText.EDIT_IMAGE, "/org/gephi/legend/graphics/edit.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.TEXT, itemIndex, cellPreviewProperties[Cell.CELL_TEXT_CONTENT], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementColor.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementColor.COLOR_MARGIN, InplaceItemRenderer.COLOR_MARGIN);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.COLOR, itemIndex, cellPreviewProperties[Cell.CELL_FONT_COLOR], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementFont.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementFont.DISPLAY_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        data.put(ElementFont.DISPLAY_FONT_COLOR, InplaceItemRenderer.FONT_DISPLAY_COLOR);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FONT, itemIndex, cellPreviewProperties[Cell.CELL_FONT], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        r = ipeditor.addRow();
                        // see ElementImage.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(true);
                        // left-alignment
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.LEFT);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/left_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/left_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_ALIGNMENT], data, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.LEFT, Alignment.LEFT);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // center-alignment
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.CENTER);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/center_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/center_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_ALIGNMENT], data, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.CENTER, Alignment.CENTER);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // right alignment
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.RIGHT);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/right_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/right_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_ALIGNMENT], data, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.RIGHT, Alignment.RIGHT);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // justified
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.JUSTIFIED);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/justified_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/justified_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_ALIGNMENT], data, cellPreviewProperties[Cell.CELL_ALIGNMENT].getValue() == Alignment.JUSTIFIED, Alignment.JUSTIFIED);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);
                        break;

                    case Cell.TYPE_SHAPE:
                        r = ipeditor.addRow();
                        // see ElementImage.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(true);
                        // rectangle
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.RECTANGLE);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/group_rectangle_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/group_rectangle_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE], data, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.RECTANGLE, LegendItem.Shape.RECTANGLE);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // circle
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.CIRCLE);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/group_circle_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/group_circle_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE], data, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.CIRCLE, LegendItem.Shape.CIRCLE);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // triangle
                        data = new HashMap<String, Object>();
                        data.put(ElementImage.IMAGE_BOOL, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.TRIANGLE);
                        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/group_triangle_selected.png");
                        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/group_triangle_unselected.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE], data, cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].getValue() == LegendItem.Shape.TRIANGLE, LegendItem.Shape.TRIANGLE);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementColor.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementColor.COLOR_MARGIN, InplaceItemRenderer.COLOR_MARGIN);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.COLOR, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_COLOR], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        r = ipeditor.addRow();
                        // see ElementLabel.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementLabel.LABEL_TEXT, "Value:");
                        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementNumber.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
                        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_VALUE], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        r = ipeditor.addRow();
                        // see ElementLabel.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementLabel.LABEL_TEXT, "Shape Width:");
                        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementNumber.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
                        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, cellPreviewProperties[Cell.CELL_SHAPE_WIDTH], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);
                        break;

                    case Cell.TYPE_IMAGE:
                        r = ipeditor.addRow();
                        // see ElementLabel.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementLabel.LABEL_TEXT, "Source: ");
                        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementFile.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementFile.FILE_PATH, "/org/gephi/legend/graphics/file.png");
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FILE, itemIndex, cellPreviewProperties[Cell.CELL_IMAGE_FILE], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        r = ipeditor.addRow();
                        // see ElementLabel.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementLabel.LABEL_TEXT, "Scale: ");
                        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementCheckbox.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementCheckbox.IS_CHECKED, (Boolean) cellPreviewProperties[Cell.CELL_IMAGE_IS_SCALING].getValue());
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.CHECKBOX, itemIndex, cellPreviewProperties[Cell.CELL_IMAGE_IS_SCALING], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        r = ipeditor.addRow();
                        // see ElementLabel.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementLabel.LABEL_TEXT, "Image Dimensions:");
                        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementNumber.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
                        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, cellPreviewProperties[Cell.CELL_IMAGE_WIDTH], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                        // see ElementNumber.java to understand how this element is being structured within the inplace editor
                        col = r.addColumn(false);
                        data = new HashMap<String, Object>();
                        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
                        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, cellPreviewProperties[Cell.CELL_IMAGE_HEIGHT], data, false, null);
                        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);
                        break;
                }

                // switching between cell types
                r = ipeditor.addRow();
                // see ElementLabel.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementLabel.LABEL_TEXT, "Cell Type:");
                data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
                data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                // text type
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_text_unselected.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                        TableItem tableItem = (TableItem) cellNode.getItem();
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        int rowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
                        int colNumber = cellNode.getData(CELLNODE_COL_NUMBER);
                        Cell cell = table.get(rowNumber).get(colNumber);
                        PreviewProperty[] previewProperties = cell.getPreviewProperties();
                        previewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_TEXT);
                        tableItem.setInplaceEditorChanged(true);
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                // shape type
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_shape_unselected.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                        TableItem tableItem = (TableItem) cellNode.getItem();
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        int rowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
                        int colNumber = cellNode.getData(CELLNODE_COL_NUMBER);
                        Cell cell = table.get(rowNumber).get(colNumber);
                        PreviewProperty[] previewProperties = cell.getPreviewProperties();
                        previewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_SHAPE);
                        tableItem.setInplaceEditorChanged(true);
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                // image type
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_image_unselected.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                        TableItem tableItem = (TableItem) cellNode.getItem();
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        int rowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
                        int colNumber = cellNode.getData(CELLNODE_COL_NUMBER);
                        Cell cell = table.get(rowNumber).get(colNumber);
                        PreviewProperty[] previewProperties = cell.getPreviewProperties();
                        previewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_IMAGE);
                        tableItem.setInplaceEditorChanged(true);
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                r = ipeditor.addRow();
                // insert_row button
                // see ElemenFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/insert_row.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        int confirmation = JOptionPane.showConfirmDialog(null, "Do you really want to add a row?", "Confirm Row Addition", JOptionPane.YES_NO_OPTION);
                        if (confirmation == JOptionPane.YES_OPTION) {
                            BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                            TableItem tableItem = (TableItem) cellNode.getItem();
                            int cellRowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
                            tableItem.addRow(cellRowNumber, Cell.defaultBackgroundColor, Cell.defaultBorderColor, Cell.defaultCellFont, Cell.defaultCellAlignment, Cell.defaultCellFontColor, Cell.defaultCellTextContent, Cell.defaultCellShapeShape, Cell.defaultCellShapeColor, Cell.defaultCellShapeValue, Cell.defaultCellImageFile, Cell.defaultCellImageIsScaling, Cell.defaultCellType);
                        }
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // insert_column button
                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/insert_column.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        int confirmation = JOptionPane.showConfirmDialog(null, "Do you really want to add a column?", "Confirm Column Addition", JOptionPane.YES_NO_OPTION);
                        if (confirmation == JOptionPane.YES_OPTION) {
                            BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                            TableItem tableItem = (TableItem) cellNode.getItem();
                            int cellColNumber = cellNode.getData(CELLNODE_COL_NUMBER);
                            tableItem.addColumn(cellColNumber, Cell.defaultBackgroundColor, Cell.defaultBorderColor, Cell.defaultCellFont, Cell.defaultCellAlignment, Cell.defaultCellFontColor, Cell.defaultCellTextContent, Cell.defaultCellShapeShape, Cell.defaultCellShapeColor, Cell.defaultCellShapeValue, Cell.defaultCellImageFile, Cell.defaultCellImageIsScaling, Cell.defaultCellType);
                        }
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // delete_row button
                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/delete_row.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        int confirmation = JOptionPane.showConfirmDialog(null, "Do you really want to delete this row?", "Confirm Row Deletion", JOptionPane.YES_NO_OPTION);
                        if (confirmation == JOptionPane.YES_OPTION) {
                            BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                            TableItem tableItem = (TableItem) cellNode.getItem();
                            int cellRowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
                            tableItem.deleteRow(cellRowNumber);
                        }
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                // delete_column button
                // see ElementFunction.java to understand how this element is being structured within the inplace editor
                col = r.addColumn(false);
                data = new HashMap<String, Object>();
                data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/delete_column.png");
                data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
                    @Override
                    public void performAction(InplaceEditor ipeditor) {
                        int confirmation = JOptionPane.showConfirmDialog(null, "Do you really want to delete this column?", "Confirm Column Deletion", JOptionPane.YES_NO_OPTION);
                        if (confirmation == JOptionPane.YES_OPTION) {
                            BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                            TableItem tableItem = (TableItem) cellNode.getItem();
                            int cellColNumber = cellNode.getData(CELLNODE_COL_NUMBER);
                            tableItem.deleteColumn(cellColNumber);
                        }
                    }
                });
                addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
                addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

                cellNode.setInplaceEditor(ipeditor);

                // reset the legend model's inplace editor
                if (activeCellRow != null && rowNumber == activeCellRow && activeCellColumn != null && colNumber == activeCellColumn) {
                    int originX = currentInplaceEditor.getData(InplaceEditor.ORIGIN_X);
                    int originY = currentInplaceEditor.getData(InplaceEditor.ORIGIN_Y);
                    ipeditor.setData(InplaceEditor.ORIGIN_X, originX);
                    ipeditor.setData(InplaceEditor.ORIGIN_Y, originY);
                    legendModel.setInplaceEditor(ipeditor);
                }
            }
        }
    }

    private void addTablePropertiesToCells(BlockNode tableNode, TableItem item, Graphics2D graphics2d, RenderTarget target) {
        PreviewProperty[] tablePreviewProperties = item.getData(LegendItem.OWN_PROPERTIES);
        int itemIndex = item.getData(LegendItem.ITEM_INDEX);
        ArrayList<BlockNode> cellNodes = tableNode.getChildren();
        for (BlockNode cellNode : cellNodes) {
            InplaceEditor ipeditor = cellNode.getInplaceEditor();
            // build controls for general properties of the table
            buildTableProperties(ipeditor, itemIndex, tablePreviewProperties, graphics2d, target);
        }
    }

    private void buildTableProperties(InplaceEditor ipeditor, int itemIndex, PreviewProperty[] tablePreviewProperties, Graphics2D graphics2d, RenderTarget target) {
        Row r;
        Column col;
        Map<String, Object> data;
        BaseElement addedElement;

        // modify inplace editors
        r = ipeditor.addRow();
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementLabel.LABEL_TEXT, "Table :");
        data.put(ElementLabel.LABEL_COLOR, InplaceItemRenderer.LABEL_COLOR);
        data.put(ElementLabel.LABEL_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.LABEL, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // cell spacing
        r = ipeditor.addRow();
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementImage.IMAGE_BOOL, true);
        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/cell_spacing.png");
        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/cell_spacing.png");
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, tablePreviewProperties[TableProperty.TABLE_CELL_SPACING], data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // cell padding
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementImage.IMAGE_BOOL, true);
        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/cell_padding.png");
        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/cell_padding.png");
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, tablePreviewProperties[TableProperty.TABLE_CELL_PADDING], data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // border size
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementImage.IMAGE_BOOL, true);
        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/cell_border.png");
        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/cell_border.png");
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementNumber.NUMBER_COLOR, InplaceItemRenderer.NUMBER_COLOR);
        data.put(ElementNumber.NUMBER_FONT, InplaceItemRenderer.INPLACE_DEFAULT_DISPLAY_FONT);
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.NUMBER, itemIndex, tablePreviewProperties[TableProperty.TABLE_BORDER_SIZE], data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        r = ipeditor.addRow();
        //background color
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementColor.COLOR_MARGIN, InplaceItemRenderer.COLOR_MARGIN);
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.COLOR, itemIndex, tablePreviewProperties[TableProperty.TABLE_BACKGROUND_COLOR], data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // border color
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/table_cell_border_color.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the border color of all cells?", "Confirm Border Color Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    PreviewProperty[] tablePreviewProperties = tableItem.getData(TableItem.OWN_PROPERTIES);
                    Color tableCellBorderColor = tablePreviewProperties[TableProperty.TABLE_BORDER_COLOR].getValue();
                    Color selectedColor = ColorPicker.showDialog(null, tableCellBorderColor, true);
                    if (selectedColor != null) {
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        tablePreviewProperties[TableProperty.TABLE_BORDER_COLOR].setValue(selectedColor);
                        PreviewProperty[] cellPreviewProperties = null;
                        int numberOfRows = tableItem.getNumberOfRows();
                        int numberOfCols = tableItem.getNumberOfColumns();
                        for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                            for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                                Cell cell = table.get(rowNumber).get(colNumber);
                                cellPreviewProperties = cell.getPreviewProperties();
                                cellPreviewProperties[Cell.BORDER_COLOR].setValue(selectedColor);
                            }
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // background color
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/table_cell_background_color.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the background color of all cells?", "Confirm Background Color Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    PreviewProperty[] tablePreviewProperties = tableItem.getData(TableItem.OWN_PROPERTIES);
                    Color selectedColor = ColorPicker.showDialog(null, new Color(1f, 1f, 1f, 0.8f), true);
                    if (selectedColor != null) {
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        PreviewProperty[] cellPreviewProperties = null;
                        int numberOfRows = tableItem.getNumberOfRows();
                        int numberOfCols = tableItem.getNumberOfColumns();
                        for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                            for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                                Cell cell = table.get(rowNumber).get(colNumber);
                                cellPreviewProperties = cell.getPreviewProperties();
                                cellPreviewProperties[Cell.BACKGROUND_COLOR].setValue(selectedColor);
                            }
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // table font
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/table_font.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the font for all cells?", "Confirm Font Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    PreviewProperty[] tablePreviewProperties = tableItem.getData(TableItem.OWN_PROPERTIES);
                    Font tableCellFont = tablePreviewProperties[TableProperty.TABLE_FONT].getValue();
                    JFontChooser chooser = new JFontChooser(tableCellFont);
                    Font chosenFont = chooser.showDialog(new JFrame("choose a font"), tableCellFont);
                    if (chosenFont != null) {
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        tablePreviewProperties[TableProperty.TABLE_FONT].setValue(chosenFont);
                        PreviewProperty[] cellPreviewProperties = null;
                        int numberOfRows = tableItem.getNumberOfRows();
                        int numberOfCols = tableItem.getNumberOfColumns();
                        for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                            for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                                Cell cell = table.get(rowNumber).get(colNumber);
                                cellPreviewProperties = cell.getPreviewProperties();
                                cellPreviewProperties[Cell.CELL_FONT].setValue(chosenFont);
                            }
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // table font color
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/table_font_color.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the font color for all cells?", "Confirm Font Color Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    PreviewProperty[] tablePreviewProperties = tableItem.getData(TableItem.OWN_PROPERTIES);
                    Color tableCellFontColor = tablePreviewProperties[TableProperty.TABLE_FONT_COLOR].getValue();
                    Color selectedColor = ColorPicker.showDialog(null, tableCellFontColor, true);
                    if (selectedColor != null) {
                        ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                        tablePreviewProperties[TableProperty.TABLE_FONT_COLOR].setValue(selectedColor);
                        PreviewProperty[] cellPreviewProperties = null;
                        int numberOfRows = tableItem.getNumberOfRows();
                        int numberOfCols = tableItem.getNumberOfColumns();
                        for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                            for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                                Cell cell = table.get(rowNumber).get(colNumber);
                                cellPreviewProperties = cell.getPreviewProperties();
                                cellPreviewProperties[Cell.CELL_FONT_COLOR].setValue(selectedColor);
                            }
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // occupy full width
        col = r.addColumn(false);
        data = new HashMap<String, Object>();
        data.put(ElementImage.IMAGE_BOOL, (Boolean) tablePreviewProperties[TableProperty.TABLE_WIDTH_FULL].getValue());
        data.put(ElementImage.IMAGE_IF_TRUE, "/org/gephi/legend/graphics/column_collapse.png");
        data.put(ElementImage.IMAGE_IF_FALSE, "/org/gephi/legend/graphics/column_expand.png");
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.IMAGE, itemIndex, tablePreviewProperties[TableProperty.TABLE_WIDTH_FULL], data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        // making shape changes to all cells
        r = ipeditor.addRow();
        col = r.addColumn(false);
        // text type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_text_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the cell type to text for all cells?", "Confirm Cell Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_TEXT);
                        }
                    }
                    tableItem.setInplaceEditorChanged(true);
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        // shape type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_shape_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the cell type to shape for all cells?", "Confirm Cell Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_SHAPE);
                        }
                    }
                    tableItem.setInplaceEditorChanged(true);
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        // image type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/type_image_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the cell type to image for all cells?", "Confirm Cell Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_TYPE].setValue(Cell.TYPE_IMAGE);
                        }
                    }
                    tableItem.setInplaceEditorChanged(true);
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        // shape - rectangle type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/group_rectangle_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the shape type to rectangle for all cells?", "Confirm Shape Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].setValue(Shape.RECTANGLE);
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        // shape - circle type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/group_circle_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the shape type to circle for all cells?", "Confirm Shape Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].setValue(Shape.CIRCLE);
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);

        col = r.addColumn(false);
        // shape - rectangle type
        data = new HashMap<String, Object>();
        data.put(ElementFunction.FUNCTION_IMAGE, "/org/gephi/legend/graphics/group_triangle_unselected.png");
        data.put(ElementFunction.FUNCTION_CLICK_RESPONDER, new InplaceClickResponse() {
            @Override
            public void performAction(InplaceEditor ipeditor) {
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to change the shape type to triangle for all cells?", "Confirm Shape Type Change", JOptionPane.YES_NO_OPTION);
                if (confirmation == JOptionPane.YES_NO_OPTION) {
                    BlockNode cellNode = ipeditor.getData(InplaceEditor.BLOCKNODE);
                    TableItem tableItem = (TableItem) cellNode.getItem();
                    ArrayList<ArrayList<Cell>> table = tableItem.getTable();
                    PreviewProperty[] cellPreviewProperties = null;
                    int numberOfRows = tableItem.getNumberOfRows();
                    int numberOfCols = tableItem.getNumberOfColumns();
                    for (int rowNumber = 0; rowNumber < numberOfRows; rowNumber++) {
                        for (int colNumber = 0; colNumber < numberOfCols; colNumber++) {
                            Cell cell = table.get(rowNumber).get(colNumber);
                            cellPreviewProperties = cell.getPreviewProperties();
                            cellPreviewProperties[Cell.CELL_SHAPE_SHAPE].setValue(Shape.TRIANGLE);
                        }
                    }
                }
            }
        });
        addedElement = col.addElement(BaseElement.ELEMENT_TYPE.FUNCTION, itemIndex, null, data, false, null);
        addedElement.computeNumberOfBlocks(graphics2d, (G2DTarget) target, InplaceItemRenderer.DEFAULT_INPLACE_BLOCK_UNIT_SIZE);
    }

    private void updateCellGeometry(BlockNode tableNode, int rowHeight, Integer[] colWidths) {
        ArrayList<BlockNode> cellNodes = tableNode.getChildren();
        int tableOriginX = (int) tableNode.getOriginX();
        int tableOriginY = (int) tableNode.getOriginY();
        //precompute the relative positions of the columns from the table legend's origin.
        int[] columnOrigins = new int[colWidths.length];
        columnOrigins[0] = 0;
        int sum = 0;
        for (int i = 0; i < colWidths.length; i++) {
            columnOrigins[i] = sum;
            sum += colWidths[i];
        }
        for (BlockNode cellNode : cellNodes) {
            int rowNumber = cellNode.getData(CELLNODE_ROW_NUMBER);
            int colNumber = cellNode.getData(CELLNODE_COL_NUMBER);

            int tableCellOriginX = tableOriginX + columnOrigins[colNumber] + (colNumber + 1) * tableCellSpacing + (2 * colNumber + 1) * tableCellPadding + (2 * colNumber + 1) * tableCellBorderSize;
            int tableCellOriginY = tableOriginY + rowNumber * rowHeight + (rowNumber + 1) * tableCellSpacing + (2 * rowNumber + 1) * tableCellPadding + (2 * rowNumber + 1) * tableCellBorderSize;
            int tableCellWidth = colWidths[colNumber];
            int tableCellHeight = rowHeight;

            cellNode.updateGeometry(tableCellOriginX, tableCellOriginY, tableCellWidth, tableCellHeight);
        }
    }

    private void renderCells(Graphics2D graphics2D, BlockNode tableNode, TableItem item) {
        Font saveFont = graphics2D.getFont();
        Color saveColor = graphics2D.getColor();

        ArrayList<BlockNode> cellNodes = tableNode.getChildren();
        PreviewProperty[] previewProperties = null;

        BlockNode cellNode;
        Cell cell;
        Color cellBackgroundColor;
        Color cellBorderColor;
        Font cellFont;
        Alignment cellAlignment;;
        Color cellFontColor;
        String cellTextContent;
        Shape cellShapeShape;
        Color cellShapeColor;
        Integer cellShapeWidth;
        Float cellShapeNormalizedValue;
        File cellImageFile;
        Boolean cellImageIsScaling;
        Integer cellImageWidth;
        Integer cellImageHeight;
        Integer cellType;

        int cellOriginX;
        int cellOriginY;
        int cellWidth;
        int cellHeight;

        // compute all the normalized values for rendering shapes
        Float[] normalizedValues = new Float[tableNumberOfRows * tableNumberOfColumns];
        // utility variables
        Cell tempCell;
        PreviewProperty[] tempPreviewProperties;
        Integer tempCellType;
        for (int rowNumber = 0; rowNumber < tableNumberOfRows; rowNumber++) {
            for (int colNumber = 0; colNumber < tableNumberOfColumns; colNumber++) {
                tempCell = table.get(rowNumber).get(colNumber); // the properties of the cell can be found here
                tempPreviewProperties = tempCell.getPreviewProperties();
                tempCellType = (Integer) tempPreviewProperties[Cell.CELL_TYPE].getValue();
                // compute the maximum value only from those cells which have their shapes displaying
                if (tempCellType == Cell.TYPE_SHAPE) {
                    normalizedValues[rowNumber * tableNumberOfColumns + colNumber] = (Float) tempCell.getPreviewProperties()[Cell.CELL_SHAPE_VALUE].getValue();
                } else {
                    normalizedValues[rowNumber * tableNumberOfColumns + colNumber] = 0f;
                }
            }
        }
        // find the maximum value of all the shape typed cells
        Float maxValue = Float.MIN_VALUE;
        for (int i = 0; i < normalizedValues.length; i++) {
            if (maxValue < normalizedValues[i]) {
                maxValue = normalizedValues[i];
            }
        }

        // normalize the values: divide each element by the maximum
        for (int i = 0; i < normalizedValues.length; i++) {
            normalizedValues[i] /= maxValue;
        }

        for (int rowNumber = 0; rowNumber < tableNumberOfRows; rowNumber++) {
            for (int colNumber = 0; colNumber < tableNumberOfColumns; colNumber++) {
                cellNode = cellNodes.get(rowNumber * tableNumberOfColumns + colNumber); // the geometric information related to block node can be found here
                cell = table.get(rowNumber).get(colNumber); // the properties of the cell can be found here
                previewProperties = cell.getPreviewProperties();

                // cell geometry
                cellOriginX = (int) (cellNode.getOriginX() - currentRealOriginX);
                cellOriginY = (int) (cellNode.getOriginY() - currentRealOriginY);
                cellWidth = (int) cellNode.getBlockWidth();
                cellHeight = (int) cellNode.getBlockHeight();

                // cell properties
                cellBackgroundColor = (Color) previewProperties[Cell.BACKGROUND_COLOR].getValue();
                cellBorderColor = (Color) previewProperties[Cell.BORDER_COLOR].getValue();
                cellFont = (Font) previewProperties[Cell.CELL_FONT].getValue();
                cellAlignment = (Alignment) previewProperties[Cell.CELL_ALIGNMENT].getValue();
                cellFontColor = (Color) previewProperties[Cell.CELL_FONT_COLOR].getValue();
                cellTextContent = (String) previewProperties[Cell.CELL_TEXT_CONTENT].getValue();
                cellShapeShape = (Shape) previewProperties[Cell.CELL_SHAPE_SHAPE].getValue();
                cellShapeColor = (Color) previewProperties[Cell.CELL_SHAPE_COLOR].getValue();
                cellShapeWidth = (Integer) previewProperties[Cell.CELL_SHAPE_WIDTH].getValue();
                cellShapeNormalizedValue = normalizedValues[rowNumber * tableNumberOfColumns + colNumber];
                cellImageFile = (File) previewProperties[Cell.CELL_IMAGE_FILE].getValue();
                cellImageIsScaling = (Boolean) previewProperties[Cell.CELL_IMAGE_IS_SCALING].getValue();
                cellImageWidth = (Integer) previewProperties[Cell.CELL_IMAGE_WIDTH].getValue();
                cellImageHeight = (Integer) previewProperties[Cell.CELL_IMAGE_HEIGHT].getValue();
                cellType = (Integer) previewProperties[Cell.CELL_TYPE].getValue();

                // BACKGROUND - render the background first, then go with the border
                graphics2D.setColor(cellBackgroundColor);
                graphics2D.fillRect(cellOriginX, cellOriginY, cellWidth, cellHeight);

                // BORDER - external border
                graphics2D.setColor(cellBorderColor);
                graphics2D.fillRect(cellOriginX - tableCellBorderSize, cellOriginY - tableCellBorderSize, cellWidth + 2 * tableCellBorderSize, tableCellBorderSize);  // top
                graphics2D.fillRect(cellOriginX - tableCellBorderSize, cellOriginY + cellHeight, cellWidth + 2 * tableCellBorderSize, tableCellBorderSize);   // bottom
                graphics2D.fillRect(cellOriginX - tableCellBorderSize, cellOriginY, tableCellBorderSize, cellHeight);
                graphics2D.fillRect(cellOriginX + cellWidth, cellOriginY, tableCellBorderSize, cellHeight); // left

                switch (cellType) {
                    case Cell.TYPE_SHAPE:
                        graphics2D.setColor(cellShapeColor);
                        drawShape(graphics2D, cellShapeShape, cellShapeColor, (int) (cellOriginX + cellWidth / 2 - cellShapeWidth / 2), (int) (cellOriginY + (1 - cellShapeNormalizedValue) * cellHeight), cellShapeWidth, (int) (cellShapeNormalizedValue * cellHeight));
                        break;

                    case Cell.TYPE_TEXT:
                        legendDrawText(graphics2D, cellTextContent, cellFont, cellFontColor, cellOriginX, cellOriginY, cellWidth, cellHeight, cellAlignment);
                        break;

                    case Cell.TYPE_IMAGE:
                        // draw image based on the scaling condition
                        try {
                            if (cellImageFile.exists() && cellImageFile.isFile()) {
                                BufferedImage image;
                                image = ImageIO.read(cellImageFile);
                                int scaledOriginX;
                                int scaledOriginY;
                                int scaledWidth = cellImageWidth;
                                int scaledHeight = cellImageHeight;

                                int imageWidth = image.getWidth();
                                int imageHeight = image.getHeight();

                                if (cellImageIsScaling) {
                                    // if aspect ratio needs to be maintained, originX, originY, width and height must be set appropriately
                                    scaledHeight = cellWidth * imageHeight / imageWidth;
                                    if (scaledHeight <= cellHeight) {
                                        // width fits completely. height is either perfect or shorter.
                                        scaledWidth = cellWidth;
                                        scaledHeight = cellWidth * imageHeight / imageWidth; //not necessary, since the calue isnt changed. added for readability and consistency.
                                    } else {
                                        // height fits completely. width is short perfect or shorter.
                                        scaledWidth = cellHeight * imageWidth / imageHeight;
                                        scaledHeight = cellHeight;
                                    }
                                }

                                scaledOriginX = cellOriginX + cellWidth / 2 - scaledWidth / 2;
                                scaledOriginY = cellOriginY + cellHeight / 2 - scaledHeight / 2;

                                graphics2D.drawImage(image,
                                        scaledOriginX,
                                        scaledOriginY,
                                        scaledOriginX + scaledWidth,
                                        scaledOriginY + scaledHeight,
                                        0,
                                        0,
                                        imageWidth,
                                        imageHeight, null);
                            }
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        break;
                }
            }
        }

        graphics2D.setColor(saveColor);
        graphics2D.setFont(saveFont);
    }

    private void drawShape(Graphics2D graphics2D, Shape shape, Color color, int x, int y, Integer width, Integer height) {
        graphics2D.setColor(color);
        switch (shape) {
            case RECTANGLE: {
                graphics2D.fillRect(x, y, width, height);
                break;
            }
            case CIRCLE: {
                graphics2D.fillOval(x, y, width, height);
                break;
            }
            case TRIANGLE: {
                int[] xpoints = {x, x + width, x + width / 2};
                int[] ypoints = {y + height, y + height, y};
                Polygon triangle = new Polygon(xpoints, ypoints, xpoints.length);
                graphics2D.fillPolygon(triangle);
            }
        }
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return itemBuilder instanceof TableItemBuilder;
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(TableItemRenderer.class, "TableItemRenderer.name");
    }
}