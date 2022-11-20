package com.example.detection;

import android.graphics.RectF;

//결과를 모아놓은 클래스
public class Result {
    int classIndex;
    float score;
    RectF rect;

    public Result(int classIndex, float score, RectF rect) {
        this.classIndex = classIndex;
        this.score = score;
        this.rect = rect;
    }
}
