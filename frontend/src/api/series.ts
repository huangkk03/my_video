import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

export interface Season {
  id: number
  seriesId: number
  seasonNumber: number
  name: string
  posterPath: string
  overview: string
  tmdbId: number
  createdAt: string
  updatedAt: string
}

export interface Series {
  id: number
  name: string
  slug: string
  posterPath: string
  backdropPath: string
  overview: string
  tmdbId: number
  categoryId: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface SeriesDetail {
  series: Series
  seasons: Season[]
  videos: Video[]
  videoCount: number
  seasonCount: number
}

export interface BatchAssignRequest {
  videoUuids: string[]
  seriesId: number
  seasonId?: number
  episodeStart?: number
}

interface Video {
  uuid: string
  title: string
  originalFilename: string
  originalPath: string
  hlsPath: string
  thumbnailPath: string
  posterPath: string
  backdropPath: string
  duration: number
  fileSize: number
  width: number
  height: number
  status: string
  currentPosition: number
  seriesId: number
  seasonId: number
  episodeNumber: number
  createdAt: string
  updatedAt: string
}

export const seriesApi = {
  getAll(): Promise<Series[]> {
    return api.get('/series').then(res => res.data)
  },

  getPage(page = 0, size = 20): Promise<{ content: Series[]; totalElements: number }> {
    return api.get('/series/page', { params: { page, size } }).then(res => res.data)
  },

  getDetail(id: number): Promise<SeriesDetail> {
    return api.get(`/series/${id}`).then(res => res.data)
  },

  create(data: Partial<Series>): Promise<Series> {
    return api.post('/series', data).then(res => res.data)
  },

  update(id: number, data: Partial<Series>): Promise<Series> {
    return api.put(`/series/${id}`, data).then(res => res.data)
  },

  delete(id: number): Promise<void> {
    return api.delete(`/series/${id}`).then(res => res.data)
  },

  search(name: string): Promise<Series[]> {
    return api.get('/series/search', { params: { name } }).then(res => res.data)
  },

  rescrap(id: number): Promise<Series> {
    return api.post(`/series/${id}/rescrap`).then(res => res.data)
  },

  getSeasons(seriesId: number): Promise<Season[]> {
    return api.get(`/series/${seriesId}/seasons`).then(res => res.data)
  },

  createSeason(seriesId: number, data: Partial<Season>): Promise<Season> {
    return api.post(`/series/${seriesId}/seasons`, data).then(res => res.data)
  },

  updateSeason(id: number, data: Partial<Season>): Promise<Season> {
    return api.put(`/seasons/${id}`, data).then(res => res.data)
  },

  deleteSeason(id: number): Promise<void> {
    return api.delete(`/seasons/${id}`).then(res => res.data)
  },

  getSeasonDetail(id: number): Promise<{ season: Season; videos: Video[]; videoCount: number }> {
    return api.get(`/seasons/${id}`).then(res => res.data)
  },

  assignVideo(uuid: string, seasonId: number, episodeNumber?: number): Promise<void> {
    return api.put(`/videos/${uuid}/assign`, null, { 
      params: { seasonId, episodeNumber } 
    }).then(res => res.data)
  },

  unassignVideo(uuid: string): Promise<void> {
    return api.put(`/videos/${uuid}/unassign`).then(res => res.data)
  },

  getUnassigned(page = 0, size = 20): Promise<{ content: Video[]; totalElements: number }> {
    return api.get('/series/unassigned', { params: { page, size } }).then(res => res.data)
  },

  batchAssign(data: BatchAssignRequest): Promise<{ success: boolean; assignedCount: number; message?: string }> {
    return api.post('/series/batch-assign', data).then(res => res.data)
  },
}

export default api
