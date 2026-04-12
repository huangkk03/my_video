package com.video.repository;

import com.video.entity.DownloadQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadQueueRepository extends JpaRepository<DownloadQueue, Long> {
    Optional<DownloadQueue> findByTaskId(String taskId);

    Optional<DownloadQueue> findByVideoUuid(String videoUuid);

    List<DownloadQueue> findByStatusOrderByPriorityAscCreatedAtAsc(String status);

    List<DownloadQueue> findByStatusOrderByPriorityAscCreatedAtAsc(String status, Pageable pageable);

    List<DownloadQueue> findByStatusInOrderByPriorityAscCreatedAtAsc(List<String> statuses);

    long countByStatus(String status);

    long countByStatusIn(List<String> statuses);
}
