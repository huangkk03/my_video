package com.video.repository;

import com.video.entity.ImportTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportTaskRepository extends JpaRepository<ImportTask, Long> {
    Optional<ImportTask> findByTaskId(String taskId);
    Optional<ImportTask> findByVideoUuid(String videoUuid);
    List<ImportTask> findByStatusNotOrderByCreatedAtDesc(String status);
    List<ImportTask> findByStatusNotInOrderByCreatedAtDesc(List<String> statuses);
    List<ImportTask> findAllByOrderByCreatedAtDesc();
}
