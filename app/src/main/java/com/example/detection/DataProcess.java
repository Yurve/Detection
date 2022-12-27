package com.example.detection;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//서버로 전송을 위한 정보 처리 클래스
public class DataProcess {

    //시간을 비교하는 메소드
    public boolean diffTime(Long time1, Long time2, float intervalTime) {
        //초기에는 past_date 가 없다. 그냥 true 리턴하자
        if (time2 == null) {
            return true;
        }
        //시간차이 구하기
        long difference = Math.abs(time1 - time2);
        //time 초 이내라면 사진 전송을 하지 않는다.
        return difference >= (intervalTime * 1000L);
    }

    //현재  시간을 구하는 메소드
    public String saveTime() {
        //현재 시간을 구한다.
        Date date = new Date(System.currentTimeMillis());
        //데이터의 형태를 지정한다. "년도-달-일 시.분.초" 형태이다.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.KOREA);
        //형태에 맞게 시간을 String 형태로 변환한다.
        return simpleDateFormat.format(date);
    }

    //비트맵을 Base64 인코딩을 통해 문자열로 변환하는 메소드
    public String bitmapToString(Bitmap bitmap) {
        //바이트를 보낼 통로
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //비트맵을 jpg 로 압축. 비트맵 용량이 보내기엔 크다. 90%로 압축
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        //바이트 배열로 받기
        byte[] image = byteArrayOutputStream.toByteArray();
        //String 으로 반환
        return Base64.encodeToString(image, Base64.NO_WRAP);
    }


}
