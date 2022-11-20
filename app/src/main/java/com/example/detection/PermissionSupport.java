package com.example.detection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

//권한을 허용하는 클래스
public class PermissionSupport {
    public final static int MULTIPLE_PERMISSION = 1;
    private final Context context;
    private final Activity activity;
    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.INTERNET};


    //생성자
    public PermissionSupport(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    //권한 확인 메소드
    public void checkPermissions() {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    Toast.makeText(context, "권한 허용", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(activity, permissions, MULTIPLE_PERMISSION);
                }
            }
        }
    }

}
