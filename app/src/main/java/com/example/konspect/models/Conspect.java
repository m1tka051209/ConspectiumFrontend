package com.example.konspect.models;

public class Conspect {
    private long id;
    private String title;
    private String content;

    private String subject;

    private String createdAt;

    public Conspect(long id, String title, String content, String subject, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.createdAt = createdAt;
    }

    public Conspect() {
    }

    public String getFormattedDate() {
        return "Дата: " + createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
}
