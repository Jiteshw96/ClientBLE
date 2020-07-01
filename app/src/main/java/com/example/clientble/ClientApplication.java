package com.example.clientble;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ClientApplication extends Application {
    public static final String CHANNEL_ID = "serverService";
    private String receivedMsg = "";

    public String getSendMsg() {
        return sendMsg;
    }

    public void setSendMsg(String sendMsg) {
        this.sendMsg = sendMsg;
    }

    private String sendMsg = " ";



    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    private String deviceAddress = "";

    public String getReceivedMsg() {
        return receivedMsg;
    }


    public void setReceivedMsg(String receivedMsg) {
        this.receivedMsg = receivedMsg;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    private  void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Example Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


}
