/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.legend.manager;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.gephi.project.api.Workspace;
import org.gephi.project.spi.WorkspacePersistenceProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author edubecks
 */
@ServiceProvider(service = WorkspacePersistenceProvider.class)
public class LegendPersistenceProvider implements WorkspacePersistenceProvider{

    @Override
    public void writeXML(XMLStreamWriter writer, Workspace workspace) {
        System.out.printf("__HERE __ trying to write from LegendPersistenceProvider UI \n");
        LegendController.getInstance().writeXML(writer, workspace);
    }

    @Override
    public void readXML(XMLStreamReader reader, Workspace workspace) {
        System.out.printf("__HERE __ trying to read from LegendPersistenceProvider UI \n");
        LegendController.getInstance().readXMLToLegendManager(reader, workspace);
    }

    @Override
    public String getIdentifier() {
        return LEGEND;
    }
    
    private final static String LEGEND = "legend";
}
