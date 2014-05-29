package com.atex.h11.custom.scranton.export.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.Properties;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import org.w3c.dom.Document;

public class Commons {
	
	private static final int QUEUE_RETRIES = 120; 
	
    private static final String loggerName = Commons.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
    
	private TransformerFactory tf = TransformerFactory.newInstance();
    private Transformer t = null;	
    
    private String encoding = "UTF-8"; // default = UTF-8
    
    protected void setEncoding(String encoding) {
    	if (encoding != null && !encoding.isEmpty()) {
	    	this.encoding = encoding;
	    	logger.finer("Encoding set to: " + this.encoding);
    	}
    	else {
    		logger.warning("Empty encoding value received.");
    	}
    }
    
    protected String getEncoding() {
    	// logger.finer("Using encoding: " + this.encoding);
    	return encoding;
    }
	
    protected void enqueue(AbstractQueue<QueueItem> queue, QueueItem item) 
    		throws InterruptedException, TimeoutException {
        boolean qStatus = false;
        int retries = QUEUE_RETRIES;
        while (!(qStatus = queue.offer(item)) && retries-- > 0)
            Thread.sleep(1000);
        if (!qStatus) {
            logger.warning("Could not queue element.");
            throw new TimeoutException("Could not queue element.");
        } else if ((QUEUE_RETRIES - retries) > 0) {
            logger.fine("Document queued after " + (QUEUE_RETRIES - retries) + " retries.");
        }    		
    }
    
    protected QueueItem dequeue(AbstractQueue<QueueItem> queue) 
    		throws InterruptedException {
        QueueItem item = null;
        while (item == null) {
            if ((item = queue.poll()) == null)
                Thread.sleep(200);
        }
        return item;
    }
    
	protected void dump(Document doc, File file)
			throws UnsupportedEncodingException, FileNotFoundException, TransformerConfigurationException, TransformerException {
        DOMSource source = new DOMSource(doc);
        dump(source, file);		
	}	
	
	protected void dump(Source source, File file) 
			throws UnsupportedEncodingException, FileNotFoundException, TransformerConfigurationException, TransformerException {
        PrintWriter out = new PrintWriter(file, getEncoding());
        StreamResult result = new StreamResult(out);
        dump(source, result);
        out.close();			
	}
	
	protected void dump(Source source, Result result)
			throws TransformerConfigurationException, TransformerException {
        if (t == null) 
        	t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.STANDALONE, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, getEncoding());
        t.setOutputProperty("{http://xml.apache.org/xsl}indent-amount", "4");	        
        t.transform(source, result);			
	}
	
	protected void dump(Source source, Result result, Properties outputProps)
			throws TransformerConfigurationException, TransformerException {
        if (t == null) 
        	t = tf.newTransformer();
        t.setOutputProperties(outputProps);
        t.transform(source, result);		
	}
	
    protected void streamCopy (InputStream in, OutputStream out)
    		throws IOException {
    	byte[] buf = new byte[8192];
    	int bytesRead;
    	do {
    		bytesRead = in.read(buf);
    		if (bytesRead > 0)
    			out.write(buf, 0, bytesRead);
    	} while (bytesRead >= 0);
    }
    
    protected File loadStylesheetFile(Properties props, String key, boolean required)
    		throws IOException, RuntimeException {
    	File xslFile = null;
        String val = props.getProperty(key);
        if (val != null && !val.isEmpty()) {
        	logger.config("Using " + key + " stylesheet=" + val);
        	xslFile = new File(val);
            if (!xslFile.exists())
                throw new RuntimeException(xslFile.getCanonicalPath() + " does not exist.");
            else if (!xslFile.isFile())
                throw new RuntimeException(xslFile.getCanonicalPath() + " is not a file.");
            else if (!xslFile.canRead())
                throw new RuntimeException(xslFile.getCanonicalPath() + " is not readable.");
        } else {
        	String msg = "Stylesheet " + key + " not configured.";
        	if (required)
        		throw new RuntimeException(msg);
        	else
        		logger.config(msg);
        }
        return xslFile;
    }
    
    protected URL loadURL(Properties props, String key)
    		throws MalformedURLException {
        String val = props.getProperty(key);
        if (val == null)
            throw new RuntimeException("URL " + key + " not configured.");
        return new URL(val);   
    }
   
}
