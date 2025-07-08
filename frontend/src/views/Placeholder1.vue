<template>
  <div class="placeholder-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>PDF 文档管理 - 检测配置</h2>
        </div>
      </template>
      <div class="filter-section">
        <el-form :model="filters" label-width="100px" size="small">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="ISBN码">
                <el-input v-model="filters.isbn" placeholder="请输入ISBN码" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="书籍名称">
                <el-input v-model="filters.book_name" placeholder="请输入书籍名称" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="学科名称">
                <el-input v-model="filters.subject" placeholder="请输入学科名称" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="年级">
                <el-input v-model="filters.grade" placeholder="请输入年级" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="书籍页码">
                <el-input v-model="filters.book_page" placeholder="请输入书籍页码" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="是否调用检测">
                <el-select v-model="filters.object_detection" placeholder="请选择状态" clearable style="width: 100%">
                  <el-option label="未调用" value="1" />
                  <el-option label="已调用" value="0" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row justify="end">
            <el-button @click="applyFilters">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-row>
        </el-form>
      </div>
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
              <th>年级</th>
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
              <td>{{ doc.grade }}</td>
              <td>{{ doc.book_page }}</td>
              <td>
                <img :src="getImageUrl(doc.id)" style="width: 100px;" @error="handleImageError" @mousedown="startDrawing(doc.id, $event)" @mousemove="drawBox(doc.id, $event)" @mouseup="endDrawing(doc.id, $event)" @mouseleave="endDrawing(doc.id, $event)" />
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
                :page-sizes="[10]"
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
      width="90%"
      :before-close="handleClose">
    <div class="questions-container-vertical">
      <div v-for="(question, index) in questionList" :key="index" class="question-item-horizontal">
        <img :src="getQuestionImageUrl(question.path)" alt="题目图片" class="question-image" />
        <div class="question-table-container">
          <el-table :data="[question]" border style="width: 100%">
            <el-table-column prop="question_number" label="题号" width="80">
              <template #default="scope">
                <el-input v-model="scope.row.question_number" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="题目信息" width="780">
              <template #default="scope">
                <div class="vertical-fields">
                  <div class="field-item">
                    <label>答案：</label>
                    <el-input v-model="scope.row.answer" size="small" type="textarea" :rows="2" placeholder="请输入答案" />
                  </div>
                  <div class="field-item">
                    <label>解析：</label>
                    <el-input v-model="scope.row.analysis" size="small" type="textarea" :rows="2" placeholder="请输入解析" />
                  </div>
                  <div class="field-item">
                    <label>知识点：</label>
                    <el-input v-model="scope.row.knowledge" size="small" type="textarea" :rows="2" placeholder="请输入知识点" />
                  </div>
                </div>
              </template>
            </el-table-column>
            <!-- 操作列 -->
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="showDetailDialog(row)">
                  题目详情
                </el-button>
                <div v-if="waitingDialogVisible" class="mini-waiting-dialog">
                  <div class="mini-waiting-content">
                    正在分析题目，请耐心等待...
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
    <template #footer>
        <span class="dialog-footer">
            <el-button @click="saveQuestions">保存</el-button>
            <el-button @click="questionDialogVisible = false">关闭</el-button>
        </span>
    </template>
  </el-dialog>
  <!-- 查看题目详情弹窗 -->
  <el-dialog title="题目详情" v-model="detailDialogVisible" width="60%" :before-close="closeDetailDialog">
    <div v-if="currentDetailQuestion">
      <h3>题号：{{ currentDetailQuestion.question_number }}</h3>
      <div style="margin-bottom: 10px;">
        <img :src="getQuestionImageUrl(currentDetailQuestion.path)" alt="题目图片" style="max-width: 100%; border-radius: 4px;" />
      </div>
      <div style="margin-bottom: 10px; border-bottom: 1px solid #eee;">
        <div><b>图片：</b> {{ currentDetailQuestion.file || currentDetailQuestion.pages }}</div>
        <div><b>年级：</b> {{ currentDetailQuestion.grade }}</div>
        <div><b>学科：</b> {{ currentDetailQuestion.subject }}</div>
      </div>
      <div>
        <div><b>题目：</b> {{ currentDetailQuestion.topic }}</div>
        <div><b>答案：</b> {{ currentDetailQuestion.answer }}</div>
        <div><b>解析：</b> {{ currentDetailQuestion.points || currentDetailQuestion.analysis }}</div>
        <div><b>知识点：</b> {{ currentDetailQuestion.analysis || currentDetailQuestion.knowledge }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="closeDetailDialog">关闭</el-button>
    </template>
  </el-dialog>
</template>



<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { getImageUrl } from '../api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus';
import {Loading} from "@element-plus/icons-vue";
const waitingDialogVisible = ref(false)
const filters = ref({
  isbn: '',
  book_name: '',
  subject: '',
  book_page: '',
  object_detection: '',
  grade: ''
});

const filteredDocuments = ref([]); // 存储筛选后的文档列表

const detailDialogVisible = ref(false);
const currentDetailQuestion = ref(null);

const currentDocForQuestions = ref(null);

function showQuestions(doc) {
  currentDocForQuestions.value = doc;
  try {
    // 提取数字部分
    const pageNumber = doc.book_page.replace(/[^0-9]/g, '');
    fetch(`http://localhost:8080/api/pdf-pages/by-page?isbn=${doc.isbn}&page=${pageNumber}`)
      .then(response => response.json())
      .then(data => {
        data.sort((a, b) => a.question_number - b.question_number);
        questionList.value = data;
        questionDialogVisible.value = true;
      })
      .catch(error => {
        console.error('获取题目失败:', error);
        ElMessage.error('获取题目失败');
      });
  } catch (error) {
    console.error('获取题目失败:', error);
    ElMessage.error('获取题目失败');
  }
}

function refreshQuestionList() {
  if (currentDocForQuestions.value) {
    showQuestions(currentDocForQuestions.value);
  }
}

function showDetailDialog(question) {
  waitingDialogVisible.value = true;
  fetch('http://localhost:8080/api/pdf-pages/workflow-detail', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: question.id })
  })
  // 超时Promise
  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => reject(new Error('调用超时，请稍后重试')), 180000); // 180000ms = 3分钟
  });

  // fetchPromise
  const fetchPromise = fetch('http://localhost:8080/api/pdf-pages/workflow-detail', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: question.id })
  }).then(res => res.json());

  Promise.race([fetchPromise, timeoutPromise])
    .then(res => res.json())
    .then(() => {
      // 不弹出详情弹窗，直接刷新题目列表
    })
    .catch((err) => {
      // 超时或异常
      ElMessage.error(err.message || '调用失败');
    })
    .finally(() => {
      waitingDialogVisible.value = false;
      refreshQuestionList();
    });
}

function closeDetailDialog() {
  detailDialogVisible.value = false;
  currentDetailQuestion.value = null;
}
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
const pageSize = ref(10)
const total = ref(0)

const pagedDocuments = computed(() => {
  total.value = filteredDocuments.value.length
  const start = (currentPage.value - 1) * pageSize.value
  return filteredDocuments.value.slice(start, start + pageSize.value)
})

const resetFilters = () => {
  filters.value = {
    isbn: '',
    book_name: '',
    subject: '',
    book_page: '',
    object_detection: '',
    grade: ''
  }
  applyFilters()
}

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
    }));
    applyFilters(); // 初始化筛选列表
  } catch (err) {
    console.error('获取文档失败', err)
  }
}

const applyFilters = () => {
  let filtered = [...documents.value]

  if (filters.value.isbn) {
    filtered = filtered.filter(doc =>
        doc.isbn?.toLowerCase().includes(filters.value.isbn.toLowerCase())
    )
  }

  if (filters.value.book_name) {
    filtered = filtered.filter(doc =>
        doc.book_name?.toLowerCase().includes(filters.value.book_name.toLowerCase())
    )
  }

  if (filters.value.subject) {
    filtered = filtered.filter(doc =>
        doc.subject?.toLowerCase().includes(filters.value.subject.toLowerCase())
    )
  }

  if (filters.value.book_page) {
    filtered = filtered.filter(doc =>
        String(doc.book_page).includes(filters.value.book_page)
    )
  }

  if (filters.value.object_detection !== '') {
    filtered = filtered.filter(doc =>
        String(doc.object_detection) === filters.value.object_detection
    )
  }

  if (filters.value.grade) {
    filtered = filtered.filter(doc =>
      doc.grade?.toLowerCase().includes(filters.value.grade.toLowerCase())
    )
  }

  filteredDocuments.value = filtered
  currentPage.value = 1 // 回到第一页
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

// 获取题目图片URL
const getQuestionImageUrl = (path) => {
  if (!path) return '';
  return path; // 直接用 OSS 路径
};

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
  width: 100%;
  max-width: 1400px;
  margin: 0 auto 40px auto;
  background-color: #fff;
  box-shadow: 0 2px 16px rgba(0,0,0,0.08);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  padding-bottom: 24px;
}

.card-header {
  width: 100%;
  text-align: center;
  background-color: #84aee8;
  padding: 18px 0 12px 0;
  border-radius: 16px 16px 0 0;
}

.card-header h2 {
  margin: 0;
  color: #fff;
  font-size: 2rem;
  letter-spacing: 2px;
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

.question-table-container {
  flex: 1;
  min-width: 0;
}


.filter-section {
  margin-bottom: 20px;
  background-color: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
}


.vertical-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-item {
  display: flex;
  flex-direction: column;
}

.field-item label {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 5px;
}
.mini-waiting-dialog {
  position: fixed;
  z-index: 99999;
  left: 0; top: 0; width: 100vw; height: 100vh;
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
}
.mini-waiting-content {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.12);
  padding: 32px 48px;
  font-size: 20px;
  color: #333;
  font-weight: bold;
  border: none;
  pointer-events: all;
}
</style>