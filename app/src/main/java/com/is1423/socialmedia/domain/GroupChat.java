package com.is1423.socialmedia.domain;

public class GroupChat {
    String id, title, description, icon, created_by, created_datetime;

    public GroupChat(String id, String title, String description, String icon, String created_by, String created_datetime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.created_by = created_by;
        this.created_datetime = created_datetime;
    }

    public GroupChat() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public String getCreated_datetime() {
        return created_datetime;
    }

    public void setCreated_datetime(String created_datetime) {
        this.created_datetime = created_datetime;
    }
}
