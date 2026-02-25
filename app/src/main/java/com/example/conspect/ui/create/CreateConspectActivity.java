package com.example.conspect.ui.create;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.conspect.databinding.ActivityCreateConspectBinding;
import com.example.conspect.models.Conspect;
import com.example.conspect.network.SecureSessionManager;
import com.example.conspect.ui.camera.CameraActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class CreateConspectActivity extends AppCompatActivity {

    private ActivityCreateConspectBinding binding;
    private CreateConspectViewModel viewModel;
    private boolean isEditMode = false;
    private long editConspectId = -1;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateConspectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreateConspectViewModel.class);

        setupToolbar();
        setupViews();
        loadEditData();
        setupSaveButton();
        observeViewModel();

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String recognizedText = result.getData().getStringExtra("recognized_text");
                        if (recognizedText != null) {
                            String currentText = binding.contentEdittext.getText() != null ? binding.contentEdittext.getText().toString() : "";
                            binding.contentEdittext.setText(currentText + "\n\n" + recognizedText);
                        }
                    }
                }
        );
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        binding.tilContent.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            cameraLauncher.launch(intent);
        });
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveConspect();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadEditData() {
        Intent intent = getIntent();
        if ("edit".equals(intent.getStringExtra("mode"))) {
            isEditMode = true;
            editConspectId = intent.getLongExtra("id", -1);
            binding.titleEdittext.setText(intent.getStringExtra("title"));
            binding.subjectEdittext.setText(intent.getStringExtra("subject"));
            binding.contentEdittext.setText(intent.getStringExtra("content"));
            binding.saveButton.setText("Сохранить изменения");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Редактирование");
            }
        }
    }

    private void observeViewModel() {
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                handleUiForFailure(error);
            }
        });
    }

    private boolean validateInputs() {
        String title = Objects.requireNonNull(binding.titleEdittext.getText()).toString().trim();
        String subject = Objects.requireNonNull(binding.subjectEdittext.getText()).toString().trim();

        binding.tilTitle.setError(title.isEmpty() ? "Введите название конспекта" : null);
        binding.tilSubject.setError(subject.isEmpty() ? "Введите предмет" : null);

        return !title.isEmpty() && !subject.isEmpty();
    }

    @SuppressLint("SetTextI18n")
    private void saveConspect() {
        handleUiForSave();

        SecureSessionManager sessionManager = new SecureSessionManager(this);
        String accessToken = sessionManager.getAccessToken();
        if (accessToken == null) {
            handleUiForFailure("Ошибка: не авторизован");
            return;
        }

        Conspect conspect = new Conspect();
        conspect.setTitle(Objects.requireNonNull(binding.titleEdittext.getText()).toString().trim());
        conspect.setSubject(Objects.requireNonNull(binding.subjectEdittext.getText()).toString().trim());
        conspect.setContent(Objects.requireNonNull(binding.contentEdittext.getText()).toString().trim());

        viewModel.saveConspect(conspect, accessToken, isEditMode, editConspectId);
    }

    @SuppressLint("SetTextI18n")
    private void handleUiForSave() {
        binding.saveButton.setEnabled(false);
        binding.saveButton.setText(isEditMode ? "Сохранение изменений..." : "Сохранение...");
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void handleUiForFailure(String errorMessage) {
        binding.saveButton.setEnabled(true);
        binding.saveButton.setText(isEditMode ? "Сохранить изменения" : "Сохранить конспект");
        binding.progressBar.setVisibility(View.GONE);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}
