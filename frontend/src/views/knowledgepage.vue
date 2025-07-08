<template>
  <div class="knowledge-container">
    <div class="welcome-card">
      <el-card class="main-card">
        <template #header>
          <div class="card-header">
            <h2>知识分类汇总管理系统</h2>
          </div>
        </template>

        <div class="card-content">
          <div class="document-list">
            <h3>知识分类汇总数据</h3>

            <!-- Excel上传区域 -->
            <div class="excel-upload-area">
              <div class="upload-button-container">
                <!-- 上传Excel按钮 -->
                <el-button
                    type="primary"
                    size="large"
                    @click="openFileDialog"
                    :loading="isUploading"
                >
                  <i class="el-icon-upload"></i> 上传Excel文件
                </el-button>

                <!-- 文件选择输入框，隐藏显示 -->
                <input
                    type="file"
                    id="excelFileInput"
                    accept=".xlsx,.xls"
                    @change="handleFileUpload"
                    style="display: none"
                >
              </div>

              <!-- 模板下载链接 -->
              <div class="template-download">
                <el-link type="primary" @click="downloadTemplate">
                  <i class="el-icon-document"></i> 下载Excel模板
                </el-link>
              </div>

            </div>

            <!-- 查询区域 -->
            <div class="query-area">
              <el-row :gutter="20">
                <el-col :span="6">
                  <el-form-item label="一级标注">
                    <el-input v-model="queryParams.categoryLevel1" placeholder="请输入一级标注" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="二级标注">
                    <el-input v-model="queryParams.categoryLevel2" placeholder="请输入二级标注" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="三级标注">
                    <el-input v-model="queryParams.knowledge" placeholder="请输入三级标注" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="题目数量">
                    <el-input v-model="queryParams.number" placeholder="请输入题目数量" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20" style="margin-top: 10px;">
                <el-col :span="6">
                  <el-form-item label="所属学科">
                    <el-select v-model="queryParams.subject" placeholder="请选择所属学科" style="width: 100%;">
                      <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="年级">
                    <el-select v-model="queryParams.stage" placeholder="请选择年级" style="width: 100%;">
                      <el-option v-for="item in gradeOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="6" style="text-align: right; padding-top: 30px;">
                  <el-button type="primary" @click="handleQuery">查询</el-button>
                  <el-button @click="resetQuery">重置</el-button>
                </el-col>
              </el-row>
            </div>

            <!-- 新增：批量操作按钮 -->
            <div class="batch-actions">
              <el-button
                  type="primary"
                  :disabled="selectedRows.length === 0"
                  @click="handleBatchDelete"
              >
                <i class="el-icon-delete"></i> 批量删除
              </el-button>

              <el-button
                  type="primary"
                  @click="handleAdd">
                新增
              </el-button>
            </div>





            <!-- 新增：数据库数据表格区域 -->
            <div class="table-area">
              <!-- 单选表格，用 element-plus 的 ElTable 实现 -->
              <el-table
                  border
                  :data="tableData"
                  style="width: 100%"
                  @selection-change="handleSelectionChange"
              >
                <!-- 第一列：单选框 -->
                <el-table-column
                    align="center"
                    type="selection"
                />
                <!-- 数据库字段列，对应 KnowledgeSummary 实体类 -->
                <el-table-column
                    sortable="sortable"
                    align="center"
                    prop="id"
                    label="ID"
                    min-width="40px"
                />
                <el-table-column
                    align="center"
                    prop="categoryLevel1"
                    label="一级标注"
                />
                <el-table-column
                    align="center"
                    prop="categoryLevel2"
                    label="二级标注"
                />
                <el-table-column
                    align="center"
                    prop="knowledge"
                    label="三级标注"
                />
                <el-table-column
                    align="center"
                    prop="subject"
                    label="所属学科"
                />
                <el-table-column
                    align="center"
                    prop="number"
                    label="题目数量"
                    min-width="60px"
                />
                <el-table-column
                    align="center"
                    prop="stage"
                    label="年级"
                    min-width="60px"
                />
                <!-- 可扩展操作列（如编辑、删除） -->
                <el-table-column
                    align="center"
                    label="操作"
                    fixed="right"
                    min-width="130px"
                >
                  <template #default="scope">
                    <!--                    衍题按钮-->
                    <el-button
                        type="primary"
                        size="small"
                        round
                        @click="handleYanti(scope.row)"
                    >衍题</el-button>
                    <el-button
                        type="primary"
                        size="small"
                        round
                        @click="handleEdit(scope.row)"
                    >编辑</el-button>
                    <el-button
                        type="primary"
                        size="small"
                        round
                        @click="handleDelete(scope.row)"
                    >删除</el-button>
<!--                    新增一个查看详情的按钮，携带整条数据的传递点击可以调转都查看存入到了数据库中题目详情的页面-->
                    <el-button
                        type="primary"
                        size="small"
                        round
                        @click="handleXiangQing(scope.row)"
                    >详情</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 新增：分页组件 -->
              <!-- :page-sizes="[10, 20, 50, 100]"  可切换的每页条数 -->
              <div class="pagination-container">
                <span class="total-count">共 {{ total }} 条数据</span>
                <el-pagination
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    :current-page="currentPage"
                    :background="background"
                    :page-sizes="[10, 20, 50, 100]"
                    :page-size="pageSize"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                />
              </div>


            </div>



          </div>
        </div>

      </el-card>

    </div>
  </div>


  <!-- 新增对话框（与编辑对话框共用结构，通过标志位区分） -->
  <el-dialog
      v-model="dialogVisible"
      :title="isAdding ? '新增知识分类' : '编辑知识分类'"
      width="50%"
      :before-close="handleDialogClose"
  >
    <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
    >
      <!-- ID字段在新增时隐藏 -->
      <el-form-item label="ID" v-if="!isAdding">
        <el-input v-model="form.id" disabled />
      </el-form-item>
      <el-form-item label="一级标注" prop="categoryLevel1">
        <el-input v-model="form.categoryLevel1" placeholder="请输入一级标注" />
      </el-form-item>
      <el-form-item label="二级标注" prop="categoryLevel2">
        <el-input v-model="form.categoryLevel2" placeholder="请输入二级标注" />
      </el-form-item>
      <el-form-item label="三级标注" prop="knowledge">
        <el-input v-model="form.knowledge" placeholder="请输入三级标注" />
      </el-form-item>
      <el-form-item label="所属学科" prop="subject">
        <el-input v-model="form.subject" placeholder="请输入所属学科" />
      </el-form-item>
      <el-form-item label="题目数量" prop="number">
        <el-input v-model.number="form.number" placeholder="请输入题目数量" />
      </el-form-item>
      <el-form-item label="年级" prop="stage">
        <el-input v-model="form.stage" placeholder="请输入年级" />
      </el-form-item>
    </el-form>
    <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="submitForm">
            {{ isAdding ? '新增' : '确认修改' }}
          </el-button>
        </span>
    </template>
  </el-dialog>



  <!-- 衍题对话框 -->
  <el-dialog
      v-model="yantiDialogVisible"
      title="设置衍题数量"
      width="30%"
  >
    <el-form
        ref="yantiFormRef"
        :model="yantiForm"
        :rules="yantiFormRules"
        label-width="120px"
    >
      <el-form-item label="衍题数量" prop="number">
        <el-input-number
            v-model="yantiForm.number"
            :min="1"
            :max="1000"
            placeholder="请输入衍题数量"
        />
      </el-form-item>

      <!-- 新增：标注级别选择下拉框 -->
      <el-form-item label="标注级别" prop="markLevel">
        <el-select
            v-model="yantiForm.markLevel"
            placeholder="请选择标注级别"
            style="width: 100%;"
        >
          <el-option label="一级标注" value="categoryLevel1" />
          <el-option label="二级标注" value="categoryLevel2" />
          <el-option label="三级标注" value="knowledge" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
    <span class="dialog-footer">
      <el-button @click="yantiDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmYanti">确定</el-button>
    </span>
    </template>
  </el-dialog>

</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElTable, ElTableColumn, ElButton, ElPagination } from 'element-plus';
import { getImageUrl } from "../api";
import * as XLSX from 'xlsx'; // 导入 xlsx 库用于读取 Excel 文件
import { ElMessageBox } from 'element-plus'; // 若需要删除确认，可引入
import { useRouter, useRoute } from 'vue-router'; // 确保导入 useRoute




// 上传状态管理
const isUploading = ref(false);
const uploadMessage = ref('');
const uploadStatus = ref('info');



// 表格数据
const tableData = ref([]);
// 选中的行（单选）
const selectedRow = ref(null);
// 分页相关变量
const currentPage = ref(1); // 当前页
const pageSize = ref(10);   // 每页条数
const total = ref(0);      // 总数据量
const background = ref(true)
// 初始化路由和当前路由
const router = useRouter();
// 未修改好，不一定要
const route = useRoute(); // 定义 route 变量

// 新增功能相关变量
const dialogVisible = ref(false);
const isAdding = ref(false);
const form = ref({
  id: null,
  categoryLevel1: '',
  categoryLevel2: '',
  knowledge: '',
  subject: '',
  number: 0,
  stage: ''
});
const formRef = ref(null);
const formRules = reactive({
  categoryLevel1: [{ required: true, message: '请输入一级标注', trigger: 'blur' }],
  categoryLevel2: [{ required: false, message: '请输入二级标注', trigger: 'blur' }],
  knowledge: [{ required: false, message: '请输入三级标注', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入所属学科', trigger: 'blur' }],
  number: [
    { required: true, message: '请输入题目数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        // 处理空值（若允许空值则跳过验证）
        if (!value) {
          return callback();
        }

        // 检查是否为数字字符串（支持整数和小数）
        const num = Number(value);
        if (isNaN(num)) {
          callback(new Error('请输入有效数字'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }],
  stage: [{ required: true, message: '请输入年级', trigger: 'blur' }]
});

// 批量删除相关变量
const selectedKeys = ref([]); // 选中的行ID
const selectedRows = ref([]); // 选中的行数据

// 查询相关变量
const queryParams = ref({
  categoryLevel1: '',
  categoryLevel2: '',
  knowledge: '',
  subject: '',
  number: '',
  stage: ''
});

// 下拉选择框选项
const subjectOptions = ref([
  '语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '政治'
]);

const gradeOptions = ref([
  '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
  '七年级', '八年级', '九年级', '高一', '高二', '高三'
]);


// 处理查询
const handleQuery = () => {
  currentPage.value = 1; // 重置到第一页
  fetchTableData();
};

// 重置查询条件
const resetQuery = () => {
  queryParams.value = {
    categoryLevel1: '',
    categoryLevel2: '',
    knowledge: '',
    subject: '',
    number: '',
    stage: ''
  };
  currentPage.value = 1;
  fetchTableData();
};


// 打开文件选择对话框
const openFileDialog = () => {
  document.getElementById('excelFileInput').click();
};

// 处理文件上传
const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // 验证文件类型
  const fileExtension = file.name.split('.').pop().toLowerCase();
  if (fileExtension!== 'xlsx' && fileExtension!== 'xls') {
    ElMessage.error('请上传Excel文件（.xlsx或.xls格式）');
    return;
  }
  console.log("file:", file)

  // 初步校验文件格式是否符合模板
  const isValidFormat = await checkFileFormat(file);
  if (!isValidFormat) {
    ElMessage.error('上传的文件格式不符合规定模板，请下载模板后重新填写上传');
    return;
  }

  // 开始上传
  uploadExcelFile(file);
};

// 校验文件格式是否符合模板
const checkFileFormat = async (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const data = e.target.result;
      const workbook = XLSX.read(data, { type: 'binary' });
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];

      // 检查工作表是否有内容
      if (!worksheet['!ref']) {
        resolve(false);
        return;
      }

      // 新方法 - 使用sheet_to_json工具函数，指定header: 1表示获取第一行为表头
      const firstRow = XLSX.utils.sheet_to_json(worksheet, { header: 1 })[0];
      console.log("firstRow", firstRow);
      if (!firstRow) {
        resolve(false);
        return;
      }

      const expectedHeaders = [
        '一级标注',
        '二级标注',
        '三级标注',
        '所属学科',
        '年级',
        '题目数量'
      ];

      // 检查实际列标签是否与期望的一致，且顺序严格相同
      const isMatch = expectedHeaders.length === firstRow.length &&
          expectedHeaders.every((header, index) => header === firstRow[index]);

      resolve(isMatch);
    };
    reader.readAsBinaryString(file);
  });
};


// 上传Excel到后台
const uploadExcelFile = (file) => {
  isUploading.value = true;
  uploadMessage.value = '正在上传...';
  uploadStatus.value = 'info';

  const formData = new FormData();
  formData.append('file', file);

  // 这里调用后台API上传文件，假设API路径为'/api/upload/excel'
  fetch('http://localhost:8080/api/excel', {
    method: 'POST',
    body: formData
  })
      .then(response => {
        if (!response.ok) {
          throw new Error('上传失败');
        }
        return response.json();
      })
      .then(data => {
        isUploading.value = false;
        uploadMessage.value = data.message || '文件上传成功';
        uploadStatus.value ='success';
        ElMessage.success('文件上传成功');

        // 上传成功后可以刷新列表或执行其他操作
        // loadDocumentList();
      })
      .catch(error => {
        isUploading.value = false;
        uploadMessage.value = '上传失败: ' + error.message;
        uploadStatus.value = 'error';
        ElMessage.error('文件上传失败');
      });
};

// 下载模板 - 使用fetch API处理二进制响应
const downloadTemplate = () => {
  // 后端API路径（根据你的后端配置调整）
  fetch('http://localhost:8080/api/template/excel')
      .then(response => {
        if (!response.ok) {
          throw new Error('下载失败: ' + response.statusText);
        }
        return response.blob();
      })
      .then(blob => {
        // 创建下载链接
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = '知识分类模板.xlsx';
        document.body.appendChild(a);
        a.click();

        // 清理资源
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        ElMessage.success('模板下载成功');
      })
      .catch(error => {
        ElMessage.error('下载模板失败: ' + error.message);
      });
};

// 修改后的 fetchTableData 方法
const fetchTableData = async () => {
  try {
    const page = currentPage.value - 1;
    const size = pageSize.value;

    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());

    // 添加查询参数
    if (queryParams.value.categoryLevel1) {
      params.append('categoryLevel1', queryParams.value.categoryLevel1);
    }
    if (queryParams.value.categoryLevel2) {
      params.append('categoryLevel2', queryParams.value.categoryLevel2);
    }
    if (queryParams.value.knowledge) {
      params.append('knowledge', queryParams.value.knowledge);
    }
    if (queryParams.value.number !== '') {
      params.append('number', queryParams.value.number.toString());
    }
    if (queryParams.value.subject) {
      params.append('subject', queryParams.value.subject);
    }
    if (queryParams.value.stage) {
      params.append('stage', queryParams.value.stage);
    }

    // 1. 先获取分页数据
    const response = await fetch(`http://localhost:8080/api/knowledgeSummary/page?${params.toString()}`);
    if (!response.ok) {
      throw new Error('获取数据失败');
    }

    const data = await response.json();

    // 2. 转换数据格式
    const formattedData = data.content.map(item => ({
      id: item.id,
      categoryLevel1: item.category_level1 || '',
      categoryLevel2: item.category_level2 || '',
      knowledge: item.knowledge || '',
      subject: item.subject || '',
      number: item.number || 0,
      stage: item.stage || ''
    }));

    // 3. 直接显示获取到的数据
    tableData.value = formattedData;
    total.value = data.totalElements;

    // 4. 后台静默更新数量（不阻塞UI）
    silentlyUpdateQuestionCounts(formattedData);

  } catch (error) {
    console.error('获取数据失败:', error);
    ElMessage.error('获取数据失败: ' + error.message);
    tableData.value = []; // 清空表格显示
  }
};

// 后台静默更新题目数量（不影响前端显示）
const silentlyUpdateQuestionCounts = async (data) => {
  try {
    await Promise.all(data.map(async (item) => {
      const count = await countQuestionsByKnowledge(
          item.categoryLevel1,
          item.categoryLevel2,
          item.knowledge,
          item.subject,
          item.stage
      );

      if (count !== item.number) {
        await updateKnowledgeSummaryCount(item.id, count);
      }
    }));
  } catch (error) {
    console.error('静默更新题目数量失败:', error);
  }
};

// 修改 onMounted 钩子
onMounted(() => {
  fetchTableData();
});


// 统计并更新所有知识分类的题目数量
// 修改updateAllQuestionCounts方法
const updateAllQuestionCounts = async () => {
  try {
    // 1. 获取所有知识分类数据（不分页）
    const allData = await fetchAllKnowledgeSummaries();

    // 2. 为每条数据统计题目数量
    const updatePromises = allData.map(async (item) => {
      const count = await countQuestionsByKnowledge(
          item.categoryLevel1,
          item.categoryLevel2,
          item.knowledge,
          item.subject,
          item.stage
      );

      // 如果数量有变化，则更新
      if (count !== item.number) {
        return updateKnowledgeSummaryCount(item.id, count);
      }
      return Promise.resolve();
    });

    // 3. 等待所有更新完成
    await Promise.all(updatePromises);

    // 4. 刷新当前表格数据
    fetchTableData();
    ElMessage.success(`成功更新${allData.length}条记录的题目数量`);
  } catch (error) {
    ElMessage.error('统计题目数量时出错：' + error.message);
  }
};

// 新增方法：获取所有知识分类数据
const fetchAllKnowledgeSummaries = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/all');
    if (!response.ok) {
      throw new Error('获取全部数据失败');
    }
    const data = await response.json();
    return data.map(item => ({
      id: item.id,
      categoryLevel1: item.category_level1,
      categoryLevel2: item.category_level2,
      knowledge: item.knowledge,
      subject: item.subject,
      number: item.number,
      stage: item.stage
    }));
  } catch (error) {
    console.error('获取全部数据出错:', error);
    throw error;
  }
};

// 根据知识分类统计题目数量
const countQuestionsByKnowledge = async (level1, level2, knowledge, subject, stage) => {
  try {
    // 构建查询参数
    const params = new URLSearchParams();
    if (level1) params.append('level1', level1);
    if (level2) params.append('level2', level2);
    if (knowledge) params.append('knowledge', knowledge);
    if (subject) params.append('subject', subject);
    if (stage) params.append('stage', stage);

    const response = await fetch(`http://localhost:8080/api/count?${params.toString()}`);
    if (!response.ok) {
      throw new Error('获取题目数量失败');
    }
    const data = await response.json();
    console.log("datacount:", data)
    return data.count || 0;
  } catch (error) {
    console.error('统计题目数量出错:', error);
    return 0;
  }
};

// 更新知识分类的题目数量
const updateKnowledgeSummaryCount = async (id, count) => {
  try {
    const response = await fetch(`http://localhost:8080/api/updateCount/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ number: count })
    });

    if (!response.ok) {
      throw new Error('更新题目数量失败');
    }
    return response.json();
  } catch (error) {
    console.error('更新题目数量出错:', error);
    throw error;
  }
};


// 切换每页条数
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1; // 切换条数后回到第一页
  fetchTableData();
};

// 切换当前页
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
  fetchTableData();
};

// 处理表格选择变化
const handleSelectionChange = (rows) => {
  selectedRows.value = rows;
  selectedKeys.value = rows.map(row => row.id);
};

// 删除操作（示例，需调用后端删除接口）
const handleDelete = (row) => {
  ElMessageBox.confirm(
      '此操作将永久删除该记录, 是否继续?',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    // 调用后端删除接口
    fetch(`http://localhost:8080/api/knowledgeSummary/delete/${row.id}`, {
      method: 'DELETE'
    })
        .then(response => {
          if (!response.ok) {
            throw new Error('网络响应不正常');
          }
          return response.json();
        })
        .then(data => {
          if (data.success) {
            ElMessage.success(data.message || '删除成功');
            // 优化刷新逻辑：直接从前端移除已删除项，避免全量刷新
            tableData.value = tableData.value.filter(item => item.id !== row.id);
            // 或者如果需要全量刷新
            fetchTableData();
          } else {
            throw new Error(data.message || '删除失败');
          }
        })
        .catch(error => {
          ElMessage.error(error.message);
          console.error('删除错误:', error);
        });
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};

// 批量删除操作
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录');
    return;
  }

  const ids = selectedKeys.value;
  const count = ids.length;

  ElMessageBox.confirm(
      `此操作将永久删除选中的 ${count} 条记录, 是否继续?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    // 调用后端批量删除接口
    batchDeleteKnowledgeSummaries(ids).then(() => {
      ElMessage.success(`成功删除 ${count} 条记录`);
      fetchTableData(); // 刷新表格
      selectedKeys.value = [];
      selectedRows.value = [];
    }).catch(error => {
      ElMessage.error('批量删除失败：' + error.message);
    });
  }).catch(() => {
    ElMessage.info('已取消批量删除');
  });
};

// 批量删除知识分类
const batchDeleteKnowledgeSummaries = (ids) => {
  return new Promise((resolve, reject) => {
    fetch('http://localhost:8080/api/knowledgeSummary/batchDelete', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(ids)
    })
        .then(response => {
          if (!response.ok) {
            throw new Error('网络响应不正常');
          }
          return response.json();
        })
        .then(result => {
          if (result.success !== undefined && !result.success) {
            throw new Error(result.message || '批量删除失败');
          }
          resolve();
        })
        .catch(error => {
          reject(error);
        });
  });
};

// 编辑操作
const handleEdit = (row) => {
  // 打开编辑对话框前先获取数据
  fetchKnowledgeSummaryById(row.id).then(data => {
    // 填充编辑表单
    form.value = {
      id: data.id,
      categoryLevel1: data.category_level1,
      categoryLevel2: data.category_level2,
      knowledge: data.knowledge,
      subject: data.subject,
      number: data.number,
      stage: data.stage
    };
    dialogVisible.value = true;
  }).catch(error => {
    ElMessage.error('获取编辑数据失败：' + error.message);
  });
};

// 根据ID获取知识分类详情
const fetchKnowledgeSummaryById = (id) => {
  return new Promise((resolve, reject) => {
    fetch(`http://localhost:8080/api/knowledgeSummary/${id}`)
        .then(response => {
          if (!response.ok) {
            throw new Error('网络响应不正常');
          }
          return response.json();
        })
        .then(data => {
          resolve(data);
        })
        .catch(error => {
          reject(error);
        });
  });
};

// 新增按钮点击事件
const handleAdd = () => {
  // 重置表单
  resetForm();
  // 设置为新增模式
  isAdding.value = true;
  // 打开对话框
  dialogVisible.value = true;
};

// 重置表单
const resetForm = () => {
  form.value = {
    id: null,
    categoryLevel1: '',
    categoryLevel2: '',
    knowledge: '',
    subject: '',
    number: 0,
    stage: ''
  };
  if (formRef.value) {
    formRef.value.resetFields();
  }
};

// 提交表单（新增或编辑）
const submitForm = () => {
  if (!formRef.value) return;

  formRef.value.validate((valid) => {
    if (valid) {
      // 准备提交数据
      const formData = {
        id: form.value.id,
        category_level1: form.value.categoryLevel1,
        category_level2: form.value.categoryLevel2,
        knowledge: form.value.knowledge,
        subject: form.value.subject,
        number: form.value.number,
        stage: form.value.stage
      };

      // 调用后端接口（新增或更新）
      saveKnowledgeSummary(formData).then(() => {
        ElMessage.success(isAdding.value ? '新增成功' : '修改成功');
        dialogVisible.value = false;
        fetchTableData(); // 刷新表格
      }).catch(error => {
        ElMessage.error(isAdding.value ? '新增失败' : '修改失败：' + error.message);
      });
    } else {
      console.log('表单验证失败');
      return false;
    }
  });
};

// 保存知识分类（新增或更新）
const saveKnowledgeSummary = (data) => {
  return new Promise((resolve, reject) => {
    fetch('http://localhost:8080/api/knowledgeSummary/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
        .then(response => {
          if (!response.ok) {
            throw new Error('网络响应不正常');
          }
          return response.json();
        })
        .then(result => {
          if (result.success !== undefined && !result.success) {
            throw new Error(result.message || (isAdding.value ? '新增失败' : '更新失败'));
          }
          resolve();
        })
        .catch(error => {
          reject(error);
        });
  });
};

// 关闭对话框
const handleDialogClose = () => {
  dialogVisible.value = false;
};


// 衍题对话框相关变量
const yantiDialogVisible = ref(false);
const yantiFormRef = ref(null); // 表单引用
const yantiForm = ref({
  id: null,
  number: 0,
  markLevel: '' // 新增：标注级别选择
});
const currentRow = ref(null);
const yantiFormRules = reactive({
  number: [
    { required: true, message: '请输入题目数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) {
          return callback(new Error('请输入题目数量'));
        }
        const num = Number(value);
        if (isNaN(num)) {
          callback(new Error('请输入有效数字'));
        } else if (num <= 0) {
          callback(new Error('题目数量必须大于0'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ],
  markLevel: [
    { required: false, message: '请选择标注级别', trigger: 'change' }
  ]
});



// 点击衍题按钮 - 弹出数量输入对话框
const handleYanti = (row) => {
  currentRow.value = row; // 保存当前行引用
  yantiForm.value = {
    id: row.id,
    number: row.number,
    markLevel: '' // 初始化标注级别选择
  };
  yantiDialogVisible.value = true;
};

// 确认衍题数量并跳转（不更新数据库）
// 确认衍题数量并跳转
const confirmYanti = () => {
  yantiFormRef.value.validate((valid) => {
    if (valid && currentRow.value) {
      // 1. 直接修改当前行的number值（仅前端）
      currentRow.value.number = yantiForm.value.number;

      // 2. 准备传递的数据
      const dataToPass = {
        id: currentRow.value.id,
        categoryLevel1: currentRow.value.categoryLevel1,
        categoryLevel2: currentRow.value.categoryLevel2,
        knowledge: currentRow.value.knowledge,
        subject: currentRow.value.subject,
        number: currentRow.value.number,
        stage: currentRow.value.stage,
        markLevel: yantiForm.value.markLevel // 新增：传递选择的标注级别
      };

      // 3. 跳转到题目生成页面
      router.push({
        path: '/generate-questions',
        query: {
          data: JSON.stringify(dataToPass)
        }
      });

      // 关闭对话框
      yantiDialogVisible.value = false;
    }
  });
};




// 查看详情
const handleXiangQing = (row) => {
  // 准备传递的数据
  const detailData = {
    id: row.id,
    categoryLevel1: row.categoryLevel1,
    categoryLevel2: row.categoryLevel2,
    knowledge: row.knowledge,
    subject: row.subject,
    number: row.number,
    stage: row.stage
  };

  // 跳转到题目详情页面
  router.push({
    path: '/question-detail',
    query: {
      data: JSON.stringify(detailData)
    }
  });
};








</script>

<style scoped>
/*顶部样式*/
.knowledge-container {
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

.main-card {
  height: 100%;
}

.card-header {
  text-align: center;
  background-color: #84aee8;
  padding: 10px;
}

/*主内容样式样式*/
.card-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* Excel上传区域样式 */
.excel-upload-area {
  margin: 30px 0;
  text-align: center;
}






.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 0 20px;
}
.total-count {
  color: #666;
}



/* 终极方案：强制修改表头单元格背景色 */
::v-deep .el-table__header th {
  background-color: #4a90e2 !important; /* 青绿色示例 */
  color: #fff !important;
  font-size: 16px;
}

.batch-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

/* 查询区域样式 */
.query-area {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.batch-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 衍题对话框样式 */
.yanti-dialog .el-input-number {
  width: 100%;
}
.yanti-dialog .el-form-item__label {
  font-weight: bold;
}
</style>