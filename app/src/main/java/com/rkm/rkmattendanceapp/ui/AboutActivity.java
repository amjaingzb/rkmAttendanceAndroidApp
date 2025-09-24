// In: app/src/main/java/com/rkm/rkmattendanceapp/ui/AboutActivity.java
package com.rkm.rkmattendanceapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rkm.rkmattendanceapp.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle("About");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView versionText = findViewById(R.id.text_app_version);
        TextView emailText = findViewById(R.id.text_developer_email);

        String versionName = getAppVersionName();
        versionText.setText("Version " + versionName);

        emailText.setOnClickListener(v -> {
            sendFeedbackEmail(versionName);
        });
    }

    private String getAppVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private void sendFeedbackEmail(String version) {
        String email = "amjain.gzb@gmail.com";
        String subject = "Feedback for SevaConnect Halasuru (v" + version + ")";
        String body = "\n\n" +
                      "--------------------\n" +
                      "Please describe the issue or your suggestion above.\n" +
                      "App Version: " + version + "\n" +
                      "--------------------";


        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
