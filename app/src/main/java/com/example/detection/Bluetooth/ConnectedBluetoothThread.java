package com.example.detection.Bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.detection.DB.RoomDB;
import com.example.detection.MqttClass;
import com.example.detection.SupportMqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

//블루투스 스레드를 생성하는 클래스.
public class ConnectedBluetoothThread extends Thread {
    private final BluetoothSocket mSocket;
    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final String cameraId;

    //생성자
    public ConnectedBluetoothThread(BluetoothSocket socket, String cameraId) throws IOException {
        mSocket = socket;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        this.cameraId = cameraId;
    }

    @Override
    public void run() {
        byte[] data = new byte[100];
        while (true) {
            try {
                int length = inputStream.read(data);
                String msg = new String(data, 0, length);
                String[] degreeAndAck = msg.split("/");
                String degree = degreeAndAck[0];
                String Ack = degreeAndAck[1];

                if (Ack.equals("ack")) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("CameraId", Integer.parseInt(cameraId));
                    jsonObject.put("Degree", Float.parseFloat(degree));
                    // ack 를 받으면 mqtt 로 받았다고 서버에게 전송
                    MqttClient mqttClient = SupportMqtt.getInstance().getMqttClient();
                    mqttClient.publish(MqttClass.TOPIC_MOTOR_ACK, new MqttMessage(jsonObject.toString().getBytes(StandardCharsets.UTF_8)));
                }
                data = new byte[100];
            } catch (IOException | JSONException | MqttException e) {
                e.printStackTrace();
                break;
            }
        }

    }

    //블루투스로 라즈베리파이에 전송하는 메서드
    public void write(String message) throws IOException {
        //바이트 배열로 변환
        byte[] bytes = message.getBytes();
        //전송
        outputStream.write(bytes);
    }

    //소켓 연결 해제
    public void close() throws IOException {
        mSocket.close();
    }
}
