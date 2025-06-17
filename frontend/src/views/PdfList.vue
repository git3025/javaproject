<template>
  <div class="pdf-list-container">
    <el-card>
      <template #header>
        <div class="header">
          <h2>PDF 文档列表</h2>
          <el-upload
              class="upload-demo"
              :action="`${api.defaults.baseURL}/documents/upload`"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              :show-file-list="false"
              :data="{ title: '', description: '' }"
          >
            <el-button type="primary">上传 PDF</el-button>
          </el-upload>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="pdfList"
          style="width: 100%"
      >
        <el-table-column prop="originalName" label="文件名" />
        <el-table-column prop="fileSize" label="文件大小">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="pages" label="页数" />
        <el-table-column prop="uploadTime" label="上传时间">
          <template #default="{ row }">
            {{ formatDate(row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
                type="primary"
                link
                @click="viewPages(row.id)"
            >
              查看页面
            </el-button>
            <el-button
                type="danger"
                link
                @click="deletePdf(row.id)"
            >
              删除
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const pdfList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

const loadPdfList = async () => {
  try {
    loading.value = true
    const response = await api.get('/pdf', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    pdfList.value = response.content
    total.value = response.totalElements
  } catch (error) {
    console.error('Failed to load PDF list:', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadPdfList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadPdfList()
}

const beforeUpload = (file) => {
  const isPDF = file.type === 'application/pdf'
  if (!isPDF) {
    ElMessage.error('只能上传 PDF 文件！')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  ElMessage.success('上传成功')
  loadPdfList()
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}

const viewPages = (id) => {
  router.push(`/dashboard/pdfs/${id}/pages`)
}

const deletePdf = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个 PDF 文件吗？', '警告', {
      type: 'warning'
    })
    await api.delete(`/pdf/${id}`)
    ElMessage.success('删除成功')
    loadPdfList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete PDF:', error)
    }
  }
}

const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadPdfList()
})
</script>

<style scoped>
.pdf-list-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>