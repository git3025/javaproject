<template>
  <div class="generate-container">
    <div class="welcome-card">
      <el-card class="main-card">
        <template #header>
          <div class="card-header">
            <h2>知识点衍题页面</h2>
          </div>
        </template>

        <div class="card-content">
          <h3 class="page-title">题目生成页面</h3>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading">
            <el-spinner size="large" />
            <p class="loading-text">正在生成题目...请稍候</p>
          </div>

          <!-- 数据预览与题目展示 -->
          <div v-else-if="receivedData" class="data-preview">
            <div class="info-card">
              <h4 class="info-title">分类信息</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">知识点：</span>
                  <span class="info-value">
                    <span class="info-value">{{ getSelectedKnowledge }}</span>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">学科：</span>
                  <span class="info-value">{{ receivedData.subject }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">学段：</span>
                  <span class="info-value">{{ receivedData.stage }}</span>
                </div>
                <!-- 在info-item中修改题目数量显示 -->
                <div class="info-item">
                  <span class="info-label">题目数量：</span>
                  <span class="info-value">{{ totalQuestionCount }}</span>
                </div>

              </div>
              <div class="action-buttons">
                <el-button type="primary" @click="saveAllQuestions" :loading="isSaving">保存所有修改</el-button>
                <el-button @click="resetAllQuestions">重置所有修改</el-button>
                <el-button type="success" @click="showAddQuestionDialog">
                  <el-icon><Plus /></el-icon> 新增题目
                </el-button>
              </div>
            </div>

            <!-- 题目展示区域 -->
            <div v-if="questionsLoaded" class="questions-container">
              <!-- 题目类型切换标签 -->
              <div class="question-type-tabs">
                <el-tabs v-model="activeQuestionType" @tab-change="handleTypeTabChange">
                  <el-tab-pane v-if="responseData.choice && responseData.choice.length > 0"
                               label="选择题" name="choice">
                    <template #label>
                      <el-badge :value="responseData.choice.length" :max="99" class="tab-badge">
                        选择题
                      </el-badge>
                    </template>
                  </el-tab-pane>

                  <el-tab-pane v-if="responseData.fill && responseData.fill.length > 0"
                               label="填空题" name="fill">
                    <template #label>
                      <el-badge :value="responseData.fill.length" :max="99" class="tab-badge">
                        填空题
                      </el-badge>
                    </template>
                  </el-tab-pane>

                  <el-tab-pane v-if="responseData.judge && responseData.judge.length > 0"
                               label="判断题" name="judge">
                    <template #label>
                      <el-badge :value="responseData.judge.length" :max="99" class="tab-badge">
                        判断题
                      </el-badge>
                    </template>
                  </el-tab-pane>

                  <el-tab-pane v-if="responseData.comprehensive && responseData.comprehensive.length > 0"
                               label="综合体" name="comprehensive">
                    <template #label>
                      <el-badge :value="responseData.comprehensive.length" :max="99" class="tab-badge">
                        综合体
                      </el-badge>
                    </template>
                  </el-tab-pane>
                </el-tabs>

                <!-- 分页控件 -->
                <div class="pagination-container">
                  <el-pagination
                      v-if="currentQuestions.length > 0"
                      background
                      layout="prev, pager, next, sizes, total"
                      :total="currentQuestions.length"
                      :page-size="pageSize"
                      :current-page="currentPage"
                      @current-change="handlePageChange"
                      @size-change="handleSizeChange"
                      :page-sizes="[5, 10, 20, 50]"
                  />
                </div>
              </div>

              <!-- 当前显示的题目列表 -->
              <div v-if="paginatedQuestions.length > 0">
                <!-- 选择题 -->
                <div v-if="activeQuestionType === 'choice'" class="question-type">
                  <div class="question-type-header">
                    <el-tag type="success" size="large">选择题</el-tag>
                    <span class="question-count">共 {{ responseData.choice.length }} 题</span>
                  </div>

                  <!-- 遍历当前页的选择题 -->
                  <div v-for="(item, index) in paginatedQuestions" :key="'choice-'+getQuestionIndex(item)"
                       class="question-item">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <div class="question-actions">
                        <el-button size="small" @click="toggleEdit('choice', getQuestionIndex(item)-1)" v-if="!item.editing">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-button>
                        <el-button size="small" type="success" @click="saveQuestion('choice', getQuestionIndex(item)-1)" v-else>
                          <el-icon><Check /></el-icon> 保存
                        </el-button>
                        <el-button size="small" @click="resetQuestion('choice', getQuestionIndex(item)-1)" v-if="item.editing">
                          <el-icon><Refresh /></el-icon> 取消
                        </el-button>
                        <el-button
                            size="small"
                            type="danger"
                            @click="confirmDeleteQuestion(item.id, activeQuestionType, getQuestionIndex(item)-1)"
                            :icon="Delete"
                            v-if="!item.editing"
                        >
                          删除
                        </el-button>
                      </div>
                    </div>

                    <div v-if="!item.editing">
                      <p class="question-content">{{ item.question }}</p>
                      <div class="options-container" v-if="item.option && item.option.length > 0">
                        <template v-for="(option, optIndex) in item.option" :key="'choice-opt-'+optIndex">
                          <div class="option-item"
                               :class="{'correct-option': isCorrectOption(option, item.answer)}">
                            <span class="option-letter">{{ getOptionLabel(option) }}.</span>
                            <span class="option-text">{{ getOptionText(option) }}</span>
                          </div>
                        </template>
                      </div>
                    </div>

                    <div v-else>
                      <el-input v-model="item.editedQuestion" class="edit-question-input" />
                      <div class="edit-options-container">
                        <div v-for="(option, optIndex) in item.editedOptions" :key="'edit-opt-'+optIndex" class="edit-option-item">
                          <el-input v-model="item.editedOptions[optIndex]" class="edit-option-input" />
                          <el-button
                              type="danger"
                              size="small"
                              circle
                              @click="removeOption('choice', getQuestionIndex(item)-1, optIndex)"
                              :icon="Delete"
                          />
                        </div>
                        <el-button @click="addOption('choice', getQuestionIndex(item)-1)" class="add-option-btn">
                          <el-icon><Plus /></el-icon> 添加选项
                        </el-button>
                      </div>
                      <div class="edit-answer-section">
                        <span class="answer-label">正确答案：</span>
                        <el-select v-model="item.editedAnswer" class="edit-answer-select">
                          <el-option
                              v-for="(option, optIndex) in item.editedOptions"
                              :key="'answer-opt-'+optIndex"
                              :label="getOptionLabel(option)"
                              :value="getOptionLabel(option)"
                          />
                        </el-select>
                      </div>
                    </div>

                    <div class="answer-section">
                      <p><strong>答案：</strong><span class="correct-answer">{{ item.answer }}</span></p>
                      <div class="analysis-section">
                        <p><strong>解析：</strong>{{ item.analysis }}</p>
                        <el-input
                            v-if="item.editing"
                            v-model="item.editedAnalyze"
                            type="textarea"
                            :rows="3"
                            placeholder="请输入题目解析"
                            class="edit-analyze-input"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 填空题 -->
                <div v-else-if="activeQuestionType === 'fill'" class="question-type">
                  <div class="question-type-header">
                    <el-tag type="primary" size="large">填空题</el-tag>
                    <span class="question-count">共 {{ responseData.fill.length }} 题</span>
                  </div>
                  <div v-for="(item, index) in paginatedQuestions" :key="'fill-'+getQuestionIndex(item)" class="question-item">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <div class="question-actions">
                        <el-button size="small" @click="toggleEdit('fill', getQuestionIndex(item)-1)" v-if="!item.editing">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-button>
                        <el-button size="small" type="success" @click="saveQuestion('fill', getQuestionIndex(item)-1)" v-else>
                          <el-icon><Check /></el-icon> 保存
                        </el-button>
                        <el-button size="small" @click="resetQuestion('fill', getQuestionIndex(item)-1)" v-if="item.editing">
                          <el-icon><Refresh /></el-icon> 取消
                        </el-button>
                        <el-button
                            size="small"
                            type="danger"
                            @click="confirmDeleteQuestion(item.id, activeQuestionType, getQuestionIndex(item)-1)"
                            :icon="Delete"
                            v-if="!item.editing"
                        >
                          删除
                        </el-button>
                      </div>
                    </div>

                    <div v-if="!item.editing">
                      <p class="question-content">{{ formatFillQuestion(item.question) }}</p>
                    </div>
                    <div v-else>
                      <el-input
                          v-model="item.editedQuestion"
                          type="textarea"
                          :rows="3"
                          class="edit-question-input"
                          placeholder="请输入填空题题目（使用_______表示填空位置）"
                      />
                    </div>

                    <div class="answer-section">
                      <p v-if="!item.editing"><strong>答案：</strong><span class="correct-answer">{{ item.answer }}</span></p>
                      <div v-else class="edit-answer-section">
                        <span class="answer-label">正确答案：</span>
                        <el-input v-model="item.editedAnswer" class="edit-answer-input" />
                      </div>
                      <div class="analysis-section">
                        <p v-if="!item.editing"><strong>解析：</strong>{{ item.analysis }}</p>
                        <el-input
                            v-else
                            v-model="item.editedAnalyze"
                            type="textarea"
                            :rows="3"
                            placeholder="请输入题目解析"
                            class="edit-analyze-input"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 判断题 -->
                <div v-else-if="activeQuestionType === 'judge'" class="question-type">
                  <div class="question-type-header">
                    <el-tag type="warning" size="large">判断题</el-tag>
                    <span class="question-count">共 {{ responseData.judge.length }} 题</span>
                  </div>
                  <div v-for="(item, index) in paginatedQuestions" :key="'judge-'+getQuestionIndex(item)" class="question-item">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <div class="question-actions">
                        <el-button size="small" @click="toggleEdit('judge', getQuestionIndex(item)-1)" v-if="!item.editing">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-button>
                        <el-button size="small" type="success" @click="saveQuestion('judge', getQuestionIndex(item)-1)" v-else>
                          <el-icon><Check /></el-icon> 保存
                        </el-button>
                        <el-button size="small" @click="resetQuestion('judge', getQuestionIndex(item)-1)" v-if="item.editing">
                          <el-icon><Refresh /></el-icon> 取消
                        </el-button>
                        <el-button
                            size="small"
                            type="danger"
                            @click="confirmDeleteQuestion(item.id, activeQuestionType, getQuestionIndex(item)-1)"
                            :icon="Delete"
                            v-if="!item.editing"
                        >
                          删除
                        </el-button>
                      </div>
                    </div>

                    <div v-if="!item.editing">
                      <p class="question-content">{{ item.question }}</p>
                    </div>
                    <div v-else>
                      <el-input
                          v-model="item.editedQuestion"
                          type="textarea"
                          :rows="3"
                          class="edit-question-input"
                          placeholder="请输入判断题题目"
                      />
                    </div>

                    <div class="answer-section">
                      <p v-if="!item.editing"><strong>答案：</strong>
                        <el-tag :type="item.answer === '正确' ? 'success' : 'danger'" size="small">
                          {{ item.answer }}
                        </el-tag>
                      </p>
                      <div v-else class="edit-answer-section">
                        <span class="answer-label">正确答案：</span>
                        <el-radio-group v-model="item.editedAnswer">
                          <el-radio label="正确">正确</el-radio>
                          <el-radio label="错误">错误</el-radio>
                        </el-radio-group>
                      </div>
                      <div class="analysis-section">
                        <p v-if="!item.editing"><strong>解析：</strong>{{ item.analysis }}</p>
                        <el-input
                            v-else
                            v-model="item.editedAnalyze"
                            type="textarea"
                            :rows="3"
                            placeholder="请输入题目解析"
                            class="edit-analyze-input"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 综合体 -->
                <div v-else-if="activeQuestionType === 'comprehensive'" class="question-type">
                  <div class="question-type-header">
                    <el-tag type="danger" size="large">综合体</el-tag>
                    <span class="question-count">共 {{ responseData.comprehensive.length }} 题</span>
                  </div>
                  <div v-for="(item, index) in paginatedQuestions" :key="'comp-'+getQuestionIndex(item)" class="question-item">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <div class="question-actions">
                        <el-button size="small" @click="toggleEdit('comprehensive', getQuestionIndex(item)-1)" v-if="!item.editing">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-button>
                        <el-button size="small" type="success" @click="saveQuestion('comprehensive', getQuestionIndex(item)-1)" v-else>
                          <el-icon><Check /></el-icon> 保存
                        </el-button>
                        <el-button size="small" @click="resetQuestion('comprehensive', getQuestionIndex(item)-1)" v-if="item.editing">
                          <el-icon><Refresh /></el-icon> 取消
                        </el-button>
                        <el-button
                            size="small"
                            type="danger"
                            @click="confirmDeleteQuestion(item.id, activeQuestionType, getQuestionIndex(item)-1)"
                            :icon="Delete"
                            v-if="!item.editing"
                        >
                          删除
                        </el-button>
                      </div>
                    </div>

                    <div v-if="!item.editing">
                      <p class="question-content">{{ item.question }}</p>
                    </div>
                    <div v-else>
                      <el-input
                          v-model="item.editedQuestion"
                          type="textarea"
                          :rows="4"
                          class="edit-question-input"
                          placeholder="请输入综合题题目"
                      />
                    </div>

                    <div class="answer-section">
                      <p v-if="!item.editing"><strong>答案：</strong><span class="correct-answer">{{ item.answer }}</span></p>
                      <div v-else class="edit-answer-section">
                        <span class="answer-label">正确答案：</span>
                        <el-input
                            v-model="item.editedAnswer"
                            type="textarea"
                            :rows="3"
                            class="edit-answer-input"
                            placeholder="请输入综合题答案"
                        />
                      </div>
                      <div class="analysis-section">
                        <p v-if="!item.editing"><strong>解析：</strong>{{ item.analysis }}</p>
                        <el-input
                            v-else
                            v-model="item.editedAnalyze"
                            type="textarea"
                            :rows="4"
                            placeholder="请输入题目解析"
                            class="edit-analyze-input"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 无数据提示 -->
              <div v-else class="no-questions">
                <el-empty description="当前题型暂无题目" />
              </div>
            </div>

            <!-- 加载失败提示 -->
            <div v-else-if="errorOccurred" class="error-message">
              <el-alert type="error" title="题目生成失败，请重试" show-icon />
              <el-button type="primary" @click="fetchQuestions" class="retry-btn">重试</el-button>
            </div>
          </div>

          <!-- 数据未加载提示 -->
          <div v-else class="no-data">
            <el-alert type="warning" title="未接收到分类数据" show-icon />
          </div>

          <!-- 在card-content底部添加新增题目对话框 -->
          <el-dialog v-model="addQuestionDialogVisible" title="新增题目" width="70%">
            <el-form :model="newQuestionForm" label-width="100px">
              <el-form-item label="题目类型" required>
                <el-select v-model="newQuestionForm.type" placeholder="请选择题目类型" @change="handleQuestionTypeChange">
                  <el-option label="选择题" value="choice" />
                  <el-option label="填空题" value="fill" />
                  <el-option label="判断题" value="judge" />
                  <el-option label="综合体" value="comprehensive" />
                </el-select>
              </el-form-item>

              <el-form-item label="难度" required>
                <el-select v-model="newQuestionForm.dificulty" placeholder="请选择难度">
                  <el-option label="简单" value="简单" />
                  <el-option label="中等" value="中等" />
                  <el-option label="困难" value="困难" />
                </el-select>
              </el-form-item>

              <el-form-item label="题目内容" required>
                <el-input
                    v-model="newQuestionForm.question"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入题目内容"
                />
              </el-form-item>

              <!-- 选择题选项 -->
              <div v-if="newQuestionForm.type === 'choice'">
                <el-form-item label="选项" required>
                  <div class="edit-options-container">
                    <div v-for="(option, index) in newQuestionForm.options" :key="index" class="edit-option-item">
                      <el-input v-model="newQuestionForm.options[index]" class="edit-option-input">
                        <template #prepend>{{ String.fromCharCode(65 + index) }}.</template>
                      </el-input>
                      <el-button
                          type="danger"
                          size="small"
                          circle
                          @click="removeNewOption(index)"
                          :icon="Delete"
                      />
                    </div>
                    <el-button @click="addNewOption" class="add-option-btn">
                      <el-icon><Plus /></el-icon> 添加选项
                    </el-button>
                  </div>
                </el-form-item>

                <el-form-item label="正确答案" required>
                  <el-select v-model="newQuestionForm.answer" placeholder="请选择正确答案">
                    <el-option
                        v-for="(option, index) in newQuestionForm.options"
                        :key="index"
                        :label="String.fromCharCode(65 + index)"
                        :value="String.fromCharCode(65 + index)"
                    />
                  </el-select>
                </el-form-item>
              </div>

              <!-- 填空题答案 -->
              <el-form-item v-if="newQuestionForm.type === 'fill'" label="正确答案" required>
                <el-input v-model="newQuestionForm.answer" placeholder="请输入填空题答案" />
              </el-form-item>

              <!-- 判断题答案 -->
              <el-form-item v-if="newQuestionForm.type === 'judge'" label="正确答案" required>
                <el-radio-group v-model="newQuestionForm.answer">
                  <el-radio label="正确">正确</el-radio>
                  <el-radio label="错误">错误</el-radio>
                </el-radio-group>
              </el-form-item>

              <!-- 综合体答案 -->
              <el-form-item v-if="newQuestionForm.type === 'comprehensive'" label="正确答案" required>
                <el-input
                    v-model="newQuestionForm.answer"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入综合题答案"
                />
              </el-form-item>

              <el-form-item label="题目解析">
                <el-input
                    v-model="newQuestionForm.analysis"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入题目解析"
                />
              </el-form-item>
            </el-form>

            <template #footer>
              <el-button @click="addQuestionDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="addNewQuestion">确认添加</el-button>
            </template>
          </el-dialog>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Edit, Check, Refresh, Delete, Plus } from '@element-plus/icons-vue';

const route = useRoute();
const receivedData = ref(null);
const isLoading = ref(false);
const isSaving = ref(false);
const questionsLoaded = ref(false);
const responseData = ref({});
const errorOccurred = ref(false);
const errorMessage = ref('');
const selectedMarkLevel = ref(''); // 标注级别选择

// 新增的分页和类型切换相关变量
const activeQuestionType = ref('choice'); // 当前激活的题目类型
const currentPage = ref(1); // 当前页码
const pageSize = ref(10); // 每页显示数量

// 计算属性：获取当前类型的题目列表
const currentQuestions = computed(() => {
  if (!responseData.value[activeQuestionType.value]) return [];
  return responseData.value[activeQuestionType.value];
});

// 计算属性：获取分页后的题目列表
const paginatedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return currentQuestions.value.slice(start, end);
});

// 计算属性：获取题目在完整列表中的索引（用于显示题号）
const getQuestionIndex = (question) => {
  return currentQuestions.value.findIndex(q => q.id === question.id) + 1;
};

// 处理题目类型切换
const handleTypeTabChange = (type) => {
  activeQuestionType.value = type;
  currentPage.value = 1; // 切换类型时重置页码
};

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page;
  // 滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

// 处理每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1; // 数量变化时重置页码
};


// 获取选项文本（去掉A. B.等前缀）
const getOptionText = (option) => {
  const parts = option.split('.');
  return parts.length > 1 ? parts.slice(1).join('.').trim() : option;
};

// 获取选项字母
const getOptionLabel = (option) => {
  return option.charAt(0).toUpperCase();
};

// 统一处理选项数据格式
const getFormattedOptions = (options) => {
  if (!options || options.length === 0) return [];

  // 如果是字符串数组（新增题目）
  if (Array.isArray(options)){
    // 检查是否已经是分开的选项（如 ['A. xxx', 'B. xxx']）
    if (options.some(opt => opt.match(/^[A-Z]\.\s.+/))) {
      return options;
    }
    // 如果是单个字符串（如 ['A. xxx B. xxx']）
    return splitOptions(options[0]);
  }

  // 如果是字符串（理论上不应该出现）
  return splitOptions(options);
};


// 新增方法：拆分选项字符串为数组
const splitOptions = (optionString) => {
  // 使用正则表达式匹配 A. xxx B. xxx 这种格式
  const regex = /([A-Z])\.\s*([^A-Z]*)/g;
  const options = [];
  let match;

  while ((match = regex.exec(optionString)) !== null) {
    options.push(`${match[1]}. ${match[2].trim()}`);
  }

  return options.length > 0 ? options : [optionString];
};

// 在setup()中添加以下变量和函数
const addQuestionDialogVisible = ref(false);
const newQuestionForm = ref({
  type: 'choice',
  dificulty: '简单',
  question: '',
  answer: '',
  analysis: '',
  options: ['', '']
});

const showAddQuestionDialog = () => {
  // 重置表单
  newQuestionForm.value = {
    type: 'choice',
    dificulty: '简单',
    question: '',
    answer: '',
    analysis: '',
    options: ['', '']
  };
  addQuestionDialogVisible.value = true;
};

const handleQuestionTypeChange = (type) => {
  // 切换题目类型时重置相关字段
  newQuestionForm.value.answer = '';
  if (type === 'choice') {
    newQuestionForm.value.options = ['', ''];
  }
};

const addNewOption = () => {
  newQuestionForm.value.options.push('');
};

const removeNewOption = (index) => {
  if (newQuestionForm.value.options.length > 2) {
    newQuestionForm.value.options.splice(index, 1);
  } else {
    ElMessage.warning('至少需要两个选项');
  }
};

const addNewQuestion = () => {
  // 验证表单
  if (!newQuestionForm.value.question) {
    ElMessage.warning('请输入题目内容');
    return;
  }

  if (!newQuestionForm.value.answer) {
    ElMessage.warning('请设置正确答案');
    return;
  }

  if (newQuestionForm.value.type === 'choice' && newQuestionForm.value.options.some(opt => !opt.trim())) {
    ElMessage.warning('请填写所有选项内容');
    return;
  }

  // 创建新题目对象
  const newQuestion = {
    question: newQuestionForm.value.question,
    answer: newQuestionForm.value.answer,
    analysis: newQuestionForm.value.analysis || '暂无解析',
    dificulty: newQuestionForm.value.dificulty,
    editing: false
  };

  // 处理选择题选项 - 确保格式统一
  if (newQuestionForm.value.type === 'choice') {
    newQuestion.option = newQuestionForm.value.options.map((opt, index) => {
      return `${String.fromCharCode(65 + index)}. ${opt}`;
    });
  }

  // 添加到对应题型列表
  if (!responseData.value[newQuestionForm.value.type]) {
    responseData.value[newQuestionForm.value.type] = [];
  }

  responseData.value[newQuestionForm.value.type].push(newQuestion);
  addQuestionDialogVisible.value = false;
  calculateTotalQuestions(); // 添加后更新计数
  ElMessage.success('题目添加成功');
};



// 修改选项格式化方法
const formatOptionContent = (option) => {
  // 处理选项字符串，确保正确分割
  const match = option.match(/^([A-Z])\.\s*(.*)/);
  return match ? match[2].trim() : option;
};



// 获取选项内容（去除A. B.等前缀）
const getOptionContent = (option) => {
  const parts = option.split('.');
  if (parts.length > 1) {
    return parts.slice(1).join('.').trim();
  }
  return option;
};

// 判断是否是正确答案选项
const isCorrectOption = (option, answer) => {
  return option.startsWith(answer.split('.')[0]);
};


// 格式化填空题题目
const formatFillQuestion = (question) => {
  return question.replace(/______/g, '_______');
};

// 根据难度返回不同的tag类型
const getDifficultyTagType = (difficulty) => {
  switch (difficulty) {
    case '简单': return '';
    case '中等': return 'info';
    case '困难': return 'danger';
    default: return '';
  }
};

// 切换编辑状态
const toggleEdit = (type, index) => {
  const question = responseData.value[type][index];
  question.editing = !question.editing;

  if (question.editing) {
    // 初始化编辑数据
    question.editedQuestion = question.question;
    question.editedAnswer = question.answer;
    question.editedAnalyze = question.analysis;

    // 选择题特殊处理选项
    if (type === 'choice') {
      question.editedOptions = Array.isArray(question.option)
          ? [...question.option]
          : question.option.split(' ').filter(opt => opt.trim());
    }
  }
};

// 保存题目修改
const saveQuestion = (type, index) => {
  const question = responseData.value[type][index];

  // 验证数据
  if (!question.editedQuestion || !question.editedAnswer) {
    ElMessage.warning('题目和答案不能为空');
    return;
  }

  if (type === 'choice' && (!question.editedOptions || question.editedOptions.length < 2)) {
    ElMessage.warning('选择题至少需要2个选项');
    return;
  }

  // 更新原始数据
  question.question = question.editedQuestion;
  question.answer = question.editedAnswer;
  question.analysis = question.editedAnalyze;

  if (type === 'choice') {
    question.option = [...question.editedOptions];
  }

  question.editing = false;
  calculateTotalQuestions(); // 保存后更新计数
  ElMessage.success('题目修改已保存');
};

// 重置题目修改
const resetQuestion = (type, index) => {
  const question = responseData.value[type][index];
  question.editing = false;
};

// 添加选择题选项
const addOption = (type, index) => {
  const question = responseData.value[type][index];
  const optionLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const nextLetter = optionLetters[question.editedOptions.length % optionLetters.length];
  question.editedOptions.push(`${nextLetter}. 新选项`);
};

// 删除选择题选项
const removeOption = (type, index, optIndex) => {
  const question = responseData.value[type][index];
  if (question.editedOptions.length <= 2) {
    ElMessage.warning('选择题至少需要2个选项');
    return;
  }
  question.editedOptions.splice(optIndex, 1);
  calculateTotalQuestions(); // 删除选项后可能影响题目存在性，更新计数
};

const saveAllQuestions = async () => {
  // 根据选择的标注级别优先使用对应字段
  let knowledgeValue = '';
  if (selectedMarkLevel.value === 'categoryLevel1') {
    knowledgeValue = receivedData.value.categoryLevel1;
  } else if (selectedMarkLevel.value === 'categoryLevel2') {
    knowledgeValue = receivedData.value.categoryLevel2;
  } else if (selectedMarkLevel.value === 'knowledge') {
    knowledgeValue = receivedData.value.knowledge;
  } else {
    // 未选择时使用原有逻辑
    knowledgeValue = receivedData.value.categoryLevel2;
    if (!knowledgeValue) knowledgeValue = receivedData.value.categoryLevel1;
  }

  // 准备要保存的完整数据结构
  const questionsToSave = {
    id: receivedData.value.id,
    knowledge: knowledgeValue,
    subject: receivedData.value.subject,
    stage: receivedData.value.stage,
    choice: prepareQuestions(responseData.value.choice),
    fill: prepareQuestions(responseData.value.fill),
    judge: prepareQuestions(responseData.value.judge),
    comprehensive: prepareQuestions(responseData.value.comprehensive)
  };

  // 使用函数声明替代箭头函数
  function prepareQuestions(questions) {
    if (!questions) return [];
    return questions.map(q => {
      const prepared = {
        difficulty: q.dificulty,
        question: q.question,
        answer: q.answer,
        analysis: q.analysis
      };
      // 处理选择题选项
      if (q.option && Array.isArray(q.option)) {
        prepared.option = q.option; // 直接使用原始选项，不做处理
      }
      return prepared;
    });
  }


  try {
    const response = await fetch('http://localhost:8080/api/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(questionsToSave)
    });
    const result = await response.json();
    if (response.ok) {
      ElMessage.success(result.message);
    } else {
      ElMessage.error(result.message);
    }
  } catch (error) {
    ElMessage.error('保存题目失败：' + error.message);
  }
};

// 重置所有修改
const resetAllQuestions = () => {
  ElMessageBox.confirm('确定要重置所有修改吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 重置所有编辑状态
    Object.keys(responseData.value).forEach(type => {
      if (Array.isArray(responseData.value[type])) {
        responseData.value[type].forEach(question => {
          if (question.editing) {
            question.editing = false;
          }
        });
      }
    });
    calculateTotalQuestions(); // 重置后更新计数
    ElMessage.success('已重置所有修改');
  }).catch(() => {});
};

// 确认删除题目
const confirmDeleteQuestion = (questionId, type, index) => {
  ElMessageBox.confirm('确定要删除这道题目吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    dangerouslyUseHTMLString: true,
    message: `
      <p>删除操作将<b style="color: red">永久删除</b>这道题目！</p>
      <p>请确认是否继续？</p>
    `
  }).then(() => {
    // 从对应类型的题目列表中删除该题目
    if (responseData.value[type] && Array.isArray(responseData.value[type])) {
      responseData.value[type].splice(index, 1);
      calculateTotalQuestions(); // 更新题目总数
      ElMessage.success('题目已删除');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};



// 在script setup中添加以下代码
const totalQuestionCount = ref(0);

// 计算总题目数量的函数
const calculateTotalQuestions = () => {
  let count = 0;
  if (responseData.value.choice) count += responseData.value.choice.length;
  if (responseData.value.fill) count += responseData.value.fill.length;
  if (responseData.value.judge) count += responseData.value.judge.length;
  if (responseData.value.comprehensive) count += responseData.value.comprehensive.length;
  totalQuestionCount.value = count;
};

// 在数据更新时触发计算
watch(responseData, () => {
  calculateTotalQuestions();
}, { deep: true });



// 获取题目数据
const fetchQuestions = async () => {
  if (!receivedData.value) return;

  isLoading.value = true;
  questionsLoaded.value = false;
  errorOccurred.value = false;
  errorMessage.value = '';

  try {
    // 根据选择的标注级别优先使用对应字段
    let knowledgeValue = '';
    if (selectedMarkLevel.value === 'categoryLevel1') {
      knowledgeValue = receivedData.value.categoryLevel1;
    } else if (selectedMarkLevel.value === 'categoryLevel2') {
      knowledgeValue = receivedData.value.categoryLevel2;
    } else if (selectedMarkLevel.value === 'knowledge') {
      knowledgeValue = receivedData.value.knowledge;
    } else {
      // 未选择时使用原有逻辑
      knowledgeValue = receivedData.value.knowledge;
      if (!knowledgeValue) knowledgeValue = receivedData.value.categoryLevel2;
      if (!knowledgeValue) knowledgeValue = receivedData.value.categoryLevel1;
    }

    console.log("knowledgeValue:")
    console.log(knowledgeValue)


    // 如果最终knowledgeValue仍然为空，抛出错误
    if (!knowledgeValue || knowledgeValue.trim() === '') {
      throw new Error('无法确定知识点：knowledge、category_level1和category_level2都为空');
    }

    const response = await fetch('http://localhost:8080/api/questions/generate', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        knowledge: knowledgeValue,  // 使用确定后的knowledge值
        stage: receivedData.value.stage,
        subject: receivedData.value.subject,
        number: receivedData.value.number,
      })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `请求失败，状态码: ${response.status}`);
    }

    const data = await response.json();

    // 统一格式化选择题选项
    if (data.choice) {
      data.choice = data.choice.map(question => {
        if (question.option && !Array.isArray(question.option[0])) {
          question.option = splitOptions(question.option);
        }
        return question;
      });
    }

    responseData.value = data;
    questionsLoaded.value = true;

    console.log("responseData.value:")

    console.log(responseData.value)

    console.log(questionsLoaded.value)

    // 初始化activeQuestionType为第一个有题目的类型
    const availableTypes = ['choice', 'fill', 'judge', 'comprehensive'].filter(
        type => responseData.value[type] && responseData.value[type].length > 0
    );

    if (availableTypes.length > 0) {
      activeQuestionType.value = availableTypes[0];
    }

    currentPage.value = 1; // 重置页码

  } catch (error) {
    console.error('获取题目数据失败:', error);
    errorOccurred.value = true;
    errorMessage.value = error.message;
    ElMessage.error('获取题目数据失败: ' + error.message);

    if (import.meta.env.DEV) {
      ElMessageBox.alert(
          `错误详情:\n${error.stack}`,
          '开发环境错误',
          { dangerouslyUseHTMLString: true }
      );
    }
  } finally {
    isLoading.value = false;
  }
};

// 计算属性：根据选择的标注级别获取知识点
const getSelectedKnowledge = computed(() => {
  if (!receivedData.value) return '未指定知识点';

  // 根据选择的标注级别优先获取对应字段
  if (selectedMarkLevel.value) {
    if (selectedMarkLevel.value === 'categoryLevel1' && receivedData.value.categoryLevel1) {
      return receivedData.value.categoryLevel1;
    } else if (selectedMarkLevel.value === 'categoryLevel2' && receivedData.value.categoryLevel2) {
      return receivedData.value.categoryLevel2;
    } else if (selectedMarkLevel.value === 'knowledge' && receivedData.value.knowledge) {
      return receivedData.value.knowledge;
    }
  }

  // 未选择标注级别时，使用原有优先级
  return receivedData.value.categoryLevel2 ||
      receivedData.value.categoryLevel1 ||
      '未指定知识点';
});

onMounted(() => {
  if (route.query.data) {
    try {
      receivedData.value = JSON.parse(decodeURIComponent(route.query.data));
      console.log("=============receivedData.value===============")
      console.log(receivedData.value)
      // 解析选择的标注级别
      selectedMarkLevel.value = receivedData.value.markLevel || '';
      console.log('选择的标注级别:', selectedMarkLevel.value);
      fetchQuestions();
      calculateTotalQuestions(); // 初始计算
    } catch (error) {
      console.error('数据解析失败:', error);
      ElMessage.error('数据解析失败: ' + error.message);
    }
  }
});
</script>

<style scoped>
.generate-container {
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
  border: none;
  box-shadow: none;
}

.card-header {
  text-align: center;
  background: linear-gradient(135deg, #4a90e2, #3a7bd5);
  padding: 15px 0;
  color: white;
}

.card-header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 500;
}

.card-content {
  padding: 25px;
}

.page-title {
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
  margin-bottom: 25px;
  font-size: 1.2rem;
  font-weight: 500;
}

.loading {
  text-align: center;
  padding: 60px 0;
}

.loading-text {
  margin-top: 15px;
  color: #666;
  font-size: 1rem;
}

.info-card {
  background-color: #f9fafc;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 25px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
}

.info-title {
  color: #444;
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 1.1rem;
  font-weight: 500;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.info-label {
  font-weight: bold;
  color: #666;
  min-width: 80px;
  font-size: 0.95rem;
}

.info-value {
  color: #333;
  font-size: 0.95rem;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.section-title {
  color: #444;
  margin: 30px 0 20px;
  font-size: 1.1rem;
  font-weight: 500;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background-color: #4a90e2;
  border-radius: 2px;
}

.question-type {
  margin-bottom: 35px;
}

.question-type-header {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #eaeaea;
}

.question-count {
  margin-left: 15px;
  color: #666;
  font-size: 0.9rem;
}

.question-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-left: 4px solid #4a90e2;
  transition: all 0.3s ease;
}

.question-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.question-meta {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.question-index {
  margin-left: 15px;
  color: #666;
  font-size: 0.85rem;
}

.question-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.question-content {
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  margin-bottom: 15px;
  padding: 8px 0;
}





.options-container {
  margin: 15px 0;
}

.option-item {
  padding: 10px 15px;
  margin-bottom: 8px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #ddd;
  transition: all 0.2s ease;
}

.option-item:hover {
  background-color: #f1f3f5;
}

.option-item.correct-option {
  background-color: #f0f9eb;
  border-left-color: #67c23a;
  color: #67c23a;
  font-weight: 500;
}

.options-container {
  margin: 15px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #ddd;
  transition: all 0.2s ease;
  line-height: 1.5;
}

.option-label {
  font-weight: bold;
  min-width: 24px;
  color: #666;
}

.option-content {
  flex: 1;
  white-space: normal;
  word-break: break-word;
}

/* 编辑模式下的选项样式 */
.edit-option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.edit-option-input {
  flex: 1;
}

/* 确保选项在编辑模式下也独占一行 */
.edit-options-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}



.options-container {
  margin: 15px 0;
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  margin-bottom: 8px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #ddd;
  width: 100%;
  box-sizing: border-box;
  line-height: 1.5;
  white-space: normal;
}

.option-letter {
  font-weight: bold;
  min-width: 20px;
  color: #666;
}

.option-text {
  flex: 1;
  word-break: break-word;
}

.option-item.correct-option {
  background-color: #f0f9eb;
  border-left-color: #67c23a;
  color: #67c23a;
}




.answer-section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px dashed #eee;
}

.correct-answer {
  color: #67c23a;
  font-weight: 500;
  padding: 2px 6px;
  background-color: #f0f9eb;
  border-radius: 4px;
}

.analysis-section {
  margin-top: 15px;
  padding: 12px 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #e6a23c;
}

/* 编辑相关样式 */
.edit-question-input {
  margin-bottom: 15px;
}

.edit-options-container {
  margin: 15px 0;
}

.edit-option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.edit-option-input {
  flex: 1;
}

.add-option-btn {
  margin-top: 10px;
  width: 100%;
}

.edit-answer-section {
  margin: 15px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.answer-label {
  font-weight: bold;
  color: #666;
  white-space: nowrap;
}

.edit-answer-select {
  min-width: 120px;
}

.edit-analyze-input {
  margin-top: 10px;
}

/* 错误提示 */
.error-message {
  margin: 20px 0;
}

.retry-btn {
  margin-top: 15px;
}

.no-data {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .generate-container {
    padding: 10px;
  }

  .card-content {
    padding: 15px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .question-actions {
    flex-direction: column;
    gap: 5px;
  }

  .edit-answer-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-buttons {
    flex-direction: column;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.question-item {
  animation: fadeIn 0.3s ease forwards;
}

.question-item:nth-child(odd) {
  animation-delay: 0.05s;
}

.question-item:nth-child(even) {
  animation-delay: 0.1s;
}


/* 新增题目对话框样式 */
.add-question-form {
  padding: 0 20px;
}

.add-question-form .el-form-item {
  margin-bottom: 22px;
}

.add-question-form .el-select {
  width: 100%;
}

/* 选项添加按钮 */
.add-option-btn {
  margin-top: 10px;
  width: 100%;
}

/* 选项容器 */
.edit-options-container {
  width: 100%;
}

.edit-option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.edit-option-input {
  flex: 1;
  margin-right: 10px;
}


/* 新增样式分页设置相关样式 */
.question-type-tabs {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.tab-badge {
  margin-right: 8px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.no-questions {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-top: 20px;
}

/* 调整题目索引显示 */
.question-index {
  margin-left: 15px;
  color: #666;
  font-size: 0.85rem;
  font-weight: bold;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .question-type-tabs {
    flex-direction: column;
  }

  .pagination-container {
    justify-content: center;
  }

  .el-tabs__nav {
    flex-wrap: wrap;
  }
}
</style>