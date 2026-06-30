package com.security.hub;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProgressBar ramProgressBar, storageProgressBar;
    private TextView ramText, storageText, securityAuditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout views
        LinearLayout ramCard = findViewById(R.id.ramCard);
        ramProgressBar = findViewById(R.id.ramProgressBar);
        storageProgressBar = findViewById(R.id.storageProgressBar);
        ramText = findViewById(R.id.ramText);
        storageText = findViewById(R.id.storageText);
        securityAuditText = findViewById(R.id.securityAuditText);

        // Initialize display readouts
        updateRamMeter();
        updateStorageMap();
        runSecurityAuditor();

        // Tap execution for RAM refresh
        ramCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRamMeter();
            }
        });
    }

    // 1. Visual RAM Meter Logic
    private void updateRamMeter() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            
            long totalRam = memoryInfo.totalMem / (1024 * 1024); // Convert bytes to Megabytes
            long availableRam = memoryInfo.availMem / (1024 * 1024);
            long usedRam = totalRam - availableRam;

            int percentage = (int) ((usedRam * 100) / totalRam);
            ramProgressBar.setProgress(percentage);
            
            ramText.setText("Used: " + usedRam + "MB / Total: " + totalRam + "MB (" + percentage + "% In Use)");
        }
    }

    // 2. Storage Capacity Mapping Logic
    private void updateStorageMap() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();

        long totalStorage = (totalBlocks * blockSize) / (1024 * 1024 * 1024); // Convert to Gigabytes
        long freeStorage = (availableBlocks * blockSize) / (1024 * 1024 * 1024);
        long usedStorage = totalStorage - freeStorage;

        int percentage = (int) ((usedStorage * 100) / totalStorage);
        storageProgressBar.setProgress(percentage);

        storageText.setText("Occupied: " + usedStorage + "GB / Total Space: " + totalStorage + "GB");
    }

    // 3. Security Auditor Logic (Checks Background Apps for Hidden Privileges)
    private void runSecurityAuditor() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        StringBuilder threatLog = new StringBuilder();
        int alertCount = 0;

        for (PackageInfo pkg : installedPackages) {
            // Filter out pre-installed system apps, examine only user-installed apps
            if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                boolean hasMicrophone = false;
                boolean hasLocation = false;

                if (pkg.requestedPermissions != null) {
                    for (String permission : pkg.requestedPermissions) {
                        if (permission.equals("android.permission.RECORD_AUDIO")) {
                            hasMicrophone = true;
                        }
                        if (permission.equals("android.permission.ACCESS_FINE_LOCATION")) {
                            hasLocation = true;
                        }
                    }
                }

                // Security flag criteria: App contains sensitive structural accesses
                if (hasMicrophone && hasLocation) {
                    CharSequence appName = packageManager.getApplicationLabel(pkg.applicationInfo);
                    threatLog.append("⚠️ ").append(appName).append(" holds background mic + GPS access.\n");
                    alertCount++;
                }
            }
        }

        if (alertCount == 0) {
            securityAuditText.setText("✅ Zero hidden background tracking patterns found in your local apps.");
            securityAuditText.setTextColor(0xFF388E3C); // Green indicator
        } else {
            securityAuditText.setText(threatLog.toString());
            securityAuditText.setTextColor(0xFFD32F2F); // Red alert indicator
        }
    }
}
