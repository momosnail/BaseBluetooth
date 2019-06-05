package com.wgl.basebluetooth.Adapter;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.wgl.basebluetooth.R;
import com.wgl.basebluetooth.common.CachedBluetoothDevice;

import java.util.List;

public class CachedDevicesAdapter extends BaseQuickAdapter<CachedBluetoothDevice, BaseViewHolder> {


    public CachedDevicesAdapter(int layoutResId, @Nullable List<CachedBluetoothDevice> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, CachedBluetoothDevice item) {
        String deviceName = item.getDevice().getName();
        String deviceAddress = item.getDevice().getAddress();
        String disPlayName = (deviceName == null || deviceName.isEmpty()) ? deviceAddress : deviceName;
        helper.setText(R.id.tv_item_remote_device_name, disPlayName);
        helper.setText(R.id.tv_item_remote_connect_status, item.getConnectedState());
    }
}
