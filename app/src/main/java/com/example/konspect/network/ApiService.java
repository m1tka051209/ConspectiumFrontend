package com.example.konspect.network;

import com.example.konspect.models.Conspect;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("api/conspects")
    Call<List<Conspect>> getAllConspects();

    @POST("api/conspects")
    Call<Conspect> createConspect(@Body Conspect conspect);

    @GET("api/test")
    Call<String> testConnection();
}
