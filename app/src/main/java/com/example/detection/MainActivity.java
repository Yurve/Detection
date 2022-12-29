package com.example.detection;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.detection.Bluetooth.BluetoothConnect;
import com.example.detection.DB.ID;
import com.example.detection.DB.RoomDB;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends AppCompatActivity {
    private String cameraID;
    private String userID;
    private String bluetoothAddress;
    private EditText editText;
    private BluetoothConnect bluetoothConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.editText);
        Button button = findViewById(R.id.button);
        Button buttonQR = findViewById(R.id.buttonqr);

        //권한 확인하기
        permissionCheck();

        //DB 생성
        RoomDB roomDB = RoomDB.getInstance(this);
        if (roomDB.userDAO().getAll().size() > 0 && cameraID == null) {
            //기존의 id를 유지하려면 그냥 버튼만 누르면 되게 text 에 기존의 id를 넣는다.
            editText.setText(roomDB.userDAO().getAll().get(0).getCameraId());
        }

        button.setOnClickListener(v -> {
            ID id = new ID();
            //QR 코드를 통해 cameraID를 받지 않고 기존의 ID를 쓰는 경우
            if (cameraID == null) {
                id.setCameraId(editText.getText().toString().trim());
                id.setUserId(roomDB.userDAO().getAll().get(0).getUserId());
                id.setAddress(roomDB.userDAO().getAll().get(0).getAddress());
                //QR 코드를 통해 새로운 cameraID를 받는 경우
            } else {
                id.setCameraId(cameraID.trim());
                id.setUserId(userID);

                //페어링된 기기 주소 가져오기
                bluetoothAddress = bluetoothConnect.getAddress();
                id.setAddress(bluetoothAddress.trim());
            }
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

        //QR 코드 인식 버튼 여기서 카메라 id를 지정한다.
        buttonQR.setOnClickListener(v -> new IntentIntegrator(this).initiateScan());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // 초기유저 or 재설정 

        //qr 코드가 스캔되면
        if (result != null) {
            if (result.getContents() != null) {
                // UserId 와 CameraID를 받는다. ex) "sons/4"
                String id = result.getContents();
                int slash = id.indexOf("/");
                userID = id.substring(0, slash);
                cameraID = id.substring(slash + 1);
                editText.setText(this.cameraID);
            }

            //라즈베리파이와 블루투스 연결
            bluetoothConnect = new BluetoothConnect(this);
            //블루투스 켜기
            bluetoothConnect.bluetoothOn();
            //페어링된 기기 알람 띄우기
            bluetoothConnect.listPairedDevices();
        }
        super.onActivityResult(requestCode, resultCode, data);
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