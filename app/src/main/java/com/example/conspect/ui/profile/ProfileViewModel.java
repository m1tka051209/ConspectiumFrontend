package com.example.conspect.ui.profile;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.conspect.repository.ConspectRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final ConspectRepository repository;
    private final MutableLiveData<Integer> conspectsCount = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ConspectRepository(application);
    }

    public LiveData<Integer> getConspectsCount() {
        return conspectsCount;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadConspectsCount() {
        repository.getConspectsCount(new ConspectRepository.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                conspectsCount.postValue(data);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }
}