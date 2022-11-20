package com.example.detection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ProcessOnnx {
    private final Context context;
    static int INPUT_SIZE = 640;
    static int BATCH_SIZE = 1;
    static int PIXEL_SIZE = 3;
    public float imageMean = 0.0f;
    public float imageSTD = 255.0f;
    static String fileName = "fire_640.onnx";
    String labelName = "label_fire.txt";
    String[] classes;
    public float objectThresh = 0.8f; //확률값 80%가 넘겨야 화면에 보이게 설정

    public ProcessOnnx(Context context) {
        this.context = context;
    }

    //모델 불러오기
    public void loadModel() {
        //asset 파일 가져올 매니저 클래스
        AssetManager assetManager = context.getAssets();
        //가져올 파일 (현재는 빈 공간)
        File outputFile = new File(context.getFilesDir() + "/" + fileName);

        try {
            //asset 파일에 담겨있는 파일 가져오는 입력 스트림
            InputStream inputStream = assetManager.open(fileName);
            //받아올 공간을 지정한 (outputFile) 출력 스트림
            OutputStream outputStream = new FileOutputStream(outputFile);
            //바이트값으로 하나씩 복사 asset -> outputFile
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            //읽는 과정이 끝나면 모두 닫기
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //data 셋 가져오기
    public void loadDataSet() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelName)));
            String line;
            List<String> classList = new ArrayList<>();
            //한줄씩 읽어서 ArrayList 에 담기 크기 지정을 못하므로 동적 할당 (list)
            while ((line = reader.readLine()) != null) {
                classList.add(line);
            }
            //list -> 배열
            classes = new String[classList.size()];
            classList.toArray(classes);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FloatBuffer bitmapToFloatBuffer(Bitmap bitmap) {
        //새로운 Float Buffer 할당
        FloatBuffer buffer = FloatBuffer.allocate(BATCH_SIZE * PIXEL_SIZE * INPUT_SIZE * INPUT_SIZE);
        //position 을 0으로 할당
        buffer.rewind();
        //총 넓이
        int area = INPUT_SIZE * INPUT_SIZE;
        //넓이만큼의 배열 생성
        int[] bitmapData = new int[area];
        //pixel 값 받아와서 bitmapData 배열에 넣기
        bitmap.getPixels(bitmapData, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < INPUT_SIZE - 1; i++) {
            for (int j = 0; j < INPUT_SIZE - 1; j++) {
                //0~640*640 차례대로 하나씩 가져오기
                int idx = INPUT_SIZE * i + j;
                int pixelValue = bitmapData[idx];
                buffer.put(idx, (((pixelValue >> 16 & 0xFF) - imageMean) / imageSTD)); //r값을 정수형 -> 실수형
                buffer.put(idx + area, (((pixelValue >> 8 & 0xFF) - imageMean) / imageSTD)); //g값을 정수형 -> 실수형
                buffer.put(idx + area * 2, (((pixelValue & 0xFF) - imageMean) / imageSTD)); //b값을 정수형 -> 실수형
            }
        }
        //실수형 버퍼의 position 을 다시 0으로 바꾸기
        buffer.rewind();
        return buffer;
    }

    public Bitmap imageToBitmap(Image image) {
        //화면에 비춰지는 2차원 평면
        Image.Plane[] planes = image.getPlanes();

        //yuv 형태의 색상 표현 RGB 색상 표현 방식은 색 전부를 표현하지만, 용량이 너무 크기에 yuv 형태로 표현한다.
        //y = 빛의 밝기를 나타내는 휘도, u,v = 색상신호,  u가 클수록 보라색,파란색 계열 ,v가 클수록 빨간색, 초록색 계열이다.
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        //buffer 에 담긴 데이터를 limit 까지 읽어들일 수 있는 데이터의 갯수를 리턴한다.
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        //버퍼 최대크기를 모아놓은 바이트 배열
        byte[] yuvBytes = new byte[ySize + uSize + vSize];
        //buffer 의 offset 위치부터 length 길이 만큼 읽음.
        //각각 y,u,v 읽어오기
        yBuffer.get(yuvBytes, 0, ySize);
        vBuffer.get(yuvBytes, ySize, vSize);
        uBuffer.get(yuvBytes, ySize + vSize, uSize);

        //이 바이트 배열을 가지고 YUV 이미지 만들기
        YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //jpeg 파일로 압축
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 90, out);

        //비트맵으로 변환
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        //비트맵은 현재 회전되어있는 상태, 모델에 넣으려면 각도를 돌리고 넣어야 한다.
        Matrix matrix = new Matrix();
        //90도로 돌리고 넣어야한다.
        matrix.postRotate(90.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    //모델에 넣기 위해 사이즈를 압축한다.
    public Bitmap rescaleBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
    }


    public ArrayList<Result> outputsToNMSPredictions(float[][][] output, int rows) {
        ArrayList<Result> results = new ArrayList<>();

        // 각 bounding box 에 대해 가장 확률이 높은 Class 예측
        for (int i = 0; i < rows; ++i) {
            float confidence = output[0][i][4];
            int detectionClass = -1;
            float maxClass = 0;

            float[] _classes = new float[classes.length];
            //3차원 output 배열에서 5번부터있는 label 만 따로 빼서 새로운 1차원 클래스를 만든다.
            System.arraycopy(output[0][i], 5, _classes, 0, classes.length);

            //그 label 중에서 가장 값이 큰 값을 선정한다.
            for (int c = 0; c < classes.length; ++c) {
                if (_classes[c] > maxClass) {
                    detectionClass = c;
                    maxClass = _classes[c];
                }
            }

            //실제 확률 값은 4번의 확률값과 해당 label 의 확률값의 곱이다.
            float confidenceInClass = maxClass * confidence;
            //만약 그 확률 값이 특정 확률을 넘어서면 List 형태로 저장한다.
            if (confidence > objectThresh) {
                float xPos = output[0][i][0];
                float yPos = output[0][i][1];
                float width = output[0][i][2];
                float height = output[0][i][3];

                //사각형은 화면 밖으로 나갈 수 없으니 화면을 넘기면 최대 화면 값을 가지게 한다.
                RectF rectF = new RectF(Math.max(0, xPos - width / 2), Math.max(0, yPos - height / 2),
                        Math.min(INPUT_SIZE - 1, xPos + width / 2), Math.min(INPUT_SIZE - 1, yPos + height / 2));
                Result recognition = new Result(detectionClass, confidenceInClass, rectF);
                results.add(recognition);
            }
        }
        //0~25200개까지 결과에 담길 수 있다. 따라서 비최대 억제비를 통해 중복되는 사각형들을 제거하는 NMS 를 사용한다.
        return nms(results);
    }

    //nms 처리 메소드
    public ArrayList<Result> nms(ArrayList<Result> results) {
        ArrayList<Result> nmsList = new ArrayList<>();

        for (int k = 0; k < classes.length; k++) {
            //1.find max confidence per class
            PriorityQueue<Result> pq =
                    new PriorityQueue<>(50,
                            new Comparator<Result>() {
                                @Override
                                public int compare(Result o1, Result o2) {
                                    return Float.compare(o1.score, o2.score);
                                }
                            });

            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).classIndex == k) {
                    pq.add(results.get(i));
                }
            }

            //2.do non maximum suppression
            while (pq.size() > 0) {
                //insert detection with max confidence
                Result[] a = new Result[pq.size()];
                Result[] detections = pq.toArray(a);
                Result max = detections[0];
                nmsList.add(max);
                pq.clear();

                for (int j = 1; j < detections.length; j++) {
                    Result detection = detections[j];
                    RectF b = detection.rect;
                    if (box_iou(max.rect, b) < objectThresh) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsList;
    }

    protected float box_iou(RectF a, RectF b) {
        return box_intersection(a, b) / box_union(a, b);
    }

    protected float box_intersection(RectF a, RectF b) {
        float w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left);
        float h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top);
        if (w < 0 || h < 0) return 0;
        return w * h;
    }

    protected float box_union(RectF a, RectF b) {
        float i = box_intersection(a, b);
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
    }

    protected float overlap(float x1, float w1, float x2, float w2) {
        float l1 = x1 - w1 / 2;
        float l2 = x2 - w2 / 2;
        float left = Math.max(l1, l2);
        float r1 = x1 + w1 / 2;
        float r2 = x2 + w2 / 2;
        float right = Math.min(r1, r2);
        return right - left;
    }

}

