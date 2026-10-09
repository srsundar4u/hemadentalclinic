package com.dentalclinic.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import java.net.URI;

/** A small Java shell: Spring Boot still owns login, screens and booking rules. */
public class ClinicActivity extends Activity {
    private WebView clinic;
    private TextView connection;
    private Button print;
    private String server;
    private boolean loadFailed;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        LinearLayout controls = new LinearLayout(this);
        controls.addView(button("Home", () -> open("/")));
        controls.addView(button("Book", () -> open("/appointments/new")));
        controls.addView(button("Server", this::chooseServer));
        print = button("Print", this::printPrescription);
        print.setVisibility(View.GONE);
        controls.addView(print);
        HorizontalScrollView toolbar = new HorizontalScrollView(this);
        toolbar.addView(controls);
        layout.addView(toolbar);
        connection = new TextView(this);
        connection.setPadding(12, 4, 12, 4);
        layout.addView(connection);
        clinic = new WebView(this);
        layout.addView(clinic, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);

        WebSettings settings = clinic.getSettings();
        settings.setJavaScriptEnabled(true); // Required for searchable patients and consultation tabs.
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(clinic, false);
        clinic.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                loadFailed = false;
                print.setVisibility(View.GONE);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri destination = request.getUrl();
                if (sameServer(destination)) return false;
                if (request.isForMainFrame() && "https".equals(destination.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, destination));
                }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (!loadFailed && sameServer(Uri.parse(url))) {
                    connection.setText("");
                    print.setVisibility(Uri.parse(url).getPath().matches("/consultations/[0-9]+/prescription") ? View.VISIBLE : View.GONE);
                    CookieManager.getInstance().flush();
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    loadFailed = true;
                    connection.setText("Cannot connect. Check the laptop, internet and VPN, then tap Home.");
                    print.setVisibility(View.GONE);
                }
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                loadFailed = true;
                connection.setText("The clinic certificate could not be verified. Check the HTTPS server address.");
                print.setVisibility(View.GONE);
            }
        });
        server = getPreferences(MODE_PRIVATE).getString("server", "");
        if (server.isEmpty()) chooseServer(); else open("/");
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private void open(String path) {
        if (server == null || server.isEmpty()) { chooseServer(); return; }
        connection.setText("Connecting to clinic…");
        print.setVisibility(View.GONE);
        clinic.loadUrl(server + path);
    }

    private boolean sameServer(Uri uri) {
        if (server == null || server.isEmpty()) return false;
        Uri base = Uri.parse(server);
        return "https".equals(uri.getScheme()) && base.getHost().equalsIgnoreCase(uri.getHost())
                && base.getPort() == uri.getPort() && uri.getUserInfo() == null;
    }

    private void chooseServer() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://your-clinic-server");
        input.setText(server);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Clinic server")
                .setMessage("Enter the laptop's HTTPS clinic address. Use the address from your remote-access setup.")
                .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                URI address = new URI(input.getText().toString().trim());
                if (!"https".equals(address.getScheme()) || address.getHost() == null
                        || address.getUserInfo() != null || address.getQuery() != null || address.getFragment() != null
                        || !(address.getPath().isEmpty() || address.getPath().equals("/"))
                        || address.getPort() > 65535 || address.getPort() == 0) throw new IllegalArgumentException();
                String chosen = "https://" + address.getRawAuthority();
                if (!chosen.equals(server)) {
                    clinic.stopLoading();
                    clinic.loadUrl("about:blank");
                    clinic.clearHistory();
                    clinic.clearCache(true);
                    CookieManager.getInstance().removeAllCookies(done -> {
                        server = chosen;
                        getPreferences(MODE_PRIVATE).edit().putString("server", server).apply();
                        open("/");
                    });
                } else open("/");
                dialog.dismiss();
            } catch (Exception invalid) {
                input.setError("Enter an HTTPS address with no login details, query or path.");
            }
        }));
        dialog.show();
    }

    private void printPrescription() {
        if (clinic.getUrl() != null && sameServer(Uri.parse(clinic.getUrl()))
                && Uri.parse(clinic.getUrl()).getPath().matches("/consultations/[0-9]+/prescription")) {
            PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
            manager.print("Clinic prescription", clinic.createPrintDocumentAdapter("Prescription"), null);
        }
    }

    @Override public void onBackPressed() {
        if (clinic.canGoBack()) clinic.goBack(); else super.onBackPressed();
    }

    @Override public void onDestroy() {
        clinic.destroy();
        super.onDestroy();
    }
}
