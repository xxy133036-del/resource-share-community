
import { createApp } from 'vue'
import App from './App.vue'
// 1. 引入我们刚刚写的路由配置文件
// 注意路径，如果你的路由文件放在 src/router/index.js，这里直接写 './router' 即可
import router from './router'

const app = createApp(App)

// 2. 告诉 Vue 实例去使用这个路由（非常重要！！漏掉就会报你的那个错）
app.use(router)

app.mount('#app')