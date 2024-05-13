package com.example.igmpchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class DeviceIPAdapter extends RecyclerView.Adapter<DeviceIPAdapter.DeviceIPViewHolder> {
    private Map<String, String> ipNicknameMap;
    private OnItemClickListener mListener;

    public DeviceIPAdapter(Map<String, String> ipNicknameMap) {
        this.ipNicknameMap = ipNicknameMap;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // Метод для обновления списка устройств
    public void updateDeviceIPs(Map<String, String> ipNicknameMap) {
        this.ipNicknameMap = ipNicknameMap;
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
        String ipAddress = (new ArrayList<>(ipNicknameMap.keySet())).get(position);
        String nickname = ipNicknameMap.get(ipAddress);
        holder.bind(nickname);

        final int itemPosition = position; // объявление финальной переменной для позиции
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(itemPosition); // использование itemPosition вместо position
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return ipNicknameMap.size();
    }

    public static class DeviceIPViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDeviceIP;

        public DeviceIPViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDeviceIP = itemView.findViewById(R.id.textViewDeviceIP);
        }

        public void bind(String nickname) {
            textViewDeviceIP.setText(nickname);
        }
    }
}
