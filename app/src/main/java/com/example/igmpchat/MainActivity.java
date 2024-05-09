package com.example.igmpchat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Button myButton;
    private EditText editMessage;
    private MultiAutoCompleteTextView messageOut;
    private MulticastManager multicastManager;
    private MessageHandler messageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        myButton = findViewById(R.id.button);
        editMessage = findViewById(R.id.editMessage);
        messageOut = findViewById(R.id.idMessages);

        multicastManager = new MulticastManager("239.255.255.250", 1900);
        try {
            multicastManager.connect();
            messageHandler = new MessageHandler(multicastManager.getSocket());
            messageHandler.startListening();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        // Отображение полученных сообщений
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String receivedMessage = messageHandler.getLastReceivedMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageOut.setText(receivedMessage);
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

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