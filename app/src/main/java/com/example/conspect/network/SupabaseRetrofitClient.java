package com.example.conspect.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseRetrofitClient {
    private static SupabaseApiService instance;

    public static SupabaseApiService getInstance() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(SupabaseConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(SupabaseConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(SupabaseConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.SUPABASE_REST_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            instance = retrofit.create(SupabaseApiService.class);
        }
        return instance;
    }
}
