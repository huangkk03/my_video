const API_BASE = '/api';

function showPage(page, e) {
    console.log('showPage called:', page);
    if (e) {
        e.preventDefault();
        e.stopPropagation();
    }
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.sidebar-nav li').forEach(l => l.classList.remove('active'));
    document.getElementById('page-' + page).classList.add('active');
    console.log('Page switched to:', page);
    
    if (page === 'videos') fetchVideos();
    if (page === 'settings') {
        console.log('Loading settings...');
        loadSettings();
    }
}

async function fetchVideos() {
    const res = await fetch(`${API_BASE}/videos`);
    const data = await res.json();
    const tbody = document.getElementById('videoTable');
    tbody.innerHTML = data.content.map(v => `
        <tr>
            <td>${v.title}</td>
            <td>${formatSize(v.fileSize)}</td>
            <td><span class="status status-${v.status}">${getStatusText(v.status)}</span></td>
            <td>
                ${v.status === 'transcoding' ? `<div class="progress"><div class="progress-bar" style="width:50%"></div></div>` : '-'}
            </td>
            <td>
                <button onclick="deleteVideo('${v.uuid}')" style="background:#ef4444">删除</button>
            </td>
        </tr>
    `).join('');
}

async function deleteVideo(uuid) {
    if (!confirm('确定删除?')) return;
    await fetch(`${API_BASE}/videos/${uuid}`, { method: 'DELETE' });
    fetchVideos();
}

async function searchAliyunDrive() {
    const query = document.getElementById('searchInput').value;
    if (!query) return;
    
    document.getElementById('searchResults').innerHTML = '<p>搜索中...</p>';
    
    try {
        const res = await fetch(`${API_BASE}/videos/aliyun/search?query=${encodeURIComponent(query)}`);
        const files = await res.json();
        
        if (!files || files.length === 0) {
            document.getElementById('searchResults').innerHTML = '<p>未找到结果，请检查阿里云盘配置</p>';
            return;
        }
        
        document.getElementById('searchResults').innerHTML = files.map(r => `
            <div class="search-result">
                <span>${r.name} (${formatSize(r.size)})</span>
                <button onclick="downloadFromAliyun('${r.id}', '${r.name}')">下载</button>
            </div>
        `).join('');
    } catch (e) {
        document.getElementById('searchResults').innerHTML = '<p>搜索失败: ' + e.message + '</p>';
    }
}

async function downloadFromAliyun(fileId, fileName) {
    try {
        const res = await fetch(`${API_BASE}/videos/aliyun/download?fileId=${fileId}&title=${encodeURIComponent(fileName)}`, {
            method: 'POST'
        });
        const data = await res.json();
        alert('下载任务已创建，视频UUID: ' + data.uuid);
    } catch (e) {
        alert('下载失败: ' + e.message);
    }
}

async function searchMetadata() {
    const query = document.getElementById('metaSearchInput').value;
    if (!query) return;
    
    document.getElementById('metadataResults').innerHTML = '<p>正在搜索TMDB和豆瓣...</p>';
    
    try {
        const res = await fetch(`${API_BASE}/metadata/search?query=${encodeURIComponent(query)}`);
        const data = await res.json();
        
        if (!data || !data.title) {
            document.getElementById('metadataResults').innerHTML = '<p>未找到元数据，请检查TMDB配置</p>';
            return;
        }
        
        document.getElementById('metadataResults').innerHTML = `
            <div class="card">
                <h3>${data.title} ${data.releaseDate ? '(' + data.releaseDate.substring(0,4) + ')' : ''}</h3>
                ${data.posterUrl ? `<img src="${data.posterUrl}" style="max-width:200px;">` : ''}
                <p style="margin-top:10px;">评分: ${data.rating || 'N/A'}</p>
                <p style="margin-top:10px;color:#6b7280">${data.overview || '暂无简介'}</p>
            </div>
        `;
    } catch (e) {
        document.getElementById('metadataResults').innerHTML = '<p>搜索失败: ' + e.message + '</p>';
    }
}

async function loadSettings() {
    console.log('Loading settings...');
    try {
        const res = await fetch(`${API_BASE}/config`);
        if (!res.ok) throw new Error('Network error: ' + res.status);
        const configs = await res.json();
        console.log('Configs loaded:', configs);
        document.getElementById('aliyunToken').value = configs['aliyundrive.refresh_token'] || '';
        document.getElementById('aliyunFolderId').value = configs['aliyundrive.root_folder_id'] || 'root';
        document.getElementById('tmdbApiKey').value = configs['tmdb.api_key'] || '';
        document.getElementById('tmdbLanguage').value = configs['tmdb.language'] || 'zh-CN';
        console.log('Settings loaded into form');
    } catch (e) {
        console.error('Failed to load settings:', e);
        alert('加载失败: ' + e.message);
    }
}

async function saveSettings() {
    console.log('Saving settings...');
    const configs = {
        'aliyundrive.refresh_token': document.getElementById('aliyunToken').value,
        'aliyundrive.root_folder_id': document.getElementById('aliyunFolderId').value,
        'tmdb.api_key': document.getElementById('tmdbApiKey').value,
        'tmdb.language': document.getElementById('tmdbLanguage').value
    };
    console.log('Configs to save:', configs);
    
    try {
        const res = await fetch(`${API_BASE}/config`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(configs)
        });
        if (!res.ok) throw new Error('Network error: ' + res.status);
        const result = await res.json();
        console.log('Save result:', result);
        document.getElementById('saveStatus').textContent = '保存成功!';
        setTimeout(() => document.getElementById('saveStatus').textContent = '', 3000);
    } catch (e) {
        console.error('Save failed:', e);
        alert('保存失败: ' + e.message);
    }
}

function formatSize(bytes) {
    if (!bytes) return '0 B';
    const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];
    return parseFloat((bytes / Math.pow(k, Math.floor(bytes.toString().length / 3))).toFixed(1) + ' ' + sizes[Math.floor(Math.log(bytes) / Math.log(k))];
}

function getStatusText(status) {
    const map = { pending: '待处理', transcoding: '转码中', completed: '已完成', failed: '失败' };
    return map[status] || status;
}

document.addEventListener('DOMContentLoaded', function() {
    fetchVideos();
});
