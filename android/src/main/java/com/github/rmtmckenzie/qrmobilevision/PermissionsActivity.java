package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;

@TargetApi(23)
public class PermissionsActivity extends Activity {

    static final int CAMERA_PERMISSIONS_REQUEST = 123;

    interface PermissionsCallback {
        void permissionsGranted(boolean wereGranted);
    }

    private static PermissionsCallback callback;
    static void setCallback(PermissionsCallback callback) {
        PermissionsActivity.callback = callback;
    }
    static void clearCallback() {
        PermissionsActivity.callback = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setTheme(R.style.MyTransparentTheme);

        super.onCreate(savedInstanceState);

        System.out.println("REQUYESTING PERMISSIONS");

        this.requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        System.out.println("onRequestPermissionsResult");

        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST: {
                System.out.println("CAMERA_PERMISSIONS_REQUEST");

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("GRanted");

                    // granted
                    if (PermissionsActivity.callback != null) {
                        PermissionsActivity.callback.permissionsGranted(true);
                    }
                } else {
                    System.out.println("dENIED");

                    // denied
                    if (PermissionsActivity.callback != null) {
                        PermissionsActivity.callback.permissionsGranted(false);
                    }
                }

                finish();
            }
        }
    }
}
