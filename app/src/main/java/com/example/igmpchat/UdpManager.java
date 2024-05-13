package com.example.igmpchat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpManager {
    private static final String TAG = "UdpManager";
    private DatagramSocket socket;
    private boolean running;
    private String ipAddress;
    private int port;
    private String nickname;
    private MessageListener messageListener;
    private Handler handler;

    public UdpManager(String ipAddress, int port, String nickname) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.nickname = nickname;
        running = false;
        handler = new Handler(Looper.getMainLooper());
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void startListening() {
        if (!running) {
            new Thread(() -> {
                try {
                    socket = new DatagramSocket(port);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    running = true;
                    while (running) {
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        Log.d(TAG, "Received message: " + message);
                        if (messageListener != null) {
                            // Оповещаем слушателя о новом сообщении на основном потоке
                            handler.post(() -> messageListener.onMessageReceived(message));
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error while listening for UDP messages: " + e.getMessage());
                }
            }).start();
        }
    }

    public void stopListening() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(this.ipAddress);
                String finalMessage = this.nickname + ": " + message;
                byte[] sendData = finalMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                DatagramSocket sendSocket = new DatagramSocket();
                sendSocket.send(sendPacket);
                sendSocket.close();
                Log.d(TAG, "Sent message: " + finalMessage + " " + this.ipAddress);
            } catch (IOException e) {
                Log.e(TAG, "Error while sending UDP message: " + e.getMessage());
            }
        }).start();
    }

    public void sendLeaveMessage() {
        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(this.ipAddress);
                String finalMessage = "CODE___200___EXIT";
                byte[] sendData = finalMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                DatagramSocket sendSocket = new DatagramSocket();
                sendSocket.send(sendPacket);
                sendSocket.close();
                Log.d(TAG, "Sent leave message: " + finalMessage + " " + this.ipAddress);
            } catch (IOException e) {
                Log.e(TAG, "Error while sending UDP leave message: " + e.getMessage());
            }
        }).start();
    }

    public interface MessageListener {
        void onMessageReceived(String message);
    }
}