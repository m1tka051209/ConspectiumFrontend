package com.example.conspect.network;

import com.example.conspect.BuildConfig;

public class SupabaseConfig {
    // Теперь читаем из BuildConfig
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;

    // Остальные константы, которые зависят от URL
    public static final String SUPABASE_REST_URL = SUPABASE_URL + "/rest/v1/";
    public static final String SUPABASE_AUTH_URL = SUPABASE_URL + "/auth/v1/";

    public static final String HEADER_API_KEY = "apikey";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_PREFER = "Prefer";

    public static final String PREFER_RETURN_MINIMAL = "return=minimal";
    public static final String PREFER_RETURN_REPRESENTATION = "return=representation";

    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    public static final String TABLE_CONSPECTS = "conspects";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_CLASSES = "classes";

    private SupabaseConfig() {
    }
}
