/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) $year OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.bes.dap2Responders.*;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.DataResponse.NormativeDR;
import opendap.bes.dap4Responders.DatasetMetadata.HtmlDMR;
import opendap.bes.dap4Responders.DatasetMetadata.NormativeDMR;
import opendap.bes.dap4Responders.DatasetServices.NormativeDSR;
import opendap.bes.dap4Responders.FileAccess;
import opendap.bes.dap4Responders.Iso19115.IsoDMR;
import opendap.bes.dap4Responders.Iso19115.IsoRubricDMR;
//import opendap.bes.dap4Responders.DataResponse.JsonDR;
import opendap.bes.dap4Responders.Version;
import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ServletUtil;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/11/11
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class BesDapDispatcher implements DispatchHandler {

    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;

    private String systemPath;
    private Element _config;
    private Vector<Dap4Responder> responders;
    private static boolean _allowDirectDataSourceAccess = false;
    private static boolean _useDAP2ResourceUrlResponse = false;


    private BesApi _besApi;



    public BesDapDispatcher() {
        log = LoggerFactory.getLogger(getClass());
        responders = new Vector<Dap4Responder>();

    }


    public String getSystemPath(){
        return systemPath;

    }

    public static boolean allowDirectDataSourceAccess() {
        return _allowDirectDataSourceAccess;
    }

    public static boolean useDAP2ResourceUrlResponse() {
        return _useDAP2ResourceUrlResponse;
    }


    protected Vector<Dap4Responder> getResponders() {
        return responders;
    }

    protected void addResponder(Dap4Responder r) {
        responders.add(r);
    }


    public BesApi getBesApi(){
        return _besApi;
    }

    public void setBesApi(BesApi besApi){
        _besApi = besApi;
    }

    public void init(HttpServlet servlet, Element config) throws Exception {

        BesApi besApi = new BesApi();

        init(servlet, config, besApi);


    }


    private void ingestConfig(Element config) throws Exception {

        if(config!=null){
            _config = config;

            Element besApiImpl = _config.getChild("BesApiImpl");
            if (besApiImpl != null) {
                String className = besApiImpl.getTextTrim();
                log.debug("Building BesApi: " + className);
                Class classDefinition = Class.forName(className);

                Object classInstance = classDefinition.newInstance();

                if (classInstance instanceof BesApi) {
                    log.debug("Loading BesApi from configuration.");
                    BesApi besApi = (BesApi) classDefinition.newInstance();
                    setBesApi(besApi);
                }

            }


            _allowDirectDataSourceAccess = false;
            Element dv = _config.getChild("AllowDirectDataSourceAccess");
            if (dv != null) {
                _allowDirectDataSourceAccess = true;
            }


            _useDAP2ResourceUrlResponse = false;
            dv = _config.getChild("UseDAP2ResourceUrlResponse");
            if (dv != null) {
                _useDAP2ResourceUrlResponse = true;
            }


            dv = _config.getChild("PostBodyMaxLength");
            if (dv != null) {
                try {
                    int maxLength = Integer.parseInt(dv.getTextTrim());
                    ReqInfo.setMaxPostBodyLength(maxLength);
                }
                catch(NumberFormatException e){
                    log.warn("Unable to parse the value of MaxPostBodyLength! Value: {} ",dv.getTextTrim());

                }
            }
            log.info("PostBodyMaxLength is set to {}", ReqInfo.getPostBodyMaxLength());

        }





    }

    /**
     *  This method is where the behavior of the BesDapDispatcher is defined. In here the various Responder classes
     *  are instantiated and loaded in to an ordered list. The types of the responders and their order defines the
     *  behaviour of the DAP dispatch activity.
     * @param servlet    The Servlet instance that this dispatcher is running in.
     * @param config  The configuration element loaded from the olfs.xml file for this dispatcher
     * @param besApi    The BesApi instance to use when servicing requests.
     * @throws Exception  When the bad things happen.
     */
    protected void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {

        if (initialized) return;

        setBesApi(besApi);

        ingestConfig(config);

        log.debug("Using BesApi implementation: {}", getBesApi().getClass().getName());

        dispatchServlet = servlet;

        systemPath = ServletUtil.getSystemPath(dispatchServlet, "");



        // DAP4 Responses
        responders.add(new NormativeDR(systemPath, besApi));
        responders.add(new NormativeDMR(systemPath, besApi));
        responders.add(new IsoDMR(systemPath, besApi));

        responders.add(new Version(systemPath, besApi));
        if (!_useDAP2ResourceUrlResponse) {

            FileAccess dfa = new FileAccess(systemPath, besApi);
            dfa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);
            responders.add(dfa);

            responders.add(new NormativeDSR(systemPath, besApi, responders));
        }



        // DAP2 Data Responses
        responders.add(new Dap2Data(systemPath, besApi));
        responders.add(new Ascii(systemPath, besApi));
        //responders.add(new Ascii(systemPath, null, ".asc", besApi)); // We can uncomment this if we want to support both the dap2 ".ascii" suffix and ".asc"
        responders.add(new CsvData(systemPath, besApi));
        responders.add(new Netcdf3(systemPath, besApi));
        responders.add(new Netcdf4(systemPath, besApi));
        responders.add(new XmlData(systemPath, besApi));


        // DAP2 GeoTIFF Response
        Dap4Responder geoTiff = new GeoTiff(systemPath, besApi);
        responders.add(geoTiff);


        // DAP2 JPEG2000 Response
        Dap4Responder jp2 = new GmlJpeg2000(systemPath, besApi);
        responders.add(jp2);

        // DAP2 w10n JSON Response
        Dap4Responder json = new Json(systemPath, besApi);
        responders.add(json);

        // DAP2 Instance Object JSON Response
        Dap4Responder ijsn = new Ijson(systemPath, besApi);
        responders.add(ijsn);


        // DAP2 Metadata responses
        Dap4Responder d4r = new DDX(systemPath, besApi);
        responders.add(d4r);
        responders.add(new DDS(systemPath, besApi));
        responders.add(new DAS(systemPath, besApi));
        responders.add(new RDF(systemPath, besApi));
        responders.add(new DatasetInfoHtmlPage(systemPath, besApi));

        Dap4Responder iso = new Iso19115(systemPath, besApi);
        responders.add(iso);

        Dap4Responder rubric = new IsoRubricDMR(systemPath, null, ".rubric", besApi);
        rubric.clearAltResponders();
        rubric.setCombinedRequestSuffixRegex(rubric.buildRequestMatchingRegex());
        responders.add(rubric);



        if (_useDAP2ResourceUrlResponse) {

            // Add the HTML form conditionally because the ".html" suffix is used
            // by the NormativeDSR's HTML representation. Since we aren't using the DSR response
            // We should make sure that the old HTML ".html" response is available.
            Dap4Responder htmlForm = new DatasetHtmlForm(systemPath, besApi);
            responders.add(htmlForm);

            FileAccess d4fa = new FileAccess(systemPath, null, "", besApi);
            d4fa.clearAltResponders();
            d4fa.setCombinedRequestSuffixRegex(d4fa.buildRequestMatchingRegex());
            d4fa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);
            responders.add(d4fa);
        }



        log.info("Initialized. Direct Data Source Access: " + (_allowDirectDataSourceAccess ? "Enabled" : "Disabled") + "  " +
                "Resource URL returns: " + (_useDAP2ResourceUrlResponse ? "DAP2 File Response" : "DAP4 Service Description"));

        initialized = true;


    }


    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        log.debug("************************************************************");
        if (requestDispatch(request, null, false)) {
            log.debug("Request can be handled.");
            return true;
        }
        log.debug("Request can not be handled.");
        log.debug("************************************************************");
        return false;
    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        if (!requestDispatch(request, response, true)) {
            log.error("Unable to service request.");
        }


    }


    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        // String dataSource = getBesApi().getBesDataSourceID(relativeUrl, false);

        log.debug("The client requested this resource: {}",relativeUrl);

        for (HttpResponder r : responders) {
            log.debug("Checking responder: "+ r.getClass().getSimpleName()+ " (pathPrefix: "+r.getPathPrefix()+")");
            if (r.matches(relativeUrl)) {

                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                if (sendResponse){

                    r.respondToHttpGetRequest(request, response);

                }

                return true;
            }
        }


        return false;

    }


    public long getLastModified(HttpServletRequest req) {



        String relativeUrl = ReqInfo.getLocalUrl(req);


        if(!initialized)
            return -1;


        for (HttpResponder r : responders) {
            if (r.matches(relativeUrl)) {
                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                try {

                    long lmt =  r.getLastModified(req);
                    log.debug("getLastModified(): Returning: {}", new Date(lmt));
                    return lmt;

                } catch (Exception e) {
                    log.debug("getLastModified(): Returning: -1");
                    return -1;
                }

            }

        }

        return -1;


    }



    public void destroy() {
        log.info("Destroy complete.");

    }


}