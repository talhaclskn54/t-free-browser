package com.freebrowser.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class MainActivity extends Activity {
    static final String[] ADS = {"doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google", "google-analytics.com", "googletagmanager.com", "facebook.net", "adnxs.com",
        "taboola.com", "outbrain.com", "criteo.com", "scorecardresearch.com", "adsafeprotected.com",
        "moatads.com", "pubmatic.com", "rubiconproject.com", "openx.net", "amazon-adsystem.com",
        "adform.net", "mc.yandex.ru", "hotjar.com", "ads.twitter.com", "popads.net", "propellerads.com"};

    static class Tab { WebView w; boolean priv; String title = "Yeni sekme"; }

    SharedPreferences sp;
    LinearLayout root, top, bottom;
    FrameLayout content;
    EditText urlBox;
    ImageView logoView;
    ProgressBar progress;
    TextView tabBtn;
    List<TextView> btns = new ArrayList<>();
    List<Tab> tabs = new ArrayList<>();
    int cur = 0;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
    String brand() { return sp.getString("brand", "Free Browser"); }
    int color() { return sp.getInt("color", 0xFFFF1B2D); }
    String engine() { return sp.getString("engine", "https://www.google.com/search"); }
    Tab cur() { return tabs.get(cur); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("fb", MODE_PRIVATE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(10), dp(6), dp(10), dp(6));
        logoView = new ImageView(this);
        top.addView(logoView, new LinearLayout.LayoutParams(dp(32), dp(32)));
        urlBox = new EditText(this);
        urlBox.setSingleLine(true);
        urlBox.setTextSize(14);
        urlBox.setHint("Ara veya adres yaz");
        urlBox.setTextColor(0xFF222222);
        urlBox.setHintTextColor(0xFF888888);
        urlBox.setPadding(dp(14), 0, dp(14), 0);
        urlBox.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBox.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFFFFFFF);
        g.setCornerRadius(dp(20));
        urlBox.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1);
        lp.leftMargin = dp(10);
        top.addView(urlBox, lp);
        urlBox.setOnEditorActionListener((v, a, e) -> {
            go(urlBox.getText().toString());
            InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(urlBox.getWindowToken(), 0);
            return true;
        });
        urlBox.setOnFocusChangeListener((v, f) -> { if (f) urlBox.selectAll(); });

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        content = new FrameLayout(this);
        bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        btn("◀", v -> { if (cur().w.canGoBack()) cur().w.goBack(); });
        btn("▶", v -> { if (cur().w.canGoForward()) cur().w.goForward(); });
        btn("⌂", v -> loadHome(cur()));
        tabBtn = btn("1", v -> showTabs());
        btn("⋮", v -> showMenu());

        root.addView(top);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottom, new LinearLayout.LayoutParams(-1, dp(48)));
        setContentView(root);

        Uri d = getIntent().getData();
        newTab(false, d != null ? d.toString() : null);
    }

    TextView btn(String t, View.OnClickListener l) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(22);
        v.setGravity(Gravity.CENTER);
        v.setOnClickListener(l);
        bottom.addView(v, new LinearLayout.LayoutParams(0, -1, 1));
        btns.add(v);
        return v;
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getData() != null) newTab(false, i.getData().toString());
    }

    @Override protected void onResume() {
        super.onResume();
        for (Tab t : tabs) applySettings(t.w, t);
        applyTheme();
    }

    @Override protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    void applyTheme() {
        int c = color();
        top.setBackgroundColor(c);
        bottom.setBackgroundColor(c);
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[2] *= 0.8f;
        getWindow().setStatusBarColor(Color.HSVToColor(hsv));
        double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255;
        int fg = lum > 0.6 ? 0xFF000000 : 0xFFFFFFFF;
        for (TextView t : btns) t.setTextColor(fg);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(fg));
        File f = new File(getFilesDir(), "logo.png");
        if (f.exists()) logoView.setImageBitmap(BitmapFactory.decodeFile(f.getPath()));
        else logoView.setImageResource(R.drawable.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(brand(), null, c));
    }

    @SuppressWarnings("deprecation")
    void applySettings(WebView w, Tab t) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(sp.getBoolean("js", true));
        s.setDomStorageEnabled(!t.priv);
        s.setBlockNetworkImage(sp.getBoolean("img", false));
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        if (t.priv) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); s.setSaveFormData(false); }
        if (sp.getBoolean("desk", false))
            s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
        else s.setUserAgentString(null);
        if (Build.VERSION.SDK_INT >= 29)
            s.setForceDark(sp.getBoolean("dark", false) ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
    }

    void go(String s) {
        s = s.trim();
        if (s.isEmpty()) return;
        String u;
        if (s.startsWith("http://") || s.startsWith("https://")) u = s;
        else if (!s.contains(" ") && s.contains(".")) u = "https://" + s;
        else {
            try { u = engine() + "?q=" + URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { u = s; }
        }
        cur().w.loadUrl(u);
    }

    void newTab(boolean priv, String url) {
        final Tab t = new Tab();
        t.priv = priv;
        t.w = makeWebView(t);
        tabs.add(t);
        showTab(tabs.size() - 1);
        if (url == null) loadHome(t); else t.w.loadUrl(url);
        if (priv) Toast.makeText(this, "Gizli sekme açıldı", Toast.LENGTH_SHORT).show();
    }

    void showTab(int i) {
        content.removeAllViews();
        cur = i;
        content.addView(tabs.get(i).w);
        String u = tabs.get(i).w.getUrl();
        urlBox.setText(u == null || u.contains("home.local") ? "" : u);
        tabBtn.setText(String.valueOf(tabs.size()));
    }

    void closeTab(int i) {
        Tab t = tabs.remove(i);
        content.removeAllViews();
        if (t.priv) {
            t.w.clearHistory();
            t.w.clearCache(true);
            CookieManager.getInstance().removeAllCookies(null);
        }
        t.w.destroy();
        if (tabs.isEmpty()) newTab(false, null);
        else showTab(Math.min(i, tabs.size() - 1));
    }

    WebView makeWebView(final Tab t) {
        WebView w = new WebView(this);
        applySettings(w, t);
        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (sp.getBoolean("ad", true)) {
                    String h = r.getUrl().getHost();
                    if (h != null) for (String a : ADS)
                        if (h.contains(a))
                            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http") || u.startsWith("about:") || u.startsWith("data:")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                return true;
            }
            @Override public void onPageStarted(WebView v, String u, Bitmap f) {
                if (tabs.indexOf(t) == cur) urlBox.setText(u.contains("home.local") ? "" : u);
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                if (tabs.indexOf(t) != cur) return;
                progress.setProgress(p);
                progress.setVisibility(p >= 100 ? View.INVISIBLE : View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView v, String title) { t.title = title; }
        });
        w.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setMimeType(mime);
                r.addRequestHeader("User-Agent", ua);
                r.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                Toast.makeText(this, "İndirme başladı", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "İndirilemedi", Toast.LENGTH_SHORT).show();
            }
        });
        return w;
    }

    String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;"); }

    void loadHome(Tab t) {
        String hex = String.format("#%06X", 0xFFFFFF & color());
        String b = esc(brand());
        String logo = "";
        File f = new File(getFilesDir(), "logo.png");
        try {
            if (f.exists()) {
                byte[] data = new byte[(int) f.length()];
                FileInputStream in = new FileInputStream(f);
                in.read(data);
                in.close();
                logo = "<img class=l src='data:image/png;base64," + Base64.encodeToString(data, Base64.NO_WRAP) + "'>";
            }
        } catch (Exception e) { }
        if (logo.isEmpty()) logo = "<div class=l style='background:" + hex + ";color:#fff;font:700 40px sans-serif;line-height:72px'>"
            + esc(b.substring(0, 1)) + "</div>";
        String[][] dial = {{"Google", "https://www.google.com"}, {"YouTube", "https://www.youtube.com"},
            {"Wikipedia", "https://tr.wikipedia.org"}, {"GitHub", "https://github.com"},
            {"Reddit", "https://www.reddit.com"}, {"X", "https://x.com"}};
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta name=viewport content='width=device-width,initial-scale=1'><style>")
          .append(":root{--c:").append(hex).append(";--bg:#fff;--fg:#222;--card:#f2f2f2}")
          .append("@media(prefers-color-scheme:dark){:root{--bg:#121212;--fg:#eee;--card:#242424}}")
          .append("body{margin:0;padding:40px 20px;background:var(--bg);color:var(--fg);font-family:sans-serif;text-align:center}")
          .append(".l{width:72px;height:72px;border-radius:18px;object-fit:cover;margin:0 auto}")
          .append("h1{margin:12px 0 24px;color:var(--c)}")
          .append("input{width:100%;max-width:440px;box-sizing:border-box;padding:14px 20px;border-radius:26px;border:2px solid var(--c);font-size:16px;background:var(--card);color:var(--fg);outline:none}")
          .append(".g{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;max-width:440px;margin:32px auto}")
          .append(".g a{background:var(--card);border-radius:14px;padding:22px 6px;color:var(--fg);text-decoration:none;font-size:14px;border-bottom:3px solid var(--c)}")
          .append("</style></head><body>").append(logo).append("<h1>").append(b).append("</h1>")
          .append("<form action='").append(engine()).append("' method=get><input name=q placeholder='Ara veya adres yaz' autocomplete=off></form>")
          .append("<div class=g>");
        for (String[] d : dial) sb.append("<a href='").append(d[1]).append("'>").append(d[0]).append("</a>");
        sb.append("</div></body></html>");
        t.w.loadDataWithBaseURL("https://home.local/", sb.toString(), "text/html", "utf-8", null);
        if (tabs.indexOf(t) == cur) urlBox.setText("");
    }

    void showTabs() {
        String[] names = new String[tabs.size()];
        for (int i = 0; i < names.length; i++)
            names[i] = (i == cur ? "● " : "") + (tabs.get(i).priv ? "🕶 " : "") + tabs.get(i).title;
        new AlertDialog.Builder(this).setTitle("Sekmeler")
            .setItems(names, (d, i) -> showTab(i))
            .setPositiveButton("+ Yeni", (d, i) -> newTab(false, null))
            .setNeutralButton("Bu sekmeyi kapat", (d, i) -> closeTab(cur))
            .show();
    }

    String onoff(String k, boolean def) { return sp.getBoolean(k, def) ? "Açık" : "Kapalı"; }

    void toggle(String k, boolean def) {
        sp.edit().putBoolean(k, !sp.getBoolean(k, def)).apply();
        for (Tab t : tabs) applySettings(t.w, t);
        cur().w.reload();
    }

    void showMenu() {
        String[] items = {"Yeni sekme", "Gizli sekme",
            "Reklam engelleyici: " + onoff("ad", true),
            "Gece modu: " + onoff("dark", false),
            "Masaüstü sitesi: " + onoff("desk", false),
            "Veri tasarrufu (resimsiz): " + onoff("img", false),
            "Paylaş", "Ayarlar"};
        new AlertDialog.Builder(this).setItems(items, (d, i) -> {
            switch (i) {
                case 0: newTab(false, null); break;
                case 1: newTab(true, null); break;
                case 2: toggle("ad", true); break;
                case 3: toggle("dark", false); break;
                case 4: toggle("desk", false); break;
                case 5: toggle("img", false); break;
                case 6:
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, String.valueOf(cur().w.getUrl()));
                    startActivity(Intent.createChooser(s, "Paylaş"));
                    break;
                case 7: startActivity(new Intent(this, SettingsActivity.class)); break;
            }
        }).show();
    }

    @Override public void onBackPressed() {
        if (cur().w.canGoBack()) cur().w.goBack();
        else if (tabs.size() > 1) closeTab(cur);
        else super.onBackPressed();
    }
}
