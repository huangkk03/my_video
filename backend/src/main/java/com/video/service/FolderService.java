package com.video.service;

import com.video.entity.Folder;
import com.video.entity.Video;
import com.video.repository.FolderRepository;
import com.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FolderService {
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private VideoRepository videoRepository;
    
    public static class FolderTreeNode {
        private Long id;
        private String name;
        private Long parentId;
        private List<FolderTreeNode> children = new ArrayList<>();
        private int videoCount;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public List<FolderTreeNode> getChildren() { return children; }
        public void setChildren(List<FolderTreeNode> children) { this.children = children; }
        public int getVideoCount() { return videoCount; }
        public void setVideoCount(int videoCount) { this.videoCount = videoCount; }
    }
    
    public List<FolderTreeNode> getFolderTree() {
        List<Folder> allFolders = folderRepository.findAllByOrderByNameAsc();
        Map<Long, FolderTreeNode> nodeMap = new HashMap<>();
        
        for (Folder folder : allFolders) {
            FolderTreeNode node = new FolderTreeNode();
            node.setId(folder.getId());
            node.setName(folder.getName());
            node.setParentId(folder.getParentId());
            node.setVideoCount(videoRepository.countByFolderId(folder.getId()));
            nodeMap.put(folder.getId(), node);
        }
        
        List<FolderTreeNode> rootNodes = new ArrayList<>();
        for (Folder folder : allFolders) {
            FolderTreeNode node = nodeMap.get(folder.getId());
            if (folder.getParentId() == null) {
                rootNodes.add(node);
            } else {
                FolderTreeNode parent = nodeMap.get(folder.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    rootNodes.add(node);
                }
            }
        }
        
        return rootNodes;
    }
    
    public Folder getFolderById(Long id) {
        return folderRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public Folder createFolder(String name, Long parentId) {
        if (parentId != null && !folderRepository.existsById(parentId)) {
            throw new RuntimeException("父文件夹不存在");
        }
        
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setSortOrder(0);
        
        return folderRepository.save(folder);
    }
    
    @Transactional
    public Folder updateFolder(Long id, String name) {
        Folder folder = folderRepository.findById(id).orElse(null);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        
        folder.setName(name);
        return folderRepository.save(folder);
    }
    
    @Transactional
    public void deleteFolder(Long id) {
        if (!folderRepository.existsById(id)) {
            throw new RuntimeException("文件夹不存在");
        }
        
        List<Video> videos = videoRepository.findByFolderId(id);
        for (Video video : videos) {
            video.setFolderId(null);
            videoRepository.save(video);
        }
        
        folderRepository.deleteById(id);
    }
    
    public Page<Video> getVideosInFolder(Long folderId, Pageable pageable) {
        if (folderId == null) {
            return videoRepository.findByFolderIdIsNull(pageable);
        }
        return videoRepository.findByFolderId(folderId, pageable);
    }
    
    public int getUngroupedVideoCount() {
        return videoRepository.countByFolderIdIsNull();
    }
    
    @Transactional
    public Video moveVideoToFolder(String videoUuid, Long folderId) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }
        
        if (folderId != null && !folderRepository.existsById(folderId)) {
            throw new RuntimeException("目标文件夹不存在");
        }
        
        video.setFolderId(folderId);
        return videoRepository.save(video);
    }
    
    @Transactional
    public int batchMoveToFolder(List<String> videoUuids, Long folderId) {
        int count = 0;
        for (String uuid : videoUuids) {
            try {
                moveVideoToFolder(uuid, folderId);
                count++;
            } catch (Exception e) {
            }
        }
        return count;
    }
}