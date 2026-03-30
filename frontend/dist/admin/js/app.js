const API_BASE = '/api';

const VideosComponent = {
    template: `
        <div class="card">
            <h3>视频列表</h3>
            <button @click="fetchVideos">🔄 刷新</button>
            <table style="margin-top: 15px;">
                <thead>
                    <tr>
                        <th>标题</th>
                        <th>大小</th>
                        <th>状态</th>
                        <th>进度</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="v in videos" :key="v.uuid">
                        <td>{{ v.title }}</td>
                        <td>{{ formatSize(v.fileSize) }}</td>
                        <td><span :class="'status status-' + v.status">{{ getStatusText(v.status) }}</span></td>
                        <td>
                            <div v-if="v.status === 'transcoding'" class="progress"><div class="progress-bar" style="width:50%"></div></div>
                            <span v-else>-</span>
                        </td>
                        <td><button @click="deleteVideo(v.uuid)" style="background:#ef4444">删除</button></td>
                    </tr>
                </tbody>
            </table>
        </div>
    `,
    data() {
        return { videos: [] }
    },
    mounted() {
        this.fetchVideos();
    },
    methods: {
        async fetchVideos() {
            const res = await fetch(API_BASE + '/videos');
            const data = await res.json();
            this.videos = data.content || [];
        },
        async deleteVideo(uuid) {
            if (!confirm('确定删除?')) return;
            await fetch(API_BASE + '/videos/' + uuid, { method: 'DELETE' });
            this.fetchVideos();
        },
        formatSize(bytes) {
            if (!bytes) return '0 B';
            const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];
            return parseFloat((bytes / Math.pow(k, Math.floor(bytes.toString().length / 3))).toFixed(1)) + ' ' + sizes[Math.floor(Math.log(bytes) / Math.log(k))];
        },
        getStatusText(status) {
            const map = { pending: '待处理', transcoding: '转码中', completed: '已完成', failed: '失败' };
            return map[status] || status;
        }
    }
};

const CloudStorageComponent = {
    template: `
        <div class="cloud-page">
            <div class="search-bar">
                <input type="text" v-model="searchQuery" placeholder="输入电影/视频关键词搜索..." class="search-input" @keyup.enter="search">
                <button @click="search" class="search-btn">🔍 搜索</button>
                <button @click="browseRoot" class="browse-btn">📁 浏览全部</button>
            </div>
            <div v-if="currentPath" class="path-bar">
                <span class="path-label">当前目录:</span>
                <span class="path-value">{{ currentPath }}</span>
                <button v-if="parentPath" @click="browse(parentPath)" class="back-btn">⬆️ 返回上级</button>
            </div>
            <div v-if="loading" class="loading">加载中...</div>
            <div v-else class="poster-wall">
                <div v-for="r in results" :key="r.name" class="poster-card" :class="{'is-folder': r.is_folder}" @click="handleClick(r)">
                    <div class="poster">
                        <img v-if="r.thumb" :src="r.thumb" class="poster-img" @error="imgError($event)">
                        <div v-else class="poster-placeholder">{{ r.is_folder ? '📁' : '🎬' }}</div>
                    </div>
                    <div class="poster-info">
                        <div class="poster-title" :title="r.name">{{ r.is_folder ? r.name : extractTitle(r.name) }}</div>
                        <div class="poster-size">{{ r.is_folder ? '文件夹' : formatSize(r.size) }}</div>
                        <button v-if="!r.is_folder" @click.stop="importVideo(r)" class="import-btn">📥 导入并转码</button>
                    </div>
                </div>
            </div>
            <p v-if="!loading && results.length === 0 && searched" class="no-result">未找到结果，请尝试其他关键词</p>
        </div>
    `,
    data() {
        return { 
            searchQuery: '', 
            results: [], 
            searched: false, 
            loading: false,
            currentPath: '',
            parentPath: ''
        }
    },
    mounted() {
        this.browseRoot();
    },
    methods: {
        async search() {
            if (!this.searchQuery) return;
            this.searched = true;
            this.loading = true;
            this.currentPath = '';
            this.parentPath = '';
            this.results = [];
            try {
                const res = await fetch(API_BASE + '/cloud/search?keyword=' + encodeURIComponent(this.searchQuery));
                const data = await res.json();
                this.results = Array.isArray(data) ? data.filter(f => !f.is_folder) : [];
            } catch (e) {
                this.results = [];
            }
            this.loading = false;
        },
        async browseRoot() {
            this.searchQuery = '';
            this.searched = true;
            await this.browse('/');
        },
        async browse(path) {
            this.loading = true;
            this.currentPath = path;
            this.parentPath = path === '/' ? null : path.split('/').slice(0, -1).join('/') || '/';
            this.results = [];
            try {
                const res = await fetch(API_BASE + '/cloud/files?path=' + encodeURIComponent(path));
                const data = await res.json();
                this.results = Array.isArray(data) ? data : [];
            } catch (e) {
                this.results = [];
            }
            this.loading = false;
        },
        handleClick(item) {
            if (item.is_folder) {
                const newPath = this.currentPath === '/' ? '/' + item.name : this.currentPath + '/' + item.name;
                this.browse(newPath);
            }
        },
        extractTitle(fileName) {
            return fileName.replace(/\.[^/.]+$/, '').replace(/[._]/g, ' ');
        },
        imgError(e) {
            e.target.style.display = 'none';
            e.target.nextElementSibling.style.display = 'flex';
        },
        async importVideo(file) {
            const filePath = this.currentPath === '/' ? file.name : this.currentPath + '/' + file.name;
            if (!confirm('确认导入 "' + file.name + '" 并转码?')) return;
            try {
                const res = await fetch(API_BASE + '/cloud/import?fileName=' + encodeURIComponent(file.name) + 
                    '&filePath=' + encodeURIComponent(filePath) + 
                    '&fileSize=' + file.size, { method: 'POST' });
                const data = await res.json();
                if (data.success) {
                    alert('导入任务已创建! Task ID: ' + data.taskId + '\n请在"转码队列"中查看进度');
                    this.$router.push('/admin/queue');
                } else {
                    alert('导入失败: ' + (data.message || '未知错误'));
                }
            } catch (e) {
                alert('导入失败: ' + e.message);
            }
        },
        formatSize(bytes) {
            if (!bytes) return '0 B';
            const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];
            return parseFloat((bytes / Math.pow(k, Math.floor(bytes.toString().length / 3))).toFixed(1)) + ' ' + sizes[Math.floor(Math.log(bytes) / Math.log(k))];
        }
    }
};

const QueueComponent = {
    template: `
        <div class="queue-page">
            <h3>转码队列</h3>
            <button @click="refreshTasks" style="margin-bottom: 15px;">🔄 刷新</button>
            <div class="queue-list">
                <div v-for="task in tasks" :key="task.taskId" class="queue-item">
                    <div class="queue-info">
                        <div class="queue-title">{{ task.sourceName }}</div>
                        <div class="queue-status">
                            <span :class="'status-badge status-' + task.status">{{ getStatusText(task.status) }}</span>
                            <span class="queue-message">{{ task.message }}</span>
                        </div>
                    </div>
                    <div class="queue-progress">
                        <div class="progress-bar">
                            <div class="progress-fill" :style="{width: task.progress + '%'}"></div>
                        </div>
                        <span class="progress-text">{{ task.progress }}%</span>
                    </div>
                    <div class="queue-time">{{ formatTime(task.createdAt) }}</div>
                </div>
                <p v-if="tasks.length === 0" class="no-result">暂无转码任务</p>
            </div>
        </div>
    `,
    data() {
        return { tasks: [], interval: null }
    },
    mounted() {
        this.refreshTasks();
        this.interval = setInterval(() => this.refreshTasks(), 3000);
    },
    beforeUnmount() {
        if (this.interval) clearInterval(this.interval);
    },
    methods: {
        async refreshTasks() {
            try {
                const res = await fetch(API_BASE + '/cloud/tasks');
                this.tasks = await res.json();
            } catch (e) {
                console.error('Failed to load tasks:', e);
            }
        },
        getStatusText(status) {
            const map = {
                'pending': '等待中',
                'downloading': '下载中',
                'scraping': '刮削中',
                'transcoding': '转码中',
                'completed': '已完成',
                'failed': '失败'
            };
            return map[status] || status;
        },
        formatTime(timestamp) {
            if (!timestamp) return '';
            const date = new Date(timestamp);
            return date.toLocaleString('zh-CN');
        }
    }
};

const MetadataComponent = {
    template: `
        <div class="card">
            <h3>元数据聚合</h3>
            <input type="text" v-model="searchQuery" placeholder="输入视频标题搜索...">
            <button @click="search">🔍 搜索</button>
            <div style="margin-top: 15px;">
                <div v-if="result" class="card">
                    <h3>{{ result.title }} {{ result.releaseDate ? '(' + result.releaseDate.substring(0,4) + ')' : '' }}</h3>
                    <img v-if="result.posterUrl" :src="result.posterUrl" style="max-width:200px;">
                    <p style="margin-top:10px;">评分: ⭐ {{ result.rating || 'N/A' }}</p>
                    <p style="margin-top:10px;color:#6b7280">{{ result.overview || '暂无简介' }}</p>
                </div>
                <p v-else-if="searched">未找到元数据</p>
            </div>
        </div>
    `,
    data() {
        return { searchQuery: '', result: null, searched: false }
    },
    methods: {
        async search() {
            if (!this.searchQuery) return;
            this.searched = true;
            try {
                const res = await fetch(API_BASE + '/metadata/search?query=' + encodeURIComponent(this.searchQuery));
                this.result = await res.json();
            } catch (e) {
                this.result = null;
            }
        }
    }
};

const SettingsComponent = {
    template: `
        <div class="card">
            <h3>系统设置</h3>
            <div style="margin-bottom: 15px;">
                <label>AList 存储密码:</label><br>
                <input type="text" v-model="configs['alist.storage_password']" style="width: 100%; margin-top: 5px;" placeholder="输入AList存储密码">
            </div>
            <div style="margin-bottom: 15px;">
                <label>TMDB API Key:</label><br>
                <input type="text" v-model="configs['tmdb.api_key']" style="width: 100%; margin-top: 5px;" placeholder="输入TMDB API Key">
            </div>
            <div style="margin-bottom: 15px;">
                <label>TMDB 语言:</label><br>
                <select v-model="configs['tmdb.language']" style="width: 100%; margin-top: 5px;">
                    <option value="zh-CN">中文</option>
                    <option value="en-US">English</option>
                </select>
            </div>
            <button @click="saveSettings">💾 保存设置</button>
            <button @click="testCloud" style="margin-left: 10px;">🔗 测试云盘连接</button>
            <span v-if="saveStatus" style="margin-left: 10px; color: #10b981;">{{ saveStatus }}</span>
        </div>
        <div v-if="cloudTestResult" class="card">
            <h3>云盘测试结果</h3>
            <p v-if="cloudTestResult.success" style="color: #10b981;">✅ {{ cloudTestResult.message }}</p>
            <p v-else style="color: #ef4444;">❌ {{ cloudTestResult.message }}</p>
        </div>
        <div class="card">
            <h3>其他设置</h3>
            <p>FFmpeg路径: <input type="text" value="/usr/bin/ffmpeg" disabled></p>
            <p>视频存储路径: <input type="text" value="/data/videos" disabled></p>
            <p>最大文件大小: 500MB</p>
        </div>
    `,
    data() {
        return {
            configs: {
                'alist.storage_password': '',
                'tmdb.api_key': '',
                'tmdb.language': 'zh-CN'
            },
            saveStatus: '',
            cloudTestResult: null
        }
    },
    mounted() {
        this.loadSettings();
    },
    methods: {
        async loadSettings() {
            try {
                const res = await fetch(API_BASE + '/config');
                const data = await res.json();
                this.configs = { ...this.configs, ...data };
            } catch (e) {
                console.error('Failed to load settings:', e);
            }
        },
        async saveSettings() {
            try {
                await fetch(API_BASE + '/config', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(this.configs)
                });
                this.saveStatus = '保存成功!';
                setTimeout(() => this.saveStatus = '', 3000);
            } catch (e) {
                alert('保存失败: ' + e.message);
            }
        },
        async testCloud() {
            try {
                const res = await fetch(API_BASE + '/cloud/test');
                this.cloudTestResult = await res.json();
            } catch (e) {
                this.cloudTestResult = { success: false, message: e.message };
            }
        },
        formatSize(bytes) {
            if (!bytes) return '0 B';
            const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];
            return parseFloat((bytes / Math.pow(k, Math.floor(bytes.toString().length / 3))).toFixed(1)) + ' ' + sizes[Math.floor(Math.log(bytes) / Math.log(k))];
        }
    }
};

const DashboardComponent = {
    template: `
        <div class="dashboard">
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-icon">📊</div>
                    <div class="stat-info">
                        <div class="stat-value">{{ stats.totalVideos }}</div>
                        <div class="stat-label">视频总数</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">⏳</div>
                    <div class="stat-info">
                        <div class="stat-value">{{ stats.activeTasks }}</div>
                        <div class="stat-label">转码中</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">✅</div>
                    <div class="stat-info">
                        <div class="stat-value">{{ stats.completedTasks }}</div>
                        <div class="stat-label">已完成</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">❌</div>
                    <div class="stat-info">
                        <div class="stat-value">{{ stats.failedTasks }}</div>
                        <div class="stat-label">失败</div>
                    </div>
                </div>
            </div>
            <div class="card" style="margin-top: 20px;">
                <h3>系统状态</h3>
                <div class="status-grid">
                    <div class="status-item">
                        <span class="status-dot" :class="systemStatus.alist ? 'online' : 'offline'"></span>
                        <span>AList 云盘</span>
                        <span class="status-text">{{ systemStatus.alist ? '已连接' : '未连接' }}</span>
                    </div>
                    <div class="status-item">
                        <span class="status-dot online"></span>
                        <span>数据库</span>
                        <span class="status-text">正常</span>
                    </div>
                </div>
            </div>
            <div class="card" style="margin-top: 20px;">
                <h3>最近导入任务</h3>
                <div class="recent-tasks">
                    <div v-for="task in recentTasks" :key="task.taskId" class="recent-task">
                        <span class="task-name">{{ task.sourceName }}</span>
                        <span :class="'status-badge status-' + task.status">{{ getStatusText(task.status) }}</span>
                    </div>
                    <p v-if="recentTasks.length === 0" class="no-result">暂无导入任务</p>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            stats: { totalVideos: 0, activeTasks: 0, completedTasks: 0, failedTasks: 0 },
            systemStatus: { alist: false },
            recentTasks: [],
            interval: null
        }
    },
    mounted() {
        this.loadDashboard();
        this.interval = setInterval(() => this.loadDashboard(), 10000);
    },
    beforeUnmount() {
        if (this.interval) clearInterval(this.interval);
    },
    methods: {
        async loadDashboard() {
            try {
                const [videoRes, taskRes, statusRes] = await Promise.all([
                    fetch(API_BASE + '/videos'),
                    fetch(API_BASE + '/cloud/tasks'),
                    fetch(API_BASE + '/cloud/test')
                ]);
                const videos = await videoRes.json();
                const tasks = await taskRes.json();
                const status = await statusRes.json();
                
                this.stats.totalVideos = videos.content?.length || 0;
                this.stats.activeTasks = tasks.filter(t => t.status === 'transcoding' || t.status === 'downloading' || t.status === 'scraping').length;
                this.stats.completedTasks = tasks.filter(t => t.status === 'completed').length;
                this.stats.failedTasks = tasks.filter(t => t.status === 'failed').length;
                
                this.systemStatus.alist = status.success || false;
                this.recentTasks = tasks.slice(0, 5);
            } catch (e) {
                console.error('Failed to load dashboard:', e);
            }
        },
        getStatusText(status) {
            const map = {
                'pending': '等待', 'downloading': '下载', 'scraping': '刮削', 
                'transcoding': '转码', 'completed': '完成', 'failed': '失败'
            };
            return map[status] || status;
        }
    }
};

const { createRouter, createWebHistory } = VueRouter;

const routes = [
    { path: '/admin/dashboard', name: 'Dashboard', component: DashboardComponent },
    { path: '/admin/videos', name: 'Videos', component: VideosComponent },
    { path: '/admin/cloud', name: 'Cloud', component: CloudStorageComponent },
    { path: '/admin/queue', name: 'Queue', component: QueueComponent },
    { path: '/admin/metadata', name: 'Metadata', component: MetadataComponent },
    { path: '/admin/settings', name: 'Settings', component: SettingsComponent }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

const app = Vue.createApp({});
app.use(router);
app.mount('#app');
