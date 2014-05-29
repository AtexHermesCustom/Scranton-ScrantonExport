<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : merlin-transform-initial.xsl
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
        
    <xsl:output method="xml" indent="yes" encoding="WINDOWS-1252"/>
    
    <xsl:param name="isPrinted" select="'false'"/>
    <xsl:param name="exportStandaloneObjects" select="'true'"/>
    
    <xsl:template match="/">
        <items>
        	<!-- loop through physpages -->
            <xsl:for-each select="//ncm-physical-page[logical-pages-linked-to-ncm-physical-page[@count>0]/ncm-logical-page/layouts-in-ncm-logical-page/@count>0 
            	and ($isPrinted!='true' or ($isPrinted='true' and is_printed='true'))]">

				<!-- In this step, get basic information on the packages laid out on the page (e.g. id, name).
					At this point, only the elements that are paginated are included in the extraction.
					Since the client needs all elements of the package to be exported (including those that are not paginated),
					the actual export of the contents of the package's elements is done on the next step/xsl transform.
					The next step extracts all elements of the package.
				 -->

            	<!-- packages --> 
            	<xsl:apply-templates select=".//ncm-object[ncm-type-property/object-type/@id=17]" mode="item-story">
             	   <xsl:with-param name="physPage" select="."/>
            	</xsl:apply-templates>
            	           	
            	<xsl:if test="$exportStandaloneObjects='true'">
            	    <!-- standalone photos - not part of any package -->
		           	<xsl:apply-templates select=".//ncm-object[sp_id=0 and ncm-type-property/object-type/@id=6]" mode="standalone-item-photo">
						<xsl:with-param name="physPage" select="."/>
		           	</xsl:apply-templates>            	    
           		</xsl:if> 
           		
            </xsl:for-each>       
        </items>
    </xsl:template>

    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=17]" mode="item-story">
    	<xsl:param name="physPage"/>
        
        <item-package>
        	<id><xsl:value-of select="./obj_id"/></id>
        	<name><xsl:value-of select="./name"/></name>
        	<page-info>
				<pub><xsl:value-of select="$physPage/edition/newspaper-level/level/@name"/></pub>
				<edition><xsl:value-of select="$physPage/edition/@name"/></edition>
				<pubdate><xsl:value-of select="local:formatDateNum($physPage/pub_date)"/></pubdate>
				<section><xsl:value-of select="$physPage/section/@name"/></section>
				<page-number><xsl:value-of select="$physPage/page_number_str"/></page-number>
        	</page-info>
        </item-package>
    </xsl:template>
        
    <xsl:template match="ncm-object[sp_id=0 and ncm-type-property/object-type/@id=6]" mode="standalone-item-photo">
    	<xsl:param name="physPage"/>

        <item-standalone-photo>
        	<xsl:choose>
        		<xsl:when test="./variant_of_obj_id != ''">
        			<!-- master -->
		        	<id><xsl:value-of select="./variant_of_obj_id"/></id>
		        	<name><xsl:value-of select="concat(./name, '_MASTER')"/></name>
        		</xsl:when>
        		<xsl:otherwise>
        			<!-- self -->
		        	<id><xsl:value-of select="./obj_id"/></id>
		        	<name><xsl:value-of select="./name"/></name>
        		</xsl:otherwise>
        	</xsl:choose>
        	<page-info>
				<pub><xsl:value-of select="$physPage/edition/newspaper-level/level/@name"/></pub>
				<edition><xsl:value-of select="$physPage/edition/@name"/></edition>
				<pubdate><xsl:value-of select="local:formatDateNum($physPage/pub_date)"/></pubdate>
				<section><xsl:value-of select="$physPage/section/@name"/></section>
				<page-number><xsl:value-of select="$physPage/page_number_str"/></page-number>
        	</page-info>
        </item-standalone-photo>     	
    </xsl:template>
       
</xsl:stylesheet>
