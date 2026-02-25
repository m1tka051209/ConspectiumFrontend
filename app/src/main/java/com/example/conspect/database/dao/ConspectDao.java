package com.example.conspect.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.conspect.database.entities.ConspectEntity;

import java.util.List;

@Dao
public interface ConspectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ConspectEntity conspect);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<ConspectEntity> conspects);

    @Update
    void update(ConspectEntity conspect);

    @Delete
    void delete(ConspectEntity conspect);

    @Query("UPDATE conspects SET server_id = :serverId, is_synced = 1 WHERE id = :localId")
    void updateServerIdAndSyncState(long localId, long serverId);

    @Query("SELECT * FROM conspects WHERE is_deleted = 0 AND user_id = :userId ORDER BY created_at DESC")
    List<ConspectEntity> getAllConspects(String userId);

    @Query("SELECT * FROM conspects WHERE id = :localId AND user_id = :userId")
    ConspectEntity getConspectById(long localId, String userId);

    @Query("SELECT * FROM conspects WHERE is_synced = 0 AND is_deleted = 0 AND user_id = :userId")
    List<ConspectEntity> getUnsyncedConspects(String userId);

    @Query("UPDATE conspects SET is_synced = 1 WHERE id = :localId")
    void markAsSynced(long localId);


    @Query("UPDATE conspects SET is_deleted = 1, is_synced = 0 WHERE id = :localId")
    void markAsDeleted(long localId);

    @Query("SELECT COUNT(*) FROM conspects WHERE is_deleted = 0 AND user_id = :userId")
    int getConspectsCount(String userId);
}