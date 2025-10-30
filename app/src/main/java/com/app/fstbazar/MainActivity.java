package com.app.fstbazar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerSites;
    ArrayList<SiteModel> siteList;
    SiteAdapter adapter;
    ImageView imgProfile;
    TextView txtUserName, txtTapForBalance;
    ViewPager2 bannerViewPager;
    BannerAdapter bannerAdapter;
    List<String> bannerUrls;
    Handler bannerHandler;
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerSites = findViewById(R.id.recyclerSites);
        recyclerSites.setLayoutManager(new GridLayoutManager(this, 4));
        siteList = new ArrayList<>();

        bannerViewPager = findViewById(R.id.bannerViewPager);
        bannerUrls = new ArrayList<>();
        bannerHandler = new Handler(Looper.getMainLooper());

        imgProfile = findViewById(R.id.imgProfile);
        txtUserName = findViewById(R.id.txtUserName);
        txtTapForBalance = findViewById(R.id.txtTapForBalance);

        Glide.with(this).load(R.drawable.ic_profile_placeholder).into(imgProfile);

        api = ApiClient.getClient().create(ApiService.class);

        loadSites();
        loadBanners();
        fetchUserData();

        // Fetch balance on tap
        txtTapForBalance.setOnClickListener(v -> fetchBalance());
    }

    private void loadSites() {
        SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        String userPhone = prefs.getString("user_phone", null); // optional cache

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
                                    String userPhone = null;
                                    userPhone = getCachedPhone();

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
        SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) return;

        api.getUser("Bearer " + token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> root = response.body();
                        Map<String, Object> user = (Map<String, Object>) root.get("user");  // ✅ get inside "user"

                        if (user != null) {
                            String name = String.valueOf(user.get("name"));
                            String balance = String.valueOf(user.get("balance"));
                            String avatar = String.valueOf(user.get("avatar_original"));

                            txtUserName.setText(name != null ? name : "Unknown User");
                           // txtTapForBalance.setText("৳ " + balance);

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
            }
        });
    }



    private void loadBanners() {
        api.getBanners().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Object dataObj = response.body().get("data");
                        if (dataObj instanceof List) {
                            List<?> list = (List<?>) dataObj;
                            bannerUrls.clear();

                            for (Object item : list) {
                                if (item instanceof Map) {
                                    Map<?, ?> obj = (Map<?, ?>) item;
                                    String photo = String.valueOf(obj.get("photo"));
                                    if (photo != null && !photo.startsWith("http")) {
                                        photo = "https://fstbazar.com" + photo;
                                    }
                                    bannerUrls.add(photo);
                                }
                            }

                            bannerAdapter = new BannerAdapter(MainActivity.this, bannerUrls);
                            bannerViewPager.setAdapter(bannerAdapter);
                            startAutoSlide();
                        }
                    } catch (Exception e) {
                        Log.e("BANNER_PARSE", "Error parsing: " + e);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load banners", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String getCachedPhone() {
        SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
        return prefs.getString("phone", null);
    }



    private void startAutoSlide() {
        bannerHandler.postDelayed(new Runnable() {
            int currentPage = 0;

            @Override
            public void run() {
                if (bannerAdapter != null && bannerAdapter.getItemCount() > 0) {
                    currentPage = (currentPage + 1) % bannerAdapter.getItemCount();
                    bannerViewPager.setCurrentItem(currentPage, true);
                    bannerHandler.postDelayed(this, 4000);
                }
            }
        }, 4000);
    }

    private void fetchBalance() {
        SharedPreferences prefs = getSharedPreferences("FST_APP", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        txtTapForBalance.setText("Checking...");
        api.getUser("Bearer " + token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> user = (Map<String, Object>) response.body().get("user");
                        if (user != null) {
                            String balance = String.valueOf(user.get("balance"));
                            txtTapForBalance.setText("৳ " + balance);

                            // ✅ Reset to "Tap for balance" after 1 minute
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                txtTapForBalance.setText("Tap for balance");
                            }, 6000); // 60,000 ms = 1 minute

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

}
