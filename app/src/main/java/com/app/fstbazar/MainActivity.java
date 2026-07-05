package com.app.fstbazar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FST_APP";
    private static final String KEY_BALANCE_REFRESH_REQUIRED = "balance_refresh_required";

    RecyclerView recyclerSites;
    ArrayList<SiteModel> siteList;
    SiteAdapter adapter;
    ImageView btnLogout, btnQrScan, imgProfile;
    LinearLayout navHome;
    TextView txtGreeting, txtUserName, txtTapForBalance, txtPhone;
    ApiService api;
    private boolean autoHideBalanceAfterFetch;
    private static final String BALANCE_PLACEHOLDER = "Tap to reveal balance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerSites = findViewById(R.id.recyclerSites);
        recyclerSites.setLayoutManager(new GridLayoutManager(this, 3));
        siteList = new ArrayList<>();

        btnLogout = findViewById(R.id.btnLogout);
        btnQrScan = findViewById(R.id.btnQrScan);
        navHome = findViewById(R.id.navHome);
        imgProfile = findViewById(R.id.imgProfile);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtUserName = findViewById(R.id.txtUserName);
        txtTapForBalance = findViewById(R.id.txtTapForBalance);
        txtPhone = findViewById(R.id.txtPhone);

        txtGreeting.setText("Primary account");
        txtTapForBalance.setText(BALANCE_PLACEHOLDER);
        String cachedPhone = getCachedPhone();
        txtPhone.setText(cachedPhone != null && !cachedPhone.isEmpty() ? cachedPhone : "Phone not available");

        Glide.with(this).load(R.drawable.ic_profile_placeholder).into(imgProfile);

        api = ApiClient.getClient().create(ApiService.class);

        loadSites();
        fetchUserData();

        btnLogout.setOnClickListener(v -> logout());
        btnQrScan.setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
        navHome.setOnClickListener(v -> Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show());

        txtTapForBalance.setOnClickListener(v -> fetchBalance());
    }

    private void loadSites() {
        api.getSites().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Object dataObj = response.body().get("data");
                        if (dataObj instanceof List) {
                            List<?> list = (List<?>) dataObj;
                            siteList.clear();

                            for (Object item : list) {
                                if (item instanceof Map) {
                                    Map<?, ?> obj = (Map<?, ?>) item;
                                    String name = String.valueOf(obj.get("name"));
                                    String logo = String.valueOf(obj.get("logo"));
                                    String link = String.valueOf(obj.get("slug"));
                                    String userPhone = getCachedPhone();

                                    if (userPhone != null && !userPhone.isEmpty()) {
                                        if (link.contains("?")) {
                                            link += "&phonenumber=" + userPhone;
                                        } else {
                                            link += "?phonenumber=" + userPhone;
                                        }
                                    }

                                    siteList.add(new SiteModel(name, logo, link));
                                }
                            }

                            adapter = new SiteAdapter(MainActivity.this, siteList);
                            recyclerSites.setAdapter(adapter);
                        }
                    } catch (Exception e) {
                        Log.e("SITE_PARSE", "Error parsing data: " + e);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load sites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) return;

        api.getUser("Bearer " + token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> root = response.body();
                        Map<String, Object> user = (Map<String, Object>) root.get("user");

                        if (user != null) {
                            String name = String.valueOf(user.get("name"));
                            String avatar = String.valueOf(user.get("avatar_original"));
                            String balance = String.valueOf(user.get("balance"));

                            txtUserName.setText(name != null ? name : "Unknown User");
                            if (autoHideBalanceAfterFetch && balance != null && !balance.equals("null") && !balance.isEmpty()) {
                                txtTapForBalance.setText("৳ " + balance);
                                scheduleBalanceHide();
                                autoHideBalanceAfterFetch = false;
                            }

                            if (avatar != null && !avatar.equals("null") && !avatar.isEmpty()) {
                                Glide.with(MainActivity.this)
                                        .load(avatar)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .into(imgProfile);
                            } else {
                                Glide.with(MainActivity.this)
                                        .load(R.drawable.ic_profile_placeholder)
                                        .into(imgProfile);
                            }
                        } else {
                            txtUserName.setText("No user data");
                        }
                    } catch (Exception e) {
                        Log.e("USER_PARSE", "Error parsing: " + e);
                        txtUserName.setText("Parse error");
                    }
                } else {
                    txtUserName.setText("Failed to load");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                txtUserName.setText("Error");
                Log.e("USER_API_FAIL", "Error: " + t.getMessage());
                autoHideBalanceAfterFetch = false;
            }
        });
    }

    private String getCachedPhone() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("phone", null);
    }

    private void fetchBalance() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        txtTapForBalance.setText("Checking balance...");
        api.getUser("Bearer " + token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> user = (Map<String, Object>) response.body().get("user");
                        if (user != null) {
                            String balance = String.valueOf(user.get("balance"));
                            txtTapForBalance.setText("৳ " + balance);
                            scheduleBalanceHide();

                        } else {
                            txtTapForBalance.setText("Balance unavailable");
                        }
                    } catch (Exception e) {
                        txtTapForBalance.setText("Balance unavailable");
                        Log.e("BALANCE_ERR", e.toString());
                    }
                } else {
                    txtTapForBalance.setText("Failed to load");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                txtTapForBalance.setText("Error loading");
            }
        });
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_BALANCE_REFRESH_REQUIRED, false)) {
            prefs.edit().putBoolean(KEY_BALANCE_REFRESH_REQUIRED, false).apply();
            autoHideBalanceAfterFetch = true;
            fetchUserData();
        }
    }

    private void scheduleBalanceHide() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            txtTapForBalance.setText(BALANCE_PLACEHOLDER);
        }, 6000);
    }
}
