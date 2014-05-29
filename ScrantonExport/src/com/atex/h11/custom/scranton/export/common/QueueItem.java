package com.atex.h11.custom.scranton.export.common;

import org.w3c.dom.Document;

public class QueueItem {
	
    private Document doc = null;
	
    public QueueItem (Document d) {
        this.doc = d;
    }
    
    public Document getDocument () {
        return this.doc;
    }
    
    public void setDocument (Document d) {
        this.doc = d;
    }
       
}
