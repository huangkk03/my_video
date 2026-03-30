package com.video.entity;

import lombok.Data;
import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String configKey;
    
    @Column(columnDefinition = "TEXT")
    private String configValue;
    
    private String description;
    
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
