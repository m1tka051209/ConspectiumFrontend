package com.example.conspect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.PeriodicWorkRequest;

import com.example.conspect.databinding.ActivityMainBinding;
import com.example.conspect.network.SecureSessionManager;
import com.example.conspect.ui.auth.AuthActivity;
import com.example.conspect.ui.create.CreateConspectActivity;
import com.example.conspect.ui.home.HomeFragment;
import com.example.conspect.workers.SyncWorker;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private SecureSessionManager sessionManager;

    private ActivityResultLauncher<Intent> createConspectLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SecureSessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActivityResultLauncher();
        setupPeriodicSync();

        setupNavigation();
        setupFAB();
    }

    private void setupPeriodicSync() {
        PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15,
                java.util.concurrent.TimeUnit.MINUTES
        ).build();

        androidx.work.WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "ConspectSync",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        syncWork
                );
    }

    private void setupActivityResultLauncher() {
        createConspectLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this,
                                "Конспект успешно сохранён!", Toast.LENGTH_SHORT).show();
                        refreshConspects();
                    }
                }
        );
    }

    private void setupFAB() {
        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateConspectActivity.class);
            createConspectLauncher.launch(intent);
        });
    }

    public void refreshConspects() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).refreshList();
            }
        }
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();

                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.homeFragment,
                        R.id.searchFragment, // Возвращаем searchFragment
                        R.id.profileFragment
                ).setOpenableLayout(binding.drawerLayout).build();

                NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
                NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
                NavigationUI.setupWithNavController(binding.navView, navController);

                binding.navView.setNavigationItemSelectedListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.btn_logout) {
                        sessionManager.logout();
                        startActivity(new Intent(this, AuthActivity.class));
                        finish();
                    } else if (id == R.id.btn_delete_account) {
                        navController.navigate(R.id.profileFragment); // Переходим в профиль для удаления
                    } else {
                        NavigationUI.onNavDestinationSelected(item, navController);
                    }
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                });

            } else {
                showErrorAndContinue();
            }
        } catch (Exception e) {
            Log.e("Main Activity", "Ошибка настройки Navigation: " + e.getMessage());
            showErrorAndContinue();
        }
    }

    private void showErrorAndContinue() {
        binding.bottomNavigation.setVisibility(View.GONE);
        Toast.makeText(this, "Навигация временно недоступна", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}