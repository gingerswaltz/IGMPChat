package com.example.igmpchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MessageObserver {
    private EditText editMessage;
    private MulticastManager multicastManager;
    private MessageHandler messageHandler;

    private Button myButtonRefresh;
    private RecyclerView recyclerViewDeviceIPs;
    private DeviceIPAdapter deviceIPAdapter;

    private String currentIP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Инициализация RecyclerView
        recyclerViewDeviceIPs = findViewById(R.id.recyclerViewDeviceIPs);
        recyclerViewDeviceIPs.setLayoutManager(new LinearLayoutManager(this));
        // Кнопка обновления списка RecyclerView
        myButtonRefresh = findViewById(R.id.buttonRefresh);
        // Кнопка submit
        Button myButton = findViewById(R.id.button);
        // поле ввода никнейма
        editMessage = findViewById(R.id.editMessage);
        // свитч режима discover
        Switch igmpHelloSwitch = findViewById(R.id.igmpHelloSwitch);

        multicastManager = new MulticastManager("239.255.255.250", 1900);
        try {
            multicastManager.connect();
            messageHandler = new MessageHandler(multicastManager.getSocket(),
                    multicastManager.getMulticastGroup(),
                    multicastManager.getMulticastPort());
            messageHandler.startListening();
            // Создание адаптера и установка его в RecyclerView
            deviceIPAdapter = new DeviceIPAdapter(messageHandler.getDeviceIPs());
            recyclerViewDeviceIPs.setAdapter(deviceIPAdapter);
            messageHandler.initUDPReceiver(12346); // Настройка приемника для прослушивания порта 12346
            messageHandler.receiveUDPMessage();
            messageHandler.addObserver(this); // Добавляем MainActivity в качестве наблюдателя
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Установка обработчика нажатия на элемент списка
        deviceIPAdapter.setOnItemClickListener(position -> {
            if (messageHandler.getNickName()==null){
                showAlert(MainActivity.this, "Не введено имя", "Пожалуйста, введите свое имя и нажмите Submit");
                return;
            }
            // Обработка нажатия на элемент списка
            currentIP = messageHandler.getDeviceIPs().get(position);
            messageHandler.setCurrentIpUdp(currentIP);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    messageHandler.sendMessageUDPStart();
                    Intent intent = new Intent(MainActivity.this, UDPChat.class);
                    startActivity(intent);
                }
            }).start();

        });
        myButton.setOnClickListener(view -> {
            String nickname = String.valueOf(editMessage.getText());

            new Thread(() -> messageHandler.setNickName(nickname)).start();
        });

        myButtonRefresh.setOnClickListener(view -> {
            if (messageHandler == null) return;
            // Обновление списка устройств при нажатии на кнопку Refresh
            ArrayList<String> updatedDeviceIPs = messageHandler.getDeviceIPs(); // Получите новый список устройств
            deviceIPAdapter.updateDeviceIPs(updatedDeviceIPs);

        });

        igmpHelloSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Запуск потока для отправки IGMP Hello
                messageHandler.enableIGMPHello();
            } else {
                // Остановка отправки IGMP Hello
                messageHandler.disableIGMPHello();
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (multicastManager != null) {
            multicastManager.disconnect();
        }
    }

    // Метод для отображения диалогового окна Alert
    public void showAlert(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)          // Установка заголовка диалогового окна
                .setMessage(message)      // Установка сообщения диалогового окна
                .setPositiveButton("OK", null);  // Добавление кнопки "OK", без действия

        AlertDialog dialog = builder.create();  // Создание диалогового окна
        dialog.show();  // Отображение диалогового окна
    }

    @Override
    public void onReceiveStartChat() {
        // Реакция на событие "START_CHAT"
        runOnUiThread(() -> {
            // Ваш код для запуска новой активности или выполнения других действий
            Intent intent = new Intent(MainActivity.this, UDPChat.class);
            startActivity(intent);
        });
    }


}