package com.example.igmpchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceIPAdapter extends RecyclerView.Adapter<DeviceIPAdapter.DeviceIPViewHolder> {
    private ArrayList<String> deviceIPs;
    private OnItemClickListener mListener;

    public DeviceIPAdapter(ArrayList<String> deviceIPs) {
        this.deviceIPs = deviceIPs;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    // Метод для обновления списка устройств
    public void updateDeviceIPs(ArrayList<String> deviceIPs) {
        this.deviceIPs = deviceIPs;
        notifyDataSetChanged(); // Уведомляем адаптер об изменениях
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }
    @NonNull
    @Override
    public DeviceIPViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_ip, parent, false);
        return new DeviceIPViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceIPViewHolder holder, int position) {
        String deviceIP = deviceIPs.get(position);
        holder.textViewDeviceIP.setText(deviceIP);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(position);
                    }
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return deviceIPs.size();
    }

    public static class DeviceIPViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDeviceIP;

        public DeviceIPViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDeviceIP = itemView.findViewById(R.id.textViewDeviceIP);
        }

        public void bind(String deviceIP) {
            textViewDeviceIP.setText(deviceIP);
        }
    }
}
