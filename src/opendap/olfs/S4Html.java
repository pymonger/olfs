/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;


import java.io.*;
import javax.servlet.http.*;

import opendap.servers.www.*;
import opendap.dap.*;
import opendap.dap.parser.ParseException;
import opendap.dap.Server.ServerDDS;

/**
 * Default handler for OPeNDAP .html requests. This class is used
 * by OLFS. This code exists as a seperate class in order to alleviate
 * code bloat in the OLFS class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class S4Html {

    private static final boolean _Debug = false;
    private String helpLocation = "http://unidata.ucar.edu/packages/dods/help_files/";


    /**
     * ************************************************************************
     * Default handler for OPeNDAP .html requests. Returns an html form
     * and javascript code that allows the user to use their browser
     * to select variables and build constraints for a data request.
     * The DDS and DAS for the data set are used to build the form. The
     * types in opendap.servers.www are integral to the form generation.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @see opendap.servers.www
     */
    public void sendDataRequestForm(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String dataSet,
                                    ServerDDS sdds,
                                    DAS myDAS) // changed jc
            throws DODSException, ParseException {


        System.out.println("Sending OPeNDAP Data Request Form For: " + dataSet +
                "    CE: '" + request.getQueryString() + "'");
        String requestURL;

/*
        // Turn this on later if we discover we're supposed to accept
        // constraint expressions as input to the Data Request Web Form
    String ce;
    if(request.getQueryString() == null){
        ce = "";
        }
    else {
        ce = "?" + request.getQueryString();
        }
*/


        int suffixIndex = request.getRequestURL().toString().lastIndexOf(".");

        requestURL = request.getRequestURL().substring(0, suffixIndex);


        try {

            //PrintWriter pw = new PrintWriter(response.getOutputStream());
            PrintWriter pw;
            if (false) {
                pw = new PrintWriter(
                        new FileOutputStream(
                                new File("debug.html")
                        )
                );
            } else
                pw = new PrintWriter(response.getOutputStream());


            wwwOutPut wOut = new wwwOutPut(pw);

            // Get the DDS and the DAS (if one exists) for the dataSet.
            DDS myDDS = getWebFormDDS(dataSet, sdds);
            //DAS myDAS = dServ.getDAS(dataSet); // change jc

            jscriptCore jsc = new jscriptCore();

            pw.println(
                    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"\n"
                            + "\"http://www.w3.org/TR/REC-html40/loose.dtd\">\n"
                            + "<html><head><title>OPeNDAP Dataset Query Form</title>\n"
                            + "<base href=\"" + helpLocation + "\">\n"
                            + "<script type=\"text/javascript\">\n"
                            + "<!--\n"
            );
            pw.flush();

            pw.println(jsc.jScriptCode);
            pw.flush();

            pw.println(
                    "DODS_URL = new dods_url(\"" + requestURL + "\");\n"
                            + "// -->\n"
                            + "</script>\n"
                            + "</head>\n"
                            + "<body>\n"
                            + "<p><h2 align='center'>OPeNDAP Dataset Access Form</h2>\n"
                            + "<hr>\n"
                            + "<font size=-1>Tested on Netscape 4.61 and Internet Explorer 5.00.</font>\n"
                            + "<hr>\n"
                            + "<form action=\"\">\n"
                            + "<table>\n"
            );
            pw.flush();

            wOut.writeDisposition(requestURL);
            pw.println("<tr><td><td><hr>\n");

            wOut.writeGlobalAttributes(myDAS, myDDS);
            pw.println("<tr><td><td><hr>\n");

            wOut.writeVariableEntries(myDAS, myDDS);
            pw.println("</table></form>\n");
            pw.println("<hr>\n");


            pw.println(
                    "<address>Send questions or comments to: "
                            + "<a href=\"mailto:support@unidata.ucar.edu\">"
                            + "support@unidata.ucar.edu"
                            + "</a></address>"
                            + "</body></html>\n"
            );

            pw.println("<hr>");
            pw.println("<h2>DDS:</h2>");

            pw.println("<pre>");
            myDDS.print(pw);
            pw.println("</pre>");
            pw.println("<hr>");
            pw.flush();


        }
        catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }


    }


    /**
     * ************************************************************************
     * Gets a DDS for the specified data set and builds it using the class
     * factory in the package <b>opendap.servers.www</b>.
     * <p/>
     * Currently this method uses a deprecated API to perform a translation
     * of DDS types. This is a known problem, and as soon as an alternate
     * way of achieving this result is identified we will implement it.
     * (Your comments appreciated!)
     *
     * @param dataSet A <code>String</code> containing the data set name.
     * @return A DDS object built using the www interface class factory.
     * @see opendap.dap.DDS
     * @see opendap.servers.www
     * @see opendap.servers.www.wwwFactory
     */
    public DDS getWebFormDDS(String dataSet, ServerDDS sDDS) // changed jc
            throws DODSException, ParseException {

        // Get the DDS we need, using the getDDS method
        // for this particular server
        // ServerDDS sDDS = dServ.getDDS(dataSet);

        // Make a new DDS using the web form (www interface) class factory
        wwwFactory wfactory = new wwwFactory();
        DDS wwwDDS = new DDS(dataSet, wfactory);

        // Make a special print writer to catch the ServerDDS's
        // persistent representation in a String.
        StringWriter ddsSW = new StringWriter();
        sDDS.print(new PrintWriter(ddsSW));

        // Now use that string to make an input stream to
        // pass to our new DDS for parsing.
        wwwDDS.parse(new StringBufferInputStream(ddsSW.toString()));


        return (wwwDDS);


    }


}




