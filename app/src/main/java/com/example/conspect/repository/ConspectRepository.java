package com.example.conspect.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.conspect.database.AppDatabase;
import com.example.conspect.database.dao.ConspectDao;
import com.example.conspect.database.entities.ConspectEntity;
import com.example.conspect.models.Conspect;
import com.example.conspect.network.SecureSessionManager;
import com.example.conspect.network.SupabaseApiService;
import com.example.conspect.network.SupabaseConfig;
import com.example.conspect.network.SupabaseRetrofitClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConspectRepository {
    private final ConspectDao conspectDao;
    private final SupabaseApiService supabaseApi;
    private final SecureSessionManager sessionManager;
    private final ExecutorService executorService;

    public ConspectRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.conspectDao = db.conspectDao();
        this.supabaseApi = SupabaseRetrofitClient.getInstance();
        this.sessionManager = new SecureSessionManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void getConspectsCount(DataCallback<Integer> callback) {
        executorService.execute(() -> {
            String userId = sessionManager.getUser().getId();
            if (userId == null || userId.isEmpty()) {
                callback.onError("User ID не найден");
                return;
            }
            int count = conspectDao.getConspectsCount(userId);
            callback.onSuccess(count);
        });
    }

    public void getConspectById(long conspectId, DataCallback<Conspect> callback) {
        executorService.execute(() -> {
            String userId = sessionManager.getUser().getId();
            if (userId == null || userId.isEmpty()) {
                callback.onError("User ID не найден");
                return;
            }
            ConspectEntity entity = conspectDao.getConspectById(conspectId, userId);
            if (entity != null) {
                callback.onSuccess(entityToModel(entity));
            } else {
                callback.onError("Конспект не найден");
            }
        });
    }

    public void deleteConspect(Conspect conspect, DataCallback<Void> callback) {
        executorService.execute(() -> {
            conspectDao.delete(modelToEntity(conspect));

            if (conspect.getServerId() != null) {
                String accessToken = sessionManager.getAccessToken();
                if (accessToken == null) {
                    callback.onSuccess(null); // Local delete is done, but can't sync
                    return;
                }
                supabaseApi.delete(SupabaseConfig.TABLE_CONSPECTS, "eq." + conspect.getServerId(), SupabaseConfig.SUPABASE_KEY, "Bearer " + accessToken)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                callback.onSuccess(null);
                            }

                            @Override
                            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                                callback.onSuccess(null); // Even on failure, for user it's deleted
                            }
                        });
            } else {
                callback.onSuccess(null);
            }
        });
    }

    public void addConspect(Conspect conspect, DataCallback<Conspect> callback) {
        String accessToken = sessionManager.getAccessToken();
        String userId = sessionManager.getUser().getId();
        if (accessToken == null || userId == null) {
            callback.onError("User not authenticated");
            return;
        }
        conspect.setUserId(userId);

        executorService.execute(() -> {
            ConspectEntity entity = modelToEntity(conspect);
            entity.setSynced(false);
            long localId = conspectDao.insert(entity);
            conspect.setLocalId(localId);

            supabaseApi.create(SupabaseConfig.TABLE_CONSPECTS, SupabaseConfig.SUPABASE_KEY, "Bearer " + accessToken, SupabaseConfig.PREFER_RETURN_REPRESENTATION, conspect)
                .enqueue(new Callback<List<Conspect>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Conspect>> call, @NonNull Response<List<Conspect>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Conspect savedConspect = response.body().get(0);
                            executorService.execute(() -> {
                                conspectDao.updateServerIdAndSyncState(localId, savedConspect.getServerId());
                                savedConspect.setLocalId(localId);
                                callback.onSuccess(savedConspect);
                            });
                        } else {
                            callback.onSuccess(conspect);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Conspect>> call, @NonNull Throwable t) {
                        callback.onSuccess(conspect);
                    }
                });
        });
    }

    public void updateConspect(Conspect conspect, String token, DataCallback<Conspect> callback) {
        executorService.execute(() -> {
            ConspectEntity entity = modelToEntity(conspect);
            entity.setSynced(false);
            conspectDao.insert(entity); 

            supabaseApi.update(SupabaseConfig.TABLE_CONSPECTS, "eq." + conspect.getServerId(), SupabaseConfig.SUPABASE_KEY, "Bearer " + token, SupabaseConfig.PREFER_RETURN_REPRESENTATION, conspect)
                .enqueue(new Callback<List<Conspect>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Conspect>> call, @NonNull Response<List<Conspect>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                             Conspect updatedConspect = response.body().get(0);
                             executorService.execute(() -> {
                                 ConspectEntity serverEntity = modelToEntity(updatedConspect);
                                 serverEntity.setSynced(true);
                                 conspectDao.insert(serverEntity);
                                 callback.onSuccess(updatedConspect);
                             });
                        } else {
                            callback.onSuccess(conspect);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Conspect>> call, @NonNull Throwable t) {
                        callback.onSuccess(conspect);
                    }
                });
        });
    }

    public void getAllConspects(DataCallback<List<Conspect>> callback) {
        executorService.execute(() -> {
            String userId = sessionManager.getUser().getId();
            if (userId == null || userId.isEmpty()) {
                callback.onError("User ID не найден");
                return;
            }
            List<ConspectEntity> entities = conspectDao.getAllConspects(userId);
            List<Conspect> conspects = entitiesToModels(entities);
            callback.onSuccess(conspects);
        });
    }

    public void syncWithServer(SyncCallback callback) {
        String accessToken = sessionManager.getAccessToken();
        if (accessToken == null) {
            if (callback != null) callback.onError("Не авторизован");
            return;
        }

        supabaseApi.getAll(SupabaseConfig.TABLE_CONSPECTS, SupabaseConfig.SUPABASE_KEY, "Bearer " + accessToken)
            .enqueue(new Callback<List<Conspect>>() {
                @Override
                public void onResponse(Call<List<Conspect>> call, Response<List<Conspect>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        executorService.execute(() -> {
                            List<ConspectEntity> serverEntities = modelsToEntities(response.body());
                            for (ConspectEntity entity : serverEntities) {
                                entity.setSynced(true);
                            }
                            conspectDao.insert(serverEntities);
                            uploadUnsyncedConspects(callback);
                        });
                    } else {
                        if (callback != null) callback.onError("Ошибка загрузки: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<List<Conspect>> call, Throwable t) {
                    uploadUnsyncedConspects(callback);
                }
            });
    }

    private void uploadUnsyncedConspects(SyncCallback callback) {
        String userId = sessionManager.getUser().getId();
        String accessToken = sessionManager.getAccessToken();
        if (userId == null || accessToken == null) {
            if (callback != null) callback.onSuccess();
            return;
        }

        executorService.execute(() -> {
            List<ConspectEntity> unsynced = conspectDao.getUnsyncedConspects(userId);
            if (unsynced.isEmpty()) {
                if (callback != null) callback.onSuccess();
                return;
            }

            AtomicInteger counter = new AtomicInteger(unsynced.size());

            for (ConspectEntity entity : unsynced) {
                Conspect conspect = entityToModel(entity);
                Callback<List<Conspect>> syncCall = new Callback<List<Conspect>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Conspect>> call, @NonNull Response<List<Conspect>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            executorService.execute(() -> conspectDao.markAsSynced(entity.getLocalId()));
                        }
                        if (counter.decrementAndGet() == 0 && callback != null) callback.onSuccess();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Conspect>> call, @NonNull Throwable t) {
                        if (counter.decrementAndGet() == 0 && callback != null) callback.onSuccess();
                    }
                };

                if (entity.getServerId() == null) {
                    supabaseApi.create(SupabaseConfig.TABLE_CONSPECTS, SupabaseConfig.SUPABASE_KEY, "Bearer " + accessToken, SupabaseConfig.PREFER_RETURN_REPRESENTATION, conspect).enqueue(syncCall);
                } else {
                    supabaseApi.update(SupabaseConfig.TABLE_CONSPECTS, "eq." + entity.getServerId(), SupabaseConfig.SUPABASE_KEY, "Bearer " + accessToken, SupabaseConfig.PREFER_RETURN_REPRESENTATION, conspect).enqueue(syncCall);
                }
            }
        });
    }

    private List<Conspect> entitiesToModels(List<ConspectEntity> entities) {
        List<Conspect> models = new ArrayList<>();
        for (ConspectEntity entity : entities) {
            models.add(entityToModel(entity));
        }
        return models;
    }

    private Conspect entityToModel(ConspectEntity entity) {
        Conspect model = new Conspect();
        model.setLocalId(entity.getLocalId());
        model.setServerId(entity.getServerId());
        model.setTitle(entity.getTitle());
        model.setSubject(entity.getSubject());
        model.setContent(entity.getContent());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        model.setUserId(entity.getUserId());
        model.setSynced(entity.isSynced());
        return model;
    }

    private List<ConspectEntity> modelsToEntities(List<Conspect> models) {
        List<ConspectEntity> entities = new ArrayList<>();
        for (Conspect model : models) {
            entities.add(modelToEntity(model));
        }
        return entities;
    }

    private ConspectEntity modelToEntity(Conspect model) {
        ConspectEntity entity = new ConspectEntity();
        entity.setLocalId(model.getLocalId());
        entity.setServerId(model.getServerId());
        entity.setTitle(model.getTitle());
        entity.setSubject(model.getSubject());
        entity.setContent(model.getContent());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        entity.setUserId(model.getUserId());
        entity.setSynced(model.isSynced());
        return entity;
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }
}