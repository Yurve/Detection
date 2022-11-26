package com.example.detection;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.detection.DB.ID;
import com.example.detection.DB.RoomDB;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText editText = findViewById(R.id.editText);
        Button button = findViewById(R.id.button);

        //권한 확인하기
        permissionCheck();

        //DB 생성
        RoomDB roomDB = RoomDB.getInstance(this);
        if (roomDB.userDAO().getAll().size() > 0) {
            //기존의 id를 유지하려면 그냥 버튼만 누르면 되게 text 에 기존의 id를 넣는다.
            editText.setText(roomDB.userDAO().getAll().get(0).getCameraId());
        }

        button.setOnClickListener(v -> {
            ID id = new ID();
            id.setCameraId(editText.getText().toString().trim());
            //기존의 데이터를 삭제한다. 카메라 id 를 1개로 유지하기 위해서 이다.
            if (roomDB.userDAO().getAll().size() > 0) {
                roomDB.userDAO().delete(roomDB.userDAO().getAll().get(0));
            }
            //다시 id를 저장한다.
            roomDB.userDAO().insert(id);
            //id를 저장한 상태로 카메라 액티비티를 실행한다.
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
            finish();
        });
    }

    //권한 허용
    public void permissionCheck() {
        PermissionSupport permissionSupport = new PermissionSupport(this, this);
        permissionSupport.checkPermissions();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionSupport.MULTIPLE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Ok", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}