package com.example.conspect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conspect.MainActivity;
import com.example.conspect.databinding.ActivityAuthBinding;
import com.example.conspect.models.User;
import com.example.conspect.network.SecureSessionManager;
import com.example.conspect.network.SupabaseAuthClient;

import java.util.Objects;

public class AuthActivity extends AppCompatActivity {

    private SecureSessionManager sessionManager;
    private ActivityAuthBinding binding;
    private SupabaseAuthClient authClient;

    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authClient = SupabaseAuthClient.getInstance();
        sessionManager = new SecureSessionManager(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnAuth.setOnClickListener(v -> {
            if (isRegisterMode) {
                signUp();
            } else {
                signIn();
            }
        });

        binding.tvSwitchMode.setOnClickListener(v -> toggleMode());

        binding.tvForgotPassword.setOnClickListener(v -> forgotPassword());

        binding.cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                binding.etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            binding.etPassword.setSelection(Objects.requireNonNull(binding.etPassword.getText()).length());
        });
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;

        if (isRegisterMode) {
            binding.tvAuthTitle.setText("Регистрация");
            binding.btnAuth.setText("Зарегистрироваться");
            binding.tvSwitchMode.setText("Уже есть аккаунт? Войти");
            binding.tilUsername.setVisibility(View.VISIBLE);
            binding.tvForgotPassword.setVisibility(View.GONE);
        } else {
            binding.tvAuthTitle.setText("Вход в аккаунт");
            binding.btnAuth.setText("Войти");
            binding.tvSwitchMode.setText("Нет аккаунта? Зарегистрироваться");
            binding.tilUsername.setVisibility(View.GONE);
            binding.tvForgotPassword.setVisibility(View.VISIBLE);
        }

        binding.etUsername.setError(null);
        binding.etEmail.setError(null);
        binding.etPassword.setError(null);
    }

    private void signIn() {
        String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();

        if (!validateLoginInputs(email, password)) return;

        setLoading(true);

        authClient.signIn(email, password, new SupabaseAuthClient.AuthCallback() {
            @Override
            public void onSuccess(User user, String accessToken, String refreshToken) {
                runOnUiThread(() -> {
                    setLoading(false);
                    sessionManager.saveAuthData(user, accessToken, refreshToken);

                    Toast.makeText(AuthActivity.this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Ошибка входа: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void signUp() {
        String username = Objects.requireNonNull(binding.etUsername.getText()).toString().trim();
        String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();

        if (!validateRegisterInputs(username, email, password)) return;

        setLoading(true);

        authClient.signUp(email, password, username, new SupabaseAuthClient.AuthCallback() {
            @Override
            public void onSuccess(User user, String accessToken, String refreshToken) {
                runOnUiThread(() -> {
                    setLoading(false);
                    sessionManager.saveAuthData(user, accessToken, refreshToken);

                    Toast.makeText(AuthActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Ошибка регистрации: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void forgotPassword() {
        String email = Objects.requireNonNull(Objects.requireNonNull(binding.etEmail.getText()).toString().trim());
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Введите email для восстановления пароля");
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Неверный формат email");
            return;
        }

        Toast.makeText(this, "Инструкции отправлены на " + email, Toast.LENGTH_LONG).show();
    }

    private boolean validateLoginInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Введите email");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Неверный формат email");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Введите пароль");
            return false;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Пароль должен быть минимум 6 символов");
            return false;
        }
        return true;
    }

    private boolean validateRegisterInputs(String username, String email, String password) {
        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("Введите имя пользователя");
            return false;
        }
        if (username.length() < 3) {
            binding.etUsername.setError("Имя должно быть минимум 3 символа");
            return false;
        }
        return validateLoginInputs(email, password);
    }

    private void setLoading(boolean loading) {
        binding.btnAuth.setEnabled(!loading);
        binding.tvSwitchMode.setEnabled(!loading);
        binding.etUsername.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
        binding.cbShowPassword.setEnabled(!loading);
        binding.tvForgotPassword.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}