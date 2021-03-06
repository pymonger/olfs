/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.http;

import net.sf.saxon.s9api.SaxonApiException;
import opendap.bes.dap2Responders.BesApi;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.IDN;
import java.security.Principal;

/**
 * Created by ndp on 4/22/15.
 */
public class AuthenticationControls {

    public static final String CONFIG_ELEMENT = "AuthenticationControls";

    private static Logger _log;
    private static boolean _initialized ;


    private BesApi _besApi;

    private static String _loginPath;
    private static String _logoutPath;

    static {
        _log = LoggerFactory.getLogger(AuthenticationControls.class);
        _loginPath = null;
        _logoutPath = null;
        _initialized = false;
    }

    private AuthenticationControls() {

    }


    /**
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The IsoDispatchHandler that creates an instance of IsoDispatchHandler will
     * pass itself into it along with the XML element that declared the
     * IsoDispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not availableInChunk by
     * interogating the IsoDispatchHandler's methods.
     *
     * @param config A JDOM Element objct containing the XML Element that
     *               announced which implementation of IsoDispatchHandler to use. It may (or
     *               may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see opendap.coreServlet.DispatchServlet
     */
    public static void init(Element config) throws Exception {

       if(_initialized)
           return;



        if (config != null) {
            if(config.getName().equals(CONFIG_ELEMENT)) {


                Element e = config.getChild("login");
                if (e != null) {
                    _loginPath = e.getTextTrim();
                }
                e = config.getChild("logout");
                if (e != null) {
                    _logoutPath = e.getTextTrim();
                }
                _initialized = true;
            }

        }

    }

    public static String getLoginPath() {
        return _logoutPath;
    }

    public static String getLogoutPath() {
        return _loginPath;
    }

    public static void setLoginParameters(Transformer transformer, HttpServletRequest request) throws SaxonApiException {

        Logger log = _log;
        String userId = null;
        Principal userPrinciple = request.getUserPrincipal();
        if (request.getRemoteUser() != null) {
            userId = request.getRemoteUser();

        } else if (userPrinciple != null) {
            userId = userPrinciple.getName();
        }

        log.debug("xsltDir() - UserId: {}", userId);
        if (userId != null) {
            transformer.setParameter("userId", userId);
        }


        log.debug("xsltDir() - _loginPath: {}", _loginPath);
        if (_loginPath != null) {
            transformer.setParameter("loginLink", _loginPath);
        }

        log.debug("xsltDir() - _logoutPath: {}", _logoutPath);
        if (_logoutPath != null) {
            transformer.setParameter("logoutLink", _logoutPath);
        }


    }
}
