package com.example.detection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

//권한을 허용하는 클래스
public class PermissionSupport {
    public final static int MULTIPLE_PERMISSION = 1;
    private final Context context;
    private final Activity activity;
    private final String[] permissions;


    //생성자
    public PermissionSupport(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        //권한 할당
        ArrayList<String> permission = new ArrayList<>();
        permission.add(Manifest.permission.CAMERA);
        permission.add(Manifest.permission.INTERNET);
        permission.add(Manifest.permission.RECORD_AUDIO);
        permission.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permission.add(Manifest.permission.BLUETOOTH);
            permission.add(Manifest.permission.BLUETOOTH_CONNECT);
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            permission.add(Manifest.permission.BLUETOOTH);
        }
        //String[] 배열로 변환
        permissions = permission.toArray(new String[0]);
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
