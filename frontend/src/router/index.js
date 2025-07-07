import { createRouter, createWebHistory } from 'vue-router'

import Dashboard from '../views/Dashboard.vue'
import PdfList from '../views/PdfList.vue'
import PdfPages from '../views/PdfPages.vue'
import PdfSplit from '../views/PdfSplit.vue'
import Home from "../views/Home.vue"
import Placeholder1 from '../views/Placeholder1.vue'
import QuestionPreview from '../views/QuestionPreview.vue'

const routes = [
  {
    path: '/',
    redirect: '/home'  // 将根路径重定向到首页
  },
  {
    path: '/home',
    name: 'Home',
    component:Home
  },
  {
    path: '/placeholder1',
    name: 'Placeholder1',
    component: Placeholder1
  },
  {
    path: '/question-preview',
    component: () => import('../components/NoSidebar.vue'),
    children: [
      {
        path: '',
        component: () => import('../views/QuestionPreview.vue')
      }
    ]
  },

  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    children: [
      {
        path: 'pdfs',
        name: 'PdfList',
        component: PdfList
      },
      {
        path: 'pdfs/:id/pages',
        name: 'PdfPages',
        component: PdfPages
      },
      {
        path: 'pdfs/:id/split',
        name: 'PdfSplit',
        component: PdfSplit
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


export default router 