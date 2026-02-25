package com.example.conspect.ui.create;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.conspect.models.Conspect;
import com.example.conspect.repository.ConspectRepository;

public class CreateConspectViewModel extends AndroidViewModel {

    private final ConspectRepository repository;
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public CreateConspectViewModel(@NonNull Application application) {
        super(application);
        repository = new ConspectRepository(application);
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void saveConspect(Conspect conspect, String token, boolean isEditMode, long editConspectId) {
        if (isEditMode) {
            updateConspect(conspect, token, editConspectId);
        } else {
            createConspect(conspect, token);
        }
    }

    private void createConspect(Conspect conspect, String token) {
        repository.addConspect(conspect, new ConspectRepository.DataCallback<Conspect>() {
            @Override
            public void onSuccess(Conspect data) {
                saveSuccess.postValue(true);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }

    private void updateConspect(Conspect conspect, String token, long editConspectId) {
        repository.updateConspect(conspect, editConspectId, token, new ConspectRepository.DataCallback<Conspect>() {
            @Override
            public void onSuccess(Conspect data) {
                saveSuccess.postValue(true);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }
}