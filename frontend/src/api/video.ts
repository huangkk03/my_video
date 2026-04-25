import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

export interface Video {
  uuid: string
  title: string
  originalFilename: string
  originalPath: string
  hlsPath: string
  thumbnailPath: string
  duration: number
  fileSize: number
  width: number
  height: number
  status: string
  currentPosition: number
  createdAt: string
  updatedAt: string
  posterPath?: string
  backdropPath?: string
  overview?: string
  rating?: number
  releaseYear?: number
  genres?: string
  actors?: string
  director?: string
  actorListJson?: string
  seriesId?: number
  seasonId?: number
  episodeNumber?: number
}

export interface VideoUploadResponse {
  uuid: string
  status: string
  message: string
}

export interface Subtitle {
  language: string
  label: string
  status: 'pending' | 'searching' | 'downloaded' | 'failed' | 'not_found'
  fileName?: string
  fileSize?: number
  hasFile: boolean
}

export interface SubtitlesResponse {
  videoUuid: string
  subtitles: Subtitle[]
  total: number
  hasSubtitles: boolean
}

export interface SubtitleStatusResponse {
  videoUuid: string
  hasSubtitles: boolean
  languages: Subtitle[]
}

export const videoApi = {
  upload(file: File, title?: string): Promise<VideoUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return api.post('/videos/upload', formData).then(res => res.data)
  },

  getList(page = 0, size = 20): Promise<{ content: Video[]; totalElements: number }> {
    return api.get('/videos', { params: { page, size } }).then(res => res.data)
  },

  getByUuid(uuid: string): Promise<Video> {
    return api.get(`/videos/${uuid}`).then(res => res.data)
  },

  getStreamUrl(uuid: string): string {
    return `/api/videos/${uuid}/stream`
  },

  updateProgress(uuid: string, position: number): Promise<void> {
    return api.put(`/videos/${uuid}/progress`, { position }).then(res => res.data)
  },

  delete(uuid: string): Promise<void> {
    return api.delete(`/videos/${uuid}`).then(res => res.data)
  },

  getSubtitles(uuid: string): Promise<SubtitlesResponse> {
    return api.get(`/videos/${uuid}/subtitles`).then(res => res.data)
  },

  searchSubtitles(uuid: string): Promise<{ status: string; message: string }> {
    return api.post(`/videos/${uuid}/subtitles/search`).then(res => res.data)
  },

  searchSubtitle(uuid: string, language: string): Promise<{ status: string; message: string }> {
    return api.post(`/videos/${uuid}/subtitles/${language}/search`).then(res => res.data)
  },

  deleteSubtitle(uuid: string, language: string): Promise<{ status: string; message: string }> {
    return api.delete(`/videos/${uuid}/subtitles/${language}`).then(res => res.data)
  },

  getSubtitleStatus(uuid: string): Promise<SubtitleStatusResponse> {
    return api.get(`/videos/${uuid}/subtitles/status`).then(res => res.data)
  },

  getSubtitleUrl(uuid: string, lang: string): string {
    return `/api/subtitles/${uuid}/${lang}.vtt`
  },
}

export default api