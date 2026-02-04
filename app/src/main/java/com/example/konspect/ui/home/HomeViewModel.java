package com.example.konspect.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.konspect.models.Conspect;
import com.example.konspect.network.ApiService;
import com.example.konspect.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<List<Conspect>> conspectLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ApiService apiService = RetrofitClient.getApiService();

    public HomeViewModel() {
//        loadConpects();
        loadTestData();
    }

    private void loadTestData() {
        isLoadingLiveData.setValue(true);

        new android.os.Handler().postDelayed(() -> {
            List<Conspect> testData = new ArrayList<>();

            Conspect c1 = new Conspect();
            c1.setId(1L);
            c1.setTitle("Квадратные уравнения");
            c1.setContent("Формула дискриминанта: D = b² - 4ac. Корни уравнения находятся по формуле: x = (-b ± √D) / 2a");
            c1.setSubject("Математика");
            c1.setCreatedAt("2024-02-15 10:30");


            Conspect c2 = new Conspect();
            c2.setId(2L);
            c2.setTitle("Фотосинтез");
            c2.setContent("Процесс преобразования световой энергии в химическую энергию органических соединений");
            c2.setSubject("Биология");
            c2.setCreatedAt("2024-02-14 14:20");

            Conspect c3 = new Conspect();
            c3.setId(3L);
            c3.setTitle("Вторая мировая война");
            c3.setContent("1939-1945, основные события: нападение на Польшу, Сталинградская битва, открытие второго фронта");
            c3.setSubject("История");
            c3.setCreatedAt("2024-02-13 16:45");

            testData.add(c1);
            testData.add(c2);
            testData.add(c3);

            conspectLiveData.setValue(testData);
            isLoadingLiveData.setValue(false);
        }, 2000);
    }

    public void loadConpects() {
        isLoadingLiveData.setValue(true);

        apiService.getAllConspects().enqueue(new Callback<List<Conspect>>() {
            @Override
            public void onResponse(Call<List<Conspect>> call, Response<List<Conspect>> response) {
                isLoadingLiveData.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    conspectLiveData.setValue(response.body());
                } else {
                    errorLiveData.setValue("Ошибка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Conspect>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                errorLiveData.setValue("Ошибка сети: " + t.getMessage());
            }
        });
    }

    public LiveData<List<Conspect>> getConspects() {
        return conspectLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void refresh() {
        loadConpects();
    }
}