package org.androidpn.server.xmpp.net;

import org.androidpn.server.service.UserNotFoundException;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: ygt
 * Date: 6/4/13
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotifiedXmpploHandler extends XmppIoHandler{
    private static final Log log = LogFactory.getLog(NotifiedXmpploHandler.class);
    private final SessionManager sessionManager;

    private URI registerURI;
    private URI deregisterURI;
    private URI pnURI;

    protected NotifiedXmpploHandler() {
        sessionManager = SessionManager.getInstance();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.properties");
        Properties p = new Properties();
        try {
            p.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            registerURI = new URI(p.getProperty("registerUri"));
            deregisterURI = new URI(p.getProperty("deregisterUri"));
            pnURI =new URI(p.getProperty("pnUri"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);
        register(session);
        log.debug("Register a opened session.");
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
        deregister(session);
        log.debug("De-register a closed session.");
    }

    private void deregister(IoSession session) {
        post(session, deregisterURI);
    }

    private void register(IoSession session) {
        post(session, registerURI);
    }

    private void post(IoSession session, URI uri) {
        ArrayList<BasicNameValuePair> params = getBasicNameValuePairs(session);
        HttpPost httpPost = new HttpPost(uri);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sendRequest(httpPost);
    }

    private ArrayList<BasicNameValuePair> getBasicNameValuePairs(IoSession session) {
        String deviceId = null;
        try {
            deviceId = sessionManager.getSession(String.valueOf(session.getId())).getUsername();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("device_id", deviceId));
        params.add(new BasicNameValuePair("push_uri", pnURI.toString()));
        return params;
    }

    private void sendRequest(HttpRequestBase httpRequest) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(httpRequest);
            HttpEntity respEntity = response.getEntity();

            if (respEntity != null) {
                String content =  EntityUtils.toString(respEntity);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
