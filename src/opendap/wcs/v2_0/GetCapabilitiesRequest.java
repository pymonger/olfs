/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
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
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.wcs.v2_0;

import opendap.coreServlet.Scrub;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:00:35 PM
 */
public class GetCapabilitiesRequest {



    private static final String   _request = "GetCapabilities";

    private String          updateSequence = null;
    private String[]        AcceptVersions = null;
    private HashSet<String> Sections = new HashSet<String>();
    private boolean hasSectionsElement;

    private String[]        AcceptFormats = null;
    private String[]        AcceptLanguages = null;


    public static String SERVICE_IDENTIFICATION = "ServiceIdentification";
    public static String SERVICE_PROVIDER = "ServiceProvider";
    public static String OPERATIONS_METADATA = "OperationsMetadata";
    public static String SERVICE_METADATA = "ServiceMetadata";
    public static String CONTENTS = "Contents";
    public static String ALL = "All";


    public static final HashSet<String> sectionNames = new HashSet<String>();
    static {
        sectionNames.add(SERVICE_IDENTIFICATION);
        sectionNames.add(SERVICE_PROVIDER);
        sectionNames.add(OPERATIONS_METADATA);
        sectionNames.add(CONTENTS);
        sectionNames.add(ALL);
    }


    


    public String getUpdateSequence() {
        return updateSequence;
    }

    public void setUpdateSequence(String updateSequence) {
        this.updateSequence = updateSequence;
    }

    public String[] getAcceptVersions() {
        return AcceptVersions;
    }

    public void setAcceptVersions(String[] acceptVersions) {
        AcceptVersions = acceptVersions;
    }

    public Iterator<String> getSections() {
        return Sections.iterator();
    }

    public boolean hasSection(String s) {
        return Sections.contains(s);
    }

    public boolean sectionsIsEmtpty() {
        return Sections.isEmpty();
    }

    public boolean hasSectionsElement() {
        return hasSectionsElement;
    }


    public String[] getAcceptFormats() {
        return AcceptFormats;
    }

    public void setAcceptFormats(String[] acceptFormats) {
        AcceptFormats = acceptFormats;
    }

    public GetCapabilitiesRequest(Element getCRElem) throws WcsException{

        Element e;
        String s;
        Iterator i;
        int index;


        // Make sure we got the correct request object.
        WCS.checkNamespace(getCRElem,_request,WCS.WCS_NS);

        // Make sure the client is looking for a WCS service....
        WCS.checkService(getCRElem.getAttributeValue("service"));


        s=getCRElem.getAttributeValue("updateSequence");
        if(s!=null)
            updateSequence =s;

        // Get the clients accepted versions.
        e = getCRElem.getChild("AcceptVersions",WCS.WCS_NS);
        if(e!=null ){
            List vlist = e.getChildren("Version",WCS.WCS_NS);
            if(vlist.size()==0){
                throw new WcsException("The ows:AcceptVersions element is required " +
                        "to have one or more ows:Version child elements.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "ows:Version");
            }
            s = "";
            AcceptVersions =  new String[vlist.size()];
            i = vlist.iterator();
            index = 0;
            while(i.hasNext()){
                e = (Element) i.next();
                AcceptVersions[index++] = e.getTextNormalize();
                s += s.equals("")?s:", "+ e.getTextNormalize();
            }

            boolean compatible = false;
            for(String version:AcceptVersions){
                if(WCS.CURRENT_VERSION.equals(version)){
                    compatible=true;
                }
            }
            if(!compatible)
                throw new WcsException("Client requested unsupported WCS " +
                        "version(s): ["+s+"]\nThis WCS supports version(s) "+WCS.CURRENT_VERSION,
                        WcsException.VERSION_NEGOTIATION_FAILED,null);

        }

        // Get the sections the client wants.
        e = getCRElem.getChild("Sections",WCS.WCS_NS);
        if(e!=null ){

            hasSectionsElement = true;

            List vlist = e.getChildren("Section",WCS.WCS_NS);

            i = vlist.iterator();
            index = 0;
            while(i.hasNext()){
                e = (Element) i.next();
                s = e.getTextNormalize();
                if(!sectionNames.contains(s))
                    throw new WcsException("Client requested unsupported " +
                            "section name: "+ Scrub.simpleString(s),
                            WcsException.INVALID_PARAMETER_VALUE,"ows:Section");
                Sections.add(e.getTextNormalize());
            }

        }



        // Get the formats the client wants.
        e = getCRElem.getChild("AcceptFormats",WCS.WCS_NS);
        if(e!=null ){
            List vlist = e.getChildren("OutputFormat",WCS.WCS_NS);
            if(vlist.size()==0){
                throw new WcsException("The ows:AcceptFormats element is required to have " +
                        "one or more ows:OutputFormat child elements.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "ows:OutputFormat");
            }

            AcceptFormats =  new String[vlist.size()];
            i = vlist.iterator();
            index = 0;
            while(i.hasNext()){
                e = (Element) i.next();
                AcceptFormats[index++] = e.getTextNormalize();

            }

        }

        // Get the formats the client wants.
        e = getCRElem.getChild("AcceptLanguages",WCS.WCS_NS);
        if(e!=null ){
            List vlist = e.getChildren("Language",WCS.WCS_NS);
            if(vlist.size()==0){
                throw new WcsException("The ows:AcceptFormats element is required to have " +
                        "one or more ows:OutputFormat child elements.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "ows:OutputFormat");
            }

            AcceptLanguages =  new String[vlist.size()];
            i = vlist.iterator();
            index = 0;
            while(i.hasNext()){
                e = (Element) i.next();
                AcceptLanguages[index++] = e.getTextNormalize();

            }

        }
    }







    public GetCapabilitiesRequest(Map<String,String[]> kvp)
            throws WcsException {

        String tmp[], s[];

        // Make sure the client is looking for a WCS service....
        s = kvp.get("service");
        WCS.checkService(s==null? null : s[0]);

        // Make sure the client is actually asking for this operation
        s = kvp.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(!s[0].equalsIgnoreCase(_request)){
            throw new WcsException("The servers internal dispatch operations " +
                    "have failed. The WCS request for the operation '"+s+"' " +
                    "has been incorrectly routed to the 'GetCapabilities' " +
                    "request processor.",
                    WcsException.NO_APPLICABLE_CODE);
        }



        // Make sure the client can accept the correct WCS version...
        boolean compatible = false;
        s = kvp.get("AcceptVersions".toLowerCase());
        if(s!=null){
            tmp = s[0].split(",");
            for(String ver:tmp){
                if(WCS.CURRENT_VERSION.equals(ver)){
                    compatible=true;
                }
            }
            if(!compatible)
                throw new WcsException("Client requested unsupported WCS " +
                        "version(s) ["+s[0]+"]\nThis WCS supports version(s) "+WCS.CURRENT_VERSION,
                        WcsException.VERSION_NEGOTIATION_FAILED,null);

            AcceptVersions = tmp;
        }





        // Get the list of section the client has requested. Returning
        // individual sections may not be supported, but we'll keep track of
        // that partof the request regardless.
        s = kvp.get("Sections".toLowerCase());
        if(s!=null){
            hasSectionsElement = true;

            tmp = s[0].split(",");
            for(String section:tmp){
                if(!sectionNames.contains(section)){
                    throw new WcsException("Client requested unsupported " +
                            "section name: "+ Scrub.simpleString(section),
                            WcsException.INVALID_PARAMETER_VALUE,"Sections");
                }
                Sections.add(section);
            }

        }

        // Store the updatSequence information in the event that the server
        // supports it at some point...
        s = kvp.get("updateSequence".toLowerCase());
        updateSequence = s==null? null : s[0];


        // Store the AccptedFormats offered by the client in the event that the
        // the server eventually supports more than text/html
        s = kvp.get("AcceptFormats".toLowerCase());
        if(s!=null){
            tmp = s[0].split(",");
            AcceptFormats = tmp;

        }



        // Store the AccptedFormats offered by the client in the event that the
        // the server eventually supports more than text/html
        s = kvp.get("AcceptLanguages".toLowerCase());
        if(s!=null){
            tmp = s[0].split(",");
            AcceptLanguages = tmp;

        }


    }

    public Element getRequestElement(){

        Element requestElement;
        String _schemaLocation = "";

        requestElement = new Element(_request, WCS.WCS_NS);
        _schemaLocation += WCS.WCS_NAMESPACE_STRING + "  " +WCS.WCS_SCHEMA_LOCATION_BASE+"wcsGetCapabilities.xsd  ";

        requestElement.addNamespaceDeclaration(WCS.XSI_NS);

        requestElement.addNamespaceDeclaration(WCS.OWS_NS);
        _schemaLocation += WCS.OWS_NAMESPACE_STRING + "  " +WCS.OWS_SCHEMA_LOCATION_BASE+"owsGetCapabilities.xsd  ";

        requestElement.setAttribute("schemaLocation", _schemaLocation,WCS.XSI_NS);


        requestElement.setAttribute("service", WCS.SERVICE);

        if(updateSequence !=null)
            requestElement.setAttribute("updateSequence", updateSequence);



        if(AcceptVersions != null){
            Element av = new Element("AcceptVersions",WCS.WCS_NS);
            Element ver;
            for(String v: AcceptVersions){
                ver = new Element("Version",WCS.WCS_NS);
                ver.setText(v);
                av.addContent(ver);
            }
            requestElement.addContent(av);
        }

        if(!Sections.isEmpty()){
            Element sections = new Element("Sections",WCS.WCS_NS);
            Element sec;
            for(String sv : Sections){
                sec = new Element("Section",WCS.WCS_NS);
                sec.setText(sv);
                sections.addContent(sec);
            }
            requestElement.addContent(sections);
        }

        if(AcceptFormats != null){
            Element af = new Element("AcceptFormats",WCS.WCS_NS);
            Element fe;
            for(String f : AcceptFormats){
                fe = new Element("OutputFormat",WCS.WCS_NS);
                fe.setText(f);
                af.addContent(fe);
            }
            requestElement.addContent(af);

        }

        if(AcceptLanguages != null){
            Element af = new Element("AcceptLanguages",WCS.WCS_NS);
            Element fe;
            for(String f : AcceptFormats){
                fe = new Element("Language",WCS.WCS_NS);
                fe.setText(f);
                af.addContent(fe);
            }
            requestElement.addContent(af);

        }

        return requestElement;

    }

    public Document getRequestDoc(){
        return new Document(getRequestElement());
    }


    public void serialize(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getRequestDoc(), os);
    }

    public String toString(){
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        return xmlo.outputString(getRequestDoc());

    }

}
