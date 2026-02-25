package com.example.conspect.models;

import android.annotation.SuppressLint;

import com.google.gson.annotations.SerializedName;

public class Conspect {
    @SerializedName("id")
    private Long serverId; // ID с сервера Supabase

    private transient long localId; // ID из локальной базы Room

    private String title;
    private String content;

    private String subject;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("user_id")
    private String userId;

    private transient boolean isSynced;

    public Conspect() {}

    // Getters and Setters

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public String getFormattedDate() {
        return formatDate(createdAt, "Создано: ");
    }

    public String getFormattedUpdatedDate() {
        return formatDate(updatedAt, "Изменено: ");
    }

    @SuppressLint("DefaultLocale")
    private String formatDate(String dateStr, String prefix) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            String datePart = dateStr.split("T")[0];
            String timePart = dateStr.split("T")[1].substring(0, 5);

            int hours = Integer.parseInt(timePart.split(":")[0]);
            int minutes = Integer.parseInt(timePart.split(":")[1]);

            hours += 3;

            String[] dateParts = datePart.split("-");
            int day = Integer.parseInt(dateParts[2]);
            int month = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[0]);

            if (hours >= 24) {
                hours -= 24;
                day += 1;
                int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
                if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
                    daysInMonth[2] = 29;
                }
                if (day > daysInMonth[month]) {
                    day = 1;
                    month += 1;
                    if (month > 12) {
                        month = 1;
                        year += 1;
                    }
                }
            }

            return prefix + String.format("%02d.%02d.%d %02d:%02d",
                    day, month, year, hours, minutes);

        } catch (Exception e) {
            return prefix + dateStr;
        }
    }
}