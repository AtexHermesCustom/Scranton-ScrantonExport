<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : obit-transform-initial.xsl
    Description: 
    	Transform from Hermes to Obituary
    Revision History:
    	20140417 jpm - export if page the obit package is on is on PG_READY
    				   do not require the TEXT element to be in READY status 
    	20130412 jpm - Legacy format
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
        
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    
    <xsl:param name="styleFile" select="'incopy.xml'"/>
    <xsl:param name="channel" select="'PRINT'"/>
    <xsl:param name="isPrinted" select="'false'"/>
    <xsl:param name="obitLevelsList" select="'obitLevels.xml'"/>
    
  	<xsl:variable name="tagRules" select="doc($styleFile)/configuration/tag-rules/rule"/>
  	<xsl:variable name="obitLevels" select="doc($obitLevelsList)/levels/level"/>
  	
  	<!-- <xsl:variable name="lineBreak" select="'&lt;br/&gt;'"/> -->
  	<xsl:variable name="lineBreak" select="'&#x0A;'"/>

    <xsl:template match="/">
        <items>
        	<!-- loop through physpages -->
            <xsl:for-each select="//ncm-physical-page[logical-pages-linked-to-ncm-physical-page/@count>0 
            	and ($isPrinted!='true' or ($isPrinted='true' and is_printed='true'))]">
            	<xsl:variable name="physPage" select="."/>

				<!-- loop through logpages -->
				<xsl:for-each select="./logical-pages-linked-to-ncm-physical-page/ncm-logical-page[layouts-in-ncm-logical-page/@count>0 
					and status/status/@name='PG_READY']">
			
	            	<!-- packages -->
	            	<xsl:for-each select=".//ncm-object[ncm-type-property/object-type/@id=17]">
	            		<!-- process only obit packages -->
	            		<xsl:if test="local:isObitPackage(.)=1">
	            			<!-- obit package validation -->
	            			<xsl:if test="local:isObitReady(.)=1">
				            	<xsl:apply-templates select="." mode="item">
				             	   <xsl:with-param name="physPage" select="$physPage"/>
				            	</xsl:apply-templates>
	            			</xsl:if>
			            </xsl:if>
	            	</xsl:for-each>
				</xsl:for-each>
				            	
            </xsl:for-each>       
        </items>
    </xsl:template>
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=17]" mode="item">
    	<xsl:param name="physPage"/>
        <xsl:variable name="pkgId" select="./obj_id"/>
        <xsl:variable name="pkgName" select="./name"/>
        <xsl:variable name="pub" select="$physPage/edition/newspaper-level/level/@name"/>
        
		<item>
        	<!-- output filename -->
        	<xsl:processing-instruction name="file-name" 
        		select="concat($pub, '-', $pkgId, '-', local:getTimestamp(), '.xml')"/>
        			
        	<unique-id><xsl:value-of select="$pkgId"/></unique-id>
        	<start-date><xsl:value-of select="local:formatDateDelim($physPage/pub_date, '/')"/></start-date>
        	
        	<!-- values taken from metadata -->
        	<xsl:variable name="obitMetadata" select="./extra-properties/OBITS"/>
        	<city><xsl:value-of select="$obitMetadata/CITY"/></city>
        	<state><xsl:value-of select="$obitMetadata/STATE"/></state>
			<free><xsl:value-of select="$obitMetadata/FREE"/></free>
			<veteran><xsl:value-of select="$obitMetadata/VETERAN"/></veteran>
			        	
        	<item-content>
        		<!-- headline objects -->
        		<xsl:apply-templates select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=2]" mode="content"/>
        		<!-- summary objects -->
        		<xsl:apply-templates select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=14]" mode="content"/>
				<!-- header objects -->
				<xsl:apply-templates select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=4]" mode="content"/>
				<!-- text objects -->
				<xsl:apply-templates select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=1]" mode="content"/>
        	</item-content>
        	
			<!-- photos -->
	       	<xsl:for-each select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=6]">
	       		<xsl:variable name="objId" select="./obj_id"/>
	       		<xsl:variable name="objName" select="./name"/>
	       		<xsl:variable name="countStr">
	       			<xsl:choose>
	       				<xsl:when test="position() &gt; 1"><xsl:value-of select="concat('-', position())"/></xsl:when>
	       				<xsl:otherwise></xsl:otherwise>
	       			</xsl:choose>
	       		</xsl:variable>
	        	<xsl:variable name="photoName" 
	        		select="concat($pkgId, $countStr)"/>
	        	<xsl:variable name="photoExt" 
	        		select="local:substring-after-last(./content-property/file-property/original-file/server-path, '.')"/>
       			
       			<item-photo>
	       			<!-- photo output filename --> 
		        	<xsl:processing-instruction name="photo-file-name" select="concat($photoName, '.', $photoExt)"/>
		        	
       				<!-- these data are no longer needed 
       					because the WEB variant is the one that needs to be exported
					### photo source 
       				<xsl:processing-instruction name="photo-source-file" select="./content-property/file-property/original-file/server-path"/>					
					### cropping/transformation info
	                <xsl:processing-instruction name="dimension" select="concat(./content-property/image-size/width, ' ', ./content-property/image-size/height)"/>
	                <xsl:if test="./content-property/crop-rect">
	                    <xsl:processing-instruction name="crop-rect" select="concat(./content-property/crop-rect/@bottom, ' ', ./content-property/crop-rect/@left, ' ', ./content-property/crop-rect/@top, ' ', ./content-property/crop-rect/@right)"/>
	                </xsl:if>
	                <xsl:if test="./content-property/xy-transf">
	                    <xsl:processing-instruction name="rotation" select="./content-property/xy-transf/@rotate"/>
	                    <xsl:processing-instruction name="flip-x" select="./content-property/xy-transf/@flip-x"/>
	                    <xsl:processing-instruction name="flip-y" select="./content-property/xy-transf/@flip-y"/>
	                </xsl:if>
	                 -->

					<photo><xsl:value-of select="concat($photoName, '.', $photoExt)"/></photo>
					<!-- pass the master variant -->
					<variantOfObjId><xsl:value-of select="./variant_of_obj_id"/></variantOfObjId>
					<!-- caption -->
					<xsl:apply-templates select="$physPage//ncm-object[sp_id=$pkgId and ncm-type-property/object-type/@id=3
	       				and relation_obj_id=$objId]" mode="content"/><!-- caption by relation -->
	       		</item-photo>
	       	</xsl:for-each>
  		</item>
    </xsl:template>
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=2]" mode="content">
    	<headline>
    		<xsl:attribute name="id" select="./obj_id"/>
    		<xsl:attribute name="name" select="./name"/>
    		<xsl:attribute name="channel" select="./variant_type/@name"/>       	
    		<xsl:apply-templates select="convert-property[@format='Neutral']/story" mode="content"/>
    	</headline>
    </xsl:template>
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=14]" mode="content">
    	<summary>
    		<xsl:attribute name="id" select="./obj_id"/>
    		<xsl:attribute name="name" select="./name"/>
    		<xsl:attribute name="channel" select="./variant_type/@name"/>       	
    		<xsl:apply-templates select="convert-property[@format='Neutral']/story" mode="content"/>
    	</summary>
    </xsl:template>    
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=3]" mode="content">
    	<caption>
    		<xsl:attribute name="id" select="./obj_id"/>
    		<xsl:attribute name="name" select="./name"/>
    		<xsl:attribute name="channel" select="./variant_type/@name"/>       	
    		<xsl:apply-templates select="convert-property[@format='Neutral']/story" mode="content"/>
    	</caption>
    </xsl:template>     
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=4]" mode="content">
    	<header>
    		<xsl:attribute name="id" select="./obj_id"/>
    		<xsl:attribute name="name" select="./name"/>
    		<xsl:attribute name="channel" select="./variant_type/@name"/>       	
    		<xsl:apply-templates select="convert-property[@format='Neutral']/story" mode="content"/>
    	</header>
    </xsl:template>
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=1]" mode="content">
    	<text>
    		<xsl:attribute name="id" select="./obj_id"/>
    		<xsl:attribute name="name" select="./name"/>
    		<xsl:attribute name="channel" select="./variant_type/@name"/>       	
    		<xsl:apply-templates select="convert-property[@format='Neutral']/story" mode="content"/>
    	</text>
    </xsl:template>

    <xsl:template match="story" mode="content">
    	<!-- loop through all par nodes -->	
   		<xsl:for-each select=".//par">
	    	<xsl:variable name="tag" select="@name"/><!-- para tag -->
	    	<xsl:variable name="tagRule" select="local:lookupTagRule($tag, $tagRules)"/><!-- see if tag has a configured rule -->	  
        	<xsl:choose>
	        	<xsl:when test="exists($tagRule)">
	            	<xsl:apply-templates select="text()|char" mode="content">
		            	<xsl:with-param name="tag" select="$tagRule/@replacement"/><!-- replace tag -->
			        	<xsl:with-param name="toUpper" select="$tagRule/@to-upper"/><!-- flag to convert to upper -->
		        	</xsl:apply-templates>
  		    	</xsl:when>
		    	<xsl:otherwise>
	      			<xsl:apply-templates select="text()|char" mode="content">
		    			<xsl:with-param name="tag" select="$tag"/><!-- use orig tag -->
		  			</xsl:apply-templates>
				</xsl:otherwise>
	  		</xsl:choose>
	  		<xsl:if test="position() != last()">
				<xsl:element name="{if (exists($tagRule/@replacement)) then $tagRule/@replacement else $tag}">
		  			<xsl:value-of select="$lineBreak"/>
				</xsl:element>
	  		</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
 	<xsl:template match="char" mode="content">
    	<xsl:choose>
	  		<xsl:when test="@name='note'">
	    		<!-- to do: handle notes (e.g. webhed, breaking news, keywords) -->
	  		</xsl:when>
	  		<!--
	  		<xsl:when test="@name='glyph'">
	    		- to do: handle special chars
	  		</xsl:when>
	  		 -->
	  		<xsl:otherwise>
		    	<xsl:variable name="tag" select="if (exists(@override-by)) then @override-by else @name"/><!-- char tag -->
				<xsl:variable name="tagRule" select="local:lookupTagRule($tag, $tagRules)"/><!-- see if tag has a configured rule -->		  
				<xsl:choose>
	  	      		<xsl:when test="exists($tagRule)">
		        		<xsl:apply-templates select="text()" mode="content">
			      			<xsl:with-param name="tag" select="$tagRule/@replacement"/><!-- replace tag -->
				  			<xsl:with-param name="toUpper" select="$tagRule/@to-upper"/><!-- flag to convert to upper -->
			    		</xsl:apply-templates>
	  		  		</xsl:when>
			  		<xsl:otherwise>
		        		<xsl:apply-templates select="text()" mode="content">
			      			<xsl:with-param name="tag" select="$tag"/><!-- use orig tag -->
			    		</xsl:apply-templates>
			  		</xsl:otherwise>
				</xsl:choose>
	  		</xsl:otherwise>
		</xsl:choose>
  	</xsl:template>
  	
  	<xsl:template match="text()" mode="content">
	    <xsl:param name="tag"/>
		<xsl:param name="toUpper"/>
		<xsl:param name="toLower"/>
		<xsl:element name="{$tag}">
			<xsl:choose>
		    	<xsl:when test="$toUpper='1'"><xsl:value-of select="local:toUpper(.)"/></xsl:when>
		    	<xsl:when test="$toLower='1'"><xsl:value-of select="local:toLower(.)"/></xsl:when>
		    	<xsl:otherwise>
		    		<xsl:choose>
		    			<xsl:when test="string-length($lineBreak) gt 0">
		    				<xsl:value-of select="replace(., '&#x0A;', $lineBreak)"/>
		    			</xsl:when>
		    			<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>		    		
		    		</xsl:choose>
		    	</xsl:otherwise>
		  	</xsl:choose>
		</xsl:element>
  	</xsl:template>
  	
  	<xsl:function name="local:isObitPackage">
  		<xsl:param name="pkg"/>
  		<xsl:choose>
  			<xsl:when test="$obitLevels[local:toUpper(.)=local:toUpper($pkg/level/@path)]">
  				<xsl:value-of select="1"/><!-- package in an obit level -->
  			</xsl:when>
  			<xsl:otherwise>
  				<xsl:value-of select="0"/><!-- not an obit package -->
  			</xsl:otherwise>
  		</xsl:choose>
  	</xsl:function>

  	<xsl:function name="local:isObitReady">
  		<xsl:param name="pkg"/>
  		
  		<xsl:variable name="pkgId" select="$pkg/obj_id"/>
  		<xsl:variable name="pkgName" select="$pkg/name"/>
  		<xsl:message>Checking obit pkg [<xsl:value-of select="concat($pkgName, ':', $pkgId)"/>] ...</xsl:message>  		
  		
  		<!-- 20140417: not required
  		<xsl:variable name="readyTextObjs"
  			select="$physPage//ncm-layout[objects-of-a-ncm-layout/ncm-object/sp_id=$pkgId 
  			and ncm-type-property/object-type/@id=1 and status/status/@name='READY']"/>
  		-->
  			
  		<xsl:choose>
  			<xsl:when test="local:toLower($pkg/extra-properties/POLOPOLY/NOWEB)='true'">
  				<xsl:message>Obit pkg [<xsl:value-of select="concat($pkgName, ':', $pkgId)"/>] 'NOWEB' meta is set to True. Skip export.</xsl:message>
  				<xsl:value-of select="0"/><!-- not ready -->
  			</xsl:when>
  			<!-- 20140417: not required
  			<xsl:when test="count($readyTextObjs) &lt;= 0">
  				<xsl:message>Obit pkg [<xsl:value-of select="concat($pkgName, ':', $pkgId)"/>] doesn't have any Text in READY status. Skip export.</xsl:message>
  				<xsl:value-of select="0"/>### not ready ###
  			</xsl:when>
  			-->
  			<xsl:otherwise>
  				<xsl:message>Obit pkg [<xsl:value-of select="concat($pkgName, ':', $pkgId)"/>] is ready for export.</xsl:message>
  				<xsl:value-of select="1"/><!-- ready -->
  			</xsl:otherwise>
  		</xsl:choose>
  	</xsl:function>  
</xsl:stylesheet>
