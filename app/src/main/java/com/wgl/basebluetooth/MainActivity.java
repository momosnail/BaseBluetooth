package com.wgl.basebluetooth;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.mediatek.bluetooth.BluetoothProfileManager;
import com.wgl.basebluetooth.Adapter.CachedDevicesAdapter;
import com.wgl.basebluetooth.common.BluetoothCallback;
import com.wgl.basebluetooth.common.CachedBluetoothDevice;
import com.wgl.basebluetooth.common.LocalBluetoothAdapter;
import com.wgl.basebluetooth.common.LocalBluetoothManager;
import com.wgl.basebluetooth.common.LocalBluetoothProfileManager;
import com.wgl.basebluetooth.util.SPUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends Activity implements BluetoothCallback, BaseQuickAdapter.OnItemClickListener {
    private SwitchCompat mSwitchBluetooth;

    private Context mContext;
    private boolean mOpenBluetooth = false;
    private static final String A2DP_ROLE_CHANGED = "android.bluetooth.a2dp.role_change";
    private LocalBluetoothAdapter mLocalBluetoothAdapter;
    private IntentFilter mIntentFilter;
    private LocalBluetoothManager mLocalBluetoothManager;
    public static String mPin = "123456";
    private List<CachedBluetoothDevice> mDeviceList;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String receivedAction = intent.getAction();
            Timber.d("receivedAction: " + receivedAction);
            if (receivedAction.equals(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE)) {

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
    private RecyclerView mRecyclerView;
    private CachedDevicesAdapter mCachedDevicesAdapter;

    private void receiverInit() {
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
        mIntentFilter.addAction(A2DP_ROLE_CHANGED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        receiverInit();
        bluetoothInit();
        this.registerReceiver(mReceiver, mIntentFilter);
        initView();
        initData();
    }

    private void bluetoothInit() {
        mLocalBluetoothManager = LocalBluetoothManager.getInstance(mContext);
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        mLocalBluetoothAdapter = mLocalBluetoothManager.getBluetoothAdapter();
    }

    private void initData() {

        mSwitchBluetooth.setOnCheckedChangeListener(mOnCheckedChangeListener);
        checkBluetoothButtonState();
        openBluetooth();
        mDeviceList = (ArrayList) mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (int i = 0; i < mDeviceList.size(); i++) {
            Timber.i("------------------getName: " + mDeviceList.get(i).getDevice().getName());
            Timber.i("------------------getAddress: " + mDeviceList.get(i).getDevice().getAddress());
            Timber.i("------------------getAliasName: " + mDeviceList.get(i).getDevice().getAliasName());//别名
            Timber.i("------------------getAlias: " + mDeviceList.get(i).getDevice().getAlias());//别名
            Timber.i("------------------getBondState: " + mDeviceList.get(i).getDevice().getBondState());
            Timber.i("------------------getUuids: " + mDeviceList.get(i).getDevice().getUuids());
            Timber.i("------------------getType: " + mDeviceList.get(i).getDevice().getType());
            Timber.i("----------------------------------------------------");


        }
        initRecyclerView();

        String deviceName = mLocalBluetoothAdapter.getName();
        writeDeviceNamePin(deviceName, mPin);


    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mCachedDevicesAdapter = new CachedDevicesAdapter(R.layout.device_list_item, mDeviceList);
        mRecyclerView.setAdapter(mCachedDevicesAdapter);
        mCachedDevicesAdapter.setOnItemClickListener(this);

    }


    private void initView() {
        mSwitchBluetooth = (SwitchCompat) findViewById(R.id.switch_bluetooth);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);

    }

    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            Timber.i("isChecked: " + isChecked);
            if (isChecked) {
                mOpenBluetooth = true;

                mLocalBluetoothAdapter.setBluetoothEnabled(true);
            } else {
                mOpenBluetooth = false;
                mLocalBluetoothAdapter.setBluetoothEnabled(false);

            }
            Timber.i("bluetoothState: " + mLocalBluetoothAdapter.getBluetoothState());

        }
    };

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        checkBluetoothButtonState();
        handleStateChanged(bluetoothState);
    }


    @Override
    public void onScanningStateChanged(boolean started) {

    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {

    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {

    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void openBluetooth() {
        if (!mLocalBluetoothManager.getBluetoothAdapter().isEnabled()) {
            mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(true);
        }
    }

    private void checkBluetoothButtonState() {
        Timber.i("bluetoothState: " + mLocalBluetoothAdapter.getBluetoothState());
        if (mLocalBluetoothManager != null && mLocalBluetoothManager.getBluetoothAdapter().isEnabled()) {
            mOpenBluetooth = true;
            mSwitchBluetooth.setChecked(true);
            mSwitchBluetooth.setEnabled(true);

        } else {
            mOpenBluetooth = false;
            mSwitchBluetooth.setChecked(false);
            mSwitchBluetooth.setEnabled(false);
        }
    }

    private void handleStateChanged(int state) {
        Timber.i("handleStateChanged:state->" + state);
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitchBluetooth.setEnabled(false);
                mOpenBluetooth = false;
                break;
            case BluetoothAdapter.STATE_ON:
                mSwitchBluetooth.setEnabled(true);
                mOpenBluetooth = true;
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchBluetooth.setEnabled(false);
                mOpenBluetooth = true;
                break;
            case BluetoothAdapter.STATE_OFF:
                mSwitchBluetooth.setEnabled(true);
                mOpenBluetooth = false;
                break;
        }
    }


    private void writeDeviceNamePin(String deviceName, String pin) {
        Timber.i("deviceName: " + deviceName);
        SPUtils.putString(mContext, "DEVICE_NAME", deviceName);
        SPUtils.putString(mContext, "PIN", pin);
    }


    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        Toast.makeText(mContext,"点击了item"+position,Toast.LENGTH_SHORT).show();
    }
}
