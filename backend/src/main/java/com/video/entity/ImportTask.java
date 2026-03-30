package com.video.entity;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "import_tasks")
public class ImportTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_id", unique = true, nullable = false)
    private String taskId;
    
    @Column(name = "source_name")
    private String sourceName;
    
    @Column(name = "source_path")
    private String sourcePath;
    
    @Column(name = "source_size")
    private Long sourceSize;
    
    @Column(nullable = false)
    private String status = "pending";
    
    private Integer progress = 0;
    
    private String message;
    
    @Column(name = "video_uuid")
    private String videoUuid;
    
    @Column(name = "created_at")
    private Timestamp createdAt;
    
    @Column(name = "updated_at")
    private Timestamp updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    
    public Long getSourceSize() { return sourceSize; }
    public void setSourceSize(Long sourceSize) { this.sourceSize = sourceSize; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getVideoUuid() { return videoUuid; }
    public void setVideoUuid(String videoUuid) { this.videoUuid = videoUuid; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
