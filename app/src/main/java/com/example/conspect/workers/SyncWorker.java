package com.example.conspect.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.example.conspect.repository.ConspectRepository;
import com.google.common.util.concurrent.ListenableFuture;

public class SyncWorker extends ListenableWorker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            Log.d(TAG, "Starting background sync...");

            ConspectRepository repository = new ConspectRepository(getApplicationContext());

            repository.syncWithServer(new ConspectRepository.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Sync successful");
                    completer.set(Result.success());
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Sync failed: " + error);
                    completer.set(Result.retry());
                }
            });
            // Этот объект используется для отладки, чтобы CallbackToFutureAdapter не жаловался
            return "Syncing";
        });
    }
}