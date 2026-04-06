import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

export interface Category {
  id: number
  name: string
  slug: string
  description: string
  parentId: number | null
  sortOrder: number
}

export const categoryApi = {
  getAll(): Promise<Category[]> {
    return api.get('/categories').then(res => res.data)
  },
  
  getById(id: number): Promise<Category> {
    return api.get(`/categories/${id}`).then(res => res.data)
  },
  
  getBySlug(slug: string): Promise<Category> {
    return api.get(`/categories/slug/${slug}`).then(res => res.data)
  },
}

export default api
