package com.example.igmpchat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
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
    private LinearLayout layoutInput;
    private boolean keyboardVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpchat);

        editText = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
        scrollView = findViewById(R.id.scrollView);
        messageContainer = findViewById(R.id.messageContainer);
        layoutInput = findViewById(R.id.layoutInput);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                currentIpAddress = extras.getString("currentIpaddress");
                nickName = extras.getString("nickName");
            }
        }

        try {
            udpManager = new UdpManager(currentIpAddress, 12347, nickName);
            udpManager.setMessageListener(this);
            udpManager.startListening();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        buttonSend.setOnClickListener(v -> {
            String message = editText.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageToContainer("You: " + message);
                udpManager.sendMessage(message);
                editText.getText().clear();
            }
        });

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = scrollView.getRootView().getHeight() - scrollView.getHeight();
                if (heightDiff > 200) {
                    if (!keyboardVisible) {
                        keyboardVisible = true;
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        moveInputField(true);
                    }
                } else {
                    if (keyboardVisible) {
                        keyboardVisible = false;
                        moveInputField(false);
                    }
                }
            }
        });
    }

    private void moveInputField(boolean moveUp) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutInput.getLayoutParams();
        if (moveUp) {
            params.bottomMargin = 0; // Поле ввода должно оставаться на месте
        } else {
            params.bottomMargin = 0;
        }
        layoutInput.setLayoutParams(params);
    }

    @Override
    public void onMessageReceived(String message) {
        addMessageToContainer(message);
    }

    private void addMessageToContainer(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        messageContainer.addView(textView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
