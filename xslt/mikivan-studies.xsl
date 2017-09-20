<?xml version="1.1" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:variable name="HEX0">0123456769ABCDEF</xsl:variable>
    <xsl:variable name="HEX1">123456769ABCDEF0</xsl:variable>


    <xsl:template match="/NativeDicomModel">
        <xsl:text>{</xsl:text>

        <xsl:apply-templates mode="mikivan" select="DicomAttribute"/>

        <xsl:text>},</xsl:text>
    </xsl:template>



    <xsl:template match="DicomAttribute" mode="mikivan">

        <xsl:choose>

            <xsl:when test="@tag='0020000D'">
                <xsl:text>"0020000D": {Ok},</xsl:text>
            </xsl:when>

            <xsl:when test="@tag='00080020'">
                <xsl:text>"00080020": {Ok},</xsl:text>
            </xsl:when>

            <xsl:when test="@tag='00080030'">
                <xsl:text>"00080030": {Ok},</xsl:text>
            </xsl:when>

            <xsl:otherwise>"other":{},</xsl:otherwise>

        </xsl:choose>

    </xsl:template>



</xsl:stylesheet>