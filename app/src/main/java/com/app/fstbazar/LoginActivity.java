package com.app.fstbazar;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.WindowManager;
import android.view.ViewTreeObserver;
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
    TextView lblOtp, txtResend, txtOtpInfo;
    Button btnNext;
    ScrollView loginScrollView;
    ApiService api;

    boolean otpSent = false;
    String enteredPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
        txtOtpInfo = findViewById(R.id.txtOtpInfo);
        btnNext = findViewById(R.id.btnNext);
        loginScrollView = findViewById(R.id.loginScrollView);
        api = ApiClient.getClient().create(ApiService.class);

        setupKeyboardAwareScrolling();

        // 🔹 Main Button Logic
        btnNext.setOnClickListener(v -> {
            if (!otpSent) sendOtp();
            else verifyOtp();
        });

        // 🔹 Resend OTP
        txtResend.setOnClickListener(v -> sendOtp());
    }

    private void setupKeyboardAwareScrolling() {
        View rootView = findViewById(android.R.id.content);

        ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
            Rect visibleFrame = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - visibleFrame.bottom;

            if (keypadHeight > screenHeight * 0.15f) {
                View focusedView = getCurrentFocus();
                if (focusedView != null) {
                    loginScrollView.post(() ->
                            loginScrollView.smoothScrollTo(0, Math.max(0, focusedView.getTop() - 180))
                    );
                }
            }
        };

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(listener);

        View.OnFocusChangeListener focusChangeListener = (view, hasFocus) -> {
            if (hasFocus) {
                loginScrollView.postDelayed(() ->
                        loginScrollView.smoothScrollTo(0, Math.max(0, view.getTop() - 180)), 180);
            }
        };

        edtPhone.setOnFocusChangeListener(focusChangeListener);
        edtPin.setOnFocusChangeListener(focusChangeListener);
        edtPhone.setOnClickListener(v -> loginScrollView.postDelayed(() ->
                loginScrollView.smoothScrollTo(0, Math.max(0, edtPhone.getTop() - 180)), 120));
        edtPin.setOnClickListener(v -> loginScrollView.postDelayed(() ->
                loginScrollView.smoothScrollTo(0, Math.max(0, edtPin.getTop() - 180)), 120));
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
        txtOtpInfo.setText("Preparing your secure verification code...");

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
                    txtOtpInfo.setText("We sent an OTP to " + enteredPhone + ". Enter it below to continue.");
                    btnNext.setText("Verify OTP");
                    edtPin.requestFocus();

                    Toast.makeText(LoginActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    txtOtpInfo.setText("We couldn't send the OTP right now. Please try again.");
                    Toast.makeText(LoginActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnNext.setEnabled(true);
                btnNext.setText("Send OTP");
                txtOtpInfo.setText("Network issue while sending OTP. Please try again.");
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
        txtOtpInfo.setText("Verifying your OTP securely...");

        Map<String, String> body = new HashMap<>();
        body.put("phone", enteredPhone);
        body.put("otp", otp);

        api.verifyOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnNext.setEnabled(true);
                btnNext.setText("Verify OTP");

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
                    txtOtpInfo.setText("The OTP did not match. Please enter the latest code.");
                    Toast.makeText(LoginActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnNext.setEnabled(true);
                btnNext.setText("Verify OTP");
                txtOtpInfo.setText("Unable to verify OTP right now. Please try again.");
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
