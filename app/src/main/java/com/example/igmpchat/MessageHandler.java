package com.example.igmpchat;

import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private MulticastSocket socket;
    private ArrayList<String> deviceIPs = new ArrayList<>();
    private String currentIpUdp;
    private DatagramSocket datagramSocket;


    // Словарь для хранения соответствия IP-адресов и никнеймов
    private Map<String, String> ipNicknameMap = new HashMap<>();
    // Метод для получения словаря IP-адресов и никнеймов
    public Map<String, String> getIpNicknameMap() {
        return ipNicknameMap;
    }

    // ник
    private String nickName;
    private List<MessageObserver> observers = new ArrayList<>();

    public void addObserver(MessageObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MessageObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String ipAddress) {
        for (MessageObserver observer : observers) {
            observer.onReceiveStartChat(ipAddress);
        }
    }
    public MessageHandler(MulticastSocket socket, InetAddress multicastGroup, int multicastPort) {
        this.socket = socket;
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
    }

    public void setNickName(String nickName){
        if (nickName == null || nickName.isEmpty()) return;
        this.nickName = nickName;
    }
    public String getNickName(){
        return nickName;
    }

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
                        //Log.d("MessageHandler", "Received message: " + message);
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
    public void sendMessageUDPStart() {
        DatagramSocket socket = null;
        try {
            int port = 12346;
            String message = "START_CHAT, IP:"+getLocalIPAddress();

            socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(currentIpUdp);
            // Создание DatagramPacket
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            // Отправка сообщения
            socket.send(sendPacket);
            Log.d("SendMessage", "Сообщение UDP отправлено: " + message + " на " + address.getHostAddress() + ":" + port);
        } catch (UnknownHostException e) {
            Log.e("SendMessage", "Неизвестный хост: " + currentIpUdp);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close(); // Закрытие сокета после использования
            }
        }
    }



    // Новая логика для отправки сообщения "IGMP Hello %ip_address%" каждые 3 секунды
    private void startIGMPHello() {
        igmpHelloThread = new Thread(() -> {
            try {
                while (igmpHelloEnabled) {
                    String ipAddress = getLocalIPAddress(); // Получение IP-адреса устройства
                    String message = "IGMP Hello " + ipAddress+"_"+ nickName;
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
    // В процессе обработки сообщения "IGMP Hello" извлекаем IP и никнейм
    private void processIGMPHelloMessage(String message) {
        String[] parts = message.split(" ");
        if (parts.length >= 3) {
            String ipAddress = parts[2];
            int underscoreIndex = message.indexOf("_");
            if (underscoreIndex != -1) {
                String nickname = message.substring(underscoreIndex + 1);
                // Проверяем, есть ли IP-адрес уже в списке
                if (!ipNicknameMap.containsKey(ipAddress)) {
                    ipNicknameMap.put(ipAddress, nickname);
                }
            }
        }
    }


    public void setCurrentIpUdp(String currentIpUdp){
        this.currentIpUdp = currentIpUdp;
    }
    public void establishUDPConnection() {
        try {
            InetAddress address = InetAddress.getByName(currentIpUdp);
            DatagramSocket datagramSocket = new DatagramSocket(12345);
            // Устанавливаем адрес и порт для отправки
            datagramSocket.connect(address, 12345);

            // Log.d("UDPConnection", "UDP connection established with " + currentIpUdp + ":" + 123456);
        } catch (UnknownHostException e) {
            Log.e("UDPConnection", "Unknown host: " + currentIpUdp);
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
                    if(message.startsWith("START_CHAT")){
                        String[] parts = message.split(":");
                        String ipAddress = parts[1].trim();
                        notifyObservers(ipAddress);
                    }

                    Log.d("UDPReceiver", "Received message: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

}



