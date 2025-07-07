<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>欢迎使用 PDF 文档管理系统</h2>
        </div>
      </template>

      <div class="filter-section">
        <el-form :model="filters" label-width="100px" size="small">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="学科名称">
                <el-input v-model="filters.subject" placeholder="请输入学科名称" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="书籍名称">
                <el-input v-model="filters.fileName" placeholder="请输入书籍名称" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="书籍页码">
                <el-input v-model="filters.pages" placeholder="请输入页码" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="ISBN码">
                <el-input v-model="filters.isbn" placeholder="请输入ISBN码" clearable />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="是否切割">
                <el-select v-model="filters.slicing" placeholder="请选择状态" clearable style="width: 100%">
                  <el-option label="未切割" value="1" />
                  <el-option label="已切割" value="0" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="上传时间">
                <el-date-picker
                    v-model="filters.uploadTime"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="年级">
                <el-input v-model="filters.grade" placeholder="请输入年级" clearable />
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
          <h3>PDF文档列表</h3>
          <div class="action-section">
            <div class="button-group">
              <el-button type="primary" size="large" @click="dialogVisible = true">
                <el-icon><Upload /></el-icon>
                上传 PDF 文件
              </el-button>
              <el-button
                  type="primary"
                  size="large"
                  @click="handleDetect"
                  :disabled="!selectedDocument || isDetecting"
                  :loading="isDetecting"
              >
                <el-icon><Search /></el-icon>
                {{ isDetecting ? '检测中...' : '检测' }}
              </el-button>
            </div>
          </div>
          <table class="document-table">
            <thead>
            <tr>
              <th>
                <span class="radio-header">
                  <el-radio :model-value="false" disabled></el-radio>
                </span>
              </th>
              <th>书籍ID</th>
              <th>学科名称</th>
              <th>书籍名称</th>
              <th>书籍页码</th>
              <th>书籍路径</th>
              <th>ISBN码</th>
              <th>年级</th>
              <th>检测路径</th>
              <th>是否切割</th>
              <th>上传时间</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody id="documents">
            <tr v-for="doc in pagedDocuments" :key="doc.id">
              <td>
                <el-radio
                    v-model="selectedDocument"
                    :label="doc.id"
                    @change="handleSelect(doc)"
                >&nbsp;</el-radio>  <!-- 使用&nbsp;代替显示ID -->
              </td>
              <td>{{ doc.id }}</td>
              <td>{{ doc.subject }}</td>
              <td>{{ doc.file_name }}</td>          <!-- 书籍名称 -->
              <td>{{ doc.pages }}</td>
              <td>{{ doc.file_path }}</td>          <!-- 书籍路径 -->
              <td>{{ doc.isbn }}</td>
              <td>{{ doc.grade }}</td>
              <td>{{ doc.slicing_path }}</td>       <!-- 检测路径 -->
              <td :data-status="doc.slicing">
                {{ doc.slicing === 1 ? '未切割' : '已切割' }}
              </td>
              <td>{{ formatDate(doc.upload_time) }}</td>  <!-- 上传时间 -->
              <td>
                <el-button
                    size="small"
                    @click="handleSplit(doc)"
                    :disabled="doc.slicing === 0"
                    :class="{ 'split-button-disabled': doc.slicing === 0 }"
                >
                切割
                </el-button>
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

    <el-dialog v-model="dialogVisible" title="上传 PDF 文件" width="50%">
      <el-form :model="form" label-width="120px" ref="uploadFormRef">
        <el-form-item label="学科名称">
          <el-input v-model="form.subject" />
        </el-form-item>
        <el-form-item label="书籍名称">
          <el-input v-model="form.fileName" />
        </el-form-item>
        <el-form-item label="ISBN 码">
          <el-input v-model="form.isbn" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="form.grade" />
        </el-form-item>
        <el-form-item label="PDF 文件">
          <el-upload
              action="#"
              :on-change="handleFileChange"
              :auto-upload="false"
              :show-file-list="true"
              accept=".pdf"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpload">确认上传</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 切割中悬浮窗 -->
    <div v-if="isSplitting" class="splitting-overlay">
      <div class="splitting-message">
        正在切割中，请耐心等待...
      </div>
    </div>
  </div>
</template>

<script setup>
import '../../css/style.css'
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus' // 引入ElMessageBox用于弹出提示框
import api, { getDocuments } from '../api'


// 新增状态
const isDetecting = ref(false);
const progressPercent = ref(0);
const progressStatus = ref(null);
const progressText = ref('正在检测...');
const isSplitting = ref(false)



const router = useRouter()

const searchKeyword = ref('')
const documents = ref([])


const selectedDocument = ref(null)
const selectedDocumentData = ref(null)

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return 'Invalid Date';
  return date.toLocaleString();
};

const dialogVisible = ref(false)
const form = ref({
  subject: '',
  fileName: '',
  isbn: '',
  grade: '',
  file:null
})

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
    subject: '',
    fileName: '',
    pages: '',
    isbn: '',
    slicing: '',
    uploadTime: [],
    grade: ''
  };
  currentPage.value = 1;
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
};

const filteredDocuments = ref([]) // 存储筛选后的文档列表
const fetchDocuments = async () => {
  documents.value = await getDocuments()
  applyFilters() // 获取数据后立即应用一次筛选（默认为空）
}
const applyFilters = () => {
  let filtered = [...documents.value]

  // 学科名称过滤
  if (filters.value.subject) {
    filtered = filtered.filter(doc =>
        doc.subject?.toLowerCase().includes(filters.value.subject.toLowerCase())
    )
  }

  // 书籍名称过滤
  if (filters.value.fileName) {
    filtered = filtered.filter(doc =>
        doc.file_name?.toLowerCase().includes(filters.value.fileName.toLowerCase())
    )
  }

  // 书籍页码过滤
  if (filters.value.pages) {
    filtered = filtered.filter(doc =>
        String(doc.pages).includes(filters.value.pages)
    )
  }

  // ISBN 过滤
  if (filters.value.isbn) {
    filtered = filtered.filter(doc =>
        doc.isbn?.toLowerCase().includes(filters.value.isbn.toLowerCase())
    )
  }

  // 是否切割过滤
  if (filters.value.slicing !== '') {
    filtered = filtered.filter(doc =>
        String(doc.slicing) === filters.value.slicing
    )
  }

  // 上传时间范围过滤
  if (filters.value.uploadTime && filters.value.uploadTime.length === 2) {
    const [startDate, endDate] = filters.value.uploadTime
    if (startDate && endDate) {
      filtered = filtered.filter(doc => {
        const uploadDate = new Date(doc.upload_time)
        return uploadDate >= startDate && uploadDate <= endDate
      })
    }
  }

  // 年级过滤
  if (filters.value.grade) {
    filtered = filtered.filter(doc =>
      doc.grade?.toLowerCase().includes(filters.value.grade.toLowerCase())
    )
  }

  filteredDocuments.value = filtered
  currentPage.value = 1 // 回到第一页
}
onMounted(() => {
  fetchDocuments()
})

const handleFileChange = (file) => {
  form.value.file = file.raw
}

const submitUpload = async () => {
  const formData = new FormData()
  formData.append('subject', form.value.subject)
  formData.append('fileName', form.value.fileName)
  formData.append('isbn', form.value.isbn)
  formData.append('grade', form.value.grade)
  formData.append('file', form.value.file)

  try {
    const response = await api.post('/pdf/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    ElMessage.success('上传成功')
    dialogVisible.value = false
    await fetchDocuments()
  } catch (error) {
    ElMessage.error('上传失败: ' + error.message)
  }
}

const handleSplit = async (doc) => {
  if (doc.slicing === 0) {
    ElMessage.warning('该文件已切割');
    return;
  }

  isSplitting.value = true; // 开始切割时显示遮罩

  ElMessage.info('正在切割 PDF: ' + doc.fileName)
  try {
    const response = await api.post(`/pdf/split?id=${doc.id}`)
    ElMessage.success(response.data || '切割成功')
    await fetchDocuments()
  } catch (error) {
    ElMessage.error('切割失败: ' + error.message)
  } finally {
    isSplitting.value = false; // 切割结束时关闭遮罩
  }
}


/**
 * 提取文件路径中的目录部分或标准化文件夹路径
 * @param {string} fullPath - 完整路径（可以是文件夹路径或文件路径）
 * @returns {string} - 提取的目录路径或标准化后的文件夹路径
 */
const extractDirectory = (fullPath) => {
  // 参数验证
  if (!fullPath || typeof fullPath !== 'string') {
    console.warn('无效的路径参数:', fullPath);
    return '';
  }

  // 标准化路径（替换所有反斜杠为正斜杠）
  const normalizedPath = fullPath.replace(/\\/g, '/');

  // 判断是否为纯文件夹路径（以斜杠结尾或不包含文件名扩展名）
  const isFolderPath = normalizedPath.endsWith('/') || !/\.[a-zA-Z0-9]+$/.test(normalizedPath);

  if (isFolderPath) {
    // 纯文件夹路径：返回标准化后的路径（如果不是以斜杠结尾，则添加斜杠）
    return normalizedPath.endsWith('/') ? normalizedPath : normalizedPath + '/';
  } else {
    // 包含文件的路径：提取目录部分（去掉文件名）
    const lastSlashIndex = normalizedPath.lastIndexOf('/');
    return lastSlashIndex > -1
        ? normalizedPath.substring(0, lastSlashIndex + 1) // 保留斜杠
        : ''; // 特殊情况：路径中没有斜杠（可能是当前目录下的文件）
  }
};


// 目标检测功能
const handleSelect = (doc) => {
  selectedDocumentData.value = doc
}

// 修改handleDetect方法
const handleDetect = async () => {
  const doc = selectedDocumentData.value;

  // 判断文档切割状态
  if (doc.slicing === 0) {
    // 已切割，允许检测
    try {
      // 初始化进度状态
      isDetecting.value = true;
      progressPercent.value = 0;
      progressStatus.value = null;
      progressText.value = '正在准备检测...';

      // 模拟进度更新
      const progressInterval = setInterval(() => {
        if (progressPercent.value < 90) {
          progressPercent.value += 10;
          progressText.value = `正在检测 (${progressPercent.value}%)...`;
        }
      }, 500);

      // 提取目录（使用标准化函数）
      console.log("doc.filepath:", doc.filePath);
      const directoryPath = extractDirectory(doc.filePath);
      if (!directoryPath) {
        throw new Error('文档路径无效，无法提取目录');
      }

      // 构建检测路径
      const folderPath = `${directoryPath}/page`;
      const folderIdentifier = `${directoryPath}/labelstxt`;

      console.log('请求中的ISBN类型:', typeof doc.isbn);

      console.log('请求参数:', {
        id: doc.id,
        folder_path: folderPath,
        folder_identifier: folderIdentifier,
        ISBN: doc.isbn,
        slicing: doc.slicing
      });

      const response = await api.post('/detect/folder', {
        id: doc.id,
        folderPath: folderPath,
        folderIdentifier: folderIdentifier,
        ISBN: doc.isbn,
        slicing: doc.slicing
      }, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      // 完成进度
      clearInterval(progressInterval);
      progressPercent.value = 100;
      progressStatus.value = 'success';
      progressText.value = '检测完成!';

      // 延迟关闭加载状态，让用户看到完成状态
      setTimeout(() => {
        isDetecting.value = false;
        if (response.data.success) {
          ElMessage.success('检测成功');
          // 刷新文档列表
          fetchDocuments();
        } else {
          ElMessage.error('检测失败: ' + response.data.message);
        }
      }, 500);

    } catch (error) {
      // 错误处理
      clearInterval(progressInterval);
      progressPercent.value = 100;
      progressStatus.value = 'exception';
      progressText.value = '检测失败';

      setTimeout(() => {
        isDetecting.value = false;
        console.error('检测请求失败:', error);
        ElMessage.error('检测请求失败: ' + error.message);
      }, 500);
    }
  } else {
    // 未切割，提示用户不能检测
    ElMessageBox.confirm(
        '该文档尚未切割，无法进行检测。请先进行切割？',
        '提示',
        {
          confirmButtonText: '去切割',
          cancelButtonText: '取消',
          type: 'warning'
        }
    ).then(() => {
      // 用户点击"去切割"时调用切割功能
      handleSplit(doc);
    }).catch(() => {
      // 用户点击"取消"时不做处理
    });
  }
};
const filters = ref({
  subject: '',
  fileName: '',
  pages: '',
  isbn: '',
  slicing: '', // '1' 表示未切割, '0' 表示已切割
  uploadTime: [], // [startDate, endDate]
  grade: ''
});
</script>

<style scoped>
.home-container {
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
  padding: 32px 32px 0 32px;
  overflow-x: auto;
}

.filter-section {
  margin-bottom: 32px;
  background-color: #f9f9f9;
  padding: 24px 32px 8px 32px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}

.action-section {
  margin: 32px 0 24px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.button-group {
  display: flex;
  gap: 32px;
  width: 100%;
  justify-content: center;
  margin-bottom: 8px;
}

.document-list {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.document-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin-top: 16px;
  background: #fff;
}

.document-table th,
.document-table td {
  padding: 14px 10px;
  text-align: center;
  font-size: 15px;
}

.document-table th {
  background-color: #4a90e2;
  color: #fff;
  font-weight: 500;
  border-top: 1px solid #e0e0e0;
}

.document-table td {
  border-bottom: 1px solid #e0e0e0;
  color: #333;
}

.document-table td:nth-child(7),
.document-table td:nth-child(8),
.document-table td:nth-child(9),
.document-table td:nth-child(10),
.document-table td:nth-child(11) {
  font-size: 14px;
}

.document-table td:nth-child(6),
.document-table td:nth-child(7) {
  max-width: 180px;
  word-break: break-all;
}

.document-table td:nth-child(8) {
  font-weight: bold;
  color: #2d8cf0;
}

.document-table td:nth-child(9) {
  font-weight: bold;
  color: #e67e22;
}

.document-table td:nth-child(10) {
  font-weight: bold;
  color: #27ae60;
}

.document-table td:nth-child(11) {
  font-weight: bold;
  color: #c0392b;
}

.document-table th,
.document-table td {
  min-width: 80px;
}

.pagination {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
  padding-right: 16px;
  background: #fff;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  min-height: 64px;
  align-items: center;
}


@media (max-width: 900px) {
  .welcome-card {
    max-width: 98vw;
    padding: 0 2vw;
  }
  .card-content {
    padding: 12px 2vw 0 2vw;
  }
  .filter-section {
    padding: 12px 2vw 8px 2vw;
  }
  .document-list {
    padding: 0.5rem;
  }
  .document-table th, .document-table td {
    font-size: 13px;
    padding: 8px 4px;
  }
}

.split-button-disabled {
  background-color: #ccc; /* 灰色背景 */
  color: #666;              /* 灰色文字 */
  cursor: not-allowed;
  opacity: 0.7;
}

/* 表头单选按钮样式 */
.radio-header {
  display: inline-block;
  width: 16px;
  height: 16px;
}

.radio-header .el-radio {
  margin: 0;
  padding: 0;
  cursor: default;
}

.radio-header .el-radio__input {
  cursor: default;
}

/* 调整选择列宽度 */
.document-table thead th:nth-child(1),
.document-table tbody td:nth-child(1) {
  width: 40px !important;
  min-width: 40px;
  padding: 8px 0 !important;
  text-align: center;
}

.splitting-overlay {
  position: fixed;
  z-index: 3000;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}
.splitting-message {
  background: #fff;
  color: #333;
  font-size: 22px;
  padding: 40px 60px;
  border-radius: 12px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.15);
  font-weight: bold;
}
.pagination {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
  padding-right: 16px;
  background: #fff;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  min-height: 64px;
  align-items: center;
}
</style>