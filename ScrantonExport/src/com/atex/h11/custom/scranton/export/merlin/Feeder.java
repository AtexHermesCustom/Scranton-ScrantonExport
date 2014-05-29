package com.atex.h11.custom.scranton.export.merlin;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.atex.h11.custom.common.HermesObject;
import com.atex.h11.custom.common.StoryPackage;
import com.atex.h11.custom.scranton.export.common.CommonFeeder;
import com.atex.h11.custom.scranton.export.common.Main;
import com.atex.h11.custom.scranton.export.common.QueueItem;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.values.NCMObjectBuildProperties;

public class Feeder extends CommonFeeder {

    private static final String loggerName = Feeder.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);   	
       
	@Override
    protected void SplitThenEnqueue(Node node)
			throws InterruptedException, TimeoutException {    	
		logger.fine("Split and enqueue work items.");
		long startMillis = System.currentTimeMillis();
		
		if (node instanceof Document)
		    node = ((Document) node).getDocumentElement();    		    		
		NodeList nl = node.getChildNodes();
		logger.info("Found work items. Count=" + nl.getLength() + ".");
		
		for (int i = 0; i < nl.getLength(); i++) {		    
		    try {
			    Node n = nl.item(i);		    	
			    // retrieve object
			    String objId = (String) xp.evaluate("id", n, XPathConstants.STRING);
			    String objName = (String) xp.evaluate("name", n, XPathConstants.STRING);
			    Node pageInfoNode = (Node) xp.evaluate("page-info", n, XPathConstants.NODE);
				
			    Document doc = null;
			    
			    if (n.getNodeName().equals("item-package")) {
				    /* 
				     * Get package object
				     * this is needed so that all package elements,
				     * including non-paginated ones are included in the export
				     */
				    logger.info("Extracting package: name=" + objName + ", id=" + objId);
				    
					StoryPackage sp = new StoryPackage(Main.getDatasource());
					sp.setConvertFormat(convertFormat);		// required
				    NCMObjectPK pk = new NCMObjectPK(Integer.parseInt(objId));
				    doc = sp.getDocument(pk);			    	
				    
				    logger.info("Extracted package successfully: name=" + objName + ", id=" + objId);
			    }
			    else if (n.getNodeName().equals("item-standalone-photo")) {
				    /* 
				     * Get stand-alone image object
				     * this is needed since the master variant is required for the export
				     */
				    logger.info("Extracting stand-alone image: name=" + objName + ", id=" + objId);
				    
	    			NCMObjectBuildProperties buildProps = new NCMObjectBuildProperties();
	                buildProps.setIncludeObjContent(true);
	                buildProps.setIncludeLay(true);
	                buildProps.setIncludeLayContent(true);
	                buildProps.setIncludeLayObjContent(true);
	                buildProps.setIncludeAttachments(true);
	                buildProps.setIncludeCaption(true);
	                buildProps.setIncludeVariants(true);
	                buildProps.setIncludeMetadataChild(true);
	                buildProps.setIncludeMetadataGroups(new Vector<String>());    		
	                
				    HermesObject obj = new HermesObject(Main.getDatasource());
				    obj.setBuildProperties(buildProps);
					obj.setConvertFormat(convertFormat);		// required
				    NCMObjectPK pk = new NCMObjectPK(Integer.parseInt(objId));
				    doc = obj.getDocument(pk);
				    
				    logger.info("Extracted stand-alone image successfully: name=" + objName + ", id=" + objId);
			    }
            	else {
            		throw new RuntimeException("Unexpected document element found: " + n.getNodeName());
            	}
			    
			    // include page info retrieved from the previous XSLT step
			    doc.getDocumentElement().appendChild(doc.importNode(pageInfoNode, true));	
				
			    if (debug)
			    	dump(new DOMSource(doc), new File(debugDir, debugFileBaseName + "_" + objName + "_" + objId + ".xml"));

			    // add to output queue
			    enqueue(outQ, new QueueItem(doc));
		    }
		    catch (Exception e) {
		    	logger.log(Level.SEVERE, "Error encountered", e);
		    }
		}    
		
		long endMillis = System.currentTimeMillis();
		logger.fine("Done with split and enqueue. Duration=" + (endMillis - startMillis) + "ms.");
	}	
	
}
