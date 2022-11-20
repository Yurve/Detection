package com.example.detection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText editText = findViewById(R.id.editText);
        Button button = findViewById(R.id.button);

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
}