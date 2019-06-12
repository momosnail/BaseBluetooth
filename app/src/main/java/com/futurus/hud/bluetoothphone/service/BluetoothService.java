package com.futurus.hud.bluetoothphone.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.mediatek.bluetooth.BluetoothProfileManager;
import com.futurus.hud.bluetoothphone.common.CachedBluetoothDevice;
import com.futurus.hud.bluetoothphone.common.LocalBluetoothProfileManager;

import timber.log.Timber;

public class BluetoothService extends Service {

    private IntentFilter mIntentFilter;
    private static final String A2DP_ROLE_CHANGED = "android.bluetooth.a2dp.role_change";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String receivedAction = intent.getAction();
            Timber.d("receivedAction: " + receivedAction);
            if (receivedAction.equals(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED) || receivedAction.equals(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE)) {

            } else if (receivedAction.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {

            } else if (receivedAction.equals(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Timber.i("isConnected:" + device.isConnected());

            } else if (receivedAction.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {

            } else if (receivedAction.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

            } else if (receivedAction.equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)) {

            } else if (receivedAction.equals(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED)) {

            } else if (receivedAction.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {

            } else if (receivedAction.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {


            } else if (receivedAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initRreceiver();
    }

    private void initRreceiver() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE);
        mIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);

        mIntentFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mIntentFilter.addAction(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED);
        mIntentFilter.addAction(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE);
        mIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mIntentFilter.addAction(A2DP_ROLE_CHANGED);
        this.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
