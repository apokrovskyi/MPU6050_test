package com.example.anglecom;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private BluetoothGatt bluetoothGatt = null;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic = null;
    private TextView TX;
    private TextView TY;
    private TextView TZ;

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==1){
                ByteBuffer buffer = ByteBuffer.wrap((byte[])msg.obj);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                TX.setText("X:"+String.format("%.2f",buffer.getFloat()));
                TY.setText("Y:"+String.format("%.2f",buffer.getFloat()));
                TZ.setText("Z:"+String.format("%.simple 2f",buffer.getFloat()));
            }
            super.handleMessage(msg);
        }
    };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Disconnect();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattCharacteristic = gatt.getServices().get(3).getCharacteristics().get(0);
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                PrintMsg("connected");
            } else {
                Disconnect();
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Message msg = handler.obtainMessage();
            msg.what = 1;
            msg.obj = characteristic.getValue();
            handler.sendMessage(msg);
        }
    };

    private void Disconnect() {
        if (bluetoothGatt != null)
            bluetoothGatt.disconnect();
        bluetoothGatt = null;
        bluetoothGattCharacteristic = null;
        PrintMsg("error");
    }

    private void PrintMsg(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TX.setText(message);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TX = findViewById(R.id.textViewX);
        TY = findViewById(R.id.textViewY);
        TZ = findViewById(R.id.textViewZ);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
                PrintMsg("connecting");

                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

                List<ScanFilter> filters = new ArrayList<>();
                filters.add(new ScanFilter.Builder().setDeviceName("CC41-A").build());

                ScanSettings settings = new ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

                scanner.startScan(filters, settings, new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        BluetoothDevice device = result.getDevice();

                        bluetoothGatt = device.connectGatt(getApplicationContext(), false, bluetoothGattCallback);
                    }
                });
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });
    }
}
