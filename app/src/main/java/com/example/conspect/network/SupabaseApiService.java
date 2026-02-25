package com.example.conspect.network;

import com.example.conspect.models.Conspect;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SupabaseApiService {
    @GET("{table}")
    Call<List<Conspect>> getAll(
            @Path("table") String table,
            @Header(SupabaseConfig.HEADER_API_KEY) String apiKey,
            @Header(SupabaseConfig.HEADER_AUTHORIZATION) String authorization
    );

    @POST("{table}")
    Call<List<Conspect>> create(
            @Path("table") String table,
            @Header(SupabaseConfig.HEADER_API_KEY) String apiKey,
            @Header(SupabaseConfig.HEADER_AUTHORIZATION) String authorization,
            @Header(SupabaseConfig.HEADER_PREFER) String prefer,
            @Body Conspect conspect
    );

    @PATCH("{table}")
    Call<List<Conspect>> update(
            @Path("table") String table,
            @Query("id") String idFilter,
            @Header(SupabaseConfig.HEADER_API_KEY) String apiKey,
            @Header(SupabaseConfig.HEADER_AUTHORIZATION) String authorization,
            @Header(SupabaseConfig.HEADER_PREFER) String prefer,
            @Body Conspect conspect
    );
}
