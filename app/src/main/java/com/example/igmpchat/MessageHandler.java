package com.example.igmpchat;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MessageHandler {
    private MulticastSocket socket;
    private String lastReceivedMessage = "";

    public MessageHandler(MulticastSocket socket) {
        this.socket = socket;
    }

    public void startListening() {
        Thread receiverThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (!message.startsWith("M-SEARCH") || !message.startsWith("NOTIFY")) {
                        lastReceivedMessage = message;
                        Log.d("MessageHandler", "Received message: " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

    public void sendMessage(String message, InetAddress multicastGroup, int multicastPort) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, multicastGroup, multicastPort);
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastReceivedMessage() {
        return lastReceivedMessage;
    }
}