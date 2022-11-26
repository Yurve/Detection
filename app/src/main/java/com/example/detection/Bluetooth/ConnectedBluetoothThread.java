package com.example.detection.Bluetooth;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;

//블루투스 스레드를 생성하는 클래스. 라즈베리파이에서 값을 받아오진 않기에 inputStream 은 없다.
public class ConnectedBluetoothThread extends Thread {
    private final BluetoothSocket mSocket;
    private final OutputStream outputStream;

    //생성자
    public ConnectedBluetoothThread(BluetoothSocket socket) throws IOException {
        mSocket = socket;
        outputStream = socket.getOutputStream();
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
