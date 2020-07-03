package com.example.clientble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BluetoothReciver extends BroadcastReceiver {

    ClientApplication clientApplication;
    public void onReceive(Context context, Intent intent) {

        clientApplication = (ClientApplication) context.getApplicationContext();
        String action = intent.getAction();
        Log.d("BroadcastActions", "Action "+action+"received");
        int state;
        BluetoothDevice bluetoothDevice;

        switch(action)
        {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_OFF)
                {
                    clientApplication.setStatus("Disconnected");
                    Toast.makeText(context, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                    Log.d("BroadcastActions", "Bluetooth is off");


                    //Intent serviceIntent = new Intent(context, BleTestService.class);
                    //context.stopService(serviceIntent);
                }
                else if (state == BluetoothAdapter.STATE_TURNING_OFF)
                {
                    Toast.makeText(context, "Bluetooth is turning off", Toast.LENGTH_SHORT).show();
                    Log.d("BroadcastActions", "Bluetooth is turning off");
                }
                else if(state == BluetoothAdapter.STATE_ON)
                {
                    Log.d("BroadcastActions", "Bluetooth is on");
                    Toast.makeText(context, "Bluetooth is turning on", Toast.LENGTH_SHORT).show();
                }
                break;

            case BluetoothDevice.ACTION_ACL_CONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "Connected to "+bluetoothDevice.getName(),
                        Toast.LENGTH_SHORT).show();
                Log.d("BroadcastActions", "Connected to "+bluetoothDevice.getName());
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "Disconnected from "+bluetoothDevice.getName(),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

}