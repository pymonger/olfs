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
package opendap.gateway.dapResponders;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import opendap.gateway.BesGatewayApi;
import opendap.gateway.HttpResponder;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformer;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;





public class RDF extends HttpResponder {
    private Logger log;


    private static String defaultRegex = ".*\\.rdf";


    public RDF(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public RDF(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();


        String dataSourceUrl = BesGatewayApi.getDataSourceUrl(request, getPathPrefix());

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        log.debug("respondToHttpRequest() Sending RDF for dataset: " + dataSource);




        String xdap_accept = "3.2";
        Document reqDoc = BesGatewayApi.getRequestDocument(
                                                        BesGatewayApi.DDX,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        xmlBase,
                                                        null,
                                                        null,
                                                        BesGatewayApi.DAP2_ERRORS);



        log.debug("BesGatewayApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        Document ddx = new Document();
        if(!BesGatewayApi.besTransaction(dataSource,reqDoc,ddx)){
            BESError besError = new BESError(xmlo.outputString(ddx));
            besError.sendErrorResponse(_systemPath,response);
            log.error("sendDDX() encountered a BESError:\n" + xmlo.outputString(ddx));
            return;
        }


        ddx.getRootElement().setAttribute("dataset_id",dataSource);

        log.debug(xmlo.outputString(ddx));





        String currentDir = System.getProperty("user.dir");
        String xslDir = _systemPath + "/docs/xsl";
        log.debug("Cached working directory: "+currentDir);

        log.debug("Changing working directory to "+ xslDir);
        System.setProperty("user.dir",xslDir);

        String xsltDocName = "dap_3.2_ddxToRdfTriples.xsl";
        SAXBuilder sb = new SAXBuilder();
        Document xsltDoc = sb.build(xsltDocName);

        log.debug(xmlo.outputString(xsltDoc));

        XSLTransformer transformer = new XSLTransformer(xsltDoc);




        String accepts = request.getHeader("Accepts");

        if(accepts!=null && accepts.equalsIgnoreCase("application/rdf+xml"))
            response.setContentType("application/rdf+xml");
        else
            response.setContentType("text/xml");

        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "text/xml");


        Document rdf = transformer.transform(ddx);



        OutputStream os = response.getOutputStream();
        xmlo.output(rdf,os);
        os.flush();
        log.info("Sent RDF version of DDX.");
        log.debug("Restoring working directory to "+ currentDir);
        System.setProperty("user.dir",currentDir);

    }



}