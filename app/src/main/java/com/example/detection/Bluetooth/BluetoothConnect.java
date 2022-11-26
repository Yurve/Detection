package com.example.detection.Bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//블루투스를 연결하는 클래스 Serializable 를 상속받아 intent 로 전달 가능하게 함.
public class BluetoothConnect  {
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;  //블루투스 실행을 위한 클래스. 이 객체를 통해 장치 검색, 페어링된 기기 불러오기 등을 할 수 있음.
    private Set<BluetoothDevice> pairedBluetoothDevices; //현재 페어링된 기기 목록을 Set 형태로 저장
    private ConnectedBluetoothThread connectedBluetoothThread; //블루투스 쓰레드 클래스
    private BluetoothDevice bluetoothDevice; //블루투스 연결된 장치

    final private static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //블루투스 범용 고유 식별자

    //생성자
    public BluetoothConnect(Context context) {
        this.context = context;
    }

    //블루투스 켜기
    public void bluetoothOn() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //페어링된 기기 보여주기
    public void listPairedDevices() throws SecurityException {
        //블루투스가 켜져있는지 확인
        if (bluetoothAdapter.isEnabled()) {
            //페어링이 가능한 기기 목록을 Set 형태로 저장
            pairedBluetoothDevices = bluetoothAdapter.getBondedDevices();
            if (pairedBluetoothDevices.size() > 0) {
                //메시지 띄우기
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("장치 선택");
                //list 형태로 이름들만 저장
                List<String> listBluetoothDevices = new ArrayList<>();
                for (BluetoothDevice device : pairedBluetoothDevices) {
                    listBluetoothDevices.add(device.getName());
                }
                //동적으로 할당한 list 를 다시 배열로 변환
                CharSequence[] items = listBluetoothDevices.toArray(new CharSequence[0]);
                //기기이름을 클릭하면 해당 기기를 연결하는 메소드로 연결
                builder.setItems(items, (dialog, item) -> connectSelectDevice(items[item].toString()));
                //화면에 보여주기
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(context, "페어링 된 기기가 없습니다", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "블루투스가 비 활성화 상태입니다", Toast.LENGTH_SHORT).show();
            //활성화 되어있지 않다면 다시 키고 재실행
            bluetoothOn();
            listPairedDevices();
        }
    }

    //선택된 디바이스 블루투스 연결
    public void connectSelectDevice(String selectedDeviceName) throws SecurityException {
        //선택된 디바이스의 이름과 페어링 된 기기목록과 이름이 일치하면 연결
        for (BluetoothDevice device : pairedBluetoothDevices) {
            if (selectedDeviceName.equals(device.getName())) {
                bluetoothDevice = device;
                break;
            }
        }
        try {
            //소켓을 연결하고 블루투스 스레드에서 시작
            BluetoothSocket bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket);
            connectedBluetoothThread.start();
            Toast.makeText(context, "블루투스 연결 성공", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //종료하는 메서드
    public void close() throws IOException {
        connectedBluetoothThread.close();
    }

    //메시지 전송을 위한 메서드
    public void write(String message) throws IOException {
        connectedBluetoothThread.write(message);
    }

    //블루투스 쓰레드가 살아있는지 확인하는 메서드
    public boolean checkThread() {
        return connectedBluetoothThread != null;
    }

    //블루투스 재연결
    public void connectAgain() throws SecurityException, IOException {
        BluetoothSocket bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID);
        bluetoothSocket.connect();
        connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket);
        connectedBluetoothThread.start();
    }


}
