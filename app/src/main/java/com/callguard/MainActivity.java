package com.callguard;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Setup screen. You only need it once: type the prefixes to block, tap "Save", then tap
 * "Enable blocking" and confirm the system dialog. After that the blocking runs by itself
 * in the background — you never have to open this again.
 */
public class MainActivity extends Activity {

    static final String PREFS = "cfg";
    static final String KEY_PREFIXES = "prefixes";
    static final String DEFAULT_PREFIXES = "0942";
    private static final int REQ_ROLE = 1001;

    private EditText input;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float dp = getResources().getDisplayMetrics().density;
        int pad = (int) (24 * dp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextSize(22);
        title.setPadding(0, 0, 0, (int) (16 * dp));

        TextView label = new TextView(this);
        label.setText("پیش‌شماره یا شمارهٔ کامل برای بلاک (با , جدا کنید):");
        label.setPadding(0, 0, 0, (int) (6 * dp));

        input = new EditText(this);
        input.setHint("0942,09121234567");
        input.setText(getPrefixes(this));
        input.setSingleLine(true);

        Button save = new Button(this);
        save.setText("ذخیره");
        save.setOnClickListener(v -> {
            String t = input.getText().toString().trim();
            getPrefs(this).edit().putString(KEY_PREFIXES, t).apply();
            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show();
        });

        Button enable = new Button(this);
        enable.setText("فعال‌سازی بلاک تماس");
        enable.setOnClickListener(v -> requestRole());

        status = new TextView(this);
        status.setPadding(0, (int) (16 * dp), 0, 0);

        LinearLayout.LayoutParams full = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(title, full);
        root.addView(label, full);
        root.addView(input, full);
        root.addView(save, full);
        root.addView(enable, full);
        root.addView(status, full);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void requestRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "به اندروید ۱۰ یا بالاتر نیاز دارد", Toast.LENGTH_LONG).show();
            return;
        }
        RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "این دستگاه بلاک تماس را پشتیبانی نمی‌کند", Toast.LENGTH_LONG).show();
            return;
        }
        if (rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "از قبل فعال است", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        startActivityForResult(intent, REQ_ROLE);
    }

    private void updateStatus() {
        boolean held = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            held = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        }
        status.setText(held
                ? "وضعیت: فعال ✓ — در پس‌زمینه کار می‌کند."
                : "وضعیت: غیرفعال — دکمهٔ «فعال‌سازی» را بزنید.");
    }

    static SharedPreferences getPrefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String getPrefixes(Context c) {
        return getPrefs(c).getString(KEY_PREFIXES, DEFAULT_PREFIXES);
    }
}
