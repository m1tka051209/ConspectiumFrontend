package com.example.conspect.ui.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.conspect.databinding.ActivityViewConspectBinding;
import com.example.conspect.models.Conspect;
import com.example.conspect.ui.create.CreateConspectActivity;

public class ViewConspectActivity extends AppCompatActivity {

    private ActivityViewConspectBinding binding;
    private ViewConspectViewModel viewModel;
    private Conspect currentConspect;
    private ActivityResultLauncher<Intent> editConspectLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        editConspectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Reload data if edited
                        loadData();
                    }
                }
        );

        binding = ActivityViewConspectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ViewConspectViewModel.class);

        setupToolbar();
        setupButtons();
        observeViewModel();

        loadData();
    }

    private void loadData() {
        long conspectId = getIntent().getLongExtra("id", -1);
        if (conspectId == -1) {
            Toast.makeText(this, "Ошибка: ID конспекта не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadConspect(conspectId);
    }

    private void observeViewModel() {
        viewModel.getConspect().observe(this, conspect -> {
            binding.progressBar.setVisibility(View.GONE);
            if (conspect != null) {
                this.currentConspect = conspect;
                updateUi(conspect);
            }
        });

        viewModel.getDeleteSuccess().observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);
            if (success != null && success) {
                Toast.makeText(this, "Конспект удалён", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // Notify previous screen to refresh
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            binding.progressBar.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                binding.btnEdit.setEnabled(true);
                binding.btnDelete.setEnabled(true);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUi(Conspect conspect) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(conspect.getTitle());
        }
        binding.tvTitle.setText(conspect.getTitle());
        binding.tvSubject.setText("Предмет: " + conspect.getSubject());
        binding.tvContent.setText(conspect.getContent());

        String dateText = conspect.getFormattedDate();
        if (conspect.getUpdatedAt() != null && !conspect.getUpdatedAt().isEmpty()) {
            if (conspect.getCreatedAt() != null && conspect.getUpdatedAt().compareTo(conspect.getCreatedAt()) > 0) {
                dateText += "\n" + conspect.getFormattedUpdatedDate();
            }
        }
        binding.tvDate.setText(dateText);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtons() {
        binding.btnEdit.setOnClickListener(v -> editConspect());
        binding.btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    private void editConspect() {
        if (currentConspect == null) return;
        Intent intent = new Intent(this, CreateConspectActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("id", currentConspect.getId());
        intent.putExtra("title", currentConspect.getTitle());
        intent.putExtra("subject", currentConspect.getSubject());
        intent.putExtra("content", currentConspect.getContent());
        editConspectLauncher.launch(intent);
    }

    private void showDeleteDialog() {
        if (currentConspect == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Удалить конспект?")
                .setMessage("Это действие нельзя отменить")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnEdit.setEnabled(false);
                    binding.btnDelete.setEnabled(false);
                    viewModel.deleteConspect(currentConspect);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}