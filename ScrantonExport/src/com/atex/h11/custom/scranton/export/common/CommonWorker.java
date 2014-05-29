package com.atex.h11.custom.scranton.export.common;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.AbstractQueue;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.iptc.IptcDirectory;

public class CommonWorker extends Commons implements Runnable {

    private static final String loggerName = CommonWorker.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
    
    private static final String defaultContentType = "text/xml";

    protected Boolean debug = null;
    protected String debugDir = null;
    protected String debugFileBaseName = null;    
    
    // these are used by inheriting classes
    protected AbstractQueue<QueueItem> inQ = null;    
    protected Properties props = null;
    protected DocumentBuilderFactory dbf = null;
    protected DocumentBuilder db = null;        
    protected XPathFactory xpf = null;
    protected XPath xp = null;
    protected TransformerFactory tf = null;    
    
    protected Map<String, List<String>> httpHeaderFields = null;
    protected String httpErrorDescription = null;
    protected String httpResponse = null;
    protected int httpResponseCode = 0;
    
    private DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
	
    public void init(AbstractQueue<QueueItem> inQ, Properties props)
    		throws RuntimeException {
    	logger.entering(getClass().getName(), "init");

    	try {
    		
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
    		
		} catch (Exception e) {
			throw new RuntimeException(e);
	    }    	
		
		logger.exiting(getClass().getName(), "run");
    }
    
    protected void write (URL destURL, String destFileName, Document doc)
            throws ProtocolException, FileNotFoundException, IOException,
            UnsupportedEncodingException, TransformerException {
        write (destURL, destFileName, doc, null);
    }
    
    protected void write (URL destURL, String destFileName, Document doc, Properties outputProperties)
            throws ProtocolException, FileNotFoundException, IOException,
            UnsupportedEncodingException, TransformerException {
        Object[] logParams = new Object[3];
        logParams[0] = destURL;
        logParams[1] = destFileName;
        logParams[2] = doc;
        logger.entering(getClass().getName(), "write", logParams);

        HttpURLConnection http = null;
        PrintWriter out = null;
        FTPClient ftp = null;

        if (destFileName == null) {
            destFileName = new Date().getTime() + "-"
                    + Thread.currentThread().getId() + ".xml";
        }

        if (destURL.getProtocol().equals("file")) {
            String path = destURL.getPath();
            File file = new File(path + File.separator + destFileName);
            out = new PrintWriter(file, getEncoding());
        } else if (destURL.getProtocol().equals("ftp")) {
            ftp = getFtpConnection(destURL);
            out = new PrintWriter(new OutputStreamWriter(
                    ftp.storeFileStream(destFileName), getEncoding()));
        } else if (destURL.getProtocol().equals("http")) {
            httpHeaderFields = null;
            httpErrorDescription = null;
            httpResponse = null;
            httpResponseCode = 0;
            String encoding = props.getProperty("http.contentEncoding", getEncoding());
            String contentType = props.getProperty("http.contentType", defaultContentType);
            http = (HttpURLConnection) destURL.openConnection();
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", contentType + "; charset=" + encoding);
            http.setRequestProperty("Content-Disposition", "filename=" + destFileName);
            http.setRequestProperty("Accept", contentType);
            if (destURL.getUserInfo() != null) {
                byte[] bytes = Base64.encodeBase64(destURL.getUserInfo().getBytes());
                http.setRequestProperty("Authorization", "Basic " + new String(bytes));
            }
            http.setInstanceFollowRedirects(true);
            http.connect();
            out = new PrintWriter(new OutputStreamWriter(http.getOutputStream(), encoding));
        } else {
        	throw new ProtocolException("Unsupported protocol: "
                                        + destURL.getProtocol());
        }

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(out);
        if (outputProperties == null)
            dump(source, result);
        else
            dump(source, result, outputProperties);
        out.close();

        if (ftp != null) ftp.disconnect();
        if (http != null) {
            httpHeaderFields = http.getHeaderFields();
            httpResponseCode = http.getResponseCode();
            if (httpResponseCode != HttpURLConnection.HTTP_OK
                    && httpResponseCode != HttpURLConnection.HTTP_CREATED
                    && httpResponseCode != HttpURLConnection.HTTP_ACCEPTED) {
                try {
                    InputStream errStream = http.getErrorStream();
                    if (errStream != null) {
                        BufferedReader err = new BufferedReader(
                            new InputStreamReader(errStream));
                        StringBuffer errSB = new StringBuffer(2000);
                        String errStr = null;
                        while ((errStr = err.readLine()) != null) {
                            errSB.append(errStr);
                        }
                        err.close();
                        httpErrorDescription = errSB.toString();
                        //throw new IOException("Response code: " + httpResponseCode + " - " + httpErrorDescription);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode + " - " + httpErrorDescription);
                    } else {
                        //throw new IOException("Response code: " + httpResponseCode);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode);
                    }
                } catch (Exception e) {}
            } else {
                try {
                    InputStream inStream = http.getInputStream();
                    if (inStream != null) {
                        BufferedReader resp = new BufferedReader(
                            new InputStreamReader(inStream));
                        StringBuffer sb = new StringBuffer(2000);
                        String str = null;
                        while ((str = resp.readLine()) != null) {
                            sb.append(str);
                        }
                        resp.close();
                        httpResponse = sb.toString();
                        logger.logp(Level.FINE, getClass().getName(), "write", httpResponse);
                    }
                } catch (Exception e) {}
            }
            http.disconnect();
        }

        logger.exiting(getClass().getName(), "write");
    }

    protected void write (URL destURL, String destFileName, InputStream in)
            throws ProtocolException, FileNotFoundException, IOException,
            UnsupportedEncodingException {
        Object[] logParams = new Object[3];
        logParams[0] = destURL;
        logParams[1] = destFileName;
        logParams[2] = in;
        logger.entering(getClass().getName(), "write", logParams);
        
        HttpURLConnection http = null;
        OutputStream out = null;
        FTPClient ftp = null;

        if (destFileName == null) {
            destFileName = new Date().getTime() + "-"
                    + Thread.currentThread().getId() + ".xml";
        }

        if (destURL.getProtocol().equals("file")) {
            String path = destURL.getPath();
            File file = new File(path + File.separator + destFileName); 
            out = new FileOutputStream(file);
        } else if (destURL.getProtocol().equals("ftp")) {
            ftp = getFtpConnection(destURL);
            out = ftp.storeFileStream(destFileName);
        } else if (destURL.getProtocol().equals("http")) {
            httpHeaderFields = null;
            httpErrorDescription = null;
            httpResponse = null;
            httpResponseCode = 0;
            http = (HttpURLConnection) destURL.openConnection();
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", "application/octet-stream");
            http.setRequestProperty("Content-Disposition", "filename=" + destFileName);
            if (destURL.getUserInfo() != null) {
                byte[] bytes = Base64.encodeBase64(destURL.getUserInfo().getBytes());
                http.setRequestProperty("Authorization", "Basic " + new String(bytes));
            }
            http.setInstanceFollowRedirects(true);
            http.connect();
            out = http.getOutputStream();
        } else {
            throw new ProtocolException("Unsupported protocol: "
                                        + destURL.getProtocol());
        }
        
        streamCopy(in, out); 
        in.close();
        out.close();

        if (ftp != null) ftp.disconnect();
        if (http != null) {
            httpHeaderFields = http.getHeaderFields();
            httpResponseCode = http.getResponseCode();
            if (httpResponseCode != HttpURLConnection.HTTP_OK
                    && httpResponseCode != HttpURLConnection.HTTP_CREATED
                    && httpResponseCode != HttpURLConnection.HTTP_ACCEPTED) {
                try {
                    InputStream errStream = http.getErrorStream();
                    if (errStream != null) {
                        BufferedReader err = new BufferedReader(
                            new InputStreamReader(errStream));
                        StringBuffer errSB = new StringBuffer(2000);
                        String errStr = null;
                        while ((errStr = err.readLine()) != null) {
                            errSB.append(errStr);
                        }
                        err.close();
                        httpErrorDescription = errSB.toString();
                        //throw new IOException("Response code: " + httpResponseCode + " - " + httpErrorDescription);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode + " - " + httpErrorDescription);
                    } else {
                        //throw new IOException("Response code: " + httpResponseCode);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode);
                    }
                } catch (Exception e) {}
            } else {
                try {
                    InputStream inStream = http.getInputStream();
                    if (inStream != null) {
                        BufferedReader resp = new BufferedReader(
                            new InputStreamReader(inStream));
                        StringBuffer sb = new StringBuffer(2000);
                        String str = null;
                        while ((str = resp.readLine()) != null) {
                            sb.append(str);
                        }
                        resp.close();
                        httpResponse = sb.toString();
                        logger.logp(Level.FINE, getClass().getName(), "write", httpResponse);
                    }
                } catch (Exception e) {}
            }
            http.disconnect();
        }
        
        logger.exiting(getClass().getName(), "write");
    }
    
    protected void write (URL destURL, String destFileName, File srcFile)
            throws ProtocolException, FileNotFoundException, IOException,
            UnsupportedEncodingException {
        Object[] logParams = new Object[3];
        logParams[0] = destURL;
        logParams[1] = destFileName;
        logParams[2] = srcFile;
        logger.entering(getClass().getName(), "write", logParams);

        HttpURLConnection http = null;
        OutputStream out = null;
        FTPClient ftp = null;

        if (destFileName == null) {
            destFileName = new Date().getTime() + "-"
                    + Thread.currentThread().getId() + ".xml";
        }

        if (destURL.getProtocol().equals("file")) {
            String path = destURL.getPath();
            File file = new File(path + File.separator + destFileName);
            out = new FileOutputStream(file);
        } else if (destURL.getProtocol().equals("ftp")) {
            ftp = getFtpConnection(destURL);
            out = ftp.storeFileStream(destFileName);
        } else if (destURL.getProtocol().equals("http")) {
            httpHeaderFields = null;
            httpErrorDescription = null;
            httpResponse = null;
            httpResponseCode = 0;
            http = (HttpURLConnection) destURL.openConnection();
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestMethod("POST");
            if (srcFile.getName().endsWith(".zip"))
                http.setRequestProperty("Content-Type", "application/zip");
            else
                http.setRequestProperty("Content-Type", "application/octet-stream");
            http.setRequestProperty("Content-Disposition", "filename=" + destFileName);
            if (destURL.getUserInfo() != null) {
                byte[] bytes = Base64.encodeBase64(destURL.getUserInfo().getBytes());
                http.setRequestProperty("Authorization", "Basic " + new String(bytes));
            }
            http.setInstanceFollowRedirects(true);
            http.connect();
            out = http.getOutputStream();
        } else {
            throw new ProtocolException("Unsupported protocol: "
                                        + destURL.getProtocol());
        }

        FileInputStream in = new FileInputStream(srcFile);
        streamCopy(in, out);
        in.close();
        out.close();

        if (ftp != null) ftp.disconnect();
        if (http != null) {
            httpHeaderFields = http.getHeaderFields();
            httpResponseCode = http.getResponseCode();
            if (httpResponseCode != HttpURLConnection.HTTP_OK
                    && httpResponseCode != HttpURLConnection.HTTP_CREATED
                    && httpResponseCode != HttpURLConnection.HTTP_ACCEPTED) {
                try {
                    InputStream errStream = http.getErrorStream();
                    if (errStream != null) {
                        BufferedReader err = new BufferedReader(
                            new InputStreamReader(errStream));
                        StringBuffer errSB = new StringBuffer(2000);
                        String errStr = null;
                        while ((errStr = err.readLine()) != null) {
                            errSB.append(errStr);
                        }
                        err.close();
                        httpErrorDescription = errSB.toString();
                        //throw new IOException("Response code: " + httpResponseCode + " - " + httpErrorDescription);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode + " - " + httpErrorDescription);
                    } else {
                        //throw new IOException("Response code: " + httpResponseCode);
                        logger.logp(Level.WARNING, getClass().getName(), "write",
                            "Response code: " + httpResponseCode);
                    }
                } catch (Exception e) {}
            } else {
                try {
                    InputStream inStream = http.getInputStream();
                    if (inStream != null) {
                        BufferedReader resp = new BufferedReader(
                            new InputStreamReader(inStream));
                        StringBuffer sb = new StringBuffer(2000);
                        String str = null;
                        while ((str = resp.readLine()) != null) {
                            sb.append(str);
                        }
                        resp.close();
                        httpResponse = sb.toString();
                        logger.logp(Level.FINE, getClass().getName(), "write", httpResponse);
                    }
                } catch (Exception e) {}
            }
            http.disconnect();
        }

        logger.exiting(getClass().getName(), "write");
    }

    /**
     * Establish a FTP connection.
     *
     * @param url FTP URL describing the endpoint.
     * @return 
     * @throws java.io.IOException
     */
    private FTPClient getFtpConnection (URL url) throws IOException {
        logger.entering(getClass().getName(), "getFtpConnection", url);

        //String protocol = url.getProtocol();
        String host = url.getHost();
        String userInfo = url.getUserInfo();
        String path = url.getPath();

        String[] credentials = userInfo.split(":");

        FTPClient ftp = new FTPClient();
        ftp.connect(host);
        ftp.login(credentials[0], credentials[1]);
        int reply = ftp.getReplyCode();
        if (reply == FTPReply.NOT_LOGGED_IN) {
            try { ftp.disconnect(); ftp = null; } catch (Exception e) {}
            throw new IOException("Login failed on FTP host " + host + ".");
        }
        String sysName = ftp.getSystemName();
        System.err.println("FTP system is: " + sysName);
        boolean bStatus = ftp.changeToParentDirectory();
        bStatus = ftp.changeWorkingDirectory(path);
        if (!bStatus) {
            try { ftp.logout(); ftp.disconnect(); ftp = null; } catch (Exception e) {}
            throw new IOException("Changing working directory to " + path
                                    + " failed on FTP host " + host + ".");
        }
        if (props.getProperty("ftpPassiveMode", "false").equalsIgnoreCase("true")) {
            ftp.enterLocalPassiveMode();
        }
        if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
            try { ftp.logout(); ftp.disconnect(); ftp = null; } catch (Exception e) {}
            throw new IOException("Failed to set binary transfer mode.");
        }

        logger.exiting(getClass().getName(), "getFtpConnection");

        return ftp;
    }
    
    protected String getProcessingInstructionData(String xpath, Node sourceNode, boolean remove) 
    		throws XPathExpressionException {
		Node n = (Node) xp.evaluate(xpath, sourceNode, XPathConstants.NODE);
		String data = null;
		if (n != null) {
			data = ((ProcessingInstruction) n).getData();
			if (remove)
				n.getParentNode().removeChild(n);
		}
		return data;
	}
    
    protected String getNodeData(String xpath, Node sourceNode)
    		throws XPathExpressionException {
		Node n = (Node) xp.evaluate(xpath, sourceNode, XPathConstants.NODE);
		String data = null;    	
		if (n != null) {
			data = n.getTextContent();
		}
		return data;		
    }
    
    protected void exportPhoto(Node sourceNode, URL destURL, boolean cropPhoto) 
			throws XPathExpressionException, UnsupportedEncodingException, IOException, ParseException {
    	logger.entering(getClass().getName(), "exportPhoto");

		// get image related instructions
		String photoSourceFile = getProcessingInstructionData("processing-instruction('photo-source-file')",
			sourceNode, true);
		String photoFileName = getProcessingInstructionData("processing-instruction('photo-file-name')",
			sourceNode, true);
		String dimensionStr = getProcessingInstructionData("processing-instruction('dimension')",
			sourceNode, true);    	
		String cropRectStr = getProcessingInstructionData("processing-instruction('crop-rect')",
			sourceNode, true);    	
		String rotationStr = getProcessingInstructionData("processing-instruction('rotation')",
			sourceNode, true);    	
		String flipXStr = getProcessingInstructionData("processing-instruction('flip-x')",
			sourceNode, true);    	
		String flipYStr = getProcessingInstructionData("processing-instruction('flip-y')",
			sourceNode, true);
		
		// crop and other image manipulation
		Rectangle cropRect = null; 
		if (cropRectStr != null) {
            String[] s = cropRectStr.split(" ");
            if (s.length == 4) {
                int bottom = Integer.parseInt(s[0]);
                int left = Integer.parseInt(s[1]);
                int top = Integer.parseInt(s[2]);
                int right = Integer.parseInt(s[3]);
                cropRect = new Rectangle(left, top, right - left, bottom - top);
            } else {
                throw new RuntimeException("Invalid crop rect value: " + cropRectStr);
            }    				
		}
		Dimension dimension = null;
		if (dimensionStr != null) {
            String[] s = dimensionStr.split(" ");
            if (s.length == 2) {
                int width = df.parse(s[0]).intValue();
                int height = df.parse(s[1]).intValue();
                dimension = new Dimension(width, height);
            } else {
            	throw new RuntimeException("Invalid dimension value: " + dimensionStr);
            }				
		}
		int rotation = 0;
		if (rotationStr != null) rotation = df.parse(rotationStr).intValue();
		boolean flipX = false;
		if (flipXStr != null) flipX = flipXStr.equalsIgnoreCase("true");
		boolean flipY = false;
		if (flipYStr != null) flipY = flipYStr.equalsIgnoreCase("true");			

		logger.info("Exporting photo file: " + photoFileName);    	
		
		if (cropPhoto && cropRect != null && dimension != null) {
			write(destURL, photoFileName, new ByteArrayInputStream(
					cropPhoto(new File(photoSourceFile), cropRect, dimension, rotation, flipX, flipY)));
		}
		else {
			write(destURL, photoFileName, new File(photoSourceFile));
		}
		
		logger.exiting(getClass().getName(), "exportPhoto");
	}
    
    protected byte[] cropPhoto(File sourcePhoto, Rectangle cropRect, Dimension dimension, int rotation, boolean flipX, boolean flipY) 
    		throws IOException {
    	logger.entering(getClass().getName(), "cropPhoto");
    	
    	BufferedImage sourceImage = ImageIO.read(sourcePhoto);
    	
        // get orig image dimensions
        int w = sourceImage.getWidth();
        int h = sourceImage.getHeight();
        
        // compute adjusted crop
        logger.finest("Photo " + sourcePhoto.getName() + ": w=" + w + ", h=" + h);
        float ratioX = (float) dimension.width / (float) w;
        float ratioY = (float) dimension.height / (float) h;      
        logger.finest("Photo " + sourcePhoto.getName() + ": x-ratio=" + ratioX + ", y-ratio=" + ratioY);
        int cropX = (int) ((float) cropRect.x / ratioX);
        int cropY = (int) ((float) cropRect.y / ratioY);
        int cropW = (int) ((float) cropRect.width / ratioX);
        int cropH = (int) ((float) cropRect.height / ratioY);
        logger.finest("Photo " + sourcePhoto.getName() + 
            ": adjusted crop: x=" + cropX + ", y=" + cropY + ", w=" + cropW + ", h=" + cropH);
        Rectangle adjustedCropRect = new Rectangle(cropX, cropY, cropW, cropH);
        
        BufferedImage croppedImage = sourceImage.getSubimage(adjustedCropRect.x, adjustedCropRect.y, 
        	adjustedCropRect.width, adjustedCropRect.height);
        logger.info("Photo " + sourcePhoto.getName() + " cropped.");
        
        if (flipX || flipY)
        	croppedImage = flipPhoto(croppedImage, flipX, flipY, sourcePhoto.getName());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(croppedImage, "jpg", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        
        logger.exiting(getClass().getName(), "cropPhoto");
        return imageBytes;
    }
    
    protected BufferedImage flipPhoto(BufferedImage image, boolean flipX, boolean flipY, String photoName) {
    	logger.entering(getClass().getName(), "flipPhoto");
    	
    	AffineTransform tx = null;
    	AffineTransformOp op = null;
    	
    	if (flipX && flipY) {
	    	// Flip the image vertically and horizontally; equivalent to rotating the image 180 degrees
    		tx = AffineTransform.getScaleInstance(-1, -1);
	    	tx.translate(-image.getWidth(null), -image.getHeight(null));
	    	op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	    	image = op.filter(image, null);
	    	logger.info("Photo " + photoName + " flipped horizontally and vertically.");
    	}
    	else if (flipX) {
	    	// Flip the image horizontally
    		tx = AffineTransform.getScaleInstance(-1, 1);
	    	tx.translate(-image.getWidth(null), 0);
	    	op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	    	image = op.filter(image, null);
	    	logger.info("Photo " + photoName + " flipped horizontally.");
    	}
    	else if (flipY) {
	    	// Flip the image vertically
	    	tx = AffineTransform.getScaleInstance(1, -1);
	    	tx.translate(0, -image.getHeight(null));
	    	op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	    	image = op.filter(image, null);	    	
	    	logger.info("Photo " + photoName + " flipped vertically.");
    	}
	
    	logger.exiting(getClass().getName(), "flipPhoto");
    	return image;
    }

    public static String getIptcCopyrightNotice(String imgFilename) {
    	logger.entering("CommonWorker", "getIptcCopyrightNotice");
    	String copyright = "";
    	
    	try {
    		File imgFile = new File(imgFilename);
    		Metadata metadata = ImageMetadataReader.readMetadata(imgFile);
    		Directory directory = metadata.getDirectory(IptcDirectory.class);
    		if (directory.getDescription(IptcDirectory.TAG_COPYRIGHT_NOTICE) != null) {
    			copyright = directory.getDescription(IptcDirectory.TAG_COPYRIGHT_NOTICE);
    		}
    		else {
    			logger.warning("Iptc Copyright Notice not found for image: " + imgFilename);
    		}
    	} catch (Exception e) {
    		logger.log(Level.SEVERE, "Error encountered", e);
    	}
    	
    	logger.exiting("CommonWorker", "getIptcCopyrightNotice");
    	return copyright;
    }     
}
