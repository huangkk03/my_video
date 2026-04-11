import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

export interface FolderTreeNode {
  id: number
  name: string
  parentId: number | null
  children: FolderTreeNode[]
  videoCount: number
}

export interface Folder {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface VideoPageResult {
  content: any[]
  totalElements: number
  totalPages: number
  currentPage: number
}

export const folderApi = {
  getTree(): Promise<FolderTreeNode[]> {
    return api.get('/folders').then(res => res.data)
  },

  getById(id: number): Promise<Folder> {
    return api.get(`/folders/${id}`).then(res => res.data)
  },

  getVideos(id: number, page = 0, size = 20): Promise<VideoPageResult> {
    return api.get(`/folders/${id}/videos`, { params: { page, size } }).then(res => res.data)
  },

  getUngroupedVideos(page = 0, size = 20): Promise<VideoPageResult> {
    return api.get('/folders/ungrouped/videos', { params: { page, size } }).then(res => res.data)
  },

  getUngroupedCount(): Promise<{ count: number }> {
    return api.get('/folders/ungrouped/count').then(res => res.data)
  },

  create(name: string, parentId?: number): Promise<Folder> {
    return api.post('/folders', { name, parentId }).then(res => res.data)
  },

  update(id: number, name: string): Promise<Folder> {
    return api.put(`/folders/${id}`, { name }).then(res => res.data)
  },

  delete(id: number): Promise<void> {
    return api.delete(`/folders/${id}`).then(res => res.data)
  },

  moveVideoToFolder(uuid: string, folderId: number | null): Promise<any> {
    return api.put(`/videos/${uuid}/folder`, { folderId }).then(res => res.data)
  },

  batchMoveToFolder(uuids: string[], folderId: number | null): Promise<{ success: boolean; movedCount: number }> {
    return api.post('/videos/batch-move', { uuids, folderId }).then(res => res.data)
  },
}

export default api