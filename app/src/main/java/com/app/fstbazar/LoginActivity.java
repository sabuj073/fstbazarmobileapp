package com.app.fstbazar;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText edtPhone, edtPin;
    TextView lblOtp, txtResend;
    Button btnNext;
    Button[] numButtons;
    ImageButton btnBackspace;
    ApiService api;

    boolean otpSent = false;
    String enteredPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("loggedIn", false);

        if (isLoggedIn) {
            // Skip login, go to main
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }






        // 🔹 Initialize Views
        edtPhone = findViewById(R.id.edtPhone);
        edtPin = findViewById(R.id.edtPin);
        lblOtp = findViewById(R.id.lblOtp);
        txtResend = findViewById(R.id.txtResend);
        btnNext = findViewById(R.id.btnNext);
        btnBackspace = findViewById(R.id.btnBackspace);
        api = ApiClient.getClient().create(ApiService.class);

        // 🔹 Numeric Keypad Setup
        numButtons = new Button[]{
                findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
                findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
                findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
                findViewById(R.id.btn9)
        };

        for (Button b : numButtons) {
            b.setOnClickListener(v -> {
                String text = ((Button) v).getText().toString();
                if (otpSent) edtPin.append(text);
                else edtPhone.append(text);
            });
        }

        btnBackspace.setOnClickListener(v -> {
            EditText target = otpSent ? edtPin : edtPhone;
            int len = target.getText().length();
            if (len > 0) target.getText().delete(len - 1, len);
        });

        // 🔹 Main Button Logic
        btnNext.setOnClickListener(v -> {
            if (!otpSent) sendOtp();
            else verifyOtp();
        });

        // 🔹 Resend OTP
        txtResend.setOnClickListener(v -> sendOtp());
    }

    /**
     * Send OTP API
     */
    private void sendOtp() {
        enteredPhone = edtPhone.getText().toString().trim();
        if (enteredPhone.length() < 10) {
            Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnNext.setEnabled(false);
        btnNext.setText("Sending...");

        Map<String, String> body = new HashMap<>();
        body.put("phone", enteredPhone);

        api.sendOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnNext.setEnabled(true);
                btnNext.setText("Verify OTP");

                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    otpSent = true;
                    edtPhone.setEnabled(false);

                    lblOtp.setVisibility(View.VISIBLE);
                    edtPin.setVisibility(View.VISIBLE);
                    txtResend.setVisibility(View.VISIBLE);

                    Toast.makeText(LoginActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnNext.setEnabled(true);
                btnNext.setText("Send OTP");
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Verify OTP API
     */
    private void verifyOtp() {
        String otp = edtPin.getText().toString().trim();
        if (otp.length() < 4) {
            Toast.makeText(this, "Enter valid OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        btnNext.setEnabled(false);
        btnNext.setText("Verifying...");

        Map<String, String> body = new HashMap<>();
        body.put("phone", enteredPhone);
        body.put("otp", otp);

        api.verifyOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnNext.setEnabled(true);
                btnNext.setText("Next");

                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {

                    // Extract token from response (if returned by API)
                    String token = (String) response.body().get("access_token");

                    // Save login status and token securely
                    SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("loggedIn", true);


                    if (token != null && !token.isEmpty()) {
                        editor.putString("token", token);
                    }

                    // Optional: Save phone number or user info if needed later
                    editor.putString("phone", edtPhone.getText().toString());
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // Redirect to MainActivity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnNext.setEnabled(true);
                btnNext.setText("Verify OTP");
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
