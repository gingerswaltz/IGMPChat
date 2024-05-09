package com.example.igmpchat;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class MessageHandler {
    private MulticastSocket socket;
    private String lastReceivedMessage = "";
    private ArrayList<String> deviceIPs = new ArrayList<>();
    private DatagramSocket datagramSocket;

    // Метод для настройки UDP приемника для прослушивания определенного порта
    public void initUDPReceiver(int port) {
        try {
            datagramSocket = new DatagramSocket(port, InetAddress.getByName(getLocalIPAddress())); // Прослушиваем определенный порт
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean igmpHelloEnabled = true;

    private Thread igmpHelloThread;
    private InetAddress multicastGroup;
    private int multicastPort;

    public MessageHandler(MulticastSocket socket, InetAddress multicastGroup, int multicastPort) {
        this.socket = socket;
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
    }

    public void startListening() {
        Thread receiverThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if(message.startsWith("IGMP Hello")){
                        processIGMPHelloMessage(message);
                    } else
                    if (!message.startsWith("M-SEARCH") && !message.startsWith("NOTIFY")) {
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
    public void sendMessageUDP(String message, String ipAddress, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(ipAddress);

            // Создание DatagramPacket
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            // Преобразуем строку в массив байтов
            byte[] buffer = message.getBytes();
            // Создаем пакет с данными для отправки
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            // Отправляем пакет
            socket.send(packet);
            // Отправка сообщения
            socket.send(sendPacket);
            Log.d("SendMessage", "Сообщение UDP отправлено: " + message + " на " + address.getHostAddress() + ":" + port);
        } catch (UnknownHostException e) {
            Log.e("SendMessage", "Неизвестный хост: " + ipAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    // Новая логика для отправки сообщения "IGMP Hello %ip_address%" каждые 3 секунды
    private void startIGMPHello() {
        igmpHelloThread = new Thread(() -> {
            try {
                while (igmpHelloEnabled) {
                    String ipAddress = getLocalIPAddress(); // Получение IP-адреса устройства
                    String message = "IGMP Hello " + ipAddress;
                    sendMessage(message, multicastGroup, multicastPort);
                    //Log.d("IGMP HELLO SENDER", "Sending igmp hello: " + message);

                    Thread.sleep(3000); // Отправка каждые 3 секунды
                }
            } catch (InterruptedException e) {
                // Поток был прерван, возможно, из-за отключения IGMP Hello
                // Можно не обрабатывать это исключение, так как мы просто выходим из цикла и закрываем поток
            }
        });
        igmpHelloThread.start();
    }


    private String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                String interfaceName = networkInterface.getName();
                if ("wlan0".equals(interfaceName)) { // Проверяем имя интерфейса
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        //Log.d("LocalIPAddress", "Found IP address for eth0: " + address);
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Включение отправки IGMP Hello
    public void enableIGMPHello() {
        igmpHelloEnabled = true;
        startIGMPHello();
    }

    // Отключение отправки IGMP Hello
    public void disableIGMPHello() {
        igmpHelloEnabled = false;
        if (igmpHelloThread != null && igmpHelloThread.isAlive()) {
            igmpHelloThread.interrupt();
        }
    }
    private void processIGMPHelloMessage(String message) {
        // Извлечение IP-адреса из сообщения "IGMP Hello"
        String[] parts = message.split(" ");
        if (parts.length >= 3) {
            String ipAddress = parts[2]; // IP-адрес находится в третьей части сообщения
            // Добавление IP-адреса в массив, если его еще нет там
            if (!deviceIPs.contains(ipAddress)) {
                deviceIPs.add(ipAddress);
                Log.d("MessageHandler", "New device discovered: " + ipAddress);
            }
        }
    }
    public ArrayList<String> getDeviceIPs() {
        return deviceIPs;
    }

    public void establishUDPConnection(String ipAddress, int port) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            DatagramSocket datagramSocket = new DatagramSocket(port);
            // Устанавливаем адрес и порт для отправки
            datagramSocket.connect(address, port);

            Log.d("UDPConnection", "UDP connection established with " + ipAddress + ":" + port);
        } catch (UnknownHostException e) {
            Log.e("UDPConnection", "Unknown host: " + ipAddress);
        } catch (SocketException e) {
            Log.e("UDPConnection", "Socket exception: " + e.getMessage());
        }
    }

    public void receiveUDPMessage() {
        Thread receiverThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                while (true) {
                    datagramSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    lastReceivedMessage = message;
                    Log.d("UDPReceiver", "Received message: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

}
