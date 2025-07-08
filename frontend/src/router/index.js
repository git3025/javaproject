import { createRouter, createWebHistory } from 'vue-router'

import Dashboard from '../views/Dashboard.vue'
import PdfList from '../views/PdfList.vue'
import PdfPages from '../views/PdfPages.vue'
import PdfSplit from '../views/PdfSplit.vue'
import Home from "../views/Home.vue"
import Placeholder1 from '../views/Placeholder1.vue'
import knowledgepage from '../views/knowledgepage.vue'
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
    path: '/knowledgepage',
    name: 'knowledgepage',
    component: knowledgepage
  },
  {
    path: '/generate-questions',
    name: 'GenerateQuestions',
    component: () => import('../views/GenerateQuestions.vue'),
    meta: {
      title: '自动生成题目合集'
    }
  },
  {
    path: '/question-detail',
    name: 'QuestionDetail',
    component: () => import('../views/QuestionDetail.vue'),
    meta: {
      title: '知识点题目详细'
    }
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