package com.freebrowser.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;

public class SettingsActivity extends Activity {
    SharedPreferences sp;
    LinearLayout box;
    ImageView preview;
    Button engineBtn;
    static final int PICK = 7;
    static final String[] ENG_N = {"Google", "DuckDuckGo", "Bing"};
    static final String[] ENG_U = {"https://www.google.com/search", "https://duckduckgo.com/", "https://www.bing.com/search"};
    static final int[] PRESETS = {0xFFFF1B2D, 0xFF1E88E5, 0xFF43A047, 0xFF8E24AA, 0xFFFB8C00, 0xFF00897B, 0xFF212121, 0xFFEC407A};

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("fb", MODE_PRIVATE);
        ScrollView sv = new ScrollView(this);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(32));
        sv.addView(box);
        setContentView(sv);
        refreshBar();

        header("Görünüm");
        addEdit("Uygulama adı (tarayıcı içinde görünür)", sp.getString("brand", "Free Browser"), "brand");

        label("Uygulama rengi");
        LinearLayout row = new LinearLayout(this);
        for (final int c : PRESETS) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(c);
            v.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.rightMargin = dp(8);
            row.addView(v, lp);
            v.setOnClickListener(x -> { sp.edit().putInt("color", c).apply(); refreshBar(); });
        }
        box.addView(row);
        addEdit("Özel renk (HEX, örn. #FF1B2D)", String.format("#%06X", 0xFFFFFF & sp.getInt("color", 0xFFFF1B2D)), "hex");

        label("Logo");
        preview = new ImageView(this);
        box.addView(preview, new LinearLayout.LayoutParams(dp(64), dp(64)));
        loadPreview();
        LinearLayout lr = new LinearLayout(this);
        Button pick = new Button(this);
        pick.setText("Logo seç");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, PICK);
        });
        Button reset = new Button(this);
        reset.setText("Sıfırla");
        reset.setOnClickListener(v -> { new File(getFilesDir(), "logo.png").delete(); loadPreview(); });
        lr.addView(pick);
        lr.addView(reset);
        box.addView(lr);

        header("Gizlilik ve Performans");
        addSwitch("Reklam engelleyici", "ad", true);
        addSwitch("Gece modu (siteleri karart)", "dark", false);
        addSwitch("Masaüstü sitesi", "desk", false);
        addSwitch("Veri tasarrufu (resimleri yükleme)", "img", false);
        addSwitch("JavaScript", "js", true);

        header("Arama motoru");
        engineBtn = new Button(this);
        engineBtn.setText(engineName());
        engineBtn.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(ENG_N, (d, i) -> {
            sp.edit().putString("engine", ENG_U[i]).apply();
            engineBtn.setText(ENG_N[i]);
        }).show());
        box.addView(engineBtn);

        header("Veriler");
        Button clear = new Button(this);
        clear.setText("Çerezleri, önbelleği ve site verilerini temizle");
        clear.setOnClickListener(v -> {
            CookieManager.getInstance().removeAllCookies(null);
            WebStorage.getInstance().deleteAllData();
            new WebView(this).clearCache(true);
            Toast.makeText(this, "Temizlendi", Toast.LENGTH_SHORT).show();
        });
        box.addView(clear);

        TextView note = new TextView(this);
        note.setText("\nNot: Ana ekrandaki uygulama ikonu ve adı Android tarafından derleme sırasında sabitlenir "
            + "(bkz. README). Buradaki ad, renk ve logo tarayıcı içinde anında değişir.");
        note.setTextSize(12);
        box.addView(note);
    }

    String engineName() {
        String u = sp.getString("engine", ENG_U[0]);
        for (int i = 0; i < ENG_U.length; i++) if (ENG_U[i].equals(u)) return ENG_N[i];
        return ENG_N[0];
    }

    void refreshBar() {
        if (getActionBar() != null)
            getActionBar().setBackgroundDrawable(new ColorDrawable(sp.getInt("color", 0xFFFF1B2D)));
    }

    void loadPreview() {
        File f = new File(getFilesDir(), "logo.png");
        if (f.exists()) preview.setImageBitmap(BitmapFactory.decodeFile(f.getPath()));
        else preview.setImageResource(R.drawable.ic_launcher);
    }

    void header(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(16);
        v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(sp.getInt("color", 0xFFFF1B2D));
        v.setPadding(0, dp(22), 0, dp(4));
        box.addView(v);
    }

    void label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(13);
        v.setPadding(0, dp(10), 0, dp(4));
        box.addView(v);
    }

    void addSwitch(String t, final String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(t);
        s.setChecked(sp.getBoolean(key, def));
        s.setPadding(0, dp(8), 0, dp(8));
        s.setOnCheckedChangeListener((v, c) -> sp.edit().putBoolean(key, c).apply());
        box.addView(s);
    }

    void addEdit(String l, String val, final String key) {
        label(l);
        EditText e = new EditText(this);
        e.setText(val);
        e.setSingleLine(true);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                String v = s.toString();
                if (key.equals("hex")) {
                    try { sp.edit().putInt("color", Color.parseColor(v)).apply(); refreshBar(); } catch (Exception ex) { }
                } else sp.edit().putString(key, v).apply();
            }
        });
        box.addView(e);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK && res == RESULT_OK && data != null) {
            try {
                Uri u = data.getData();
                Bitmap bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(u));
                int s = Math.min(bm.getWidth(), bm.getHeight());
                Bitmap sq = Bitmap.createBitmap(bm, (bm.getWidth() - s) / 2, (bm.getHeight() - s) / 2, s, s);
                Bitmap out = Bitmap.createScaledBitmap(sq, 256, 256, true);
                FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "logo.png"));
                out.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
                loadPreview();
            } catch (Exception e) {
                Toast.makeText(this, "Logo yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
