<!--
/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
-->
<!DOCTYPE xsl:stylesheet [
        <!ENTITY DAPOBJ  "http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#" >
        <!ENTITY DAP     "http://xml.opendap.org/ns/DAP/3.2#" >
        <!ENTITY XSD     "http://www.w3.org/2001/XMLSchema#" >
        ]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:xml="http://www.w3.org/XML/1998/namespace"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
        >

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>


    <xsl:include href="xml2rdf.xsl"/>


    <xsl:template match="/">
        <rdf:RDF>
            <rdf:Description rdf:about="">
                <xsl:apply-templates mode="xml2rdf"/>               
            </rdf:Description>
        </rdf:RDF>
    </xsl:template>

</xsl:stylesheet>