<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : merlin-transform-story.xsl
    Description: 
    	Transform from Hermes to Obituary
    Revision History:
    	20130412 jpm - Legacy output format
        20120901 jpm - creation
-->

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
    xmlns:err="http://www.w3.org/2005/xqt-errors"
    xmlns:local="http://www.atex.com/local"
    exclude-result-prefixes="xsl xs xdt err fn local">
        
    <xsl:import href="util.xsl"/>
        
	<xsl:output method="xml" indent="no" encoding="UTF-8"/>
    
    <xsl:template match="item">
    	<obit>
	    	<xsl:copy-of select="./unique-id"/>
	    	<xsl:copy-of select="./start-date"/>
	    	<deceased-name>
	    		<xsl:value-of select="local:getTextContent(./item-content/headline/*)"/>        	
	    	</deceased-name>
	    	<xsl:copy-of select="./city"/>
	    	<xsl:copy-of select="./state"/>
	    	<noticetype>
	    		<xsl:choose>
	    			<xsl:when test="local:toLower(./free)='true'">Free</xsl:when>
	    			<xsl:otherwise>Paid</xsl:otherwise>
	    		</xsl:choose>
	    	</noticetype>
	    	<images>
	    		<xsl:for-each select="item-photo">
	    			<xsl:copy-of select="./photo"/>
	    		</xsl:for-each>
	    		<xsl:if test="local:toLower(./veteran)='true'">
	    			<icon>flag.jpg</icon>
	    		</xsl:if>
    		</images>
    		<obit-text>
    			<xsl:value-of select="local:getTextContent(./item-content/text/*)"/>
    		</obit-text>
		</obit>
    </xsl:template>
        
</xsl:stylesheet>
