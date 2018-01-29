package com.kevin.lib.quickbledemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;

/**
 * @author HeQinfu
 */

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.VH> {

    Context mContext;
    ArrayList<BleDevice> mDevices = new ArrayList<>();
    private OnBleDeviceClick mOnBleDeviceClick;
    public DevicesAdapter(Context context) {
        mContext = context;
    }

    public void setOnBleDeviceClick(OnBleDeviceClick onBleDeviceClick) {
        mOnBleDeviceClick = onBleDeviceClick;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(mContext).inflate(R.layout.item_device, parent, false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        final BleDevice bleDevice = mDevices.get(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnBleDeviceClick!=null)
                    mOnBleDeviceClick.onDeviceClick(bleDevice);
            }
        });

        holder.tvName.setText(bleDevice.getName());
        holder.tvAddress.setText(bleDevice.getMac());
    }

    public void addDevice(BleDevice device) {
        mDevices.add(device);
        notifyDataSetChanged();
    }

    public void clear() {
        mDevices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public interface OnBleDeviceClick{
        void onDeviceClick(BleDevice bleDevice);
    }

    class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;

        public VH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
        }
    }
}
