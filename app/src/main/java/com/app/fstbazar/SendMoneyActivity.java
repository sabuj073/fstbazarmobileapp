package com.app.fstbazar;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendMoneyActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FST_APP";
    private static final String KEY_BALANCE_REFRESH_REQUIRED = "balance_refresh_required";

    private EditText edtRecipientNumber;
    private EditText edtAmount;
    private TextView txtScanStatus;
    private Button btnScanQr;
    private Button btnSendMoney;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        ImageView btnBack = findViewById(R.id.btnBack);
        edtRecipientNumber = findViewById(R.id.edtRecipientNumber);
        edtAmount = findViewById(R.id.edtAmount);
        txtScanStatus = findViewById(R.id.txtScanStatus);
        btnScanQr = findViewById(R.id.btnScanQr);
        btnSendMoney = findViewById(R.id.btnSendMoney);
        api = ApiClient.getClient().create(ApiService.class);

        btnBack.setOnClickListener(v -> finish());
        btnScanQr.setOnClickListener(v -> openQrScanner());
        btnSendMoney.setOnClickListener(v -> sendMoney());
    }

    private void openQrScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        Task<Barcode> task = scanner.startScan();

        task.addOnSuccessListener(barcode -> {
            String resolvedPhone = extractPhoneNumber(barcode.getRawValue());
            if (TextUtils.isEmpty(resolvedPhone)) {
                txtScanStatus.setText("QR scanned, but no valid phone number was found.");
                Toast.makeText(this, "No phone number found in QR", Toast.LENGTH_SHORT).show();
                return;
            }

            edtRecipientNumber.setText(resolvedPhone);
            txtScanStatus.setText("Recipient number filled from QR successfully.");
        });

        task.addOnFailureListener(e -> {
            txtScanStatus.setText("QR scan could not be completed. Please try again.");
            Toast.makeText(this, "QR scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        task.addOnCanceledListener(() ->
                txtScanStatus.setText("QR scanning was cancelled.")
        );
    }

    private String extractPhoneNumber(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return null;
        }

        String trimmed = rawValue.trim();
        if (trimmed.matches("^\\+?[0-9]{10,15}$")) {
            return trimmed;
        }

        Matcher matcher = Pattern.compile("(\\+?[0-9]{10,15})").matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private void sendMoney() {
        String number = edtRecipientNumber.getText().toString().trim();
        String amountText = edtAmount.getText().toString().trim();

        if (number.length() < 10) {
            edtRecipientNumber.setError("Enter a valid number");
            return;
        }

        if (TextUtils.isEmpty(amountText)) {
            edtAmount.setError("Enter an amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            edtAmount.setError("Enter a valid amount");
            return;
        }

        if (amount <= 0) {
            edtAmount.setError("Amount must be greater than 0");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendMoney.setEnabled(false);
        btnSendMoney.setText("Sending...");
        txtScanStatus.setText("Processing your send money request...");

        Map<String, Object> body = new HashMap<>();
        body.put("number", number);
        body.put("amount", amount);

        api.sendMoney("Bearer " + token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSendMoney.setEnabled(true);
                btnSendMoney.setText(getString(R.string.send_action));

                if (response.isSuccessful() && response.body() != null) {
                    Object success = response.body().get("success");
                    Object message = response.body().get("message");
                    String responseMessage = message != null ? String.valueOf(message) : "Request completed.";

                    if (Boolean.TRUE.equals(success)) {
                        txtScanStatus.setText(responseMessage);
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_BALANCE_REFRESH_REQUIRED, true)
                                .apply();
                        showSuccessDialog(number, amount, response.body());
                        edtAmount.setText("");
                    } else {
                        txtScanStatus.setText(responseMessage);
                        Toast.makeText(SendMoneyActivity.this, responseMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    txtScanStatus.setText("Send money failed. Please try again.");
                    Toast.makeText(SendMoneyActivity.this, "Send money failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSendMoney.setEnabled(true);
                btnSendMoney.setText(getString(R.string.send_action));
                txtScanStatus.setText("Network error while sending money.");
                Toast.makeText(SendMoneyActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog(String number, double amount, Map<String, Object> responseBody) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_send_money_success, null, false);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.55f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        TextView txtCloseDialog = view.findViewById(R.id.txtCloseDialog);
        TextView txtDialogAccount = view.findViewById(R.id.txtDialogAccount);
        TextView txtDialogTime = view.findViewById(R.id.txtDialogTime);
        TextView txtDialogAmount = view.findViewById(R.id.txtDialogAmount);
        TextView txtDialogCharge = view.findViewById(R.id.txtDialogCharge);
        TextView txtDialogTransactionId = view.findViewById(R.id.txtDialogTransactionId);
        TextView txtDialogReference = view.findViewById(R.id.txtDialogReference);

        String formattedTime = new SimpleDateFormat("hh:mma dd/MM/yy", Locale.getDefault())
                .format(new Date())
                .toLowerCase(Locale.getDefault());

        String transactionId = extractString(responseBody, "transaction_id");
        if (TextUtils.isEmpty(transactionId)) {
            transactionId = "FST" + System.currentTimeMillis();
        }

        String reference = extractString(responseBody, "reference");
        if (TextUtils.isEmpty(reference)) {
            reference = "-";
        }

        txtDialogAccount.setText(number);
        txtDialogTime.setText(formattedTime);
        txtDialogAmount.setText(String.format(Locale.getDefault(), "৳%.2f", amount));
        txtDialogCharge.setText("৳0.00");
        txtDialogTransactionId.setText(transactionId);
        txtDialogReference.setText(reference);

        txtCloseDialog.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    private String extractString(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
