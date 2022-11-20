package com.example.detection;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ID.class}, version = 1)
public abstract class RoomDB extends RoomDatabase {
    private static RoomDB INSTANCE = null;

    public abstract UserDAO userDAO();

    //db 생성
    public static RoomDB getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), RoomDB.class, "ID.db")
                    //메인 쓰레드에서도 쿼리가가능하게 설정
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }

}
