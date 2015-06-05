package uk.co.integrate.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;


/**
 * Spring configuration for a minimal client, demonstrating the use of Spring Security OAuth to connect to Integrate
 * APIs.
 *
 * See the Spring documentation for more on what all this means if you aren't familiar with Spring's annotation-based
 * configuration.
 */
@Configuration
public class ClientConfig {

    @Bean
    public OAuth2ClientContext oauth2ClientContext() {

        return new DefaultOAuth2ClientContext();
    }

    /**
     * Bean containing config. for the protected "resource" we wish to access, i.e. information used by the Spring
     * OAuth 2.0 client library to negotiate the protection(s) applied by the resource server. For a client credentials
     * grant this is pretty simple, because we don't have to deal with user authorisation.
     */
    @Bean
    public ClientCredentialsResourceDetails clientCredentialsResourceDetails() {

        ClientCredentialsResourceDetails clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();

        // ID of this resource, not used by the protocol (it's a Spring thing). Since we only have one resource and
        // don't do any explicit lookups we just set this to be the same as the bean name
        clientCredentialsResourceDetails.setId("clientCredentialsResourceDetails");

        // Set the client ID/secret. NB: these should be far better protected than this - leaking these compromises the
        // security of the target system. The simple route is taken here for the sake of expediency, but *MUST NOT*
        // be used in production
        clientCredentialsResourceDetails.setClientId("18acc812-ffce-442a-94a0-5cf9e76d1d80");
        clientCredentialsResourceDetails.setClientSecret(
                "bHGaq4026UuPoMQVDVRbvhiSKuNvMrj00BVSqNNtagM4PssH9LwCk_PSiqPu-U52lEM5ERzCiTMfh7hg_W1Jgg");

        // The URI used to obtain access tokens, located on the auth. server
        clientCredentialsResourceDetails.setAccessTokenUri("http://localhost:8080/openid-connect/token");

        // Make requests to the token endpoint using a basic auth. header
        clientCredentialsResourceDetails.setClientAuthenticationScheme(AuthenticationScheme.header);

        return clientCredentialsResourceDetails;
    }

    @Bean
    public OAuth2RestTemplate oAuth2RestTemplate() {

        return new OAuth2RestTemplate(clientCredentialsResourceDetails(), oauth2ClientContext());
    }
}
