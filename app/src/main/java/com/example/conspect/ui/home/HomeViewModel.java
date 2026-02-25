package com.example.conspect.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.conspect.models.Conspect;
import com.example.conspect.repository.ConspectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeViewModel extends AndroidViewModel {

    private final ConspectRepository repository;
    private final MutableLiveData<List<Conspect>> conspects = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private List<Conspect> originalConspects = new ArrayList<>(); // To store the master list
    private boolean isOfflineFilterActive = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ConspectRepository(application);
    }

    public LiveData<List<Conspect>> getConspects() {
        return conspects;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadConspects() {
        repository.getAllConspects(new ConspectRepository.DataCallback<List<Conspect>>() {
            @Override
            public void onSuccess(List<Conspect> data) {
                originalConspects = data;
                applyFilters(""); // Apply current filters to the new list
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }

    public void search(String query) {
        applyFilters(query);
    }

    public void clearSearch() {
        applyFilters("");
    }

    public void toggleOfflineFilter() {
        isOfflineFilterActive = !isOfflineFilterActive;
        applyFilters(""); // Re-apply filters with the new offline state
    }
    
    public boolean isOfflineFilterActive() {
        return isOfflineFilterActive;
    }

    private void applyFilters(String query) {
        List<Conspect> filteredList = new ArrayList<>(originalConspects);

        // Apply offline filter
        if (isOfflineFilterActive) {
            filteredList = filteredList.stream()
                    .filter(c -> !c.isSynced())
                    .collect(Collectors.toList());
        }

        // Apply search query
        if (query != null && !query.isEmpty()) {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = filteredList.stream()
                    .filter(c -> c.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                                 c.getSubject().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        
        conspects.postValue(filteredList);
    }
}