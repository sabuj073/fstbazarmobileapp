package com.app.fstbazar;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import java.util.Map;

public interface ApiService {
    @Headers("Accept: application/json")
    @POST("v1/auth/sendotp")
    Call<Map<String, Object>> sendOtp(@Body Map<String, String> body);

    @Headers("Accept: application/json")
    @POST("v1/auth/verifyotp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @Headers("Accept: application/json")
    @GET("v2/all-sites")
    Call<Map<String, Object>> getSites();

    @Headers("Accept: application/json")
    @GET("v2/banners")
    Call<Map<String, Object>> getBanners();

    @Headers("Accept: application/json")
    @GET("v1/auth/user")
    Call<Map<String, Object>> getUser(@Header("Authorization") String token);

}
