package com.example.detection.Bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.widget.Toast;

import com.example.detection.DB.ID;
import com.example.detection.DB.RoomDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.annotations.Nullable;

//블루투스를 연결하는 클래스 Serializable 를 상속받아 intent 로 전달 가능하게 함.
public class BluetoothConnect {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;  //블루투스 실행을 위한 클래스. 이 객체를 통해 장치 검색, 페어링된 기기 불러오기 등을 할 수 있음.
    private Set<BluetoothDevice> pairedBluetoothDevices; //현재 페어링된 기기 목록을 Set 형태로 저장
    private ConnectedBluetoothThread connectedBluetoothThread; //블루투스 쓰레드 클래스
    private String address; //블루투스로 연결할 장치의 주소
    private List<String> bluetoothDeviceList; // 페어링 되지 않은 기기 이름들
    private List<BluetoothDevice> newDevices; // 새로 생긴 기기들

    final private static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //블루투스 범용 고유 식별자
    final int DISCOVERY_TIMEOUT = 10000;

    //생성자
    public BluetoothConnect(Context context) {
        this.context = context;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //블루투스 켜기
    public void bluetoothOn()throws SecurityException {
        //블루투스가 켜져있는지 확인
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
            bluetoothAdapter.enable();
        }
    }

    //페어링된 기기 list 에 저장
    public void listPairedDevices() throws SecurityException {
        //페어링이 가능한 기기 목록을 Set 형태로 저장
        pairedBluetoothDevices = bluetoothAdapter.getBondedDevices();
        if (pairedBluetoothDevices.size() > 0) {
            //list 형태로 이름들만 저장
            List<String> listBluetoothDevices = new ArrayList<>();
            for (BluetoothDevice device : pairedBluetoothDevices) {
                listBluetoothDevices.add(device.getName());
            }
            listBluetoothDevices.add("새로운 기기 찾기");
            alertDialogBluetooth(listBluetoothDevices, null);
        } else {
            Toast.makeText(context, "페어링 된 기기가 없습니다", Toast.LENGTH_SHORT).show();
        }
    }


    //리스트 형태의 블루투스 목록을 화면에 보여주기
    public void alertDialogBluetooth(List<String> listBluetoothDevices, @Nullable String msg) {
        //메시지 띄우기
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("장치 선택");
        //동적으로 할당한 list 를 다시 배열로 변환
        CharSequence[] items = listBluetoothDevices.toArray(new CharSequence[0]);
        if (msg != null && msg.equals("new")) {
            builder.setItems(items, (dialog, item) -> connectSelectDevice(items[item].toString(),true));
        } else {
            //기기이름을 클릭하면 해당 기기를 연결하는 메소드로 연결
            builder.setItems(items, (dialog, item) -> connectSelectDevice(items[item].toString(),false));
        }
        //화면에 보여주기
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    //선택된 디바이스 블루투스 연결
    public void connectSelectDevice(String selectedDeviceName, boolean newDevice) throws SecurityException {
        if (selectedDeviceName.equals("새로운 기기 찾기")) {
            //새로운 블루투스 기기 찾기
            searchExtraDevice();
            //로딩창 객체 생성
            ProgressDialog progressDialog = new ProgressDialog(context);
            //로딩창을 투명하게
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            //로딩창 보여주기
            progressDialog.show();

            // 10 초 뒤에 다시 알람 발송
            new Handler().postDelayed(() -> {
                //로딩창 그만 보여주기 
                progressDialog.cancel();
                bluetoothAdapter.cancelDiscovery();
                alertDialogBluetooth(bluetoothDeviceList, "new");
            }, DISCOVERY_TIMEOUT);

        }
        if(!newDevice){
            //선택된 디바이스의 이름과 페어링 된 기기목록과 이름이 일치하면 연결
            for (BluetoothDevice device : pairedBluetoothDevices) {
                if (selectedDeviceName.equals(device.getName())) {
                    //BluetoothDevice 의 주소 저장
                    address = device.getAddress();
                    break;
                }
            }
        } else{
            // 새로운페어링 생성
            for(BluetoothDevice device : newDevices){
                if(device.getName().equals(selectedDeviceName)){
                    device.createBond();
                    //BluetoothDevice 의 주소 저장
                    address = device.getAddress();
                }
            }
        }
    }

    // 새로운 장치 찾기
    public void searchExtraDevice() throws SecurityException {
        //블루투스 검색 시작
        bluetoothAdapter.startDiscovery();
        bluetoothDeviceList = new ArrayList<>();
        newDevices = new ArrayList<>();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws SecurityException {
                String action = intent.getAction();
                // 새 장치를 찾으면
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //장치 객체 얻어오기
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    bluetoothDeviceList.add(device.getName());
                    newDevices.add(device);
                }
            }
        };
        //브로드 캐스트 등록
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);

    }

    public void bluetoothConnect() {
        try {
            ID id = RoomDB.getInstance(context).userDAO().getAll().get(0);
            String address = id.getAddress();
            //소켓을 연결하고 블루투스 스레드에서 시작
            BluetoothSocket bluetoothSocket = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
                    .createInsecureRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            String cameraId = id.getCameraId();
            connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket, cameraId);
            connectedBluetoothThread.start();
            Toast.makeText(context, "Bluetooth 연결 성공!", Toast.LENGTH_SHORT).show();
        } catch (IOException | SecurityException e) {
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


    public String getAddress() {
        return address;
    }
}
