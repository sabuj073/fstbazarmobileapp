package com.app.fstbazar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText edtPhone, edtPin;
    Button btnNext;
    Button[] numButtons;
    ImageButton btnBackspace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtPhone = findViewById(R.id.edtPhone);
        edtPin = findViewById(R.id.edtPin);
        btnNext = findViewById(R.id.btnNext);
        btnBackspace = findViewById(R.id.btnBackspace);

        numButtons = new Button[]{
                findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
                findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
                findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
                findViewById(R.id.btn9)
        };

        for (Button b : numButtons) {
            b.setOnClickListener(v -> {
                String text = ((Button) v).getText().toString();
                edtPin.append(text);
            });
        }

        btnBackspace.setOnClickListener(v -> {
            String pin = edtPin.getText().toString();
            if (!pin.isEmpty()) edtPin.setText(pin.substring(0, pin.length() - 1));
        });

        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}

