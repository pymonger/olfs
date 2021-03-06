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
package opendap.wcs.v2_0.http;

import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;


/**
 * Holds The type information and a referene to an input stream for the content of a Mutipart
 * MIME attachment.
 *
 * @see MultipartResponse
 *      User: ndp
 *      Date: Apr 28, 2006
 *      Time: 12:13:19 PM
 */
public class Attachment {

    private enum ContentModel {
        stream, url, document
    }


    private Logger log;
    private String contentTransferEncoding = "binary";
    private final String contentId = "Content-Id";
    private final String contentType = "Content-Type";
    private InputStream _istream;
    private String _sourceUrl;
    private Document _doc;
    private ContentModel _myContentModel;

    private HashMap<String, String> mimeHeaders;




    public void setHeader(String name, String value){
        mimeHeaders.put(name,value);
    }

    public String getHeader(String name){
        return mimeHeaders.get(name);
    }

    public Attachment(String ctype, String cid){
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        mimeHeaders = new HashMap<String, String>();

        setHeader(contentType,ctype);
        setHeader(contentId,"<"+cid+">");

    }



    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid   String containing the value if the HTTP header Content-Id for this attachment.
     * @param is    A stream containing the content for this attachment.
     */
    public Attachment(String ctype, String cid, InputStream is) {
        this(ctype,cid);

        _istream = is;
        _doc = null;
        _myContentModel = ContentModel.stream;

    }


    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid   String containing the value if the HTTP header Content-Id for this attachment.
     * @param url   A URL that when dereferenced will provide the content for this attachment.
     */
    public Attachment(String ctype, String cid, String url) {
        this(ctype,cid);

        _istream = null;
        _sourceUrl = url;
        _doc = null;
        _myContentModel = ContentModel.url;
    }

    /**
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid String containing the value if the HTTP header Content-Id for this attachment.
     * @param doc A JDOM XML document to provide the content for this attachment.
     */
    public Attachment(String ctype, String cid, Document doc) {
        this(ctype,cid);

        _istream = null;
        _sourceUrl = null;
        _doc = doc;
        _myContentModel = ContentModel.document;
    }


    public String getContentType(){
        return getHeader(contentType);
    }


    /**
     * Write the attchment to the indicated stream
     *
     * @param mimeBoundary MIME Boundary for the attachment.
     * @param sos          Stream to which to write the attachment.
     * @throws IOException                 When things can't be read or written.
     * @throws java.net.URISyntaxException If the target URL is hosed.
     */
    public void write(String mimeBoundary, ServletOutputStream sos) throws IOException, URISyntaxException {


        sos.println("--" + mimeBoundary);
        for(String headerName:mimeHeaders.keySet()){
            sos.print(headerName);
            sos.print(": ");
            sos.println(mimeHeaders.get(headerName));
        }
        sos.println();


        switch (_myContentModel) {
            case stream:
                try {
                    Util.drainInputStream(_istream, sos);
                } finally {
                    if (_istream != null) {
                        try {
                            _istream.close();
                        } catch (IOException e) {
                            log.error("Failed to close content source InputStream. " +
                                    "Error Message: " + e.getMessage());

                        }
                    }
                }
                break;

            case url:
                Util.forwardUrlContent(_sourceUrl, sos);
                break;

            case document:
                Util.sendDocument(_doc, sos);
                break;

            default:
                break;

        }

        //MIME Attachments need to end with a newline!
        sos.println();


    }



}

