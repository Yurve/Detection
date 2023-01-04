package com.example.detection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.detection.Bluetooth.BluetoothConnect;
import com.example.detection.DB.ID;
import com.example.detection.DB.RoomDB;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

//서버로 사진 정보 전송하는 MQTT 클래스
public class MqttClass implements MqttCallback {
    private BluetoothConnect bluetoothConnect;
    static public String CLIENT_ID = "android";
    static public String SERVER_ADDRESS = "********************";

    private final Activity activity;
    private final Context context;
    private MqttClient mqttClient;
    static public String TOPIC_PREVIEW = "camera/update/thumbnail";
    static public String TOPIC_DETECT = "event/create";
    static public String TOPIC_CONTROL = "aicms/toCam";
    static public String TOPIC_MOTOR = "camera/update/degree/syn";
    static public String TOPIC_MOTOR_ACK = "camera/update/degree/ack";
    static public String TOPIC_WEBRTC = "call/start";
    static public String TOPIC_WEBRTC_FIN = "call/stop";

    public MqttClass(Activity activity, Context context) {
        this.context = context;
        this.activity = activity;
    }

    //블루투스 객체 가져오기
    public void setBluetoothConnect(BluetoothConnect bluetoothConnect) {
        this.bluetoothConnect = bluetoothConnect;
    }

    public void connectMqtt() {
        MqttConnectOptions options = new MqttConnectOptions();
        //만약 끊겼다가 재연결 될 때 이전의 상태를 유지할 것인지 다시 새로운 연결을 시도 할 것인지 결정. false == 유지 true로 변경해보자
        options.setCleanSession(false);
        //만약 특정 시간동안 보내는 게 없으면 끊어진다. 그 특정시간을 최대로 설정.
        options.setKeepAliveInterval(Integer.MAX_VALUE);
        //시간이 지나 연결이 멈추면 바로 재연결을 시도한다.
        options.setAutomaticReconnect(true);

        MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(activity.getFilesDir().getAbsolutePath());
        try {
            mqttClient = new MqttClient(SERVER_ADDRESS, CLIENT_ID, persistence);
            mqttClient.setCallback(this);
            mqttClient.connect(options);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(activity, "mqtt 연결 성공!", Toast.LENGTH_SHORT).show());

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //만약 연결에 실패했다면 다시 연결을 시도한다.
    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    //전송하는 메소드
    public void publish(String topic, JSONObject jsonMessage) {
        MqttMessage message = new MqttMessage(jsonMessage.toString().getBytes(StandardCharsets.UTF_8));
        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            //만약에 전송을 실패했다면 재연결
            connectMqtt();
        }
    }

    //수신받는 메소드
    public void receive(String... topics) {
        try {
            mqttClient.subscribe(topics);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    cause.printStackTrace();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws IOException, JSONException {
                    if (topic.equals(MqttClass.TOPIC_CONTROL)) {
                        //문자를 받으면 사이즈를 변환하거나 기존의 전송 속도를 변경할 수 있다.
                        String s = message.toString();
                        //문자열을 자른다. ("scale = 0.5" or "interval = 1")
                        StringTokenizer st = new StringTokenizer(s);
                        String scaleOrInterval = st.nextToken();
                        //첫번째 문자열이 scale 이라면
                        if (scaleOrInterval.equals("scale")) {
                            // "="을 제거한다.
                            st.nextToken();
                            //이후 문자열을 숫자로 변환한 후 객체 검출시 전송하는 사진의 크기를 수정한다.
                            CameraActivity.scale = Float.parseFloat(st.nextToken());
                            //만약 첫번째 문자열이 interval 이라면
                        } else if (scaleOrInterval.equals("interval")) {
                            // "="을 제거한다.
                            st.nextToken();
                            //이후 문자열을 숫자로 변환한 후 서버로 보내는 미리보기 사진의 시간간격을 수정한다.
                            CameraActivity.interval_time = Float.parseFloat(st.nextToken());
                        }
                        //TOPIC 이 모터제어라면
                    } else if (topic.equals(MqttClass.TOPIC_MOTOR)) {
                        Log.d("블루투스", message.toString());
                        //블루투스 쓰레드가 살아있다면
                        if (bluetoothConnect != null && bluetoothConnect.checkThread()) {
                            //json 객체로 읽기
                            JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                            if ((jsonObject.get("CameraId") + "").equals(RoomDB.getInstance(context).userDAO().getAll().get(0).getCameraId())) {
                                String msg = jsonObject.get("Degree") + "";
                                //블루투스로 각도값 전송
                                bluetoothConnect.write(msg);
                                //제어 정보를 수정했다고 다시 서버로 알림
                                publish(MqttClass.TOPIC_MOTOR_ACK,jsonObject);
                            }
                        }
                        // webRTC 를 하자고 신청이 오면
                    } else if (topic.equals(MqttClass.TOPIC_WEBRTC)) {
                        //문자열을 읽어서 현재 내 아이디가 맞는지 확인하고 맞다면 전송을한다.
//                        JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
//                        String userId = (String) jsonObject.get("UserId");
//                        int cameraId = (int) jsonObject.get("CameraId");
                        String msg = message.toString();
                        int slash = msg.indexOf("/");
                        //처음 userID
                        String userID = msg.substring(0, slash);
                        //뒷 부분은 cameraID
                        String cameraID = msg.substring(slash + 1);
                        //만약 카메라 ID가 동일하다면 웹사이트 접속
                        if (cameraID.equals(RoomDB.getInstance(context).userDAO().getAll().get(0).getCameraId())) {
                            //해당 웹사이트 주소 이후
                            String url = "*********************************";
                            Intent intent = new Intent(activity, WebVIewActivity.class);
                            intent.putExtra("url", url);
                            activity.startActivity(intent);
                        }
                        // webRTC 종료 요청
                    } else if (topic.equals(MqttClass.TOPIC_WEBRTC_FIN)) {
                        //문자열을 읽어서 내 아이디가 맞다면 webRTC 를 종료한다.
                        JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                        String userId = (String) jsonObject.get("UserId");
                        int cameraId = (int) jsonObject.get("CameraId");
                        ID id = RoomDB.getInstance(context).userDAO().getAll().get(0);
                        if (userId.equals(id.getUserId()) && cameraId == Integer.parseInt(id.getCameraId())) {
                            WebVIewActivity webVIewActivity = (WebVIewActivity) WebVIewActivity.webViewActivity;
                            webVIewActivity.finish();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    //앱 종료시 종료를 해줘야한다.
    public void closeMqtt() {
        try {
            mqttClient.disconnect();
            mqttClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

}
