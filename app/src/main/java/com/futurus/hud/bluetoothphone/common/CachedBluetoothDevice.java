/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futurus.hud.bluetoothphone.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.futurus.hud.bluetoothphone.R;
import com.futurus.hud.bluetoothphone.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

/**
 * CachedBluetoothDevice represents a remote Bluetooth device. It contains
 * attributes of the device (such as the address, name, RSSI, etc.) and
 * functionality that can be performed on the device (connect, pair, disconnect,
 * etc.).
 * <p>
 * CachedBluetoothDevice代表一个远程蓝牙设备。 它包含设备的属性（例如地址，名称，RSSI等）和可在设备上执行的功能（连接，配对，断开连接，
 * 等）。
 */
public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private static final String TAG = "Car_CachedBluetoothDevice";
    private static final boolean DEBUG = Utils.V;

    private final Context mContext;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private static final int BLUETOOTH_NAME_MAX_LENGTH = 59;
    private final BluetoothDevice mDevice;
    private String mName;
    private short mRssi;
    private BluetoothClass mBtClass;
    private HashMap<LocalBluetoothProfile, Integer> mProfileConnectionState;
    public static final String ACTION_PROFILE_STATE_CHANGED = "PROFILE_STATE_CHANGED";

    private final List<LocalBluetoothProfile> mProfiles =
            new ArrayList<LocalBluetoothProfile>();

    // List of profiles that were previously in mProfiles, but have been removed 以前在mProfiles中但已被删除的配置文件列表
    private final List<LocalBluetoothProfile> mRemovedProfiles =
            new ArrayList<LocalBluetoothProfile>();

    // Device supports PANU but not NAP: remove PanProfile after device disconnects from NAP
    private boolean mLocalNapRoleConnected;

    private boolean mVisible;

    private int mPhonebookPermissionChoice;

    private int mMessagePermissionChoice;

    private int mMessageRejectionCount;

    private final Collection<Callback> mCallbacks = new ArrayList<Callback>();

    // Following constants indicate the user's choices of Phone book/message access settings
    // User hasn't made any choice or settings app has wiped out the memory
    public final static int ACCESS_UNKNOWN = 0;
    // User has accepted the connection and let Settings app remember the decision
    public final static int ACCESS_ALLOWED = 1;
    // User has rejected the connection and let Settings app remember the decision
    public final static int ACCESS_REJECTED = 2;

    // How many times user should reject the connection to make the choice persist.
    private final static int MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST = 2;

    private final static String MESSAGE_REJECTION_COUNT_PREFS_NAME = "bluetooth_message_reject";

    private /*final*/ LocalBluetoothManager mLocalManager;
    /**
     * When we connect to multiple profiles, we only want to display a single
     * error even if they all fail. This tracks that state.
     */
    private boolean mIsConnectingErrorPossible;

    /**
     * Last time a bt profile auto-connect was attempted.
     * If an ACTION_UUID intent comes in within
     * MAX_UUID_DELAY_FOR_AUTO_CONNECT milliseconds, we will try auto-connect
     * again with the new UUIDs
     */
    private long mConnectAttempted;

    // See mConnectAttempted
    private static final long MAX_UUID_DELAY_FOR_AUTO_CONNECT = 5000;

    /**
     * Auto-connect after pairing only if locally initiated.
     */
    private boolean mConnectAfterPairing;

    /**
     * Describes the current device and profile for logging.
     *
     * @param profile Profile to describe
     * @return Description of the device and profile
     */
    private String describe(LocalBluetoothProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:").append(mDevice);
        if (profile != null) {
            sb.append(" Profile:").append(profile);
        }

        return sb.toString();
    }

    void onProfileStateChanged(LocalBluetoothProfile profile, int newProfileState) {
        if (Utils.D) {
            Log.d(TAG, "onProfileStateChanged: profile " + profile +
                    " newProfileState " + newProfileState);
        }
        if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_TURNING_OFF) {
            if (Utils.D) Log.d(TAG, " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        mProfileConnectionState.put(profile, newProfileState);

        /// M:Add for A2DP Sink.
        if (Utils.isA2dpSinkSupport(mLocalAdapter) &&
                (profile instanceof A2dpSinkProfile || profile instanceof A2dpProfile)) {
            A2dpRoleSwitcher.getInstance(mContext).changeProfileState(
                    profile, mDevice, newProfileState);
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_PROFILE_STATE_CHANGED);
        mContext.sendBroadcast(intent);

    }

    CachedBluetoothDevice(Context context,
                          LocalBluetoothAdapter adapter,
                          LocalBluetoothProfileManager profileManager,
                          BluetoothDevice device) {
        mContext = context;
        mLocalAdapter = adapter;
        mProfileManager = profileManager;
        mDevice = device;
        mProfileConnectionState = new HashMap<LocalBluetoothProfile, Integer>();
        fillData();
    }

    public void onClicked() {
        int bondState = getBondState();
        if (Utils.D) {
            Log.d(TAG, "onClicked bondState " + bondState);
        }

        if (/*isConnected()*/false) {
            //askDisconnect();  TBD
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "onClicked bondState BOND_BONDED " + bondState);
            connect(false); //conenct() ->connect(false)
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            startPairing();
        }

        /*
         * if (bondState == BluetoothDevice.BOND_BONDED) {
         *//** auto connect HF/HS and A2DP when device is paired */
        /*
         * if (!isAutoConnected()) { connect(); }
         *//** only device supporting conncetable profiles can enter options */
        /*
         *//**
         * or else, disconncet with it if device is connceted, or unpair is
         * device is paired
         */
        /*
         * if (isSupportConnectedProfile()) { //activiateProfileManager(); }
         * else if (isConnected()) { askDisconnect(); } } else if (bondState ==
         * BluetoothDevice.BOND_NONE) { pair(); }
         */
    }

    public void disconnect() {
        Log.d(TAG, "disconnect all profiles");
        for (LocalBluetoothProfile profile : mProfiles) {
            Log.d(TAG, "profile == " + profile);
            disconnect(profile);
        }

    }

    boolean disconnect(LocalBluetoothProfile profile) {
        if (mDevice == null) return false;
        if (profile.disconnect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:DISCONNECT " + describe(profile));
            }
            refresh();
            return true;
        }
        return false;
    }

    public void connect() {
        if (!ensurePaired())
            return;
        Log.d(TAG, "CacheBluetoothDevice Connect");
        /* MTK Added : Begin */
		/*
		BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
		if (adapter.isDiscovering()) {
			adapter.cancelDiscovery();
		}*/
        /* MTK Added : End */
        mConnectAttempted = SystemClock.elapsedRealtime();

        connectWithoutResettingTimer(true);
    }

    void connect(boolean connectAllProfiles) {
        if (!ensurePaired()) {
            return;
        }
        mConnectAttempted = SystemClock.elapsedRealtime();
        connectWithoutResettingTimer(connectAllProfiles);
    }

    void onBondingDockConnect() {
        // Attempt to connect if UUIDs are available. Otherwise,
        // we will connect when the ACTION_UUID intent arrives.
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {
        // Try to initialize the profiles if they were not.
        if (mProfiles.isEmpty()) {
            // if mProfiles is empty, then do not invoke updateProfiles. This causes a race
            // condition with carkits during pairing, wherein RemoteDevice.UUIDs have been updated
            // from bluetooth stack but ACTION.uuid is not sent yet.
            // Eventually ACTION.uuid will be received which shall trigger the connection of the
            // various profiles
            // If UUIDs are not available yet, connect will be happen
            // upon arrival of the ACTION_UUID intent.
            Log.d(TAG, "No profiles. Maybe we will connect later");
            return;
        }

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (LocalBluetoothProfile profile : mProfiles) {
            if (connectAllProfiles ? profile.isConnectable() : profile.isAutoConnectable()) {
                if (profile.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    connectInt(profile);
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Preferred profiles = " + preferredProfiles);

        if (preferredProfiles == 0) {
            connectAutoConnectableProfiles();
        }
    }

    private void connectAutoConnectableProfiles() {
        if (!ensurePaired()) {
            return;
        }
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        for (LocalBluetoothProfile profile : mProfiles) { // 可以自动连接的 配置
            if (profile.isAutoConnectable()) {
                profile.setPreferred(mDevice, true);
                connectInt(profile);
            }
        }
    }

    /**
     * Connect this device to the specified profile.
     *
     * @param profile the profile to use with the remote device
     */
    void connectProfile(LocalBluetoothProfile profile) {
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        connectInt(profile);
        // Refresh the UI based on profile.connect() call
        refresh();
    }

    synchronized boolean connectInt(LocalBluetoothProfile profile) {
        if (!ensurePaired()) {
            return false;
        }
        if (Utils.isA2dpSinkSupport(mLocalAdapter) &&
                (profile instanceof A2dpSinkProfile || profile instanceof A2dpProfile)) {
            Log.d(TAG, "Connect audio profile , A2dp | A2dpSink : " + profile);
            A2dpRoleSwitcher.getInstance(mContext).processA2dpConnect(profile, mDevice);
            return false;
        } else {
            if (profile.connect(mDevice)) {
                if (Utils.D) {
                    Log.d(TAG, "Command sent successfully:CONNECT " + describe(profile));
                }
                // Refresh the UI based on profile.connect() call
                refresh();
                return true;
            }
        }
        Log.i(TAG, "Failed to connect " + profile.toString() + " to " + mName);
        return false;
    }

    public void connectProfileName(String profileName) {
        Resources res = mContext.getResources();
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        Log.d(TAG, "profileName ==" + profileName);
//        BluetoothA2dp

		/*
		LocalBluetoothProfile profile = mProfileManager.getProfileByName(profileName);
		connectProfile(profile);
		*/
        if (profileName.equals(res.getString(R.string.bluetooth_profile_headset))) {//手机
            HeadsetProfile mHeadsetProfile = mProfileManager.getHeadsetProfile();
            connectProfile(mHeadsetProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_a2dp))) {//媒体
            A2dpProfile mA2dpProfile = mProfileManager.getA2dpProfile();
            connectProfile(mA2dpProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_a2dp_sink))) {//对端媒体
            A2dpSinkProfile mA2dpSinkProfile = mProfileManager.getA2dpSinkProfile();
            connectProfile(mA2dpSinkProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_pbap))) {//共享联系人
            PbapServerProfile mPbapProfile = mProfileManager.getPbapProfile();
            connectProfile(mPbapProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_headset_client))) {//对端手机
            HeadsetClientProfile mHeadsetClientProfile = mProfileManager.getHeadsetClientProfile();
            connectProfile(mHeadsetClientProfile);
        }

    }


    public boolean disconnectProfileName(String profileName) {
        Resources res = mContext.getResources();
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        Log.d(TAG, "connectProfileName : " + profileName);
        if (profileName.equals(res.getString(R.string.bluetooth_profile_headset))) {
            HeadsetProfile mHeadsetProfile = mProfileManager.getHeadsetProfile();
            return disconnect(mHeadsetProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_a2dp))) {
            A2dpProfile mA2dpProfile = mProfileManager.getA2dpProfile();
            return disconnect(mA2dpProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_a2dp_sink))) {
            A2dpSinkProfile mA2dpSinkProfile = mProfileManager.getA2dpSinkProfile();
            return disconnect(mA2dpSinkProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_pbap))) {
            PbapServerProfile mPbapProfile = mProfileManager.getPbapProfile();
            return disconnect(mPbapProfile);
        } else if (profileName.equals(res.getString(R.string.bluetooth_profile_headset_client))) {
            HeadsetClientProfile mHeadsetClientProfile = mProfileManager.getHeadsetClientProfile();
            return disconnect(mHeadsetClientProfile);
        }
        return false;

    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            startPairing();
            return false;
        } else {
            return true;
        }
    }

    public boolean startPairing() {
        // Pairing is unreliable while scanning, so cancel discovery
        if (mLocalAdapter.isDiscovering()) {
            mLocalAdapter.cancelDiscovery();
        }

        if (!mDevice.createBond()) {
            return false;
        }

        mConnectAfterPairing = true;  // auto-connect after pairing
        return true;
    }

    /**
     * Return true if user initiated pairing on this device. The message text is
     * slightly different for local vs. remote initiated pairing dialogs.
     */
    boolean isUserInitiatedPairing() {
        return mConnectAfterPairing;
    }

    public void unpair() {
        int state = getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            mDevice.cancelBondProcess();
        }

        if (state != BluetoothDevice.BOND_NONE) {
            final BluetoothDevice dev = mDevice;
            if (dev != null) {
                final boolean successful = dev.removeBond();
                if (successful) {
                    if (Utils.D) {
                        Timber.d("Command sent successfully:REMOVE_BOND " + describe(null));
                    }
                } else if (Utils.V) {
                    Timber.v("Framework rejected command immediately:REMOVE_BOND " +
                            describe(null));
                }
            }
        }
    }

    int getProfileConnectionState(LocalBluetoothProfile profile) {
        if (mProfileConnectionState == null ||
                mProfileConnectionState.get(profile) == null) {
            // If cache is empty make the binder call to get the state
            int state = profile.getConnectionStatus(mDevice);
            mProfileConnectionState.put(profile, state);
        }
        return mProfileConnectionState.get(profile);
    }

    public void clearProfileConnectionState() {
        if (Utils.D) {
            Log.d(TAG, " Clearing all connection state for dev:" + mDevice.getName());
        }
        for (LocalBluetoothProfile profile : getProfiles()) {
            mProfileConnectionState.put(profile, BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    // TODO: do any of these need to run async on a background thread?
    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();
        migratePhonebookPermissionChoice();
        migrateMessagePermissionChoice();
        fetchMessageRejectionCount();

        mVisible = false;
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    String getName() {
        return mName;
    }

    /**
     * Populate name from BluetoothDevice.ACTION_FOUND intent
     */
    void setNewName(String name) {
        if (mName == null) {
            mName = name;
            if (mName == null || TextUtils.isEmpty(mName)) {
                mName = mDevice.getAddress();
            }
            dispatchAttributesChanged();
        }
    }

    /**
     * user changes the device name
     */
    void setName(String name) {
        if (!mName.equals(name)) {
            mName = name;
            mDevice.setAlias(name);
            dispatchAttributesChanged();
        }
    }

    void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        mName = mDevice.getAliasName();

        if (TextUtils.isEmpty(mName)) {
            mName = mDevice.getAddress();
            if (DEBUG) Log.d(TAG, "Device has no name (yet), use address: " + mName);
        }
    }

    void refresh() {
        dispatchAttributesChanged();
    }

    boolean isVisible() {
        return mVisible;
    }

    void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {//获取远程设备的绑定状态。
        return mDevice.getBondState();
    }

    void setRssi(short rssi) {
        if (mRssi != rssi) {
            mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    /**
     * Checks whether we are connected to this device (any profile counts).
     *
     * @return Whether it is connected.
     */
    public boolean isConnected() {
        for (LocalBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }
        return false;
    }

    public int getConnectedState() {
        if (getBondState() != BluetoothDevice.BOND_BONDED) {
            return R.string.bt_status_unpair;
        }
        List<LocalBluetoothProfile> mConnectedProfiles = getConnectedProfiles();
        Log.d(TAG, "mConnectedProfiles size ==" + mConnectedProfiles.size());
        if (mConnectedProfiles == null || mConnectedProfiles.size() <= 0) {
            return R.string.bt_status_paired;
        }

        if (mConnectedProfiles.size() == 1) {
            BluetoothDevice mdevice = getDevice();
            int state = mConnectedProfiles.get(0).getNameResource(mdevice);
            return state;
        }

        if (mConnectedProfiles.size() > 1) {
            return R.string.bt_status_connected;
        }
        return R.string.bt_status_unpair;

    }

    List<LocalBluetoothProfile> getConnectedProfiles() {
        List<LocalBluetoothProfile> connectedProfiles =
                new ArrayList<LocalBluetoothProfile>();
        if (mProfiles == null || !(mProfiles.size() > 0)) {
            Log.v(TAG, "mProfiles is null or it's size 0");
            updateProfiles();
        }
        for (LocalBluetoothProfile profile : mProfiles) {
            if (isConnectedProfile(profile)) {
                connectedProfiles.add(profile);
            }
        }
        return connectedProfiles;
    }

    boolean isConnectedProfile(LocalBluetoothProfile profile) {
        int status = getProfileConnectionState(profile);
        return status == BluetoothProfile.STATE_CONNECTED;

    }

    boolean isBusy() {
        for (LocalBluetoothProfile profile : mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == BluetoothProfile.STATE_CONNECTING
                    || status == BluetoothProfile.STATE_DISCONNECTING) {
                return true;
            }
        }
        return getBondState() == BluetoothDevice.BOND_BONDING;
    }

    /**
     * Fetches a new value for the cached BT class.
     */
    private void fetchBtClass() {
        mBtClass = mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = mDevice.getUuids();
        if (uuids == null) return false;

        ParcelUuid[] localUuids = mLocalAdapter.getUuids();
        if (localUuids == null) return false;


        // 更新 mProfiles    // 很重要 根据UUID 来判断 设备可以连接 那些 东西  例如 headset a2dp
        mProfileManager.updateProfiles(uuids, localUuids, mProfiles, mRemovedProfiles,
                mLocalNapRoleConnected, mDevice);

        if (DEBUG) {
            Log.e(TAG, "updating profiles for " + mDevice.getAliasName());
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();

            if (bluetoothClass != null) Log.v(TAG, "Class: " + bluetoothClass.toString());
            Log.v(TAG, "UUID:");
            for (ParcelUuid uuid : uuids) {
                Log.v(TAG, "  " + uuid);
            }
        }
        Log.e(TAG, "updateProfiles: size  " + mProfiles.size());
        return true;
    }

    /**
     * Refreshes the UI for the BT class, including fetching the latest value
     * for the class.
     */
    void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    /**
     * Refreshes the UI when framework alerts us of a UUID change.
     */
    void onUuidChanged() {
        updateProfiles();

        if (DEBUG) {
            Log.e(TAG, "onUuidChanged: Time since last connect"
                    + (SystemClock.elapsedRealtime() - mConnectAttempted));
        }

        /*
         * If a connect was attempted earlier without any UUID, we will do the
         * connect now.
         */
        if (!mProfiles.isEmpty()
                && (mConnectAttempted + MAX_UUID_DELAY_FOR_AUTO_CONNECT) > SystemClock
                .elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    void onBondingStateChanged(int bondState) {
        if (bondState == BluetoothDevice.BOND_NONE) {
            mProfiles.clear();
            mConnectAfterPairing = false;  // cancel auto-connect
            setPhonebookPermissionChoice(ACCESS_UNKNOWN);
            setMessagePermissionChoice(ACCESS_UNKNOWN);
            mMessageRejectionCount = 0;
            saveMessageRejectionCount();
        }

        refresh();

        if (bondState == BluetoothDevice.BOND_BONDED) {
            if (mDevice.isBluetoothDock()) {
                onBondingDockConnect();
            } else if (mConnectAfterPairing) {
                connect(false);
            }
            mConnectAfterPairing = false;
        }
    }

    void setBtClass(BluetoothClass btClass) {
        if (btClass != null && mBtClass != btClass) {
            mBtClass = btClass;
            dispatchAttributesChanged();
        }
    }

    BluetoothClass getBtClass() {
        return mBtClass;
    }

    List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(mProfiles);
    }

    public List<LocalBluetoothProfile> getConnectableProfiles() {
        List<LocalBluetoothProfile> connectableProfiles =
                new ArrayList<LocalBluetoothProfile>();

        //mProfiles 怎么赋值的
        for (LocalBluetoothProfile profile : mProfiles) { // 循环所有的简介（Profile）
            if (profile.isConnectable()) {//可以连接的 加入到 操作列表
                Timber.d( "wgl " + "profile: " + profile);
                connectableProfiles.add(profile);
            }
        }
        return connectableProfiles;
    }

    List<LocalBluetoothProfile> getRemovedProfiles() {
        return mRemovedProfiles;
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAttributesChanged(this);
            }
        }
    }

    @Override
    public String toString() {
        return mDevice.toString();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof CachedBluetoothDevice)) {
            return false;
        }
        return mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }

    // This comparison uses non-final fields so the sort order may change
    // when device attributes change (such as bonding state). Settings
    // will completely refresh the device list when this happens.
    public int compareTo(CachedBluetoothDevice another) {
        // Connected above not connected
        int comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) return comparison;

        // Paired above not paired
        comparison = (another.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0) -
                (getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
        if (comparison != 0) return comparison;

        // Visible above not visible
        comparison = (another.mVisible ? 1 : 0) - (mVisible ? 1 : 0);
        if (comparison != 0) return comparison;

        // Stronger signal above weaker signal
        comparison = another.mRssi - mRssi;
        if (comparison != 0) return comparison;

        // Fallback on name
        return mName.compareTo(another.mName);
    }

    public interface Callback {
        void onDeviceAttributesChanged(CachedBluetoothDevice cachedDevice);
    }

    int getPhonebookPermissionChoice() {
        int permission = mDevice.getPhonebookAccessPermission();
        if (permission == BluetoothDevice.ACCESS_ALLOWED) {
            return ACCESS_ALLOWED;
        } else if (permission == BluetoothDevice.ACCESS_REJECTED) {
            return ACCESS_REJECTED;
        }
        return ACCESS_UNKNOWN;
    }

    void setPhonebookPermissionChoice(int permissionChoice) {
        int permission = BluetoothDevice.ACCESS_UNKNOWN;
        if (permissionChoice == ACCESS_ALLOWED) {
            permission = BluetoothDevice.ACCESS_ALLOWED;
        } else if (permissionChoice == ACCESS_REJECTED) {
            permission = BluetoothDevice.ACCESS_REJECTED;
        }
        try {
            mDevice.setPhonebookAccessPermission(permission);
        } catch (Exception e) {
            Timber.i("--------------permission: "+e);
        }

    }

    // Migrates data from old data store (in Settings app's shared preferences) to new (in Bluetooth
    // app's shared preferences).
    private void migratePhonebookPermissionChoice() {
        SharedPreferences preferences = mContext.getSharedPreferences(
                "bluetooth_phonebook_permission", Context.MODE_PRIVATE);
        if (!preferences.contains(mDevice.getAddress())) {
            return;
        }

        if (mDevice.getPhonebookAccessPermission() == BluetoothDevice.ACCESS_UNKNOWN) {
            int oldPermission = preferences.getInt(mDevice.getAddress(), ACCESS_UNKNOWN);
            if (oldPermission == ACCESS_ALLOWED) {
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            } else if (oldPermission == ACCESS_REJECTED) {
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(mDevice.getAddress());
        editor.commit();
    }

    int getMessagePermissionChoice() {
        int permission = mDevice.getMessageAccessPermission();
        if (permission == BluetoothDevice.ACCESS_ALLOWED) {
            return ACCESS_ALLOWED;
        } else if (permission == BluetoothDevice.ACCESS_REJECTED) {
            return ACCESS_REJECTED;
        }
        return ACCESS_UNKNOWN;
    }

    void setMessagePermissionChoice(int permissionChoice) {
        int permission = BluetoothDevice.ACCESS_UNKNOWN;
        if (permissionChoice == ACCESS_ALLOWED) {
            permission = BluetoothDevice.ACCESS_ALLOWED;
        } else if (permissionChoice == ACCESS_REJECTED) {
            permission = BluetoothDevice.ACCESS_REJECTED;
        }
        try {
            mDevice.setMessageAccessPermission(permission);
        } catch (Exception e) {
            Timber.i("--------------permission: "+e);
        }
    }

    // Migrates data from old data store (in Settings app's shared preferences) to new (in Bluetooth
    // app's shared preferences).
    private void migrateMessagePermissionChoice() {
        SharedPreferences preferences = mContext.getSharedPreferences(
                "bluetooth_message_permission", Context.MODE_PRIVATE);
        if (!preferences.contains(mDevice.getAddress())) {
            return;
        }

        if (mDevice.getMessageAccessPermission() == BluetoothDevice.ACCESS_UNKNOWN) {
            int oldPermission = preferences.getInt(mDevice.getAddress(), ACCESS_UNKNOWN);
            if (oldPermission == ACCESS_ALLOWED) {
                mDevice.setMessageAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            } else if (oldPermission == ACCESS_REJECTED) {
                mDevice.setMessageAccessPermission(BluetoothDevice.ACCESS_REJECTED);
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(mDevice.getAddress());
        editor.commit();
    }

    /**
     * @return Whether this rejection should persist.
     */
    boolean checkAndIncreaseMessageRejectionCount() {
        if (mMessageRejectionCount < MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST) {
            mMessageRejectionCount++;
            saveMessageRejectionCount();
        }
        return mMessageRejectionCount >= MESSAGE_REJECTION_COUNT_LIMIT_TO_PERSIST;
    }

    private void fetchMessageRejectionCount() {
        SharedPreferences preference = mContext.getSharedPreferences(
                MESSAGE_REJECTION_COUNT_PREFS_NAME, Context.MODE_PRIVATE);
        mMessageRejectionCount = preference.getInt(mDevice.getAddress(), 0);
    }

    private void saveMessageRejectionCount() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(
                MESSAGE_REJECTION_COUNT_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (mMessageRejectionCount == 0) {
            editor.remove(mDevice.getAddress());
        } else {
            editor.putInt(mDevice.getAddress(), mMessageRejectionCount);
        }
        editor.commit();
    }
}