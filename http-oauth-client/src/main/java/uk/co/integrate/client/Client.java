package uk.co.integrate.client;

import com.google.gson.Gson;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example API client, calling a protected URL via an OAuth 2.0 "client credentials" grant.
 */
public class Client {

    // Client ID and secret. In production use these should be better protected. They *MUST NOT* be stored in plain
    // text like this, for example (maybe in a protected properties file, or encrypted somehow)
    private static final String CLIENT_ID = "18acc812-ffce-442a-94a0-5cf9e76d1d80";
    private static final String CLIENT_SECRET =
            "bHGaq4026UuPoMQVDVRbvhiSKuNvMrj00BVSqNNtagM4PssH9LwCk_PSiqPu-U52lEM5ERzCiTMfh7hg_W1Jgg";

    // Host and port for auth. server. In production this will the HTTPS port and the auth. server name provided
    // by Integrate
    private static final String AUTH_SERVER_HOST = "localhost";
    private static final int AUTH_SERVER_PORT = 8080;

    // The HttpClient instance itself is reused below (for the sake of efficiency). It is the responsibility of the
    // calling code to create and (when done) deallocate this
    private final CloseableHttpClient httpClient;

    // Gson instance used to deserialize JSON
    private final Gson gson = new Gson();

    // The current access token, as retrieved from the token endpoint. May be null (or expired)
    private AccessToken accessToken = null;

    // The estimated expiry time of the current access token. If the system clock reads higher than this then get a
    // new token instead of reusing the old one
    private long tokenTime = 0;

    public Client(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Gets an access token from the auth. server to use for subsequent API requests. The token endpoint requires
     * authorisation - in this case, preemptive basic auth. using the client id/secret as the username/password.
     *
     * Unfortunately HttpClient does *not* make this easy. Most of the code below is working around the HttpClient API
     * and its dislike of preemptive auth.
     */
    public void getAccessToken() throws IOException {

        // HttpClient uses a credentials provider to provide credentials when needed
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(CLIENT_ID, CLIENT_SECRET));

        // Create an AuthCache instance so we can force basic auth. even when we aren't challenged for it
        AuthCache authCache = new BasicAuthCache();

        // Generate BasicScheme object and add it to the local auth cache (effectively saying "use basic auth. for this
        // host come what may")
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(new HttpHost(AUTH_SERVER_HOST, 8080), basicAuth);

        // Create a local context, which will not be used for any other request. The ensures that our credentials are
        // *not* sent with any future API requests. This is a requirement - clients *MUST NOT* leak credentials
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCredentialsProvider(credentialsProvider);
        localContext.setAuthCache(authCache);

        // Finally, we get to the request. We want to post to the token URL
        HttpPost httpPost = new HttpPost(
                "http://" + AUTH_SERVER_HOST + ":" + AUTH_SERVER_PORT + "/openid-connect/token");

        // We are *required* to send a grant type of "client_credentials". This should be form encoded and sent as the
        // post body. There are other things we could send (see the OAuth 2.0 spec.) but none are required
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        }
        catch (UnsupportedEncodingException e) {
            // Should never happen - in production code you'd want to make sure though, maybe an assertion?
        }

        // Execute the request. We use Gson to parse the JSON returned, but you could do the parsing however you like
        // really. See the AccessToken object and comments for a better picture of what the token looks like
        CloseableHttpResponse response = httpClient.execute(httpPost, localContext);

        try {
            accessToken = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), AccessToken.class);
            System.out.println(accessToken);
        }
        finally {
            response.close();
        }

        // Estimate the token expiry from the resulting access token (= current time + expiry time from token)
        tokenTime = System.currentTimeMillis() + (accessToken.getExpiresIn() * 1000);
    }

    /**
     * Tests an HTTP response to see if the access token presented has expired (as indicated by a 401 status code and
     * a suitable error string).
     */
    private boolean tokenExpired(HttpResponse httpResponse) {

        // A 401 status *may* indicate an expired token...
        if (httpResponse.getStatusLine().getStatusCode() == 401) {

            // ...but we check the WWW-Authenticate header to be sure (there should only be one of these, but
            // HttpClient likes to make life interesting. Simplest complete solution = check each header it returns
            Header[] wwwAuthenticateHeaders = httpResponse.getHeaders("WWW-Authenticate");

            for (Header header : wwwAuthenticateHeaders) {

                for (HeaderElement headerElement : header.getElements()) {

                    // If we actually have an invalid token, return true
                    if ("error".equalsIgnoreCase(headerElement.getName()) &&
                            "invalid_token".equalsIgnoreCase(headerElement.getValue())) {

                        return true;
                    }
                }
            }
        }

        // If we get here then the token has not expired
        return false;
    }

    /**
     * Accesses a protected URL, handling token issues by refreshing the token or retrying exactly once.
     */
    public String accessResource(String url) throws IOException {

        // If we don't already have one an access token, or our existing token has timed out then call this method
        // again with a flag indicating that we should reauthorise. We run our own timer (instead of just detecting
        // failures) because it is more efficient in terms of requests/responses. Unfortunately, this doesn't get us
        // out of detecting failure - we need to do that too (see below)
        if (accessToken == null || System.currentTimeMillis() > tokenTime) {
            getAccessToken();
        }

        // Make the get request. If it fails due to an access token problem then get a new access token and retry
        // exactly once (multiple retries are probably a bad idea)
        try {

            return makeGetRequest(url);
        }
        catch (AccessTokenExpiredException e) {

            getAccessToken();

            try {

                return makeGetRequest(url);
            }
            catch (AccessTokenExpiredException e1) {

                // Return a warning in this case - in production you'd probably want to do more with this
                return "Unable to refresh access token";
            }
        }
    }

    /**
     * Makes a get request and either returns the resulting JSON or throws an exception if the access token has
     * expired. Non-access-token-related statuses and other issues are ignored for the sake of brevity.
     */
    private String makeGetRequest(String url) throws AccessTokenExpiredException, IOException {

        // Build the get request we use to test. Although we are using a simple get request here, others may be
        // possible and should follow exactly the same pattern (build request, add bearer token, send request,
        // detect failure and resend if token has expired)
        HttpGet httpGet = new HttpGet(url);

        // We add the access token as a header, using "Bearer" authentication. This is needed for any request
        httpGet.addHeader("Authorization", "Bearer " + accessToken.getAccessToken());

        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        try {

            // Check for token expiry - thrown an exception if found
            if (tokenExpired(httpResponse)) {

                // We still need to consume the http stream
                EntityUtils.consume(httpResponse.getEntity());

                // And then we throw an exception so the caller can deal with the token issue (if needed)
                throw new AccessTokenExpiredException("Access token has expired");
            }

            // If our token was ok, then read the response (we assume token ok = everything else ok - in production
            // code you would probably want to check for other status codes too)
            return EntityUtils.toString(httpResponse.getEntity());
        }
        finally {

            // Close the response stream cleanly
            httpResponse.close();
        }
    }

    /**
     * Runs a simple test, accessing the same protected URL over and over at five second intervals.
     */
    public static void main(String[] args) throws IOException {

        // Create an HttpClient to use in accessing the API
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {

            Client client = new Client(httpClient);
            client.getAccessToken();

            while (true) {

                try {

                    String jsonString = client.accessResource("http://localhost:8090/simple-web-app/api/users");

                    System.out.println(jsonString);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {

                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally {

            // Deallocate HttpClient nicely
            httpClient.close();
        }
    }
}
