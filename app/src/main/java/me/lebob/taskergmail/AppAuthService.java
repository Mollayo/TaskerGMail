package me.lebob.taskergmail;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.browser.BrowserAllowList;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import java.util.Objects;

public class AppAuthService {

    Uri authUri;
    Uri tokenUri;
    String clientId;
    Uri redirectUri;
    String authScopes;

    public AuthState authState;
    public AuthorizationService service;
    AuthorizationRequest request;
    Context context;

    private static AppAuthService instance;

    public static AppAuthService getInstance(Context context) {
        if (instance ==null)
            instance = new AppAuthService(context);
        return instance;
    }

    private AppAuthService(Context context)
    {
        this.context=context;
        initializeSettings(context);

        AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
                .setBrowserMatcher(new BrowserAllowList(
                        VersionedBrowserMatcher.CHROME_BROWSER))
                .build();

        service = new AuthorizationService(context,appAuthConfig);
        authState = new AuthState(initializeConfiguration());
    }

    private void initializeSettings(Context context) {
        authUri = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth");
        tokenUri = Uri.parse("https://www.googleapis.com/oauth2/v4/token");
        redirectUri = Uri.parse("me.lebob.taskergmail:/oauth2redirect");
        authScopes = "https://mail.google.com/";

        // To be completed by enabling the GMail API and creating credentials https://console.developers.google.com/
        clientId = "";
    }

    private AuthorizationServiceConfiguration initializeConfiguration() {
        return new AuthorizationServiceConfiguration(authUri, tokenUri);
    }

    public AuthState getAuthState() {
        return authState;
    }

    public Intent getAuthRequestIntent()
    {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                Objects.requireNonNull(authState.getAuthorizationServiceConfiguration()),
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        );
        //builder.setPrompt("login");
        builder.setScopes(authScopes);
        request = builder.build();
        return service.getAuthorizationRequestIntent(request);
    }

    public void getAuthToken(Intent intent, final TokenResponseCallback callback) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException ex = AuthorizationException.fromIntent(intent);
        authState.update(response, ex);

        if (response != null) {
            service.performTokenRequest(response.createTokenExchangeRequest(), (response1, ex1) -> {
                authState.update(response1, ex1);
                callback.onComplete();
            });
        }
    }

    public interface TokenResponseCallback {
        void onComplete();
    }

}
