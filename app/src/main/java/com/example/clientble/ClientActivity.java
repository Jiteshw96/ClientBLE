package com.example.clientble;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ClientActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private CountDownTimer timer;
    private Boolean timerRunning = false;
    private ScanCallback mScanCallback;
    private BluetoothGatt mGatt;
    TextView DeviceInfoTextView,status;
    Button startScanning,refreshBtn,stopScanning,disconnect,showMessage;
    TextView receivedMsg;
    EditText messageEditText;
    public ListView lv, messageListView;
    public Boolean register =  false;
    public List<BluetoothDevice> mDevices;
    public List<String> mDevicesName;
    boolean mEchoInitialized;
    ArrayList<String>  messageList;
    ClientApplication clientApplication;
    AppDatabase db;
   // public static String SERVICE_STRING = "18902a9a-1f4a-44fe-936f-14c8eea41800";
   public static String SERVICE_STRING = "18902a9a-1f4a-44fe-936f-14c8eea41800";

    public static String CHARACTERISTIC_ECHO_STRING = "18902a9a-1f4a-44fe-936f-14c8eea41801";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mDevices = new ArrayList<BluetoothDevice>();
        mDevicesName = new ArrayList<String>();
        DeviceInfoTextView = (TextView) findViewById(R.id.client_device_info_text_view);
        startScanning = (Button) findViewById(R.id.start_scanning_button);
        receivedMsg = findViewById(R.id.msgReceived);
        status = findViewById(R.id.status);
        stopScanning = (Button) findViewById(R.id.stop_scanning_button);
        disconnect = (Button) findViewById(R.id.disconnect_button);
        refreshBtn = findViewById(R.id.refresh);
        showMessage =  findViewById(R.id.show_message);
        messageEditText = (EditText) findViewById(R.id.message_edit_text);
        Button send = (Button) findViewById(R.id.send_message_button);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        clientApplication = (ClientApplication) getApplicationContext();
        db= (AppDatabase) AppDatabase.getAppDatabase(ClientActivity.this);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receivedMsg.setText(clientApplication.getReceivedMsg());
                status.setText(clientApplication.getStatus());
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* if (timerRunning) {
                    timer.cancel();
                    timer = null;
                }*/
                String message = messageEditText.getText().toString();
               clientApplication.setSendMsg(message);
                //resetConnection();
               // sendRepeatMessages();

            }
        });
        startScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDevices = new ArrayList<>();
                mDevicesName = new ArrayList<>();
                startScan();
            }
        });
        stopScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetConnection();

            }
        });
        lv = (ListView) findViewById(R.id.device_list);

         messageList= new ArrayList<String>();
        messageListView = findViewById(R.id.message_list);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object o = lv.getItemAtPosition(i);
                clientApplication.setDeviceAddress(mDevices.get(i).getAddress());
                startClientService();
               // String device = o.toString();
               // connect(mDevices.get(i));


              /*  BluetoothDevice macDevice = mBluetoothAdapter.getRemoteDevice("40:45:AD:3E:C1:35");
                connect(macDevice);*/
                /*Intent intent = new Intent(ClientActivity.this, ChatActivity.class);
                startActivity(intent);*/
            }
        });

        showMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new getAll().execute(db);

            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* if(register){
            unregisterReceiver(mBroadcastReceiver3);
        }

        mBluetoothAdapter.cancelDiscovery();
        mGatt.disconnect();
        timer.cancel();
        timerRunning = false;
        mGatt.close();*/
    }

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device!=null)
                    mDevices.add(device);
                    mDevicesName.add(device.getName());

                lv.setAdapter(new ArrayAdapter<String>(ClientActivity.this, android.R.layout.simple_list_item_1, mDevicesName));
                //ArrayAdapter<BluetoothDevice> arrayAdapter = new ArrayAdapter(ClientActivity.this, android.R.layout.simple_list_item_1, mDevices);
            }
        }
    };


    private void startScan() {
        mBluetoothAdapter.startDiscovery();
        register = true;
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
    }

    private void stopScan() {
        mBluetoothAdapter.cancelDiscovery();
    }

    /*private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResullt(enableBtIntent, REQUEST_ENABLE_BT);
    }*/

   /* private void connect(BluetoothDevice device) {
        //mGatt = device.connectGatt(this, true, mCallback);
        final BluetoothDevice device1 = device;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (device1 != null) {
                    mGatt = device1.connectGatt(getApplicationContext(), false, mCallback);
                    stopScan();
                }
            }
        });
    }*/


   /* private void sendMessage() {
       // EditText messageEditText = (EditText) findViewById(R.id.message_edit_text);
        Date timeStamp = Calendar.getInstance().getTime();
        String[] parts = timeStamp.toString().split(" ");
        String time = parts[3];


        *//*int hours = Calendar.get(Calendar.HOUR_OF_DAY);
        int min = Calendar.get(Calendar.MINUTE);
        int seconds = Calendar.get(Calendar.SECOND);*//*

        BluetoothGattCharacteristic characteristic = BluetoothUtils.findEchoCharacteristic(mGatt);
        if (characteristic == null) {
            Log.i("Characteristic", "Unable to find echo characteristic.");
            try
            {
                mGatt.disconnect();
                mGatt.close();
            }
            catch(Exception e)
            {
                Log.i("Disconnect", "Problem disconnecting.");
            }
            return;
        }

        String message = time;
          //  message = "{ \"data\" : \"" + messageEditText.getText().toString()+"\"}";
        *//*    JSONObject obj = null;
        try {

             obj = new JSONObject(message);

            Log.d("Json Message", obj.toString());

        } catch (Throwable t) {
            Log.e("My App", "Could not parse malformed JSON: \"" + message + "\"");
        }*//*
        //{"data": "Edittext value"}
        Log.i("send", "Sending message: " + message);

        byte[] messageBytes = StringUtils.bytesFromString(message);
        if (messageBytes.length == 0) {
            //logError("Unable to convert message to bytes");
            return;
        }

        characteristic.setValue(messageBytes);

        boolean success = mGatt.writeCharacteristic(characteristic);
        if (success) {
            Log.i("write", "Wrote: " + StringUtils.byteArrayInHexFormat(messageBytes));
        } else {
            Log.i("write", "Failed to write data");
        }
    }

    public void initializeEcho() {
        mEchoInitialized = true;
        sendMessage();
    }
*/
   /* public BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    final BluetoothGatt mGatt = gatt;
                    Handler handler;
                    handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mGatt.discoverServices();

                        }
                    });
                    //gatt.discoverServices();


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    try
                    {
                        //onDestroy();
                        Log.i("no_conn", "Connection unsuccessful with status"+status);
                        Toast.makeText(getApplicationContext(),"Connection unsuccessful",Toast.LENGTH_LONG).show();
                        //mGatt.disconnect();
                        mGatt.close();
                 }
                    catch(Exception e)
                    {

                    }
                }
            }
            *//*else {
                //final int finalStatus = status;
                //Toast.makeText(ClientActivity.this, "Error!", Toast.LENGTH_SHORT).show();

            }*//*
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("Not success", "Device service discovery unsuccessful, status " + status);
                return;
            }
            //List<BluetoothGattService> matchingServices = gatt.getServices();
            gatt.getService(UUID.fromString(SERVICE_STRING));
            //Check error here

            List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.findCharacteristics(gatt);
            if (matchingCharacteristics.isEmpty()) {
                Log.i("No characteristics", "Unable to find characteristics.");
                return;
            }

            //log("Initializing: setting write type and enabling notification");
            for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                enableCharacteristicNotification(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           // super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("Write successful", "Characteristic written successfully");
            } else {
                Log.i("Unsuccessful", "Characteristic write unsuccessful, status: " + status);
                //mGatt.disconnect();
                mGatt.close();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("Read success", "Characteristic read successfully");
                readCharacteristic(characteristic);
            } else {
                Log.i("Read unsuccessful", "Characteristic read unsuccessful, status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("Characteristic change", "Characteristic changed, " + characteristic.getUuid().toString());
            readCharacteristic(characteristic);
        }

        private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
            if (characteristicWriteSuccess) {
                Log.i("Notific success", "Characteristic notification set successfully for " + characteristic.getUuid().toString());
                if (BluetoothUtils.isEchoCharacteristic(characteristic)) {
                    initializeEcho();
                }
            } else {
                Log.i("Notification failure.", "Characteristic notification set failure for " + characteristic.getUuid().toString());
            }
        }

        private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            byte[] messageBytes = characteristic.getValue();
            Log.i("read", "Read: " + StringUtils.byteArrayInHexFormat(messageBytes));
            String message = StringUtils.stringFromBytes(messageBytes);
            if (message == null) {
                //logError("Unable to convert bytes to string");
                return;
            }
            clientApplication = (ClientApplication) getApplicationContext();

            Log.i("Received", "Received message: " + message);
            clientApplication.setReceivedMsg(message);


        }
    };

*/


    private void resetConnection(){
        if(register){
            unregisterReceiver(mBroadcastReceiver3);
        }
        mBluetoothAdapter.cancelDiscovery();
        mGatt.disconnect();
        timer.cancel();
        timerRunning = false;
        mGatt.close();
        mBluetoothAdapter = null;

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }


    private void startClientService(){

        if(isMyServiceRunning(ClientService.class)){
            stopClientService();
        }
        Intent serviceIntent = new Intent(this,ClientService.class);
        serviceIntent.putExtra("message", "Test");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        }
        else{
            startService(serviceIntent);
        }

    }

    private void stopClientService(){
        Intent  serviceIntent = new Intent(this,ClientService.class);
        stopService(serviceIntent);

    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    class getAll extends AsyncTask<AppDatabase, Void, Void> {

        List<BleDataFromClient> bleDataFromClientslist;

        @Override
        protected Void doInBackground(AppDatabase... db) {
            bleDataFromClientslist=new ArrayList<BleDataFromClient>();
            bleDataFromClientslist.addAll(db[0].userDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ListAdapter listAdapter = new ListAdapter(ClientActivity.this,bleDataFromClientslist);
            messageListView.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
        }
    }




}
