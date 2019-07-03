package com.mqtt.sdk.topic;

public class TopicBean {
    private String message;
    private String update;
    private String cmd;
    private String uploadInfo;
    private String playFeedback;

    public void setMessageTopic(String message) {
        this.message = message;
    }

    public void setUpdateTopic(String update) {
        this.update = update;
    }

    public void setCmdTopic(String cmd) {
        this.cmd = cmd;
    }

    public void setUploadInfoTopic(String uploadInfo) {
        this.uploadInfo = uploadInfo;
    }

    public void setPlayFeedbackTopic(String playFeedback) {
        this.playFeedback = playFeedback;
    }

    public String getMessageTopic() {
        return message;
    }

    public String getUpdateTopic() {
        return update;
    }

    public String getCmdTopic() {
        return cmd;
    }

    public String getUploadInfoTopic() {
        return uploadInfo;
    }

    public String getPlayFeedbackTopic() {
        return playFeedback;
    }


}
