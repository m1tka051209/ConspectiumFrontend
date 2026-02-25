package com.example.conspect.ui.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.conspect.models.Conspect;
import com.example.conspect.repository.ConspectRepository;

public class ViewConspectViewModel extends AndroidViewModel {

    private final ConspectRepository repository;
    private final MutableLiveData<Conspect> conspect = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ViewConspectViewModel(@NonNull Application application) {
        super(application);
        repository = new ConspectRepository(application);
    }

    public LiveData<Conspect> getConspect() {
        return conspect;
    }

    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadConspect(long conspectId) {
        repository.getConspectById(conspectId, new ConspectRepository.DataCallback<Conspect>() {
            @Override
            public void onSuccess(Conspect data) {
                conspect.postValue(data);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }

    public void deleteConspect(Conspect conspectToDelete) {
        repository.deleteConspect(conspectToDelete, new ConspectRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                deleteSuccess.postValue(true);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }
}