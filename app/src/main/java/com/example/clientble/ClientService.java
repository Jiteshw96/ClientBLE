package com.example.clientble;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static android.content.ContentValues.TAG;
import static com.example.clientble.ClientApplication.CHANNEL_ID;

public class ClientService extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    boolean mEchoInitialized;
    ClientApplication clientApplication;
    private CountDownTimer timer;
    AppDatabase db;
    private Boolean timerRunning = false;
   // public static String SERVICE_STRING = "18902a9a-1f4a-44fe-936f-14c8eea41800";
    public static String SERVICE_STRING = "18902a9a-1f4a-44fe-936f-14c8eea41800";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        clientApplication = (ClientApplication) getApplicationContext();
        BluetoothDevice macDevice = mBluetoothAdapter.getRemoteDevice(clientApplication.getDeviceAddress());
        db= (AppDatabase) AppDatabase.getAppDatabase(getApplicationContext());
        Intent notificationIntent = new Intent(this,ClientActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Client Service")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);
       sendRepeatMessages();

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }


    private void connect(BluetoothDevice device) {
        //mGatt = device.connectGatt(this, true, mCallback);
        final BluetoothDevice device1 = device;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (device1 != null) {
                    mGatt = device1.connectGatt(getApplicationContext(), false, mCallback);
                }
            }
        });
    }
    private void sendMessage() {
        // EditText messageEditText = (EditText) findViewById(R.id.message_edit_text);
        Date timeStamp = Calendar.getInstance().getTime();
        String[] parts = timeStamp.toString().split(" ");
        String time = parts[3];


        /*int hours = Calendar.get(Calendar.HOUR_OF_DAY);
        int min = Calendar.get(Calendar.MINUTE);
        int seconds = Calendar.get(Calendar.SECOND);*/

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

       // String message = time
        String clientMessage = clientApplication.getSendMsg().replace(" ", "");
        if(clientMessage == ""){
            clientMessage = "Test";
        }
      //  String deviceName = android.os.Build.MODEL.replace(" ","");
        String deviceName =  mBluetoothAdapter.getName();

        String message = time +" "+ clientMessage+" "+ deviceName +" "+clientApplication.getMacAddress();

        //  message = "{ \"data\" : \"" + messageEditText.getText().toString()+"\"}";
        /*    JSONObject obj = null;
        try {

             obj = new JSONObject(message);

            Log.d("Json Message", obj.toString());

        } catch (Throwable t) {
            Log.e("My App", "Could not parse malformed JSON: \"" + message + "\"");
        }*/
        //{"data": "Edittext value"}
        Log.i("send", "Sending message: " + message);

        BleDataFromClient bleDataFromClient= new BleDataFromClient();
        bleDataFromClient.setDevicename(deviceName);
        bleDataFromClient.setMessage(clientMessage);
        bleDataFromClient.setTime(time);
        bleDataFromClient.setDevicemacaddress(clientApplication.getMacAddress());
        bleDataFromClient.setStatus("0");
        db.userDao().insertAll(bleDataFromClient);

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

    public BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            clientApplication = (ClientApplication) getApplicationContext();

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
                    clientApplication.setStatus("Connected");


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    try
                    {
                        //onDestroy();
                        Log.i("no_conn", "Connection unsuccessful with status"+status);
                        Toast.makeText(getApplicationContext(),"Connection unsuccessful",Toast.LENGTH_LONG).show();
                        //mGatt.disconnect();
                        clientApplication.setStatus("Disconnected");
                        mGatt.close();
                    }
                    catch(Exception e)
                    {

                    }
                }
            }
            /*else {
                //final int finalStatus = status;
                //Toast.makeText(ClientActivity.this, "Error!", Toast.LENGTH_SHORT).show();

            }*/
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i("Not success", "Device service discovery unsuccessful, status " + status);
                return;
            }
            String uuid = gatt.getServices().get(0).getUuid().toString();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService gattService : gatt.getServices()) {
                    Log.i(TAG, "Service UUID Found: " + gattService.getUuid().toString());
                }
            }

         //  gatt.getService(UUID.fromString(SERVICE_STRING));

            Log.i("Get Service",gatt.getService(UUID.fromString(uuid)).toString());
            gatt.getService(UUID.fromString(uuid));

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
             super.onCharacteristicWrite(gatt, characteristic, status);
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
            String[] parts= message.split(" ");

            BleDataFromClient bleDataFromClient= new BleDataFromClient();
            bleDataFromClient.setDevicename(parts[2]);
            bleDataFromClient.setMessage(parts[1]);
            bleDataFromClient.setTime(parts[0]);
            bleDataFromClient.setDevicemacaddress(parts[3]);
            bleDataFromClient.setStatus("1");
            db.userDao().insertAll(bleDataFromClient);
            clientApplication.setReceivedMsg(message);

          //  mBluetoothAdapter.cancelDiscovery();
          //  mGatt.disconnect();
            // mGatt.close();
           // stopSelf();
        }
    };

    private void sendRepeatMessages(){
        timerRunning = true;
        clientApplication = (ClientApplication) getApplicationContext();
        final BluetoothDevice macDevice = mBluetoothAdapter.getRemoteDevice(clientApplication.getDeviceAddress());
        timer = new CountDownTimer(300000, 20000) {

            @Override
            public void onTick(long millisUntilFinished) {

                if(mGatt!=null) {
                  try {
                     mGatt.disconnect();
                     mGatt.close();
                     Thread.sleep(100);
                  }
                  catch(Exception e)
                  {
                      Log.e("Error", "Error: " + e.toString());
                  }
                }
                connect(macDevice);
            }

            @Override
            public void onFinish() {
                try{
                    startTimerAgain();
                }catch(Exception e){
                    Log.e("Error", "Error: " + e.toString());
                }
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    private void startTimerAgain(){

        timer.start();
    }



}
