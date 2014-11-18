package opendap.auth;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * RemotePDP is a client of a PDPService which can be used in a PEPFilter to delegate the decision making to a remote
 * PDPService instance.
 */
public class RemotePDP extends PolicyDecisionPoint {

    public static final String DEFAULT_PDP_SERVICE = "http://localhost:8080/opendap/pdpService";
    private Logger _log;
    private URI _pdpServiceEndpoint;


    RemotePDP() {
        _log = LoggerFactory.getLogger(this.getClass());
        _pdpServiceEndpoint = null;
    }

    @Override
    public void init(JSONObject config) {

    }


    public void init(String configFileName) throws IOException, JDOMException, ConfigurationException {

        File configFile = new File(configFileName);

        Element config = opendap.xml.Util.getDocumentRoot(configFile);

        init(config);


    }

    @Override
    public void init(Element config) throws ConfigurationException {

        String msg;
        Element e;

        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            _log.error("init() - {}",msg);
            throw new ConfigurationException(msg);
        }

        try {
            _pdpServiceEndpoint = new URI(DEFAULT_PDP_SERVICE);
            e = config.getChild("PDPServiceEndpoint");
            if (e != null) {
                URI uri = new URI(e.getTextTrim());
                if(!uri.getScheme().equalsIgnoreCase("https")){
                    _log.warn("init() - RemotePDP connection is not using https.");
                }
                _pdpServiceEndpoint = uri;
            }
            _log.debug("init() - RemotePDP URL: {}",_pdpServiceEndpoint);
        } catch (URISyntaxException e1) {
            throw new ConfigurationException(e1);
        }


   }


    @Override
    public boolean addPolicy(Policy policy) {
        throw new UnsupportedOperationException("Adding policies to a remote PDP is not supported.");
    }

    @Override
    public boolean removePolicy(Policy policy) {
        throw new UnsupportedOperationException("Removing policies from a remote PDP is not supported.");
    }

    @Override
    public boolean evaluate(String userId, String resourceId, String queryString, String actionId) {


        boolean result = false;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {

            StringBuilder requestUrl = new StringBuilder();
            requestUrl.append(_pdpServiceEndpoint);
            requestUrl.append("?uid=").append(userId);
            requestUrl.append("&resourceId=").append(resourceId);
            requestUrl.append("&query=").append(queryString);
            requestUrl.append("&action=").append(actionId);

            HttpGet httpget = new HttpGet(requestUrl.toString());

            _log.debug("evaluate() - Executing HTTP request: " + httpget.getRequestLine());


            // ----------- Create a custom response handler ----------
            ResponseHandler<Boolean> responseHandler = new ResponseHandler<Boolean>() {

                public Boolean handleResponse(
                        final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {


                        HttpEntity entity = response.getEntity();
                        _log.debug(entity != null ? EntityUtils.toString(entity) : "null");

                        return true;
                    } else {
                        return false;
                    }
                }

            };
            // -------------------------------------------------------

            result = httpclient.execute(httpget, responseHandler);
        } catch (Exception e) {
            _log.error("evaluate() - Caught {} Message: {}",e.getClass().getName(),e.getMessage() );
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                _log.error("evaluate() - Caught {} Message: {}",e.getClass().getName(),e.getMessage() );
                // oh well...
            }
        }

        return result;
    }
}