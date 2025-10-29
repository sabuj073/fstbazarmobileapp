package com.app.fstbazar;
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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

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

    String sitesUrl = "https://fstbazar.com/api/v2/all-sites";
    String bannersUrl = "https://fstbazar.com/api/v2/banners";

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

        loadSites();
        loadBanners();
    }

    private void loadSites() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, sitesUrl, null,
                response -> {
                    try {
                        JSONArray dataArray = response.getJSONArray("data");
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);
                            String name = obj.getString("name");
                            String logo = obj.getString("logo");
                            JSONObject links = obj.getJSONObject("links");
                            String link = links.getString("products");
                            siteList.add(new SiteModel(name, logo, link));
                        }
                        adapter = new SiteAdapter(MainActivity.this, siteList);
                        recyclerSites.setAdapter(adapter);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Failed to load icons", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", error.toString());
                });

        queue.add(request);
    }

    private void loadBanners() {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest bannerReq = new JsonObjectRequest(Request.Method.GET, bannersUrl, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            String imageUrl = obj.getString("photo");
                            bannerUrls.add(imageUrl);
                        }

                        bannerAdapter = new BannerAdapter(MainActivity.this, bannerUrls);
                        bannerViewPager.setAdapter(bannerAdapter);
                        startAutoSlide();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("BANNER_API", "Error: " + error.toString()));
        queue.add(bannerReq);
    }

    private void startAutoSlide() {
        bannerHandler.postDelayed(new Runnable() {
            int currentPage = 0;
            @Override
            public void run() {
                if (bannerAdapter != null && bannerAdapter.getItemCount() > 0) {
                    currentPage = (currentPage + 1) % bannerAdapter.getItemCount();
                    bannerViewPager.setCurrentItem(currentPage, true);
                    bannerHandler.postDelayed(this, 4000); // slide every 4 sec
                }
            }
        }, 4000);
    }
}
