package com.codingsimply.ellucianmobile.AutoLoginWebModule;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ellucian.elluciango.R;
import com.ellucian.mobile.android.app.EllucianActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Adam Saladino
 */
public class AutoLoginWebActivity extends EllucianActivity {

    private WebView webView;
    private ProgressDialog loadingDialog;

    private boolean allowInsecure = true;
    private boolean gotPostAttempt = false;

    private String usernameSelector;
    private String passwordSelector;
    private String formSelector;
    private String extraFields;
    private String redirectUrl;
    private String redirectSelector;

    private String cssUrl;
    private String jsUrl;

    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createProgressHud();
        loadViewElements();
        loadWebView();
        loadUser();
        loadLogin();
    }

    /**
     * Load web view and module title.
     */
    private void loadViewElements() {
        setContentView(R.layout.activity_auto_login_web);
        if (getActionBar() != null) {
            getActionBar().setTitle(moduleName);
        }
    }

    /**
     * Build the web view
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void loadWebView() {
        enableDebugging();
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        webView = (WebView) findViewById(R.id.auto_login_web_webview);
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new AutoLoginWebViewClient(this));
        webView.addJavascriptInterface(new WebAppInterface(this), "AutoLoginWebInterface");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53");
        loadingDialog.show();
    }

    private void loadUser() {
        username = getEllucianApp().getAppUserName();
        password = getEllucianApp().getAppUserPassword();
    }

    /**
     * Load variables for the module with the username and password.
     */
    private void loadLogin() {
        Intent intent = getIntent();

        String url = getIntentValue(intent, "loginUrl");
        usernameSelector = getIntentValue(intent, "usernameSelector");
        passwordSelector = getIntentValue(intent, "passwordSelector");
        formSelector = getIntentValue(intent, "formSelector");
        extraFields = getIntentValue(intent, "extraFields");
        redirectUrl = getIntentValue(intent, "redirectUrl");
        redirectSelector = getIntentValue(intent, "redirectSelector");

        cssUrl = getIntentValue(intent, "cssUrl");
        jsUrl = getIntentValue(intent, "jsUrl");

        if (url.startsWith("https://") || allowInsecure) {
            webView.loadUrl(url);
        } else {
            Toast.makeText(getApplicationContext(),"Site not secure.", Toast.LENGTH_LONG).show();
        }
    }

    private String getIntentValue(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        if(value == null) {
            return "";
        }
        // For localhost debugging on the emulator.
        return value.replace("http://localhost", "http://10.0.2.2");
    }

    /**
     * Handles the page loading of javascript based on url.
     */
    private void handlePageRequest() {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:AutoLoginWeb.loadUserIn('" + username + "','" + password + "');");
            }
        });
    }

    public void checkLogin() {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:AutoLoginWeb.checkForFields();");
            }
        });
    }

    public void checkLoggedInAndRedirect() {
        if (redirectUrl != null && redirectSelector != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:AutoLoginWeb.checkLoggedInAndRedirect();");
                }
            });
        }
    }

    /**
     * Load javascript and css assets
     */
    public void loadResources() {
        try {
            String js = getResourceContents("auto-login-web.js");
            js = js.replace("{{usernameSelector}}", usernameSelector);
            js = js.replace("{{passwordSelector}}", passwordSelector);
            js = js.replace("{{formSelector}}", formSelector);
            js = js.replace("{{extraFields}}", extraFields);
            js = js.replace("{{redirectUrl}}", redirectUrl);
            js = js.replace("{{redirectSelector}}", redirectSelector);
            webView.loadUrl("javascript:" + js.replace("\n", " "));
        } catch (IOException ex) {
            Log.v(getLocalClassName(), "Could not load javascript.");
        }
    }

    public void loadExternalResources() {
        if (cssUrl != "") {
            String command = "AutoLoginWeb.addCss('auto-login-css','" + cssUrl + "');";
            webView.loadUrl("javascript:" + command.replace("\n", " "));
        }
        if (jsUrl != "") {
            String command = "AutoLoginWeb.addJs('" + jsUrl + "');";
            webView.loadUrl("javascript:" + command.replace("\n", " "));
        }
    }

    /**
     * Show the web view and hide the loading dialog.
     */
    private void waitForPost() {
        runOnUiThread(new Runnable() {
            public void run() {
                loadingDialog.hide();
            }
        });
    }

    /**
     * Get the contents of the asset file.
     *
     * @param resource name of the asset
     * @return contents of the file
     * @throws IOException file access problems.
     */
    private String getResourceContents(String resource) throws IOException {
        StringBuilder returnString = new StringBuilder();
        InputStream assetStream = getAssets().open(resource);
        BufferedReader input = new BufferedReader(new InputStreamReader(assetStream));
        String line;
        while ((line = input.readLine()) != null) {
            returnString.append(line);
        }
        assetStream.close();
        input.close();
        return returnString.toString();
    }

    /**
     * Create the progress popup hud.
     */
    private void createProgressHud() {
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setCancelable(true);
        loadingDialog.setMessage("Loading...");
    }

    /**
     * Needs to be set to debug web view on device
     */
    private void enableDebugging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    // ///////////////////////////////////////
    // Resource loading
    // ///////////////////////////////////////

    /**
     * Used to watch for page loading and handle ssl errors.
     */
    public class AutoLoginWebViewClient extends WebViewClient {

        private AutoLoginWebActivity parentActivity;

        public AutoLoginWebViewClient(AutoLoginWebActivity activity) {
            parentActivity = activity;
        }

        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return null;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap image) {
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            parentActivity.loadResources();
            parentActivity.loadExternalResources();
            parentActivity.checkLoggedInAndRedirect();
            if (!gotPostAttempt) {
                parentActivity.checkLogin();
            } else {
                parentActivity.waitForPost();
            }
        }

        @Override
        public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error) {
            handler.proceed();
            parentActivity.gotPostAttempt = true;
            parentActivity.waitForPost();
        }
    }

    public class WebAppInterface {

        private AutoLoginWebActivity parentActivity;

        public WebAppInterface(AutoLoginWebActivity parentActivity) {
            this.parentActivity = parentActivity;
        }

        @JavascriptInterface
        public void foundForm() {
            parentActivity.gotPostAttempt = true;
            parentActivity.handlePageRequest();
        }
    }

}
