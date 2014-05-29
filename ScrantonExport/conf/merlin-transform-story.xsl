<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : merlin-transform-story.xsl
    Description: 
    	Transform from Hermes to Merlin
    Revision History:
        20120710 jpm - creation
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
        
	<xsl:output method="xml" indent="no" encoding="WINDOWS-1252"/>
    
    <xsl:template match="item-story">
    	<item-story>
	    	<xsl:copy-of select="./SOURCE"/>
	    	<xsl:copy-of select="./SERVICE"/>
	    	<xsl:copy-of select="./PUBDATE"/>
	    	<xsl:copy-of select="./EDITION"/>
	    	<xsl:copy-of select="./SECTION"/>
	    	<xsl:copy-of select="./ZONE"/>
	    	<xsl:copy-of select="./PAGE"/>
	    	<xsl:copy-of select="./OBJECT"/>
	    	<xsl:copy-of select="./REFERENCE"/>
	    	<xsl:copy-of select="./LASTEDITOR"/>
	    	<xsl:copy-of select="./LASTEDTIME"/>
        	<xsl:copy-of select="./CATEGORY"/>
        	<CRHOLDER>
        		<xsl:value-of select="local:getTextContent(./item-content/text/COPYRIGHT)"/>        	
        	</CRHOLDER>
        	<CREDIT>
        		<xsl:value-of select="local:getTextContent(./item-content/text/CREDIT)"/>        	
        	</CREDIT>
        	<CITY>
        		<xsl:value-of select="local:getTextContent(./item-content/text/SOURCE)"/>
        	</CITY>
        	<CAPTION>
        		<!-- multiple captions possible, separate by semi-colon -->
        		<xsl:for-each select="./item-content/caption">
        			<xsl:value-of select="local:getTextContent(./*)"/>
		            <xsl:if test="position() != last()">;</xsl:if>        			
        		</xsl:for-each>
        	</CAPTION>
        	<BYLINE>
        		<xsl:variable name="taggedByline" select="local:getTextContent(./item-content/text/BYLINE)"/>
        		<xsl:choose>
        			<xsl:when test="string-length(normalize-space($taggedByline)) &gt; 0">
        				<xsl:value-of select="$taggedByline"/>
        			</xsl:when>
        			<xsl:otherwise>
        				<xsl:value-of select="./WEBBYLINE"/>
        			</xsl:otherwise>
        		</xsl:choose>
        	</BYLINE>
        	<BYLINETITLE>
        		<xsl:value-of select="local:getTextContent(./item-content/text/BYLINETITLE)"/>
        	</BYLINETITLE>
        	<HEADLINE>
        		<xsl:value-of select="local:getTextContent(./item-content/headline/*)"/>        	
        	</HEADLINE>
        	<STORY>
        		<xsl:value-of select="local:getTextContent(./item-content/text/*[not(self::BYLINE or self::BYLINETITLE 
        			or self::SOURCE or self::COPYRIGHT or self::CREDIT)])"/>
        	</STORY>
		</item-story>
    </xsl:template>
        
</xsl:stylesheet>
