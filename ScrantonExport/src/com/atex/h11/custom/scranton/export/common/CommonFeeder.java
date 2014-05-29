package com.atex.h11.custom.scranton.export.common;

import com.atex.h11.custom.common.Newspaper;
import com.atex.h11.custom.common.Edition;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source; 
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CommonFeeder extends Commons implements Runnable {

    private static final String loggerName = CommonFeeder.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);      
  
    protected Properties props = null;
    protected AbstractQueue<QueueItem> outQ = null;
    
    protected File inputFile = null;
    protected Integer pubDate = null;
    protected String pub = null;
    protected String edition = null;
    protected String pageRange = null;

    protected Boolean debug = null;
    protected String debugDir = null;
    protected String debugFileBaseName = null;
    
    protected String convertFormat = null;
    
    protected File pageRangeFilterXSL = null;
    protected File filterXSL = null;
    protected File transformXSL = null;    
    
    protected DocumentBuilderFactory dbf = null;
    protected DocumentBuilder db = null;
    protected XPathFactory xpf = null;
    protected XPath xp = null;    
    protected TransformerFactory tf = null;

    public void init(AbstractQueue<QueueItem> outQ, Properties props, String inputFilePath) 
    		throws RuntimeException {
    	try {
        	this.outQ = outQ;
        	this.props = props; 
        	logger.config("Using input file=" + inputFilePath + ".");
        	inputFile = new File(inputFilePath);
            if (!inputFile.exists())
                throw new RuntimeException(inputFile.getCanonicalPath() + " does not exist.");
            else if (!inputFile.isFile())
                throw new RuntimeException(inputFile.getCanonicalPath() + " is not a file.");
            else if (!inputFile.canRead())
                throw new RuntimeException(inputFile.getCanonicalPath() + " is not readable.");
        	init();		// common init
    		
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
        
    public void init(AbstractQueue<QueueItem> outQ, Properties props,  
    		String credentials, Integer pubDate, String pub, String edition, String pageRange) 
    		throws RuntimeException {
    	try {
        	this.outQ = outQ;
        	this.props = props; 
        	this.pubDate = pubDate;
        	this.pub = pub;
        	this.edition = edition;
        	this.pageRange = pageRange;
        	init();		// common init    		
    		
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
		    	
	protected void init() 
				throws RuntimeException {
		logger.entering(getClass().getName(), "init");
		
		try {
	    	debug = props.getProperty("debug", "false").equalsIgnoreCase("true");
	    	if (debug) {
	    		debugDir = props.getProperty("debugDir");
	    		File dir = new File(debugDir);
	            if (!dir.exists()) {
	                throw new RuntimeException(debugDir + " does not exist.");
	            } else if (!dir.isDirectory()) {
	                throw new RuntimeException(debugDir + " is not a directory.");
	            }
	            if (inputFile != null)
	            	debugFileBaseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
	            else
		    		debugFileBaseName = Integer.toString(pubDate) + "_" + pub 
		    			+ (edition != null ? "_" + edition : "")
		    			+ (pageRange != null ? "_" + pageRange.replace(":", "-") : "");
	    	}
	    	
	    	// converter
	    	convertFormat = props.getProperty("convertFormat", "Neutral");
	    	
	        // page range filter stylesheet - required
	        pageRangeFilterXSL = loadStylesheetFile(props, "pageRangeFilterStylesheet", true);
	    	
	        // filter stylesheet - optional
	        filterXSL = loadStylesheetFile(props, "filterStylesheet", false);
	        
	        // transform stylesheet - optional
	        transformXSL = loadStylesheetFile(props, "transformStylesheet", false);
	        
	        // Prepare a document builder.
	        dbf = DocumentBuilderFactory.newInstance();
	        dbf.setNamespaceAware(true);
	        db = dbf.newDocumentBuilder();
	
    		// Prepare an XPath
    		xpf = XPathFactory.newInstance();
    		xp = xpf.newXPath();    
    		
	        // Prepare a transfomer.
	        tf = TransformerFactory.newInstance();
	
	        logger.fine("Using DocumentBuilderFactory class=" + dbf.getClass().getCanonicalName() + ".");
	        logger.fine("Using TransformerFactory class=" + tf.getClass().getCanonicalName() + ".");
    	
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
	        DOMSource source = null;
	        
	        if (inputFile != null) {
	        	// load from input file
	        	source = new DOMSource(db.parse(inputFile));
	        	logger.info("Loaded input file " + inputFile.getCanonicalPath() + ".");
	        }
	        else {
	        	// extract from Hermes
	        	NCMDataSource ds = Main.getDatasource();	// get connection
		        
		        logger.info("Extracting data from Hermes.");
		        long startMillis = System.currentTimeMillis();
		        if (edition != null) {	// Edition export
		        	Edition ed = new Edition(ds);
		        	ed.setConvertFormat(convertFormat);
		        	source = new DOMSource(ed.getDocument(pub, edition, pubDate));
		        }
		        else {					// Newspaper export
		        	Newspaper np = new Newspaper(ds);
		        	np.setConvertFormat(convertFormat);
		        	source = new DOMSource(np.getDocument(pub, pubDate));
		        }
		        long endMillis = System.currentTimeMillis();
		        logger.info("Done extracting data from Hermes. Duration=" + (endMillis - startMillis) + "ms.");
	        }
	                            
	        if (pageRange != null)	// filter page range
	        	source = FilterPageRange(source, pageRangeFilterXSL, pageRange);
	        
	        if (filterXSL != null)	// custom filter
	        	source = Filter(source, filterXSL);
	        if (debug)
	        	dump(source, new File(debugDir, debugFileBaseName + "_filtered.xml"));
	
	        if (transformXSL != null)	// custom transform
	        	source = Transform(source, transformXSL);
	        if (debug)
	        	dump(source, new File(debugDir, debugFileBaseName + "_transformed.xml"));           	
	        
	        // split by work items 
	        // and insert to target queue        
	        SplitThenEnqueue(source.getNode());
	        
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
            	
    	logger.exiting(getClass().getName(), "run");
    }
       
    protected DOMSource FilterPageRange(Source source, File xslFile, String pageRange) 
    		throws TransformerConfigurationException, TransformerException, IOException {
    	logger.fine("Running page range filter xslt=" +  xslFile.getCanonicalPath() + ", Page range=" + pageRange + ".");
    	long startMillis = System.currentTimeMillis();
    	String[] range = pageRange.split(":");
        Integer fromPage, toPage;
        
        fromPage = toPage = Integer.parseInt(range[0]);
        if (range.length > 1) toPage = Integer.parseInt(range[1]);
        if (fromPage > toPage) toPage = fromPage;
        
        DOMResult result = new DOMResult();
        Transformer t = tf.newTransformer(new StreamSource(xslFile));
        t.setParameter("fromPage", fromPage);
        t.setParameter("toPage", toPage);
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");        
        t.setOutputProperty(OutputKeys.ENCODING, getEncoding());        
        t.transform(source, result);
        
        long endMillis = System.currentTimeMillis();
        logger.fine("Done running page range filter xslt. Duration=" + (endMillis - startMillis) + "ms.");
        return new DOMSource(result.getNode());
    }
    
    protected DOMSource Filter(Source source, File xslFile) 
    		throws TransformerConfigurationException, TransformerException, IOException {
    	logger.fine("Running filter xslt=" + xslFile.getCanonicalPath() + ".");
    	long startMillis = System.currentTimeMillis();
    	
        DOMResult result = new DOMResult();
        Transformer t = tf.newTransformer(new StreamSource(xslFile));
        for (String prop : props.stringPropertyNames()) {	// parameters read from properties file
            if (prop.startsWith("filter.param."))
                t.setParameter(prop.replaceFirst("filter.param.", ""), props.getProperty(prop));
        }
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, getEncoding());        
        t.transform(source, result);
        
        long endMillis = System.currentTimeMillis();
        logger.fine("Done running filter xslt. Duration=" + (endMillis - startMillis) + "ms.");
        return new DOMSource(result.getNode());
    }    
    
    protected DOMSource Transform(Source source, File xslFile) 
			throws TransformerConfigurationException, TransformerException, IOException {
    	logger.fine("Running transform xslt=" + xslFile.getCanonicalPath() + ".");
    	long startMillis = System.currentTimeMillis();
    	
        DOMResult result = new DOMResult();
        Transformer t = tf.newTransformer(new StreamSource(xslFile));
		// to receive messages from XSLT
		Controller controller = (Controller) t;
		Receiver receiver = new XSLTMessageReceiver(logger);
		controller.setMessageEmitter(receiver);
		// parameters read from properties file
        for (String prop : props.stringPropertyNames()) {
            if (prop.startsWith("transform.param."))
                t.setParameter(prop.replaceFirst("transform.param.", ""), props.getProperty(prop));
        }
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, getEncoding());        
        t.transform(source, result);
        
        long endMillis = System.currentTimeMillis();
        logger.fine("Done running transform xslt. Duration=" + (endMillis - startMillis) + "ms.");
        return new DOMSource(result.getNode());
    }
    
    protected void SplitThenEnqueue(Node node)
			throws InterruptedException, TimeoutException {    	
		logger.fine("Split and enqueue work items.");
		long startMillis = System.currentTimeMillis();
		
        if (node instanceof Document)
            node = ((Document) node).getDocumentElement();    		    		
        NodeList nl = node.getChildNodes();
        logger.info("Found work items. Count=" + nl.getLength() + ".");

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            Document doc = db.newDocument();
            doc.appendChild(doc.importNode(n, true));
            enqueue(outQ, new QueueItem(doc));
        }    
        
        long endMillis = System.currentTimeMillis();
        logger.fine("Done with split and enqueue. Duration=" + (endMillis - startMillis) + "ms.");
    }
    
}
