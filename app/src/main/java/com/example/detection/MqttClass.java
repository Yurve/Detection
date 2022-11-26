package com.example.detection;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.detection.Bluetooth.BluetoothConnect;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

//서버로 사진 정보 전송하는 MQTT 클래스
public class MqttClass implements MqttCallback {
    private BluetoothConnect bluetoothConnect;
    static String CLIENT_ID = "s21_Ultra";
    static String SERVER_ADDRESS = "***************************";

    private final Activity activity;
    private MqttClient mqttClient;
    static String TOPIC_PREVIEW = "HkPlatform/Camera/Image";
    static String TOPIC_DETECT = "detect";
    static String TOPIC_CONTROL = "aicms/toCam";
    static String TOPIC_MOTOR = "aicms/controlCam";

    public MqttClass(Activity activity) {
        this.activity = activity;
    }

    //블루투스 객체 가져오기
    public void setBluetoothConnect(BluetoothConnect bluetoothConnect) {
        this.bluetoothConnect = bluetoothConnect;
    }

    public void connectMqtt() {
        MqttConnectOptions options = new MqttConnectOptions();
        //만약 끊겼다가 재연결 될 때 이전의 상태를 유지할 것인지 다시 새로운 연결을 시도 할 것인지 결정. false == 유지
        options.setCleanSession(false);
        //만약 특정 시간동안 보내는 게 없으면 끊어진다. 그 특정시간을 최대로 설정.
        options.setKeepAliveInterval(Integer.MAX_VALUE);
        //시간이 지나 연결이 멈추면 바로 재연결을 시도한다.
        //options.setAutomaticReconnect(true);

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
        connectMqtt();
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
    public void receive(String topic) {
        try {
            mqttClient.subscribe(topic);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
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
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void motorControl() {
        try {
            mqttClient.subscribe(TOPIC_MOTOR);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws IOException {
                    //블루투스 쓰레드가 살아있다면
                    if(bluetoothConnect.checkThread()) {
                        //블루투스로 전송
                        bluetoothConnect.write(message.toString());
                    }else{
                        //재연결
                        bluetoothConnect.connectAgain();
                        //재전송
                        bluetoothConnect.write(message.toString());
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
