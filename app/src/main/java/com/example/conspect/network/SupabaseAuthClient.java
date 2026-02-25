package com.example.conspect.network;

import com.example.conspect.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthClient {
    private static final String TAG = "SupabaseAuth";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static SupabaseAuthClient instance;
    private final OkHttpClient client;

    private SupabaseAuthClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(SupabaseConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(SupabaseConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(SupabaseConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    public static SupabaseAuthClient getInstance() {
        if (instance == null) {
            instance = new SupabaseAuthClient();
        }
        return instance;
    }

    public void signUp(String email, String password, String username, AuthCallback callback) {
        String url = SupabaseConfig.SUPABASE_AUTH_URL + "signup";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);

            JSONObject userData = new JSONObject();
            userData.put("username", username);
            json.put("data", userData);

        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .build();
        executeRequest(request, callback);
    }

    public void signIn(String email, String password, AuthCallback callback) {
        String url = SupabaseConfig.SUPABASE_AUTH_URL + "token?grant_type=password";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        executeRequest(request, callback);
    }

    private void executeRequest(Request request, AuthCallback callback) {
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                android.util.Log.d("SupabaseAuth", "Response code: " + response.code());
                android.util.Log.d("SupabaseAuth", "Response body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);

                        String accessToken = json.optString("access_token");
                        String refreshToken = json.optString("refresh_token");

                        JSONObject userJson = json.getJSONObject("user");
                        User user = new User();
                        user.setId(userJson.optString("id"));
                        user.setEmail(userJson.optString("email"));

                        callback.onSuccess(user, accessToken, refreshToken);
                    } catch (JSONException e) {
                        callback.onError("Ошибка парсинга ответа");
                    }
                } else {
                    String userMessage = parseError(response.code(), responseBody);
                    callback.onError(userMessage);
                }
            } catch (IOException e) {
                callback.onError("Нет подключения к интернету");
            }
        }).start();
    }

    private String parseError(int code, String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            String errorCode = json.optString("error_code", "");
            String msg = json.optString("msg", "");

            switch (errorCode) {
                case "invalid_credentials":
                    return "Неверный email или пароль";
                case "user_already_exists":
                    return "Пользователь с таким email уже существует";
                case "weak_password":
                    return "Слишком простой пароль";
                case "email_not_confirmed":
                    return "Подтвердите email";
                default:
                    return "Ошибка: " + msg;
            }
        } catch (JSONException e) {
            return "Ошибка сервера (код: " + code + ")";
        }
    }

    public void deleteAccount(String accessToken, AuthCallback callback) {
        String url = SupabaseConfig.SUPABASE_AUTH_URL + "user";

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        executeVoidRequest(request, callback);
    }

    private void executeVoidRequest(Request request, AuthCallback callback) {
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null, null, null);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    callback.onError("Ошибка: " + response.code() + " - " + errorBody);
                }
            } catch (IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }
        }).start();
    }

    public interface AuthCallback {
        void onSuccess(User user, String accessToken, String refreshToken);

        void onError(String error);
    }
}




