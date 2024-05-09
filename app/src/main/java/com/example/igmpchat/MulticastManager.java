package com.example.igmpchat;

import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class MulticastManager {
    private MulticastSocket socket;
    private InetAddress multicastGroup;
    private int multicastPort;
    public MulticastManager(String multicastGroup, int multicastPort) {
        try {
            this.multicastGroup = InetAddress.getByName(multicastGroup);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.multicastPort = multicastPort;
    }

    public void connect() throws IOException {
        socket = new MulticastSocket(multicastPort);
        socket.joinGroup(multicastGroup);
        Log.d("MulticastManager", "Connected to multicast group " + multicastGroup);
    }

    public void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.leaveGroup(multicastGroup);
                socket.close();
                Log.d("MulticastManager", "Disconnected from multicast group");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public MulticastSocket getSocket() {
        return socket;
    }

    public InetAddress getMulticastGroup() {
        return multicastGroup;
    }

    public int getMulticastPort() {
        return multicastPort;
    }
}