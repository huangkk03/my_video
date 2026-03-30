const { createRouter, createWebHistory } = VueRouter;

const routes = [
    {
        path: '/admin/dashboard',
        name: 'Dashboard',
        component: {
            template: `
                <div class="card">
                    <h3>控制台</h3>
                    <p>欢迎使用视频管理后台</p>
                </div>
            `
        }
    },
    {
        path: '/admin/videos',
        name: 'Videos',
        component: VideosComponent
    },
    {
        path: '/admin/cloud',
        name: 'Cloud',
        component: CloudStorageComponent
    },
    {
        path: '/admin/metadata',
        name: 'Metadata',
        component: MetadataComponent
    },
    {
        path: '/admin/settings',
        name: 'Settings',
        component: SettingsComponent
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});
