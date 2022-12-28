package com.example.detection.DB;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

//데이터 항목
@Entity
public class ID implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "camera_id")
    private String cameraId;

    @ColumnInfo(name = "user_id")
    private String userId;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}