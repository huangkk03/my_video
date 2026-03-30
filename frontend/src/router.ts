import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/HomeView.vue') },
  { path: '/upload', name: 'upload', component: () => import('./views/UploadView.vue') },
  { path: '/player/:uuid', name: 'player', component: () => import('./views/PlayerView.vue') },
  { 
    path: '/admin', 
    name: 'admin', 
    component: () => import('./views/admin/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('./views/admin/Dashboard.vue') },
      { path: 'media', name: 'admin-media', component: () => import('./views/admin/MediaLibrary.vue') },
      { path: 'aliyun', name: 'admin-aliyun', component: () => import('./views/admin/AliyunDrive.vue') },
      { path: 'settings', name: 'admin-settings', component: () => import('./views/admin/Settings.vue') },
    ]
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})
