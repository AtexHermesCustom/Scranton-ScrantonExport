package com.atex.h11.custom.scranton.export.merlin;

import com.atex.h11.custom.scranton.export.common.CommonWorker;
import com.atex.h11.custom.scranton.export.common.Main;
import com.atex.h11.custom.scranton.export.common.QueueItem;
import com.atex.h11.custom.scranton.export.common.XSLTMessageReceiver;
import com.unisys.media.ncm.cfg.common.data.values.DepartmentValue;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.AbstractQueue;
import java.util.Properties;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Worker extends CommonWorker {

    private static final String loggerName = Worker.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);      	
	
    private Templates cachedPackageXSLT = null;
    private Templates cachedStoryXSLT = null;
    private Templates cachedPhotoXSLT = null;    
    private URL destStoryURL = null;
    private URL destPhotoURL = null;
       
    private static HashMap<Integer, String> departmentsMap = new HashMap<Integer, String>();
    
    @Override
    public void init(AbstractQueue<QueueItem> inQ, Properties props)
    		throws RuntimeException {
    	logger.entering(getClass().getName(), "init");

    	try {
    		this.inQ = inQ;
    		this.props = props;    		
    		
    		debug = props.getProperty("debug", "false").equalsIgnoreCase("true");
    		debugDir = props.getProperty("debugDir");
    		
    		// Prepare an XPath
    		xpf = XPathFactory.newInstance();
    		xp = xpf.newXPath();
    		
	        // Prepare a transfomer.
	        tf = TransformerFactory.newInstance();    	
	        
	        // Prepare a document builder.
	        dbf = DocumentBuilderFactory.newInstance();
	        dbf.setNamespaceAware(true);
	        db = dbf.newDocumentBuilder(); 	        
    		
	        File packageXSL = loadStylesheetFile(props, "transformPackageStylesheet", true);
	        cachedPackageXSLT = tf.newTemplates(new StreamSource(packageXSL)); 	        
	        
	        File storyXSL = loadStylesheetFile(props, "transformStoryStylesheet", true);
	        cachedStoryXSLT = tf.newTemplates(new StreamSource(storyXSL)); 
    		destStoryURL = loadURL(props, "destinationStoryURL");
    		
	        File photoXSL = loadStylesheetFile(props, "transformPhotoStylesheet", true);
	        cachedPhotoXSLT = tf.newTemplates(new StreamSource(photoXSL));    		
    		destPhotoURL = loadURL(props, "destinationPhotoURL");
    		
    		getDepartments();	// departments
    		
    		setEncoding(props.getProperty("encoding", ""));
            
		} catch (Exception e) {
			throw new RuntimeException(e);
	    }    	
		
		logger.exiting(getClass().getName(), "init");
    }
    
    @Override
    public void run()
    		throws RuntimeException {
    	logger.entering(getClass().getName(), "run");

    	try {
    		QueueItem item = dequeue(inQ);
    		Document doc = item.getDocument();
    		
    		if (isReady(doc)) {
    			// Transform package
    			DOMResult result = new DOMResult();
    			
    			Transformer t = cachedPackageXSLT.newTransformer();
    			// to receive messages from XSLT
    			Controller controller = (Controller) t;
    			Receiver receiver = new XSLTMessageReceiver(logger);
    			controller.setMessageEmitter(receiver);
    			// parameters read from properties file		
    			for (String prop : props.stringPropertyNames()) {
    			    if (prop.startsWith("transform.package.param."))
    			        t.setParameter(prop.replaceFirst("transform.package.param.", ""), props.getProperty(prop));
    			}
    			t.setOutputProperty(OutputKeys.METHOD, "xml");
    			t.setOutputProperty(OutputKeys.INDENT, "no");
    			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    			t.setOutputProperty(OutputKeys.ENCODING, getEncoding());
    			t.transform(new DOMSource(doc), result);
    				
    			if (debug) {
	    			String pubDate = (String) xp.evaluate("page-info/pubdate", doc.getDocumentElement(), XPathConstants.STRING);
	    			String pub = (String) xp.evaluate("page-info/pub", doc.getDocumentElement(), XPathConstants.STRING);
				    String pkgId = (String) xp.evaluate("obj_id", doc.getDocumentElement(), XPathConstants.STRING);
				    String pkgName = (String) xp.evaluate("name", doc.getDocumentElement(), XPathConstants.STRING);
				    dump(new DOMSource(result.getNode()), 
				    	new File(debugDir, pubDate + "_" + pub + "_" + pkgName + "_" + pkgId + "_transformed.xml"));    				
    			}
    			
    			// Process Items
    			processItems(result.getNode());
    		}
    		
		} catch (Exception e) {
			// throw new RuntimeException(e);
			logger.log(Level.SEVERE, "Error encountered", e);
	    }    	
		
		logger.exiting(getClass().getName(), "run");
    }
    
    private void getDepartments() 
    		throws FileNotFoundException, IOException {
		UserHermesCfgValueClient cfg = Main.getDatasource().getUserHermesCfg();
		DepartmentValue[] departments = cfg.enumDepartments();
		for (int i = 0; i < departments.length; i++) {
			DepartmentValue dept = departments[i];
			departmentsMap.put(dept.getDepartmentId(), dept.getName());
		} 
    }

	public static String getDepartmentName(int deptId) {
		if (departmentsMap.containsKey(deptId))
			return departmentsMap.get(deptId);
		else
			return "";
	}    
    
    private boolean isReady(Document doc) {
    	return true;	// no checks for now
    }    

    private void processItems(Node mainNode) {
    	logger.entering(getClass().getName(), "processItems");
    	
		if (mainNode instanceof Document)
			mainNode = ((Document) mainNode).getDocumentElement();    	    	
        NodeList nl = mainNode.getChildNodes();
        logger.info("Found work items. Count=" + nl.getLength() + ".");

        for (int i = 0; i < nl.getLength(); i++) {
        	try {
                Node n = nl.item(i);
                Document doc = db.newDocument();
                doc.appendChild(doc.importNode(n, true));
                
            	Node docElem = doc.getDocumentElement();
            	
            	// different handling for story and images
            	if (docElem.getNodeName().equals("item-story"))    		
            		processStoryDoc(doc);
            	else if (docElem.getNodeName().equals("item-photo"))
            		processPhotoDoc(doc);
            	else
            		throw new RuntimeException("Unexpected document element found: " + docElem.getNodeName());
		    }
		    catch (Exception e) {
		    	logger.log(Level.SEVERE, "Error encountered", e);
		    }        	
        }        	
    	
    	logger.exiting(getClass().getName(), "processItems");
    }    
    
    private void processStoryDoc(Document doc)
			throws XPathExpressionException, UnsupportedEncodingException, IOException,
			TransformerConfigurationException, TransformerException {
    	logger.entering(getClass().getName(), "processStoryDoc");
    	
    	String fileName = getProcessingInstructionData("processing-instruction('file-name')",
    			doc.getDocumentElement(), true);    	
    	logger.info("Exporting story: " + fileName);
    	
    	// transform to final 
		DOMSource source = new DOMSource(doc);
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		
		Transformer t = cachedStoryXSLT.newTransformer();
		// to receive messages from XSLT
		Controller controller = (Controller) t;
		Receiver receiver = new XSLTMessageReceiver(logger);
		controller.setMessageEmitter(receiver);
		// parameters read from properties file		
		for (String prop : props.stringPropertyNames()) {
		    if (prop.startsWith("transform.story.param."))
		        t.setParameter(prop.replaceFirst("transform.story.param.", ""), props.getProperty(prop));
		}
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.setOutputProperty(OutputKeys.ENCODING, getEncoding());		
		t.transform(source, result);
		
		// xml output (but no doc element)
		String output = sw.getBuffer().toString();
		// remove doc element
    	output = output.replaceFirst("^<item-story>", "");
    	output = output.replaceFirst("</item-story>$", "");		
    	Charset charset = Charset.forName(getEncoding());	// output to correct encoding
		write(destStoryURL, fileName, new ByteArrayInputStream(output.getBytes(charset)));
		
		logger.exiting(getClass().getName(), "processStoryDoc");
    }

    private void processPhotoDoc(Document doc)
    		throws XPathExpressionException, UnsupportedEncodingException, IOException,
			TransformerConfigurationException, TransformerException, ParseException {
    	logger.entering(getClass().getName(), "processPhotoDoc");
    	
    	String fileName = getProcessingInstructionData("processing-instruction('file-name')",
    			doc.getDocumentElement(), true);
    	logger.info("Exporting photo info: " + fileName);

    	// transform to final 
		DOMSource source = new DOMSource(doc);
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		
		Transformer t = cachedPhotoXSLT.newTransformer();
		// to receive messages from XSLT
		Controller controller = (Controller) t;
		Receiver receiver = new XSLTMessageReceiver(logger);
		controller.setMessageEmitter(receiver);
		// parameters read from properties file
		for (String prop : props.stringPropertyNames()) {
		    if (prop.startsWith("transform.photo.param."))
		        t.setParameter(prop.replaceFirst("transform.photo.param.", ""), props.getProperty(prop));
		}
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, getEncoding());
        t.transform(source, result);
		
		// output
		String output = sw.getBuffer().toString();
		// remove doc element
    	output = output.replaceFirst("^<item-photo>", "");
    	output = output.replaceFirst("</item-photo>$", "");				
		Charset charset = Charset.forName(getEncoding());	// output to correct encoding
		write(destPhotoURL, fileName, new ByteArrayInputStream(output.getBytes(charset)));

    	// export photo
    	exportPhoto(doc.getDocumentElement(), destPhotoURL, 
    		props.getProperty("cropPhoto").equalsIgnoreCase("true"));
		
		logger.exiting(getClass().getName(), "processPhotoDoc");
	}  
    
}
