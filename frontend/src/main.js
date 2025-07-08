import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router/index'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs' // 特别注意这个路径

const app = createApp(App)


for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus, {
  locale: {
    ...zhCn,
    el: {
      ...zhCn.el,
      pagination: {
        ...zhCn.el.pagination,
        goto: '跳至',
        total: '共 {total} 条',
        pageClassifier: '页'
      }
    }
  }
})
app.mount('#app')