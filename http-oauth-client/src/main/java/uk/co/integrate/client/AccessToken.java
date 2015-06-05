package uk.co.integrate.client;

import com.google.gson.annotations.SerializedName;

/**
 * Type class for access tokens received from the token endpoint. These look like this:
 *
 * {
 *     "access_token":"eyJhbGci[snip]SvmVCe_Hdz0iJo",
 *     "token_type":"Bearer",
 *     "expires_in":9,
 *     "scope":"phone address email openid profile"
 * }
 *
 * For details of what each field means, please see the OAuth 2.0 docs.
 */
public class AccessToken {

    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("token_type")
    public String tokenType;

    @SerializedName("expires_in")
    public Integer expiresIn;

    public String scope;

    public AccessToken() {
        // Empty constructor, required by Gson
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "AccessToken {" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                '}';
    }
}
