package uk.co.integrate.client;

/**
 * Exception indicating that an access token has expired.
 */
public class AccessTokenExpiredException extends Exception {

    public AccessTokenExpiredException(String message) {
        super(message);
    }
}
