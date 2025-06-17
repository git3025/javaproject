<template>
  <div class="pdf-pages-container">
    <el-card>
      <template #header>
        <div class="header">
          <div class="left">
            <el-button @click="$router.push('/dashboard/pdfs')">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <h2>{{ pdfInfo.originalName }}</h2>
          </div>
          <div class="right">
            <el-button type="primary" @click="handleMerge">
              合并页面
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="pageList"
        style="width: 100%"
      >
        <el-table-column prop="pageNumber" label="页码" width="80" />
        <el-table-column prop="pageName" label="页面名称" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="processTime" label="处理时间" width="180">
          <template #default="{ row }">
            {{ row.processTime ? formatDate(row.processTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :disabled="row.status === 'PROCESSING'"
              @click="processPage(row.id)"
            >
              处理
            </el-button>
            <el-button
              type="primary"
              link
              @click="editPage(row.id)"
            >
              编辑
            </el-button>
            <el-button
              type="primary"
              link
              @click="previewPage(row)"
            >
              预览
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="页面预览"
      width="80%"
      :close-on-click-modal="false"
    >
      <div class="preview-container">
        <img
          v-if="currentPreview"
          :src="`${api.defaults.baseURL}/images/${currentPreview.imagePath}`"
          alt="页面预览"
          class="preview-image"
        />
      </div>
    </el-dialog>

    <!-- 合并对话框 -->
    <el-dialog
      v-model="mergeVisible"
      title="合并页面"
      width="30%"
    >
      <el-form :model="mergeForm" label-width="100px">
        <el-form-item label="排序方式">
          <el-radio-group v-model="mergeForm.orderType">
            <el-radio :label="1">按页码排序</el-radio>
            <el-radio :label="2">按处理时间排序</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="mergeVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmMerge">
            确认合并
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const pageList = ref([])
const pdfInfo = ref({})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const previewVisible = ref(false)
const currentPreview = ref(null)
const mergeVisible = ref(false)
const mergeForm = ref({
  orderType: 1
})

const loadPdfInfo = async () => {
  try {
    const response = await api.get(`/pdfs/${route.params.id}`)
    pdfInfo.value = response
  } catch (error) {
    console.error('Failed to load PDF info:', error)
  }
}

const loadPageList = async () => {
  try {
    loading.value = true
    const response = await api.get(`/pdfs/${route.params.id}/pages`, {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    pageList.value = response.content
    total.value = response.totalElements
  } catch (error) {
    console.error('Failed to load page list:', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadPageList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadPageList()
}

const getStatusType = (status) => {
  const types = {
    UNPROCESSED: 'info',
    PROCESSING: 'warning',
    PROCESSED: 'success',
    FAILED: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    UNPROCESSED: '未处理',
    PROCESSING: '处理中',
    PROCESSED: '已处理',
    FAILED: '失败'
  }
  return texts[status] || status
}

const processPage = async (id) => {
  try {
    await api.post(`/pdfs/pages/${id}/process`)
    ElMessage.success('开始处理页面')
    loadPageList()
  } catch (error) {
    console.error('Failed to process page:', error)
  }
}

const editPage = (id) => {
  router.push(`/dashboard/pages/${id}/edit`)
}

const previewPage = (page) => {
  currentPreview.value = page
  previewVisible.value = true
}

const handleMerge = () => {
  mergeVisible.value = true
}

const confirmMerge = async () => {
  try {
    await api.post(`/pdfs/${route.params.id}/merge`, mergeForm.value)
    ElMessage.success('开始合并页面')
    mergeVisible.value = false
  } catch (error) {
    console.error('Failed to merge pages:', error)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadPdfInfo()
  loadPageList()
})
</script>

<style scoped>
.pdf-pages-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header .left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header h2 {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.preview-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}
</style> 