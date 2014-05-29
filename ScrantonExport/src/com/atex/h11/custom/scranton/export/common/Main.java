package com.atex.h11.custom.scranton.export.common;

import com.atex.h11.custom.common.DataSource;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Properties;

public class Main {
	
    private static final String loggerName = Main.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);;
    
    private static final int DEFAULT_NUMWORKERS = 1;
    
    private static final String DEFAULT_HERMES_USER = "BATCH";
    private static final String DEFAULT_HERMES_PASSWORD = "BATCH";	    
    
    private static String user = null;
    private static String password = null;    
    private static NCMDataSource ds = null; 
	
	public static void main(String[] args) {
		logger.entering(loggerName, "main");
		logger.info("Export started. Arguments: " + Arrays.toString(args));
		
		Properties props = new Properties();
		String inputFilePath = null;
		String credentials = null;
		Integer pubDate = null;		
		String pub = null;
		String edition = null;
		String pageRange = null;
		
		try {
            // Gather command line parameters.
            for (int i = 0; i < args.length; i++) {
            	// properties file
                if (args[i].equals("-p"))
                    props.load(new FileInputStream(args[++i]));
                else if (args[i].startsWith("-p"))
                    props.load(new FileInputStream(args[i].substring(2)));
                // input file
                if (args[i].equals("-f"))
                	inputFilePath = args[++i].trim();
                else if (args[i].startsWith("-f"))
                	inputFilePath = args[++i].trim();   
            	// credentials user and password
                if (args[i].equals("-c"))
                	credentials = args[++i].trim();
                else if (args[i].startsWith("-c"))
                	credentials = args[++i].trim();                
                // pubdate 
                else if (args[i].equals("-d"))
                    pubDate = Integer.parseInt(args[++i].trim());
                else if (args[i].startsWith("-d"))
                	pubDate = Integer.parseInt(args[i].substring(2).trim());
                // pub level
                else if (args[i].equals("-l"))
                    pub = args[++i].trim().toUpperCase();
                else if (args[i].startsWith("-l"))
                	pub = args[i].substring(2).trim().toUpperCase();
                // edition
                else if (args[i].equals("-e"))
                    edition = args[++i].trim().toUpperCase();
                else if (args[i].startsWith("-e"))
                	edition = args[i].substring(2).trim().toUpperCase();
                // page range
                else if (args[i].equals("-r"))
                    pageRange = args[++i].trim();
                else if (args[i].startsWith("-r"))
                	pageRange = args[i].substring(2).trim();
            }
            
            // credentials for connecting to the datasource
        	user = DEFAULT_HERMES_USER;
        	password = DEFAULT_HERMES_PASSWORD;
        	if (credentials != null && ! credentials.equals("")) {
    	    	String[] creds = credentials.split(":");
    	    	if (creds[0] != null) user = creds[0];
    	    	if (creds[1] != null) password = creds[1];
        	}            
            
            // Create the worker queue
            LinkedBlockingQueue<QueueItem> workQ = new LinkedBlockingQueue<QueueItem>();
            
            // Run the feeder
            Class<?> feederClass = Class.forName(props.getProperty("feederClass"));
            CommonFeeder feeder = (CommonFeeder) feederClass.newInstance();
            if (inputFilePath != null && ! inputFilePath.equals(""))
            	feeder.init(workQ, props, inputFilePath);
            else 
            	feeder.init(workQ, props, credentials, pubDate, pub, edition, pageRange);
            Thread feederThread = new Thread(feeder);
            feederThread.setName("Feeder");
            feederThread.start();
            logger.fine("Started Feeder thread (" + feederThread.getId() + ", " + feederThread.getName() + ").");            
            		 
            // Get the number of workers to start
            int numWorkers = Integer.parseInt(props.getProperty("numWorkers", Integer.toString(DEFAULT_NUMWORKERS)));
            logger.fine("Number of worker threads=" + numWorkers + ".");
            
            // Start the workers
            Class<?> workerClass = Class.forName(props.getProperty("workerClass"));
            CommonWorker[] workers = new CommonWorker[numWorkers];
            Thread[] workerThreads = new Thread[numWorkers];
            for (int i = 0; i < workers.length; i++) {
                workers[i] = (CommonWorker) workerClass.newInstance();
                workers[i].init(workQ, props);
                workerThreads[i] = new Thread(workers[i]);
                workerThreads[i].setName("Worker-" + i);
                workerThreads[i].start();
                logger.fine("Started Worker thread (" + workerThreads[i].getId() + ", " + workerThreads[i].getName() + ").");
            }                
            
            // wait until feeder has terminated and workers and dumpers are waiting
            while (true) {
                if (workQ.isEmpty() && feederThread.getState() == Thread.State.TERMINATED) {
                    logger.fine("Work queue is empty and feeder has terminated.");
                    boolean waiting = true;
                    for (int i = 0; i < workerThreads.length && waiting; i++) {
                    	waiting = (workerThreads[i].getState() == Thread.State.TIMED_WAITING
                                || workerThreads[i].getState() == Thread.State.TERMINATED);
                        logger.finest(workerThreads[i].getName() + ": thread state=" + workerThreads[i].getState());
                    }
                    if (waiting) break;
                } else {
                    logger.finest("Work queue document count=" + workQ.size() + ".");
                    logger.finest(feederThread.getName() + ": thread state=" + feederThread.getState());
                    for (int i = 0; i < workerThreads.length; i++) {
                        logger.finest(workerThreads[i].getName() + ": thread state=" + workerThreads[i].getState());
                        if (workerThreads[i].getState() == Thread.State.TERMINATED) {
                            // Restart a terminated thread, state should be always TIMED_WAITING or RUNNABLE.
                            workers[i] = (CommonWorker) workerClass.newInstance();
                            workers[i].init(workQ, props);
                            workerThreads[i] = new Thread(workers[i]);
                            workerThreads[i].setName("Worker-" + i);
                            workerThreads[i].start();
                            logger.warning("Restarted Worker thread (" + workerThreads[i].getId() + ", " + workerThreads[i].getName() + ").");
                        }
                    }
                }
                Thread.sleep(1000);
            }            
            
	    } catch (Exception e) {
	    	logger.log(Level.SEVERE, "Error encountered", e);
	    }
	    
	    logger.info("Export completed.");
	    logger.exiting(loggerName, "main");
	    System.exit(0);
	}
	
	public static NCMDataSource getDatasource() 
			throws FileNotFoundException, IOException {
		if (ds == null)
			ds = DataSource.newInstance(user, password);	// establish data source 
		return ds;
	}
	 
}
