package com.example.conspect.ui.profile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.conspect.databinding.FragmentProfileBinding;
import com.example.conspect.models.User;
import com.example.conspect.network.SecureSessionManager;
import com.example.conspect.ui.auth.AuthActivity;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private SecureSessionManager sessionManager;
    private ProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sessionManager = new SecureSessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupUserInfo();
        setupButtons();
        observeViewModel();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.loadConspectsCount();
    }

    private void observeViewModel() {
        viewModel.getConspectsCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                binding.tvConspectsCount.setText("Всего конспектов: " + count);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setupUserInfo() {
        User user = sessionManager.getUser();
        if (user != null && user.getEmail() != null) {
            binding.textProfile.setText("Профиль: " + user.getEmail());
        } else {
            binding.textProfile.setText("Профиль пользователя");
        }
    }

    private void setupButtons() {
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    sessionManager.logout();
                    startActivity(new Intent(requireContext(), AuthActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Это действие НЕОБРАТИМО. Все ваши данные будут удалены. Продолжить?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    sessionManager.deleteAccount(new SecureSessionManager.SessionCallback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Аккаунт удалён", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(requireContext(), AuthActivity.class));
                                    getActivity().finish();
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}