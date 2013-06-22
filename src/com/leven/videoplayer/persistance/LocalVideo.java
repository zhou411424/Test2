package com.leven.videoplayer.persistance;

public class LocalVideo {
    private String videoId;
    private String displayName;
    private String data;
    private String duration;
    private String mimeType;
    private String size;
    private String dateModified;
    private String bucketId;
    private String bucketDisplayName;
    public String getVideoId() {
        return videoId;
    }
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getSize() {
        return size;
    }
    public void setSize(String size) {
        this.size = size;
    }
    public String getDateModified() {
        return dateModified;
    }
    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }
    public String getBucketId() {
        return bucketId;
    }
    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }
    public String getBucketDisplayName() {
        return bucketDisplayName;
    }
    public void setBucketDisplayName(String bucketDisplayName) {
        this.bucketDisplayName = bucketDisplayName;
    }
    
}
