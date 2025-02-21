package com.example.offer_wall_demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    TextView bonus;
    int userBonus = 0;

    final String serverUrl = "https://demo.starmobicloud.net/userdata/";
    final String testUid = "99843075";

    final String url = "https://gifts.fireflyplus.com/v2/star-wall/index.html";

    final String offerWallSecret = "557dfeada85b450bba605bdabf0dfa9c";
    final String offerWallChannel = "CS0001";

    @SuppressLint("SetJavaScriptEnabled")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        webView = findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true); // 启用 JavaScript
        webView.getSettings().setSupportZoom(false); // 禁用缩放
        webView.getSettings().setDomStorageEnabled(true); // 开启 DOM 存储
        webView.addJavascriptInterface(new OfferwallBridge(this), "Offerwall"); // 添加 JavaScript 调用的回调
        webView.setWebViewClient(new MyWebViewclient());
        webView.loadUrl(url);

        ViewGroup.LayoutParams params = webView.getLayoutParams();
        params.height = changeWebViewHeight(this);
        webView.setLayoutParams(params);
    }

    @SuppressLint("StaticFieldLeak")
    private void updateBonus() {
        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                return getJsonFromServer(params[0]);
            }

            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(JSONObject resp) {
                try {
                    if (resp != null && resp.get("status").equals("ok")) {
                        userBonus = Integer.parseInt(resp.get("response").toString());
                        bonus.setText("Bonus: " + userBonus);
                        webView.evaluateJavascript("window.updateBonus(" + String.valueOf(userBonus) + ")", new android.webkit.ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute(serverUrl + "getBonus/" + testUid);
    }

    public class OfferwallBridge {
        private Context ctx;

        OfferwallBridge(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public String initUser() {
            try {
                JSONObject resp = new JSONObject();
                resp.put("channel", offerWallChannel);
                resp.put("userId", testUid); //产品内的用户ID
                resp.put("time", System.currentTimeMillis());
                resp.put("countryCode", "CN");
                resp.put("language", "en");
                resp.put("taskGroup", "CS0001G01");
                resp.put("extra", "");
                resp.put("sign", makeSign(resp));

                Log.v("OfferwallBridge", "initUser response " + resp.toString());
                return resp.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @JavascriptInterface
        public void openBrowser(String req) {
            try {
                Log.v("OfferwallBridge", "openBrowser response " + req);
                JSONObject data = new JSONObject(req);

                String openUrl = data.get("openUrl").toString().trim();
                Log.v("OfferwallBridge", "openBrowser " + openUrl);

                Uri uri = Uri.parse(openUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void closePage(String req) {
            try {
                JSONObject data = new JSONObject(req);
                Log.v("OfferwallBridge", "closePage " + data.get("page").toString().trim());

                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void jslog(String log) {
            Log.v("OfferwallBridge", "jslog " + log);
        }
    }

    public class MyWebViewclient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), "No internet connection", Toast.LENGTH_LONG).show();
            webView.loadUrl("file:///android_asset/lost.html");
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            handler.cancel();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            updateBonus();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private JSONObject getJsonFromServer(String urlString) {
        JSONObject jsonObject = null;
        try {
            Log.v("OfferwallBridge", "getJsonFromServer request " + urlString);
            final URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            final InputStream inputStream = conn.getInputStream();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            Log.v("OfferwallBridge", "getJsonFromServer response " + responseStrBuilder.toString());
            jsonObject = new JSONObject(responseStrBuilder.toString());
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public String makeSign(@NonNull JSONObject data) {
        try {
            ArrayList<String> reqKeys = new ArrayList<>();
            Iterator<String> iterator = data.keys();
            while (iterator.hasNext()) {
                reqKeys.add(iterator.next());
            }
            reqKeys.sort(Comparator.naturalOrder());

            StringBuilder paramString = new StringBuilder();
            for (int i = 0; i < reqKeys.size(); i++) {
                if (i == 0) {
                    paramString = new StringBuilder(reqKeys.get(i) + "=" + data.get(reqKeys.get(i)));
                } else {
                    paramString.append("&").append(reqKeys.get(i)).append("=").append(data.get(reqKeys.get(i)));
                }
            }
            Log.v("OfferwallBridge", "param to sign " + paramString + offerWallSecret + " -> sign " + md5(paramString + offerWallSecret));

            return md5(paramString + offerWallSecret);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public static String md5(String string) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int changeWebViewHeight(@NonNull Context content) {
        Resources resources = content.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int screenHeight = dm.heightPixels;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int result = resources.getDimensionPixelSize(resourceId);
        return screenHeight + result;
    }
}