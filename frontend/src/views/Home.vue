<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>欢迎使用 PDF 文档管理系统</h2>
        </div>
      </template>

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
              <th>检测路径</th>
              <th>是否切割</th>
              <th>上传时间</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody id="documents">
            <tr v-for="doc in documents" :key="doc.id">
              <td>
                <el-radio
                    v-model="selectedDocument"
                    :label="doc.id"
                    @change="handleSelect(doc)"
                >&nbsp;</el-radio>  <!-- 使用&nbsp;代替显示ID -->
              </td>
              <td>{{ doc.id }}</td>
              <td>{{ doc.subject }}</td>
              <td>{{ doc.fileName }}</td>
              <td>{{ doc.pages }}</td>
              <td>{{ doc.filePath + '\\' + doc.fileName }}</td>
              <td>{{ doc.isbn }}</td>
              <td>{{ doc.txtPath }}</td>
              <td :data-status="doc.slicing">
                {{ doc.slicing === 1 ? '未切割' : '已切割' }}
              </td>
              <td>{{ new Date(doc.uploadTime).toLocaleString() }}</td>
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
  </div>
</template>

<script setup>
import '../../css/style.css'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus' // 引入ElMessageBox用于弹出提示框
import api, { getDocuments } from '../api'


// 新增状态
const isDetecting = ref(false);
const progressPercent = ref(0);
const progressStatus = ref(null);
const progressText = ref('正在检测...');



const router = useRouter()

const searchKeyword = ref('')
const documents = ref([])


const selectedDocument = ref(null)
const selectedDocumentData = ref(null)



const dialogVisible = ref(false)
const form = ref({
  subject: '',
  fileName: '',
  isbn: '',
  file: null
})

const fetchDocuments = async () => {
  documents.value = await getDocuments()
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

  ElMessage.info('正在切割 PDF: ' + doc.fileName)
  try {
    const response = await api.post(`/pdf/split?id=${doc.id}`)
    ElMessage.success(response.data || '切割成功')
    await fetchDocuments()
  } catch (error) {
    ElMessage.error('切割失败: ' + error.message)
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
  max-width: 1200px;
  margin: 0 auto;
  flex: 1;
  background-color: #fff;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.card-header {
  width: 100%;
  text-align: center;
  background-color: #84aee8;
  padding: 10px;
}

.card-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.action-section {
  text-align: right;
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: flex-end;
  align-items: center;
}



.button-group {
  display: flex;
  gap: 16px;
  width: 100%;
  justify-content: center;
}




.document-table {
  table-layout: fixed; /* 添加这一行 */
  width: 100%;
  border-collapse: collapse;
}

.document-table th,
.document-table td {
  width: 80px;
  border: 2px solid #ddd;
  padding: 8px;
  text-align: center;
}

.document-table th {
  background-color: #4a90e2;
  color: #fff;
}

.document-table td {
  vertical-align: middle;
}


/* 调整表格列宽 - 使用更具体的选择器 */
.document-table thead th:nth-child(1),
.document-table tbody td:nth-child(1) {
  width: 4% !important;  /* 进一步缩小选择列宽度 */
  min-width: 70px;       /* 设置最小宽度 */
  text-align: center;
}

.document-table thead th:nth-child(2),
.document-table tbody td:nth-child(2) {
  width: 8% !important;
}

.document-table thead th:nth-child(3),
.document-table tbody td:nth-child(3),
.document-table thead th:nth-child(4),
.document-table tbody td:nth-child(4),
.document-table thead th:nth-child(5),
.document-table tbody td:nth-child(5),
.document-table thead th:nth-child(10),
.document-table tbody td:nth-child(10),
.document-table thead th:nth-child(7),
.document-table tbody td:nth-child(7),
.document-table thead th:nth-child(8),
.document-table tbody td:nth-child(8),
.document-table thead th:nth-child(9),
.document-table tbody td:nth-child(9) {
  width: 10% !important;
}

.document-table thead th:nth-child(6),
.document-table tbody td:nth-child(6) {
  width: 20% !important;
}


.document-table thead th:nth-child(8),
.document-table tbody td:nth-child(8) {
  width: 20% !important;
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


.el-radio {
  margin-right: -10px;
}


/* 新增样式 */
.detection-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.progress-text {
  margin-top: 20px;
  color: white;
  font-size: 18px;
  font-weight: bold;
}

/* 调整按钮加载状态样式 */
.el-button.is-loading {
  position: relative;
  pointer-events: none;
}

.el-button.is-loading:before {
  content: "";
  position: absolute;
  left: -1px;
  top: -1px;
  right: -1px;
  bottom: -1px;
  border-radius: inherit;
  background-color: rgba(255, 255, 255, 0.35);
}
</style>