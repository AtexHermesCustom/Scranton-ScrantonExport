<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : util.xsl
    Description: 
    	Transform from Hermes to Merlin schema
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
    
	<xsl:function name="local:toUpper">
    	<xsl:param name="text"/>
		<xsl:value-of select="translate($text, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
  	</xsl:function>
  	
	<xsl:function name="local:toLower">
    	<xsl:param name="text"/>
		<xsl:value-of select="translate($text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
  	</xsl:function>  	

    <xsl:function name="local:formatDateNum"><!-- yyyyMMdd -->
        <xsl:param name="dateStr"/>
        <xsl:variable name="dateParts" select="tokenize(replace($dateStr, ',', ''), '\s+')"/>
        <xsl:variable name="month" select="local:shortMonthToNum($dateParts[1])"/>
        <xsl:variable name="day" select="if (string-length($dateParts[2])=1) then concat('0', $dateParts[2]) else $dateParts[2]"/>
        <xsl:variable name="year" select="$dateParts[3]"/>
        <xsl:value-of select="concat($year, $month, $day)"/>
    </xsl:function>    
    
    <xsl:function name="local:formatDateDelim"><!-- MM/dd/yyyy -->
        <xsl:param name="dateStr"/>
        <xsl:param name="delim"/>
        <xsl:variable name="dateParts" select="tokenize(replace($dateStr, ',', ''), '\s+')"/>
        <xsl:variable name="month" select="local:shortMonthToNum($dateParts[1])"/>
        <xsl:variable name="day" select="if (string-length($dateParts[2])=1) then concat('0', $dateParts[2]) else $dateParts[2]"/>
        <xsl:variable name="year" select="$dateParts[3]"/>
        <xsl:value-of select="concat($month, $delim, $day, $delim, $year)"/>
    </xsl:function>
    
    <xsl:function name="local:formatTimestamp"><!-- yyyyMMddThhmmss -->
        <xsl:param name="dateStr"/>
        <xsl:variable name="dateParts" select="tokenize(replace(replace($dateStr, ',', ''), ':', ' '), '\s+')"/>
        <xsl:variable name="month" select="local:shortMonthToNum($dateParts[1])"/>
        <xsl:variable name="day" select="if (string-length($dateParts[2])=1) then concat('0', $dateParts[2]) else $dateParts[2]"/>
        <xsl:variable name="year" select="$dateParts[3]"/>
		<xsl:variable name="mer" select="$dateParts[7]"/>
		<xsl:variable name="hour"
			select="if ($dateParts[4]='12' and $mer='AM') then '00'
				else if ($dateParts[4]='12' and $mer='PM') then '12'
				else if ($mer='PM') then (number($dateParts[4]) + 12)
				else if (string-length($dateParts[4])=1) then concat('0', $dateParts[4])
				else $dateParts[4]"/>
		<xsl:variable name="min" select="$dateParts[5]"/>
		<xsl:variable name="sec" select="$dateParts[6]"/>
        <xsl:value-of select="concat($year, $month, $day, 'T', $hour, $min, $sec)"/>
    </xsl:function>
    
    <xsl:function name="local:getTimestamp"><!-- yyyyMMddhhmmss -->
		<xsl:value-of select="format-dateTime(current-dateTime(), '[Y][M][D][H][m][s]')"/>
    </xsl:function>        
    
    <xsl:function name="local:shortMonthToNum">
        <xsl:param name="shortMonthStr"/>
        <xsl:choose>
            <xsl:when test="$shortMonthStr='Jan'">01</xsl:when>
            <xsl:when test="$shortMonthStr='Feb'">02</xsl:when>
            <xsl:when test="$shortMonthStr='Mar'">03</xsl:when>
            <xsl:when test="$shortMonthStr='Apr'">04</xsl:when>
            <xsl:when test="$shortMonthStr='May'">05</xsl:when>
            <xsl:when test="$shortMonthStr='Jun'">06</xsl:when>
            <xsl:when test="$shortMonthStr='Jul'">07</xsl:when>
            <xsl:when test="$shortMonthStr='Aug'">08</xsl:when>
            <xsl:when test="$shortMonthStr='Sep'">09</xsl:when>
            <xsl:when test="$shortMonthStr='Oct'">10</xsl:when>
            <xsl:when test="$shortMonthStr='Nov'">11</xsl:when>
            <xsl:when test="$shortMonthStr='Dec'">12</xsl:when>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="local:getSiteCode">
    	<xsl:param name="pub"/>
    	<xsl:choose>
		    <xsl:when test="$pub='PR_REPUBLICAN'">POTRH</xsl:when>
		    <xsl:when test="$pub='SH_NEWSITEM'">SHANI</xsl:when>
		    <xsl:when test="$pub='SC_TIMES_TRIB'">SCRTT</xsl:when>
		    <xsl:when test="$pub='WB_VOICE'">WBCV</xsl:when>
		    <xsl:when test="$pub='TW_REVIEW'">TOWDR</xsl:when>
		    <xsl:when test="$pub='PI_PROGINDEX'">PETPI</xsl:when>
		    <xsl:when test="$pub='HZ_STANDSPEAK'">HAZSS</xsl:when>
		    <xsl:when test="$pub='VI_DAILYNEWS'">VIDN</xsl:when>
		    <xsl:otherwise>UNKNOWN_SITE</xsl:otherwise>
    	</xsl:choose>
    </xsl:function>
    
    <xsl:function name="local:substring-before-last">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:param name="substr" as="xs:string"/>
    	<xsl:variable name="substrEsc" select="local:esc-regex-chars($substr)"/>
		<xsl:value-of 
			select="if ($substr) then 
						if (contains($input, $substr)) then string-join(tokenize($input, $substrEsc)[position() != last()], $substr) 
                  		else ''
               		else $input"/>
    </xsl:function>    
    
    <xsl:function name="local:substring-after-last">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:param name="substr" as="xs:string"/>
    	<xsl:variable name="substrEsc" select="local:esc-regex-chars($substr)"/>
 	  	<xsl:value-of 
    		select="if ($substr) then
						if (contains($input, $substr)) then tokenize($input, $substrEsc)[last()] 
               			else '' 
            		else $input"/>
    </xsl:function>
    
    <xsl:function name="local:esc-regex-chars">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:choose>
    		<xsl:when test="$input = '.'">\.</xsl:when>
    		<xsl:otherwise><xsl:value-of select="$input"/></xsl:otherwise>
    	</xsl:choose>
    </xsl:function>    
    
    <xsl:function name="local:getTextContent">
    	<xsl:param name="node"/>
      	<xsl:for-each select="$node//text()">
    		<xsl:if test="string-length(.) gt 0">
    			<xsl:value-of select="."/>
    		</xsl:if>
      	</xsl:for-each>
    </xsl:function>	
        
</xsl:stylesheet>