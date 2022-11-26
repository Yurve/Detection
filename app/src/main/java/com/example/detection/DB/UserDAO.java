package com.example.detection.DB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.detection.DB.ID;

import java.util.List;

//데이터 액세스 객체(DAO) Data Access Object
@Dao
public interface UserDAO {
    @Query("SELECT * FROM ID")
        //ID 란 이름을 가진 테이블로부터 모든 값(*)을 가져오란 의미.(Select)
    List<ID> getAll();

    @Insert
    //id 삽입
    void insert(ID id);

    @Delete
    //기존의 id 삭제
    void delete(ID id);


}
