<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : merlin-transform-photo.xsl
    Description: 
    	Transform from Hermes to Merlin
    Revision History:
        20120710 jpm - creation
-->

<!DOCTYPE xsl:stylesheet [
  <!ENTITY tab "&#x9;">
  <!ENTITY crlf "&#xD;&#xA;">
]>

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
    xmlns:err="http://www.w3.org/2005/xqt-errors"
    xmlns:local="http://www.atex.com/local"
    xmlns:worker="java:com.atex.h11.custom.scranton.export.merlin.Worker"
    exclude-result-prefixes="xsl xs xdt err fn local">
        
    <xsl:import href="util.xsl"/>    
    
	<xsl:output method="text" encoding="WINDOWS-1252"/>
	
	<xsl:variable name="companyCopyright">Times-Shamrock</xsl:variable>
    
	<xsl:template match="item-photo">
		<xsl:text>ImageName&tab;</xsl:text><xsl:value-of select="./ImageName"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>UniqueId&tab;</xsl:text><xsl:value-of select="./UniqueId"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Productname&tab;</xsl:text><xsl:value-of select="./Productname"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Pub_date&tab;</xsl:text><xsl:value-of select="./Pub_date"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Section&tab;</xsl:text><xsl:value-of select="./Section"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Zone&tab;</xsl:text><xsl:value-of select="./Zone"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Edition&tab;</xsl:text><xsl:value-of select="./Edition"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Layout_Desk&tab;</xsl:text><xsl:value-of select="worker:getDepartmentName(./Layout_Desk)"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Page_Name&tab;</xsl:text><xsl:value-of select="./Page_Name"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Article_Name&tab;</xsl:text><xsl:value-of select="./Article_Name"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Pub_Caption&tab;</xsl:text><xsl:value-of select="local:getTextContent(./Pub_Caption/caption/*)"/><xsl:text>&crlf;</xsl:text>
		<xsl:variable name="iptcCopyright" select="worker:getIptcCopyrightNotice(./CopyrightSourceFile)"/>
		<xsl:text>Copyright&tab;</xsl:text><xsl:value-of select="$iptcCopyright"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>Rights&tab;</xsl:text>
			<xsl:choose>
				<xsl:when test="local:toUpper(normalize-space($iptcCopyright))=local:toUpper($companyCopyright)">1</xsl:when>
				<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
		<xsl:text>&crlf;</xsl:text>
		<xsl:text>CropInfo1&tab;</xsl:text><xsl:value-of select="./CropInfo1"/><xsl:text>&crlf;</xsl:text>
		<xsl:text>CropInfo2&tab;</xsl:text><xsl:value-of select="./CropInfo2"/><xsl:text>&crlf;</xsl:text>
	</xsl:template>

</xsl:stylesheet>
