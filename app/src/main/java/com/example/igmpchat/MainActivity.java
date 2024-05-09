package com.example.igmpchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EditText editMessage;
    private MulticastManager multicastManager;
    private MessageHandler messageHandler;

    private Button myButtonRefresh;
    private RecyclerView recyclerViewDeviceIPs;
    private DeviceIPAdapter deviceIPAdapter;
    private boolean igmpHelloEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Инициализация RecyclerView
        recyclerViewDeviceIPs = findViewById(R.id.recyclerViewDeviceIPs);
        recyclerViewDeviceIPs.setLayoutManager(new LinearLayoutManager(this));
        myButtonRefresh = findViewById(R.id.buttonRefresh);



        Button myButton = findViewById(R.id.button);
        editMessage = findViewById(R.id.editMessage);
        Switch igmpHelloSwitch = findViewById(R.id.igmpHelloSwitch);

        multicastManager = new MulticastManager("239.255.255.250", 1900);
        try {
            multicastManager.connect();
            messageHandler = new MessageHandler(multicastManager.getSocket(), multicastManager.getMulticastGroup(), multicastManager.getMulticastPort());
            messageHandler.startListening();
            // Создание адаптера и установка его в RecyclerView
            deviceIPAdapter = new DeviceIPAdapter(messageHandler.getDeviceIPs());
            recyclerViewDeviceIPs.setAdapter(deviceIPAdapter);
            messageHandler.enableIGMPHello();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Установка обработчика нажатия на элемент списка
        deviceIPAdapter.setOnItemClickListener(new DeviceIPAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Обработка нажатия на элемент списка
                String deviceIP = messageHandler.getDeviceIPs().get(position);
                // Действия при нажатии на элемент списка
                Toast.makeText(MainActivity.this, "Selected device IP: " + deviceIP, Toast.LENGTH_SHORT).show();

            }
        });
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editMessage.getText().toString();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        messageHandler.sendMessage(message, multicastManager.getMulticastGroup(), multicastManager.getMulticastPort());
                    }
                }).start();
            }
        });

        myButtonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messageHandler == null) return;
                // Обновление списка устройств при нажатии на кнопку Refresh
                ArrayList<String> updatedDeviceIPs = messageHandler.getDeviceIPs(); // Получите новый список устройств
                deviceIPAdapter.updateDeviceIPs(updatedDeviceIPs);

            }
        });
        igmpHelloSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                igmpHelloEnabled = isChecked;
                if (isChecked) {
                    // Запуск потока для отправки IGMP Hello
                    messageHandler.enableIGMPHello();
                } else {
                    // Остановка отправки IGMP Hello
                    messageHandler.disableIGMPHello();
                }
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
}