package com.example.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RectView extends View {
    //각각 불과 연기에 대한 맵 좌표값과 이름명이 담겨있다.
    private final Map<RectF, String> fireMap = new HashMap<>();
    private final Map<RectF, String> smokeMap = new HashMap<>();
    //색상을 다 따로 지정해준다.
    private final Paint firePaint = new Paint();
    private final Paint smokePaint = new Paint();
    private final Paint textPaint = new Paint();
    //PreProcess 클래스에서 저장된 객체들의 모임 (0: smoke , 1: fire)
    private String[] classes;

    public RectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        firePaint.setStyle(Paint.Style.STROKE);     //빈 사각형 그림
        firePaint.setStrokeWidth(10.0f);            //굵기 10
        firePaint.setColor(Color.RED);              //빨간색
        firePaint.setStrokeCap(Paint.Cap.ROUND);    //끝을 뭉특하게
        firePaint.setStrokeJoin(Paint.Join.ROUND);  //끝 주위도 뭉특하게
        firePaint.setStrokeMiter(100);              //뭉특한 정도 100도

        smokePaint.setStyle(Paint.Style.STROKE);
        smokePaint.setStrokeWidth(10.0f);
        smokePaint.setColor(Color.GRAY);
        smokePaint.setStrokeCap(Paint.Cap.ROUND);
        smokePaint.setStrokeJoin(Paint.Join.ROUND);
        smokePaint.setStrokeMiter(100);

        textPaint.setTextSize(60.0f);
        textPaint.setColor(Color.WHITE);
    }

    public ArrayList<Result> transFormRect(ArrayList<Result> resultArrayList) {
        //핸드폰의 기종에 따라 PreviewView 의 크기는 변한다.
        //가로: 1080(PreviewView) -> 720(ImageAnalysis) -> 640(bitmap) 순으로 압축 되었다. 즉 스케일은 (1024/720) * (720/640) => (1024/640) == 1.6
        //이떄 1080 -> 720는 비트맵을 자른것, 720 -> 640은 압축한 것이다.
        float scaleX = getWidth() / (float) ProcessOnnx.INPUT_SIZE;
        //세로: 2049(PreviewView) -> 1280(ImageAnalysis) -> 640(bitmap) 순으로 압축되었다.
        // 스케일은 (2049/1280) * (1280/640) => (2049/640) == 3.2, 이때 2049 -> 1280은 비트맵을 자른 것, 1280 -> 640은 압축한 것이다.
        float scaleY = getHeight() / (float) ProcessOnnx.INPUT_SIZE;

        for (Result result : resultArrayList) {
            //원본 크기에 맞게 수정을 해야한다.
            result.rect.left *= scaleX;
            result.rect.right *= scaleX;
            result.rect.top *= scaleY;
            result.rect.bottom *= scaleY;
        }
        return resultArrayList;
    }

    //초기화
    public void clear() {
        fireMap.clear();
        smokeMap.clear();
    }

    // Result 가 담긴 리스트들을 받아와서 각각의 해시맵 (fireMap, smokeMap)에 담는다.
    public void resultToList(ArrayList<Result> results) {
        for (Result result : results) {
            if (result.classIndex == 0) {
                //rectF에는 상자의 좌표값 , String 에는 객체명(화재 or 연기) 과 확률을 적는다.
                smokeMap.put(result.rect, classes[0] + ", " + Math.round(result.score * 100) + "%");
            } else {
                fireMap.put(result.rect, classes[1] + ", " + Math.round(result.score * 100) + "%");
            }
        }
    }


    //그림 그리는 메소드
    @Override
    protected void onDraw(Canvas canvas) {
        //fire 해시맵에서 RectF, String 를 가져와서 캔버스에 그린다.
        for (Map.Entry<RectF, String> fire : fireMap.entrySet()) {
            canvas.drawRect(fire.getKey(), firePaint);
            canvas.drawText(fire.getValue(), fire.getKey().left + 10.0f, fire.getKey().top + 60.0f, textPaint);
        }
        //smoke 해시맵에서 RectF, String 를 가져와서 캔버스에 그린다.
        for (Map.Entry<RectF, String> smoke : smokeMap.entrySet()) {
            canvas.drawRect(smoke.getKey(), smokePaint);
            canvas.drawText(smoke.getValue(), smoke.getKey().left + 10.0f, smoke.getKey().top + 60.0f, textPaint);
        }
        super.onDraw(canvas);
    }

    //PreProcess 클래스에서 저장된 객체들의 모임 (0: smoke , 1: fire)
    public void setClasses(String[] classes) {
        this.classes = classes;
    }
}

