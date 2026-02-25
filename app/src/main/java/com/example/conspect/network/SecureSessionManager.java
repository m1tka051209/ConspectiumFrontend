package com.example.conspect.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.conspect.models.User;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureSessionManager {
    private static final String PREF_NAME = "secure_consect_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences encryptedPrefs;
    private Context context;

    public interface SessionCallback {
        void onSuccess();
        void onError(String message);
    }

    public SecureSessionManager(Context context) {
        this.context = context;
        initEncryptedPrefs();
    }

    private void initEncryptedPrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            encryptedPrefs = context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public void saveAuthData(User user, String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null;
    }

    public String getAccessToken() {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public User getUser() {
        User user = new User();
        user.setId(encryptedPrefs.getString(KEY_USER_ID, null));
        user.setEmail(encryptedPrefs.getString(KEY_USER_EMAIL, null));
        user.setUsername(encryptedPrefs.getString(KEY_USERNAME, null));
        user.setAccessToken(getAccessToken());
        return user;
    }

    public void logout() {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.clear();
        editor.apply();
    }

    public void deleteAccount(SessionCallback callback) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            callback.onError("Пользователь не авторизован");
            return;
        }

        SupabaseAuthClient.getInstance().deleteAccount(accessToken, new SupabaseAuthClient.AuthCallback() {
            @Override
            public void onSuccess(User user, String accessToken, String refreshToken) {
                logout(); // Полностью очищаем локальные данные
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}