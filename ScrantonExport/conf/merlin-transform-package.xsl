<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : merlin-transform-package.xsl
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
         
    <xsl:param name="styleFile" select="'incopy.xml'"/>
    
  	<xsl:variable name="tagRules" select="doc($styleFile)/configuration/tag-rules/rule"/>
  	<!-- <xsl:variable name="lineBreak" select="'&lt;br/&gt;'"/> -->
  	<xsl:variable name="lineBreak" select="'&#x0A;'"/>

	<xsl:template match="/">
		<items>
			<xsl:choose>	
				<!-- Story package -->
				<xsl:when test="./ncm-object/ncm-type-property/object-type/@id=17">
					<xsl:apply-templates select="./ncm-object" mode="main-story-package"/>
		    	</xsl:when>
		    
		    	<!-- Stand-alone image "package" -->
		    	<xsl:when test="./ncm-object/ncm-type-property/object-type/@id=6">
			   		<xsl:apply-templates select="./ncm-object" mode="item-photo">
			   			<xsl:with-param name="pageInfo" select="./ncm-object/page-info"/>
			   			<xsl:with-param name="pkgId" select="./ncm-object/obj_id"/>
			   			<xsl:with-param name="pkgName" select="./ncm-object/name"/>
			   			<xsl:with-param name="caption" select="''"/><!-- no related caption -->
			   		</xsl:apply-templates>		
				</xsl:when>
				
			    <!-- Exception -->
			    <xsl:otherwise>
			    	<xsl:message>Unexpected object type encountered: <xsl:value-of select="ncm-object/ncm-type-property/object-type/@id"/></xsl:message>
			    </xsl:otherwise>				
			</xsl:choose>
		</items>
	</xsl:template>

    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=17]" mode="main-story-package">
        <xsl:variable name="pageInfo" select="./page-info"/>
        <xsl:variable name="pkgId" select="./obj_id"/>
        <xsl:variable name="pkgName" select="./name"/>
            
        <!-- story content -->
    	<xsl:apply-templates select="." mode="item-story">
			<xsl:with-param name="pageInfo" select="$pageInfo"/>    	
			<xsl:with-param name="pkgId" select="$pkgId"/>
			<xsl:with-param name="pkgName" select="$pkgName"/>
    	</xsl:apply-templates>
    	
        <!-- photos -->
        <!-- get master channel images: need to export the untoned images -->
       	<xsl:for-each select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=6 
       		and variant_type/@name='']">
       		<xsl:variable name="objId" select="./obj_id"/>
       		<xsl:variable name="objIdPrint" 
       			select="./variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT']/obj_id[1]"/>

       		<!-- caption -->
       		<!-- if the print caption exists, export it, otherwise export the master caption-->
       		<xsl:variable name="caption">
       			<xsl:choose>
       				<xsl:when test="$objIdPrint != '' and /ncm-object/child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=3
       						and relation_obj_id=$objIdPrint]">
       					<xsl:copy-of select="/ncm-object/child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=3
       						and relation_obj_id=$objIdPrint][1]"/>
       				</xsl:when>
       				<xsl:otherwise>
       					<xsl:copy-of select="/ncm-object/child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=3
       						and relation_obj_id=$objId][1]"/>
       				</xsl:otherwise>       			
       			</xsl:choose>
       		</xsl:variable>
       		
       		<xsl:apply-templates select="." mode="item-photo">
       			<xsl:with-param name="pageInfo" select="$pageInfo"/>
       			<xsl:with-param name="pkgId" select="$pkgId"/>
       			<xsl:with-param name="pkgName" select="$pkgName"/>
       			<xsl:with-param name="caption" select="$caption/node()"/>
       		</xsl:apply-templates>
       	</xsl:for-each>
    </xsl:template>
   
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=17]" mode="item-story">
        <xsl:param name="pageInfo"/>
		<xsl:param name="pkgId"/>
		<xsl:param name="pkgName"/>		
        <xsl:variable name="pub" select="$pageInfo/pub"/>
        <xsl:variable name="edition" select="$pageInfo/edition"/>
        
        <item-story>
        	<!-- output filename -->
        	<xsl:processing-instruction name="file-name" 
        		select="concat($pub, '-', local:formatDateNum($pageInfo/pubdate), '-', $pageInfo/section, 
        			'-Pg', $pageInfo/page-number, '-', $pkgName, '-', $pkgId, '.xml')"/>
        			
        	<item-content>
        		<!-- Objects to include in the export:
        			- print variants, i.e. paginated objects
        			- master variants without any print variant, i.e. non-paginated objects
        		 -->
        	
        		<!-- headline objects -->
        		<xsl:apply-templates 
        			select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=2 
        				and (variant_type/@name='PRINT' or (variant_type/@name='' and not(variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT'])))]" 
        			mode="content"/>
        		<!-- summary objects -->
        		<xsl:apply-templates 
        			select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=14
        				and (variant_type/@name='PRINT' or (variant_type/@name='' and not(variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT'])))]" 
        			mode="content"/>
				<!-- caption objects -->
				<xsl:apply-templates 
					select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=3
						and (variant_type/@name='PRINT' or (variant_type/@name='' and not(variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT'])))]" 
					mode="content"/>
				<!-- header objects -->
				<xsl:apply-templates 
					select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=4
						and (variant_type/@name='PRINT' or (variant_type/@name='' and not(variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT'])))]" 
					mode="content"/>
				<!-- text objects -->
				<xsl:apply-templates 
					select="./child-of-a-ncm-sp-object/ncm-object[ncm-type-property/object-type/@id=1
						and (variant_type/@name='PRINT' or (variant_type/@name='' and not(variants-of-a-ncm-object/ncm-object[variant_type/@name='PRINT'])))]"  
					mode="content"/>
        	</item-content>
        	
        	<SOURCE><xsl:value-of select="$pub"/></SOURCE>
        	<SERVICE><xsl:value-of select="local:getSiteCode($pub, $edition)"/></SERVICE>
        	<PUBDATE><xsl:value-of select="$pageInfo/pubdate"/></PUBDATE>
        	<EDITION><xsl:value-of select="$edition"/></EDITION>
        	<SECTION></SECTION>
        	<ZONE><xsl:value-of select="$pageInfo/section"/></ZONE>
        	<PAGE><xsl:value-of select="format-number($pageInfo/page-number, '00')"/></PAGE>
        	<OBJECT><xsl:value-of select="concat(substring($pub, 1, 3), $pkgId)"/></OBJECT>
        	<REFERENCE><xsl:value-of select="$pkgName"/></REFERENCE>
        	<LASTEDITOR><xsl:value-of select="./modifier/name"/></LASTEDITOR>
        	<LASTEDTIME><xsl:value-of select="local:formatTimestamp(./modifier_ts)"/></LASTEDTIME>
        	<CATEGORY></CATEGORY>
        	<CRHOLDER></CRHOLDER>
        	<CREDIT></CREDIT>
        	<CITY></CITY>
        	<CAPTION></CAPTION>
        	<BYLINE></BYLINE>
        	<BYLINETITLE></BYLINETITLE>
        	<WEBBYLINE><xsl:value-of select="./extra-properties/POLOPOLY/WEBBYLINE"/></WEBBYLINE>
        	<HEADLINE></HEADLINE>
        	<STORY></STORY>
        </item-story>
    </xsl:template>
       
	<xsl:template match="ncm-object[ncm-type-property/object-type/@id=6]" mode="item-photo">
        <xsl:param name="pageInfo"/>
		<xsl:param name="pkgId"/>
		<xsl:param name="pkgName"/>		
		<xsl:param name="caption"/>
        <xsl:variable name="pub" select="$pageInfo/pub"/>
        <xsl:variable name="edition" select="$pageInfo/edition"/>
        <xsl:variable name="objId" select="./obj_id"/>
        <xsl:variable name="objName" select="./name"/>
        
        <item-photo>
        	<xsl:variable name="photoName" 
        		select="concat($pub, '-', local:formatDateNum($pageInfo/pubdate), '-', $pageInfo/section, 
        			'-Pg', $pageInfo/page-number, '-', $objName, '-', $objId)"/>
        	<xsl:variable name="photoExt" 
        		select="local:substring-after-last(./content-property/file-property/original-file/server-path, '.')"/>

			<!-- ascii output filename -->
        	<xsl:processing-instruction name="file-name" select="concat($photoName, '.pd')"/>
        	<!-- photo source -->
        	<xsl:processing-instruction name="photo-source-file" select="./content-property/file-property/original-file/server-path"/>
        	<!-- photo output filename -->
        	<xsl:processing-instruction name="photo-file-name" select="concat($photoName, '.', $photoExt)"/>
			<!-- cropping/transformation info -->
            <xsl:processing-instruction name="dimension" select="concat(./content-property/image-size/width, ' ', ./content-property/image-size/height)"/>
            <xsl:if test="./content-property/crop-rect">
                <xsl:processing-instruction name="crop-rect" select="concat(./content-property/crop-rect/@bottom, ' ', ./content-property/crop-rect/@left, ' ', ./content-property/crop-rect/@top, ' ', ./content-property/crop-rect/@right)"/>
            </xsl:if>
            <xsl:if test="./content-property/xy-transf">
                <xsl:processing-instruction name="rotation" select="./content-property/xy-transf/@rotate"/>
                <xsl:processing-instruction name="flip-x" select="./content-property/xy-transf/@flip-x"/>
                <xsl:processing-instruction name="flip-y" select="./content-property/xy-transf/@flip-y"/>
            </xsl:if>					
        		
        	<image>
	    		<xsl:attribute name="id" select="./obj_id"/>
	    		<xsl:attribute name="name" select="./name"/>
	    		<xsl:attribute name="channel" select="./variant_type/@name"/>
        	</image>
        	<ImageName><xsl:value-of select="$photoName"/></ImageName>
        	<UniqueId><xsl:value-of select="concat(substring($pub, 1, 3), local:getTimestamp(), $objId)"/></UniqueId>
        	<Productname><xsl:value-of select="local:getSiteCode($pub, $edition)"/></Productname>
        	<Pub_date><xsl:value-of select="local:formatDateDelim($pageInfo/pubdate, '-')"/></Pub_date>
        	<Section><xsl:value-of select="$pageInfo/section"/></Section>
        	<Zone><xsl:value-of select="$pageInfo/section"/></Zone>
        	<Edition><xsl:value-of select="$edition"/></Edition>
        	<Layout_Desk><xsl:value-of select="./department_id"/></Layout_Desk>
        	<Page_Name><xsl:value-of select="format-number($pageInfo/page-number, '00')"/></Page_Name>
        	<Article_Name><xsl:value-of select="$pkgName"/></Article_Name>
        	<Pub_Caption>
        		<xsl:if test="$caption">
        			<xsl:apply-templates select="$caption" mode="content"/>
        		</xsl:if>	
        	</Pub_Caption>
        	<CopyrightSourceFile><xsl:value-of select="./content-property/file-property/original-file/server-path"/></CopyrightSourceFile>
        	<CropInfo1></CropInfo1>
        	<CropInfo2></CropInfo2>
        </item-photo>	
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
  
</xsl:stylesheet>
