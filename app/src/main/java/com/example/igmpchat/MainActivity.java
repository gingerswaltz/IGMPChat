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

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Button myButton;
    private EditText editMessage;
    private MultiAutoCompleteTextView messageOut;
    private MulticastManager multicastManager;
    private MessageHandler messageHandler;

    private Switch igmpHelloSwitch;

    private boolean igmpHelloEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        myButton = findViewById(R.id.button);
        editMessage = findViewById(R.id.editMessage);
        messageOut = findViewById(R.id.idMessages);
        igmpHelloSwitch = findViewById(R.id.igmpHelloSwitch);

        multicastManager = new MulticastManager("239.255.255.250", 1900);
        try {
            multicastManager.connect();
            messageHandler = new MessageHandler(multicastManager.getSocket(), multicastManager.getMulticastGroup(), multicastManager.getMulticastPort());
            messageHandler.startListening();
            messageHandler.enableIGMPHello();
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

        // Отображение полученных сообщений
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String receivedMessage = '\n'+messageHandler.getLastReceivedMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageOut.append(receivedMessage);
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