<template>
  <div class="placeholder-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>PDF 文档管理 - 检测配置</h2>
        </div>
      </template>

      <div class="card-content">
        <div class="document-list">
          <h3>书籍图片列表</h3>
          <div class="action-buttons" style="margin: 20px 5px;">
            <el-button type="primary" @click="navigateToQuestionPage">切题</el-button>
          </div>
          <!-- 表格 -->
          <table class="document-table">
            <thead>
            <tr>
              <th><input type="checkbox" v-model="isAllSelected" @change="toggleAllSelection" /></th>
              <th>书籍ID</th>
              <th>ISBN码</th>
              <th>书籍名称</th>
              <th>学科名称</th>
              <th>书籍页码</th>
              <th>书籍路径</th>
              <th>是否调用目标检测</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="doc in pagedDocuments" :key="doc.id" :class="{ 'selected-row': doc.selected }">
              <td>
                <input type="checkbox" v-model="doc.selected" @change="toggleRowSelection(doc)" />
              </td>
              <td>{{ doc.id }}</td>
              <td>{{ doc.isbn }}</td>
              <td>{{ doc.book_name }}</td>
              <td>{{ doc.subject }}</td>
              <td>{{ doc.book_page }}</td>
              <td>
                <img :src="getImageUrl(doc.id)" style="width: 100px;" @error="handleImageError" />
              </td>
              <td :data-status="doc.object_detection">
                {{ doc.object_detection === 1 ? '未调用' : '已调用' }}
              </td>
              <td>
                <el-button type="primary" size="small" @click="showQuestions(doc)">查看题目</el-button>
              </td>
            </tr>
            </tbody>
          </table>
          <!-- 分页组件 -->
          <div class="pagination" style="margin-top: 20px; display: flex; justify-content: flex-end;">
            <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next, jumper, ->, total"
                :page-sizes="[15]"
                :pager-count="11"
                @current-change="handleCurrentChange"
                background
            />
          </div>
        </div>
      </div>
    </el-card>
  </div>
  <!-- 查看题目弹窗 -->
  <el-dialog
      title="题目列表"
      v-model="questionDialogVisible"
      width="80%"
      :before-close="handleClose">
    <div class="questions-container-vertical">
      <div v-for="(question, index) in questionList" :key="index" class="question-item-horizontal">
        <img :src="getQuestionImageUrl(question.path)" alt="题目图片" class="question-image" />
        <div class="question-info-right">
          <div class="question-number">题号: {{ question.question_number }}</div>
        </div>
      </div>
    </div>
    <template #footer>
    <span class="dialog-footer">
      <el-button @click="questionDialogVisible = false">关闭</el-button>
    </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { getImageUrl } from '../api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus';

// 弹窗控制
const questionDialogVisible = ref(false)
const questionList = ref([])

const router = useRouter()
const selectedDocuments = ref([])
const documents = ref([])
const isAllSelected = ref(false)
const isDrawingEnabled = ref(false)

// 存储每个图片对应的 DOM 引用
const imageWrappers = ref({})
const selectionBoxes = ref({})

// 存储已完成的框选痕迹
const selections = ref({})

let drawing = ref(false)
let startX = ref(0)
let startY = ref(0)

const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

const pagedDocuments = computed(() => {
  total.value = documents.value.length;
  const start = (currentPage.value - 1) * pageSize.value;
  return documents.value.slice(start, start + pageSize.value);
});

const handleCurrentChange = (val) => {
  currentPage.value = val;
};

// 获取所有分页数据
const fetchDocuments = async () => {
  try {
    const res = await api.get('/pdf-pages/list')
    documents.value = res.data.map(doc => ({
      ...doc,
      bookPage: extractPageFromPath(doc.book_path), // 提取页码
      selected: false, // 添加 selected 字段
    }))
  } catch (err) {
    console.error('获取文档失败', err)
  }
}

// 提取文件名中的页码（如 page1.png -> page1）
const extractPageFromPath = (path) => {
  if (!path) return ''
  const fileName = path.split('\\').pop()
  return fileName ? fileName.replace(/\.\w+$/, '') : ''
}

const handleImageError = (event) => {
  console.error('图片加载失败:', event.target.src);
  event.target.src = '/images/fallback.png';
};

// 设置图片容器引用
const setImageWrapperRef = (el, id) => {
  if (el) imageWrappers.value[id] = el
}

// 设置矩形框引用
const setSelectionBoxRef = (el, id) => {
  if (el) selectionBoxes.value[id] = el
}

// 开启或关闭框选模式
const toggleDrawingMode = () => {
  isDrawingEnabled.value = true
}

// 开始绘制矩形
const startDrawing = (id) => {
  if (!isDrawingEnabled.value) return

  const wrapper = imageWrappers.value[id]
  if (!wrapper) return

  const img = wrapper.querySelector('img')
  const rect = img.getBoundingClientRect()

  drawing.value = true
  startX.value = event.clientX - rect.left
  startY.value = event.clientY - rect.top
}

// 绘制矩形过程
const drawBox = (id) => {
  if (!drawing.value) return

  const wrapper = imageWrappers.value[id]
  const box = selectionBoxes.value[id]
  if (!wrapper || !box) return

  const img = wrapper.querySelector('img')
  const imgRect = img.getBoundingClientRect()

  const offsetX = event.clientX - imgRect.left
  const offsetY = event.clientY - imgRect.top

  const width = offsetX - startX.value
  const height = offsetY - startY.value

  box.style.left = `${Math.min(startX.value, offsetX)}px`
  box.style.top = `${Math.min(startY.value, offsetY)}px`
  box.style.width = `${Math.abs(width)}px`
  box.style.height = `${Math.abs(height)}px`
  box.style.display = 'block'
}

// 结束绘制并保存痕迹
const endDrawing = (id) => {
  if (!isDrawingEnabled.value) return

  const box = selectionBoxes.value[id]
  if (!box) return

  const x = parseInt(box.style.left)
  const y = parseInt(box.style.top)
  const width = parseInt(box.style.width)
  const height = parseInt(box.style.height)

  if (!selections.value[id]) {
    selections.value[id] = []
  }

  selections.value[id].push({ x, y, width, height })
  box.style.display = 'none'

  isDrawingEnabled.value = false // 关闭当前框选模式
}

// 切换全选状态
const toggleAllSelection = () => {
  documents.value.forEach(doc => {
    doc.selected = isAllSelected.value
  })
  selectedDocuments.value = isAllSelected.value ? [...documents.value] : []
}

// 监听单个文档的选中状态变化
const toggleRowSelection = (doc) => {
  const index = selectedDocuments.value.findIndex(d => d.id === doc.id)
  if (doc.selected && index === -1) {
    selectedDocuments.value.push(doc)
  } else if (!doc.selected && index > -1) {
    selectedDocuments.value.splice(index, 1)
  }

  // 更新全选状态
  isAllSelected.value = selectedDocuments.value.length === documents.value.length
}

const navigateToQuestionPage = () => {
  console.log("============================================")
  const doc = selectedDocuments
  console.log("doc:", doc)

  if (selectedDocuments.value.length === 0) {
    alert('请至少选择一项')
    return
  }

  // 提取选中的文档 id 作为参数
  const selectedIds = selectedDocuments.value.map(doc => doc.id).join(',')

  // 跳转到新的页面，并携带选中项的 id 参数
  router.push({
    path: '/question-preview',
    query: {ids: selectedIds}
  })
}

// 查看题目方法
const showQuestions = async (doc) => {
  try {
    // 提取数字部分
    const pageNumber = doc.book_page.replace(/[^\d]/g, '');
    const response = await fetch(`http://localhost:8080/api/pdf-pages/by-page?isbn=${doc.isbn}&page=${pageNumber}`)
    if (!response.ok) {
      throw new Error('获取题目失败')
    }
    const data = await response.json();

    // 确保按 question_number 排序（即使后端已排序，前端也可再次确认）
    data.sort((a, b) => a.question_number - b.question_number);

    questionList.value = data
    questionDialogVisible.value = true
  } catch (error) {
    console.error('获取题目失败:', error)
    ElMessage.error('获取题目失败')
  }
}

// 获取题目图片URL
const getQuestionImageUrl = (path) => {
  return `http://localhost:8080/api/pdf-pages/question-image?path=${encodeURIComponent(path)}`
}

// 关闭弹窗
const handleClose = () => {
  questionDialogVisible.value = false
  questionList.value = []
}

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  const fixedStr = dateStr.replace(' ', 'T');
  const date = new Date(fixedStr);
  if (isNaN(date.getTime())) return 'Invalid Date';
  return date.toLocaleString();
};

onMounted(() => {
  fetchDocuments()
})
</script>

<style scoped>
.placeholder-container {
  padding: 60px;
  width: 1650px;
  height: 100%;
  background-color: #eaeff6;
  display: flex;
  flex-direction: column;
}

.welcome-card {
  width: 100%; /* 占据大部分视口宽度 */
  max-width: 1400px; /* 最大宽度限制 */
  margin: 0 auto; /* 自动左右边距实现居中 */
  background-color: #fff;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.card-header {
  text-align: center;
  background-color: #84aee8;
  padding: 10px;
}

.card-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.document-table {
  width: 100%;
  border-collapse: collapse;
}

.document-table th,
.document-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: center;
}

.document-table th {
  background-color: #4a90e2;
  color: #fff;
}

.document-table tr.selected-row {
  background-color: #d0eaff;
}

/* 图片框选样式 */
.image-wrapper {
  position: relative;
  display: inline-block;
  cursor: crosshair;
}

.selection-box {
  position: absolute;
  border: 2px solid red; /* 仅红色边框 */
  pointer-events: none;
  top: 0;
  left: 0;
  display: none;
  z-index: 10;
}

.saved-selection {
  position: absolute;
  border: 2px solid red;
  pointer-events: none;
  top: 0;
  left: 0;
  z-index: 5;
}

.questions-container-vertical {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
}

.question-item-horizontal {
  display: flex;
  align-items: center;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.question-image {
  max-width: 300px;
  height: auto;
  margin-right: 20px;
  border-radius: 4px;
}

.question-info-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-number {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0;
  padding: 8px 12px;
  background: #f0f8ff;
  border-radius: 4px;
  border-left: 4px solid #4a90e2;
}
</style>