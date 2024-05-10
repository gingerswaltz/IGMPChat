package com.example.igmpchat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UDPChat extends AppCompatActivity implements UdpManager.MessageListener {

    private Button buttonSend;
    private EditText editText;
    private LinearLayout messageContainer;
    private UdpManager udpManager;
    private String currentIpAddress = null;
    private String nickName;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_udpchat);
        editText = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
        scrollView = findViewById(R.id.scrollView);
        messageContainer = findViewById(R.id.messageContainer);
        Intent intent = getIntent();

        // Получение данных из Intent
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                // Получение текущего IP-адреса
                currentIpAddress = extras.getString("currentIpaddress");

                // Получение никнейма
                nickName = extras.getString("nickName");
            }
        }
        try {
            // Создание экземпляра UdpManager с текущим IP-адресом и портом 12347
            udpManager = new UdpManager(currentIpAddress, 12347, nickName);
            udpManager.setMessageListener(this); // Установка текущей активности в качестве слушателя сообщений
            udpManager.startListening(); // Начало прослушивания входящих сообщений
        } catch (Exception e) {
            // Обработка исключения, возникшего при создании экземпляра UdpManager
            e.printStackTrace();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Добавляем слушателя для кнопки отправки
        buttonSend.setOnClickListener(v -> {

            String message = editText.getText().toString().trim();
            if (!message.isEmpty()) {
                // Добавляем отправленное сообщение в контейнер сообщений
                addMessageToContainer("You: " + message);
                udpManager.sendMessage(message);
                // Очищаем поле ввода
                editText.getText().clear();
            } else return;

        });
    }

    // Метод вызывается при получении нового сообщения
    @Override
    public void onMessageReceived(String message) {
        // Добавьте код для отображения входящего сообщения на интерфейсе
        addMessageToContainer(message);
    }

    // Метод для добавления сообщения в контейнер сообщений
    private void addMessageToContainer(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        // Добавляем текстовое представление в начало контейнера сообщений
        messageContainer.addView(textView);
        // Прокручиваем ScrollView вниз, чтобы новое сообщение было видимо
        scrollView.fullScroll(View.FOCUS_DOWN);
    }
}

