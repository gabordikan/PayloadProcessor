package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.models.ApiConfig;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class BaseApi {
    private static String host;
    private static String port;
    private static String authUserName;
    private static String authUserPassword;
    private static Boolean doHttps;
    private static final String EMPTY_JSON_RESPONSE = "[]";

    public static void init(ApiConfig apiConfig) {
        host = apiConfig.getHost();
        port = apiConfig.getPort();
        authUserName = apiConfig.getUsername();
        authUserPassword = apiConfig.getPassword();
        doHttps = apiConfig.getDoHttps();
    }




    /**
     * Trigger a WS call through HTTP for patient search
     *
     * @return The json string of patient data
     * @throws ConnectException
     */
    public static String read(String term)
            throws IOException, AuthenticationException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        String result = "";
        String webProtocol = "http";
        if (doHttps) {
            webProtocol = "https";
        }
        String strURL = webProtocol + "://" + host + ":" + port + "/site/login";

        HttpGet get = new HttpGet(strURL);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                authUserName, authUserPassword);

        Header header = new BasicScheme(StandardCharsets.US_ASCII).authenticate(credentials, get, null);
        get.addHeader(header);

        try {
            get.addHeader("Content-type", "text/html");
            CloseableHttpClient httpclient;
            if (doHttps) {
                createTrustManager();
                httpclient = createHttpsCloseableHttpClient();
            } else {
                HttpClientBuilder builder = HttpClientBuilder.create();
                httpclient = builder.build();
            }

            CloseableHttpResponse httpResponse = httpclient.execute(get);

            HttpEntity entity2 = httpResponse.getEntity();
            StringWriter writer = new StringWriter();
            result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);

        } catch (IOException | KeyStoreException e) {
            // this happens when there's no server to connect to
            e.printStackTrace();
            throw e;
        } finally {
            get.releaseConnection();
        }
        return result;
    }

    private static void createTrustManager() throws NoSuchAlgorithmException, KeyManagementException {
        //TODO FIX HTTPS CONNECTION TO VALIDATE CERTIFICATES
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static CloseableHttpClient createHttpsCloseableHttpClient() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        //TODO FIX HTTPS CONNECTION TO VALIDATE CERTIFICATES
        CloseableHttpClient httpclient;
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

        httpclient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        return httpclient;
    }
}
