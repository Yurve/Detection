package com.example.detection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.example.detection.Bluetooth.BluetoothConnect;
import com.example.detection.DB.ID;
import com.example.detection.DB.RoomDB;

import org.json.JSONObject;

import java.util.Iterator;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Async {
    private final MqttClass mqttClass;  //서버로 사진 정보 전송하는 MQTT 클래스
    private final CompositeDisposable disposable = new CompositeDisposable(); //비동기 처리
    private final DataProcess dataProcess; //서버로 전송을 위한 정보 처리 클래스
    private Long date1; //시간을 비교해서 mqtt 전송
    private Long date2; //시간을 비교해서 mqtt 전송
    private boolean next = true;
    private final Context context;

    public Async(Activity activity, Context context) {
        this.context = context;
        //MQTT 접속
        mqttClass = new MqttClass(activity, context);
        mqttClass.connectMqtt();
        //데이터 처리 클래스
        dataProcess = new DataProcess();
    }

    public Observable<JSONObject> sendRxJava(Bitmap image, @Nullable JSONObject rectJson, float intervalTime) {
        return io.reactivex.rxjava3.core.Observable.defer(new Supplier<ObservableSource<JSONObject>>() {
            @Override
            public ObservableSource<JSONObject> get() throws Throwable {
                JSONObject jsonObject = new JSONObject();
                //시간 비교를 위해 구분 true 일때는 date1 에 저장, false 일 떄는 date2 에 저장된다.
                //아래 '//전송에 성공한 경우, ture -> false 으로 바꾸거나, false -> true 로 바꾸어서 사진이 저장되는 시간을 다른 곳에 저장한다.'
                // 라고 저장한 함수가 실행 되면 다음 사진이 저장될 때 저장하는 시간을 다른 곳에 넣는다.
                // (current_date -> past_date) or (past_date -> current_date)
                if (intervalTime != 0) {
                    if (next) {
                        //현재시간을 구하는 함수, 정확히는 핸드폰이 켜지고 난 이후 부터의 절대적인 시간, 시간 비교를 할 떄 주로 사용된다.
                        date1 = SystemClock.elapsedRealtime();
                    } else {
                        date2 = SystemClock.elapsedRealtime();
                    }

                    //시간을 비교하여 특정 초 이상이 되면 not Ok 상태로 변한다.
                    boolean ok = dataProcess.diffTime(date1, date2, intervalTime);
                    if (!ok) {
                        //특정 초 이하라면 빈 json 객체를 전송한다.
                        return Observable.just(jsonObject);
                    }
                }
                //특정 초를 넘겼다면 각종 정보들을 json 에 담는다.

                //객체 검출 전송
                if (rectJson != null) {
                    ID id = RoomDB.getInstance(context).userDAO().getAll().get(0);
                    //UserId
                    jsonObject.put("UserId", id.getUserId());
                    //CameraId
                    jsonObject.put("CameraId", Integer.parseInt(id.getCameraId()));
                    //Created
                    jsonObject.put("Created", dataProcess.saveTime());
                    //Image
                    jsonObject.put("Image", dataProcess.bitmapToString(image));
                    //Info
                    //키 값을 반복자로 받아온다.
                    Iterator<String> iterator = rectJson.keys();
                    while (iterator.hasNext()) {
                        //키 값과 밸류값을 기존의 json 객체에 추가한다.
                        String name = iterator.next();
                        jsonObject.put(name, rectJson.get(name));
                    }
                    // 썸네일 전송
                } else {
                    jsonObject.put("Id", Integer.parseInt(RoomDB.getInstance(context).userDAO().getAll().get(0).getCameraId()));
                    jsonObject.put("Thumbnail", dataProcess.bitmapToString(image));
                }

                return Observable.just(jsonObject);
            }
        });
    }

    //MQTT 로 전송
    public void sendMqtt(String topic, Bitmap image, @Nullable JSONObject rectJson, float intervalTime) {
        disposable.add(sendRxJava(image, rectJson, intervalTime)
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(@NonNull JSONObject jsonObject) {
                        if (jsonObject.length() > 0) {
                            mqttClass.publish(topic, jsonObject);
                            //전송에 성공한 경우, ture -> false 으로 바꾸거나,
                            //false -> true 로 바꾸어서 사진이 저장되는 시간을 다른 곳에 저장한다.
                            next = !next;
                        }
                        Log.d("send", "success");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                }));
    }

    //mqtt 수신
    public void receiveMQTT(String... topics) {
        mqttClass.receive(topics);
    }

    //mqtt 블루투스 클래스 전달
    public void setBluetoothConnect(BluetoothConnect bluetoothConnect) {
        mqttClass.setBluetoothConnect(bluetoothConnect);
    }

    public void close() {
        disposable.clear();
        mqttClass.closeMqtt();
    }


}
