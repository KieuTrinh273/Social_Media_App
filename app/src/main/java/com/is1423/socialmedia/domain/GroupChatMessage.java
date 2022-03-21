package com.is1423.socialmedia.domain;

public class GroupChatMessage {
    String message, sender, sendDatetime, type;

    public GroupChatMessage() {
    }

    public GroupChatMessage(String message, String sender, String sendDatetime, String type) {
        this.message = message;
        this.sender = sender;
        this.sendDatetime = sendDatetime;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSendDatetime() {
        return sendDatetime;
    }

    public void setSendDatetime(String sendDatetime) {
        this.sendDatetime = sendDatetime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
