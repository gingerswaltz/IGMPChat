package com.example.igmpchat;

import android.os.Bundle;
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

public class UDPChat extends AppCompatActivity {

    private Button buttonSend;
    private EditText editText;
    private LinearLayout messageContainer;

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


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Добавляем слушателя для кнопки отправки
        buttonSend.setOnClickListener(v -> {

            String message = editText.getText().toString().trim();
            if (!message.equals("")) {
                // Добавляем отправленное сообщение в контейнер сообщений
                addMessageToContainer("You: " + message);
                // Очищаем поле ввода
                editText.getText().clear();
            } else return;

        });
    }
    // Метод для добавления сообщения в контейнер сообщений
    private void addMessageToContainer(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        // Добавляем текстовое представление в начало контейнера сообщений
        messageContainer.addView(textView);
        // Прокручиваем ScrollView вверх, чтобы новое сообщение было видимо
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    }
