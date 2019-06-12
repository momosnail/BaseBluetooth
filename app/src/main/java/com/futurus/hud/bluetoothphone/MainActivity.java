package com.futurus.hud.bluetoothphone;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.futurus.hud.bluetoothphone.adapter.CachedDevicesAdapter;
import com.futurus.hud.bluetoothphone.common.BluetoothCallback;
import com.futurus.hud.bluetoothphone.common.CachedBluetoothDevice;
import com.futurus.hud.bluetoothphone.common.LocalBluetoothAdapter;
import com.futurus.hud.bluetoothphone.common.LocalBluetoothManager;
import com.futurus.hud.bluetoothphone.common.LocalBluetoothProfile;
import com.futurus.hud.bluetoothphone.common.LocalBluetoothProfileManager;
import com.futurus.hud.bluetoothphone.util.SPUtils;
import com.futurus.hud.bluetoothphone.util.ToastUtils;
import com.futurus.hud.bluetoothphone.util.Utils;
import com.mediatek.bluetooth.BluetoothProfileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
    private List<CachedBluetoothDevice> mDeviceList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private CachedDevicesAdapter mCachedDevicesAdapter;
    private ProgressBar mProgressBar;
    private SwitchCompat mSwitchBluetoothScan;
    private SwitchCompat mSwitchDiscover;
    private boolean isScaning = false;
    private boolean mBluetoothIsDiscovery = true;
    static final int DEFAULT_DISCOVERABLE_TIMEOUT = 0; //NEVER TIME OUT
    private int mProfileState = 0;
    private CharSequence[] mConnectItems = {};
    public static boolean[] mCheckedItems = {false, false, false, false, false, false};

    private String[] arr1 = {"Pair", "Connect", "Return"};
    private String[] arr2 = {"unPair", "Connect", "Return"};
    private String[] arr3 = {"unPair", "disConnect", "Return"};
    private String[] arr = arr1;
    private ArrayList<HashMap<String, Object>> mOperationsList;
    private AlertDialog mConnectAsDialog;
    private boolean mIsconnect = false;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String receivedAction = intent.getAction();
            Timber.d("receivedAction: " + receivedAction);
            if (receivedAction.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {


            } else if (receivedAction.equals(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED) || receivedAction.equals(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE)) {
                int a2dp = mLocalBluetoothAdapter.getConnectionState();
                if (a2dp == BluetoothProfile.A2DP) {
                    Timber.e("isConnd");
                }
            } else if (receivedAction.equals(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE)) {
                mCachedDevicesAdapter.setNewData(mDeviceList);
            } else if (receivedAction.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {

            } else if (receivedAction.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

            } else if (receivedAction.equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)) {

            } else if (receivedAction.equals(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED)) {

            } else if (receivedAction.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {

            } else if (receivedAction.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);
                Timber.i("mode =" + mode);
                if (mode != BluetoothAdapter.ERROR) {
                    handleModeChanged(mode);
                }

            } else if (receivedAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

            }

        }
    };
    private CachedBluetoothDevice mCachedBluetoothDevice;

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
        mIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mIntentFilter.addAction(A2DP_ROLE_CHANGED);
        this.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        receiverInit();
        bluetoothInit();
        initView();
        initData();
    }

    private void bluetoothInit() {
        mLocalBluetoothManager = LocalBluetoothManager.getInstance(mContext);
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        mLocalBluetoothAdapter = mLocalBluetoothManager.getBluetoothAdapter();
    }

    private void initData() {
        initOnclick();
        initRecyclerView();
        checkBluetoothButtonState();
        openBluetooth();
        String deviceName = mLocalBluetoothAdapter.getName();
        writeDeviceNamePin(deviceName, mPin);

    }

    private void initOnclick() {
        mSwitchBluetooth.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchBluetoothScan.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchDiscover.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private void initRecyclerView() {
        mDeviceList = (ArrayList) mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mCachedDevicesAdapter = new CachedDevicesAdapter(R.layout.device_list_item, mDeviceList);
        mRecyclerView.setAdapter(mCachedDevicesAdapter);
        mCachedDevicesAdapter.setOnItemClickListener(this);

    }


    private void initView() {
        mSwitchBluetooth = (SwitchCompat) findViewById(R.id.switch_bluetooth);
        mSwitchDiscover = (SwitchCompat) findViewById(R.id.switch_discover);
        mSwitchBluetoothScan = (SwitchCompat) findViewById(R.id.switch_bluetooth_san);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

    }

    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            Timber.i("isChecked: " + isChecked);
            switch (buttonView.getId()) {
                case R.id.switch_bluetooth:
                    if (isChecked) {
                        mOpenBluetooth = true;

                        mLocalBluetoothAdapter.setBluetoothEnabled(true);
                    } else {
                        mOpenBluetooth = false;
                        mLocalBluetoothAdapter.cancelDiscovery();
                        mLocalBluetoothAdapter.setBluetoothEnabled(false);

                        mProgressBar.setVisibility(View.GONE);

                    }
                    break;
                case R.id.switch_bluetooth_san:
                    if (isChecked) {
                        scanDevice(false);
                    } else {
                        scanDevice(true);
                    }
                    break;
                case R.id.switch_discover:
                    bluetoothDiscoverability();
                    break;

            }

            Timber.i("bluetoothState: " + mLocalBluetoothAdapter.getBluetoothState());

        }
    };


    private void bluetoothDiscoverability() {
        if (!mBluetoothIsDiscovery) {
            int timeout = DEFAULT_DISCOVERABLE_TIMEOUT;

            if (mLocalBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,//表示本地蓝牙适配器上启用了查询扫描和页面扫描。因此，该设备既可被发现，又可从远程蓝牙设备连接。
                    timeout)) {
                setDiscoveryModeStatus(true);


                Timber.i("setScanMode : true");
            } else {
                setDiscoveryModeStatus(false);

                Timber.i("setScanMode : false 1");
            }

        } else {
            mLocalBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);//表示已禁用查询扫描，但在本地蓝牙适配器上启用了页面扫描。因此，远程蓝牙设备无法发现此设备，但可以从之前发现此设备的远程设备进行连接。
            setDiscoveryModeStatus(false);
            Timber.i("setScanMode : false 2");
        }
    }


    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        Timber.i("onBluetoothStateChanged-bluetoothState:" + bluetoothState);
        checkBluetoothButtonState();
        handleStateChanged(bluetoothState);
        refreshDataList();
    }


    @Override
    public void onScanningStateChanged(boolean started) {
        Timber.i("onScanningStateChanged-started:" + started);
        if (started) {
            mProgressBar.setVisibility(View.VISIBLE);
            mSwitchBluetoothScan.setChecked(true);

        } else {
            mProgressBar.setVisibility(View.GONE);
            mSwitchBluetoothScan.setChecked(false);
        }
        isScaning = started;
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        Timber.i("onDeviceAdded-cachedDevice:" + cachedDevice);
        if (!mDeviceList.contains(cachedDevice)) {
            mDeviceList.add(cachedDevice);
            mCachedDevicesAdapter.setNewData(mDeviceList);
        } else {
            Timber.i("onDeviceAdded contains");

        }

    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        Timber.i("onDeviceDeleted-cachedDevice:" + cachedDevice);
        for (int i = mDeviceList.size() - 1; i >= 0; i--) {
            if (cachedDevice.getDevice().getAddress().equals(mDeviceList.get(i).getDevice().getAddress())) {
                mDeviceList.remove(i);
            }
        }
        mCachedDevicesAdapter.setNewData(mDeviceList);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        Timber.i("onDeviceBondStateChanged-cachedDevice:" + cachedDevice + " bondState:" + bondState);
        mCachedDevicesAdapter.setNewData(mDeviceList);

    }

    @Override
    protected void onResume() {
        if (mLocalBluetoothManager != null && mLocalBluetoothAdapter != null) refreshDataList();
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocalBluetoothManager == null) return;
        if (mLocalBluetoothAdapter.isDiscovering()) scanDevice(true);

    }

    @Override
    protected void onStop() {
        scanDevice(true);
        super.onStop();
    }

    private void openBluetooth() {
        if (!mLocalBluetoothManager.getBluetoothAdapter().isEnabled()) {
            mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(true);
            scanDevice(false);
        } else {
            if (!mLocalBluetoothAdapter.isDiscovering()) {
                scanDevice(false);
            } else {
                mSwitchBluetoothScan.setChecked(true);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void checkBluetoothButtonState() {
        Timber.i("bluetoothState: " + mLocalBluetoothAdapter.getBluetoothState());
        if (mLocalBluetoothManager != null && mLocalBluetoothManager.getBluetoothAdapter().isEnabled()) {
            mOpenBluetooth = true;
            mSwitchBluetooth.setChecked(true);
            mSwitchBluetooth.setEnabled(true);
            mSwitchDiscover.setEnabled(true);
            mSwitchBluetoothScan.setEnabled(true);

        } else {
            mOpenBluetooth = false;
            mSwitchBluetooth.setChecked(false);
            mSwitchBluetooth.setEnabled(false);
            mSwitchDiscover.setEnabled(false);
            mSwitchBluetoothScan.setEnabled(false);
        }

        int scanMode = mLocalBluetoothAdapter.getScanMode();
        handleModeChanged(scanMode);
    }

    private void handleModeChanged(int scanMode) {
        Timber.i("handleModeChanged---scanMode:" + scanMode);
        if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {//表示本地蓝牙适配器上启用了查询扫描和页面扫描。因此，该设备既可被发现，又可从远程蓝牙设备连接。
            setDiscoveryModeStatus(true);
        } else {
            setDiscoveryModeStatus(false);
        }
    }

    private void setDiscoveryModeStatus(boolean isDiscoveryable) {
        mBluetoothIsDiscovery = isDiscoveryable;
        Timber.i("isDiscoveryable ==" + isDiscoveryable);
        mSwitchDiscover.setChecked(isDiscoveryable);
        if (isDiscoveryable) {
            mSwitchDiscover.setText("可被发现");
        } else {
            mSwitchDiscover.setText("不可发现");
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
                scanDevice(false);//开启就扫描
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchBluetooth.setEnabled(false);
                mOpenBluetooth = true;
                mDeviceList.clear();
                mCachedDevicesAdapter.setNewData(mDeviceList);
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

        Toast.makeText(mContext, "点击了item" + position, Toast.LENGTH_SHORT).show();
        CachedBluetoothDevice mDeviceInfo = mDeviceList.get(position);
        invokePairConnectDialog(mDeviceInfo);
    }

    private void invokePairConnectDialog(CachedBluetoothDevice deviceInfo) {
        String deviceStatus = getResources().getString(deviceInfo.getConnectedState());
//        mSelectedDevice = (BluetoothDevice)getIntent().getParcelableExtra("SELECTED_DEVICE");
//        if (mCachedBluetoothDevice==null){
//            Timber.d("cachedDevice is null!");
//            return;
//        }
        mCachedBluetoothDevice = deviceInfo;
        mOperationsList = new ArrayList<HashMap<String, Object>>();
        final String[] mPerationsArr = new String[3];
        if (mLocalBluetoothManager.getBluetoothAdapter() != null) {
            if (deviceStatus.equals(this.getResources().getString(R.string.bt_status_paired))) {
                arr = arr2;
            } else if (deviceStatus.equals(this.getResources().getString(R.string.bt_status_unpair))) {
                arr = arr1;
            } else {
                arr = arr3;
            }

            for (int i = 0; i < 3; i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                String name = "";
                if (arr[i].equals("unPair")) {
                    name = this.getString(R.string.dispair_bt_string);
                } else if (arr[i].equals("Pair")) {
                    name = this.getString(R.string.trypair_bt_string);
                } else if (arr[i].equals("disConnect")) {
                    name = this.getString(R.string.disconnect_bt_string);
                } else if (arr[i].equals("Connect")) {
                    for (int j = 0; j < MainActivity.mCheckedItems.length; j++) {
                        MainActivity.mCheckedItems[j] = false;
                    }
                    name = this.getString(R.string.connect_bt_string);
                } else if (arr[i].equals("Return")) {
                    name = this.getString(R.string.bt_pairconnect_return);
                }
                map.put("operations_name", name);
                mOperationsList.add(map);
                mPerationsArr[i] = name;
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(deviceInfo.getDevice().getName());
        builder.setItems(mPerationsArr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ToastUtils.showShortToast(MainActivity.this, "点击了item" + which + ":" + mPerationsArr[which]);
                HashMap<String, Object> operationsInfo = mOperationsList.get(which);
                String operations_name = (String) operationsInfo.get("operations_name");
                Timber.i("operations_name =" + operations_name);
                if (operations_name.equals(getString(R.string.trypair_bt_string))) {
                    if (mLocalBluetoothManager.getBluetoothAdapter().isDiscovering()) {
                        mLocalBluetoothManager.getBluetoothAdapter().cancelDiscovery();
                    }
                    if (mCachedBluetoothDevice != null) {
                        if (mCachedBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                            ToastUtils.showShortToast(getApplicationContext(), "The Bluetooth device is bonded already");
                        } else if (mCachedBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                            ToastUtils.showShortToast(getApplicationContext(), "The Bluetooth device is bonding ,please wait");
                        } else {

                            if (!mCachedBluetoothDevice.startPairing()) {
                                Timber.i("createBond Error");
                            }

                        }
                    }
                } else if (operations_name.equals(getString(R.string.dispair_bt_string))) {
                    if (mLocalBluetoothManager.getBluetoothAdapter().isDiscovering()) {
                        mLocalBluetoothManager.getBluetoothAdapter().cancelDiscovery();
                    }
                    if (mCachedBluetoothDevice != null) {
                        if (mCachedBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                            mCachedBluetoothDevice.unpair();
                        } else {
                            ToastUtils.showShortToast(MainActivity.this, "The Bluetooth device is unbonded already");
                        }
                    }
                } else if (operations_name.equals(getString(R.string.connect_bt_string))) {
                    if (mCachedBluetoothDevice != null) {
                        int state = mCachedBluetoothDevice.getBondState();
                        Timber.i("state ==" + state);
                        if (state == BluetoothDevice.BOND_BONDED) {
                            List<String> mProfileList = initBtSettings(mCachedBluetoothDevice, mProfileState, false);
                            mConnectItems = (CharSequence[]) mProfileList.toArray(new CharSequence[mProfileList.size()]);
                            if (mProfileList.contains(getString(R.string.bluetooth_profile_a2dp))) {
                                mCachedBluetoothDevice.connectProfileName(getString(R.string.bluetooth_profile_a2dp));

                            } else {
                                Timber.e("该配对设备不包含媒体协议");
                            }


//                            AlertDialog.Builder protocolDialog = new AlertDialog.Builder(MainActivity.this);
//                            protocolDialog.setTitle(getString(R.string.connect_bt_string));
//                            protocolDialog.setMultiChoiceItems(mConnectItems, mCheckedItems, mMultiClickListener);
//                            protocolDialog.setPositiveButton(getString(android.R.string.ok), mClickListener);
//                            mConnectAsDialog = protocolDialog.create();
//                            mConnectAsDialog.show();
                        } else {

                            Timber.e("pair Bluetooth first");
                            ToastUtils.showShortToast(getApplicationContext(), "We will pair Bluetooth device first");
                            if (!mCachedBluetoothDevice.startPairing()) {
                                Timber.e("pair Bluetooth device Error");
                            } else {
                                mIsconnect = true;
                            }
                        }

                    }
                } else if (operations_name.equals(getString(R.string.disconnect_bt_string))) {
                    Utils.disconnectBT(MainActivity.this, mCachedBluetoothDevice);
                    ToastUtils.showShortToast(getApplicationContext(), "Disconnecting Bluetooth device");
                    for (int i = 0; i < MainActivity.mCheckedItems.length; i++) {
                        MainActivity.mCheckedItems[i] = false;
                    }
                } else if (operations_name.equals(getString(R.string.bt_pairconnect_return))) {
                    ToastUtils.showShortToast(getApplicationContext(), "Return");
                } else {
                    ToastUtils.showShortToast(getApplicationContext(), "Error");
                }

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private final DialogInterface.OnClickListener mClickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Timber.d("BUTTON_POSITIVE Clicked ");

                    }
                }
            };

    private final DialogInterface.OnMultiChoiceClickListener mMultiClickListener = new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which, boolean isChecked) {
            Timber.d("Item " + which + " changed to " + isChecked);
            Timber.d("Item [which]= " + mConnectItems[which]);

            if (isChecked) {
                mCachedBluetoothDevice.connectProfileName(mConnectItems[which].toString());
                ToastUtils.showShortToast(getApplicationContext(), "Connecting Bluetooth device");
            } else {
                mCachedBluetoothDevice.disconnectProfileName(mConnectItems[which].toString());
                ToastUtils.showShortToast(getApplicationContext(), "Disconnecting Bluetooth device");
            }
            mCheckedItems[which] = isChecked;
        }
    };

    private List<String> initBtSettings(CachedBluetoothDevice cachedBluetoothDevice, int profileState, boolean b) {
        int index = 0;
        List<String> items = new ArrayList<>();
        Timber.d("mCachedBluetoothDevice =" + mCachedBluetoothDevice + " ,mProfileState=" + mProfileState);
        for (LocalBluetoothProfile profile : mCachedBluetoothDevice.getConnectableProfiles()) {
            items.add(getString(profile.getNameResource(mCachedBluetoothDevice.getDevice())));
        }
        return items;
    }


    private void scanDevice(final boolean enable) {
        Timber.i("scanDevice enable ==" + enable);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mLocalBluetoothAdapter.stopScanning();
            mProgressBar.setVisibility(View.GONE);
        } else {
            if (mDeviceList != null)
                mDeviceList.clear();
            mLocalBluetoothManager.getCachedDeviceManager().clearNonBondedDevices(); //清除没有配对的设备
            refreshDataList();
            mLocalBluetoothAdapter.startScanning(true);
        }
    }


    private void refreshDataList() {
        if (!mOpenBluetooth) {
            mDeviceList.clear();
            mCachedDevicesAdapter.setNewData(mDeviceList);
            return;
        }
        if (mDeviceList.isEmpty() || mDeviceList.size() <= 0) {
            Set<BluetoothDevice> pairedDevices = mLocalBluetoothManager.getBluetoothAdapter()
                    .getBondedDevices();
            Timber.i("pairedDevices size = " + pairedDevices.size());

            if (!pairedDevices.isEmpty() && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (mLocalBluetoothManager.getCachedDeviceManager().findDevice(device) == null) {
                        mLocalBluetoothManager.getCachedDeviceManager().addDevice(
                                mLocalBluetoothAdapter, mLocalBluetoothManager.getProfileManager(), device);
                    }

                }
            }
            mDeviceList = (ArrayList) mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        mCachedDevicesAdapter.setNewData(mDeviceList);
    }


}
