package com.recom3.capture;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;

public final class CaptureActivity extends Activity {
    private TextView status;
    private Button activate, screenshot;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        public void run() {
            CaptureService service = CaptureService.current;
            status.setText(service == null ? "Capture stopped" :
                    service.address + ":" + CaptureService.PORT + "   Code: " + service.code
                    + "\n" + service.displayState
                    + (service.lastError.length() == 0 ? "" : "\n" + service.lastError));
            activate.setEnabled(service != null && !service.displayStarting());
            screenshot.setEnabled(service != null && service.displayReady);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        startService(new Intent(this, CaptureService.class));
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(12, 4, 12, 4);
        status = new TextView(this);
        status.setTextSize(16);
        root.addView(status);
        activate = button(root, "Activate screen capture", new View.OnClickListener() {
            public void onClick(View view) {
                new AlertDialog.Builder(CaptureActivity.this)
                        .setTitle("Activate screen capture")
                        .setMessage("The app first checks existing screen access. If needed, it can request root access already installed on these glasses. Allow this only if you trust your root manager. A successful root activation is remembered for future app launches. Glasses without system permission or root still need PC setup after a reboot.")
                        .setPositiveButton("Activate", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                CaptureService service = CaptureService.current;
                                if (service != null) service.activateDisplay(true);
                            }
                        }).setNeutralButton("Disable auto-start", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                getSharedPreferences("screen", MODE_PRIVATE).edit().putBoolean("start_on_open", false).commit();
                                CaptureService service = CaptureService.current;
                                if (service != null) service.displayState = service.displayReady ?
                                        "Screen ready; auto-start disabled" : "Screen auto-start disabled";
                            }
                        }).setNegativeButton("Cancel", null).show();
            }
        });
        screenshot = button(root, "Screenshot in 5 seconds", new View.OnClickListener() {
            public void onClick(View view) {
                CaptureService service = CaptureService.current;
                if (service != null && service.displayReady) {
                    service.delayedScreenshot();
                    finish();
                }
            }
        });
        button(root, "Keep running / return to apps", new View.OnClickListener() {
            public void onClick(View view) { finish(); }
        });
        button(root, "Stop Capture", new View.OnClickListener() {
            public void onClick(View view) {
                stopService(new Intent(CaptureActivity.this, CaptureService.class));
                finish();
            }
        });
        scroll.addView(root);
        setContentView(scroll);
    }

    private Button button(LinearLayout root, String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setOnClickListener(click);
        root.addView(button, new LinearLayout.LayoutParams(-1, 44));
        return button;
    }

    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
