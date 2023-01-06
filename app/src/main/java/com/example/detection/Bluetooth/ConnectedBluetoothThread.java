package com.example.detection.Bluetooth;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//블루투스 스레드를 생성하는 클래스.
public class ConnectedBluetoothThread extends Thread {
    private final BluetoothSocket mSocket;
    private final OutputStream outputStream;
    private final InputStream inputStream;

    //생성자
    public ConnectedBluetoothThread(BluetoothSocket socket) throws IOException {
        mSocket = socket;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
    }

    @Override
    public void run() {
        byte[] data = new byte[100];
        while (true) {
            try {
                int length = inputStream.read(data);
                String msg = new String(data, 0, length);
                if (msg.equals("ack")) {
                    //mqtt 전송을 어떻게 해야할까...
                }
                data = new byte[100];
            } catch (IOException e) {
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
