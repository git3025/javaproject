<template>
  <div class="generate-container">
    <div class="welcome-card">
      <el-card class="main-card">
        <template #header>
          <div class="card-header">
            <h2>题目详情页面</h2>
          </div>
        </template>

        <div class="card-content">
          <h3 class="page-title">题目详情</h3>

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
                  <div class="info-item">
                    <div class="knowledge-selector">
                      <span class="info-value">{{ selectedKnowledge }}</span>
                      <el-select v-model="selectedMarkLevel" @change="handleMarkLevelChange" class="mark-level-select">
                        <el-option label="一级标注" value="categoryLevel1" />
                        <el-option label="二级标注" value="categoryLevel2" />
                        <el-option label="三级标注" value="knowledge" />
                      </el-select>
                    </div>
                  </div>
                </div>
                <div class="info-item">
                  <span class="info-label">学科：</span>
                  <span class="info-value">{{ receivedData.subject }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">学段：</span>
                  <span class="info-value">{{ receivedData.stage }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">题目数量：</span>
                  <span class="info-value">{{ receivedData.number }}</span>
                </div>
              </div>
              <div class="action-buttons">
                <el-button @click="resetAllQuestions">重置所有修改</el-button>
                <el-button type="success" @click="showAddQuestionDialog">
                  <el-icon><Plus /></el-icon> 新增题目
                </el-button>
                <el-button type="info" @click="findDuplicateQuestions">
                  <el-icon><Search /></el-icon> 查找重复
                </el-button>
                <el-button
                    type="warning"
                    @click="executeRemoveDuplicates"
                    :disabled="!hasDuplicates"
                >
                  <el-icon><Delete /></el-icon> 执行去重
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
                       class="question-item" :class="{'duplicate': item.isDuplicate}">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <span v-if="item.isDuplicate" class="duplicate-marker">(重复)</span>
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
                        <el-button
                            v-if="item.isDuplicate && !item.editing"
                            size="small"
                            type="info"
                            @click="excludeDuplicate(item.id, activeQuestionType)"
                        >
                          <el-icon><Close /></el-icon> 取消去重
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
                  <div v-for="(item, index) in paginatedQuestions" :key="'fill-'+getQuestionIndex(item)" class="question-item" :class="{'duplicate': item.isDuplicate}">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <span v-if="item.isDuplicate" class="duplicate-marker">(重复)</span>
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
                  <div v-for="(item, index) in paginatedQuestions" :key="'judge-'+getQuestionIndex(item)" class="question-item" :class="{'duplicate': item.isDuplicate}">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <span v-if="item.isDuplicate" class="duplicate-marker">(重复)</span>
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
                  <div v-for="(item, index) in paginatedQuestions" :key="'comp-'+getQuestionIndex(item)" class="question-item" :class="{'duplicate': item.isDuplicate}">
                    <div class="question-meta">
                      <el-tag :type="getDifficultyTagType(item.dificulty)" size="small">{{ item.dificulty }}</el-tag>
                      <span class="question-index">第 {{ getQuestionIndex(item) }} 题</span>
                      <span v-if="item.isDuplicate" class="duplicate-marker">(重复)</span>
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
              <el-alert type="error" title="题目加载失败，请重试" show-icon />
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
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
// 导入Search图标
import { Edit, Check, Refresh, Delete, Plus, Search, Close } from '@element-plus/icons-vue';


// 添加状态变量
const hasDuplicates = ref(false);
const duplicateQuestions = ref([]);
const excludedDuplicates = ref([]); // 用户排除的重复题目


const route = useRoute();
const receivedData = ref(null);
const isLoading = ref(false);
const isSaving = ref(false);
const questionsLoaded = ref(false);
const responseData = ref({});
const errorOccurred = ref(false);
const errorMessage = ref('');

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


// 对于指定标签知识点的查看
// 新增：标注级别选择相关变量
const selectedMarkLevel = ref('categoryLevel2'); // 默认选择二级标注
// 新增初始化标注级别选择的函数
const initMarkLevelSelection = () => {
  if (!receivedData.value) return;

  // 如果二级标注有值，保持默认选择
  if (receivedData.value.categoryLevel2 && receivedData.value.categoryLevel2.trim() !== '') {
    selectedMarkLevel.value = 'categoryLevel2';
  }
  // 如果一级标注有值，选择一级标注
  else if (receivedData.value.categoryLevel1 && receivedData.value.categoryLevel1.trim() !== '') {
    selectedMarkLevel.value = 'categoryLevel1';
  }
  // 如果都没有值，保持默认选择（可能显示"未设置"）
  else {
    selectedMarkLevel.value = 'categoryLevel2';
  }
};

const selectedKnowledge = computed(() => {
  if (!receivedData.value) return '未指定知识点';

  // 根据选择的标注级别获取对应值
  let knowledgeValue = '';
  switch (selectedMarkLevel.value) {
    case 'categoryLevel1':
      knowledgeValue = receivedData.value.categoryLevel1;
      break;
    case 'categoryLevel2':
      knowledgeValue = receivedData.value.categoryLevel2;
      break;
    case 'knowledge':
      knowledgeValue = receivedData.value.knowledge;
      break;
    default:
      knowledgeValue = '';
  }

  return knowledgeValue || '未设置';
});

// 处理标注级别选择变化
const handleMarkLevelChange = () => {
  // 重新获取题目数据，使用新的标注级别
  fetchQuestions();
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


// 增强版选项解析函数：彻底清洗分隔符和多余字符
const splitOptions = (optionString) => {
  if (!optionString) return [];

  // 1. 统一替换/去除特殊分隔符（支持 | ; \n 等）
  let cleaned = optionString
      .replace(/[;\n]/g, '|')  // 其他分隔符转成 |
      .replace(/\|\|+/g, '|')  // 多个 | 合并成一个
      .replace(/^\||\|$/g, ''); // 去除首尾 |

  // 2. 按 | 分割成数组
  let options = cleaned.split('|');

  // 3. 清理每个选项的首尾空白和残留符号
  options = options.map(opt => {
    // 去除首尾空白、|、] 等残留字符
    return opt.trim().replace(/[|\]]/g, '');
  });

  // 4. 过滤空选项
  return options.filter(opt => opt.length > 0);
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

// 在script setup中添加新增题目函数
const addNewQuestion = async () => {
  // 验证表单
  if (!newQuestionForm.value.question) {
    ElMessage.warning('请输入题目内容');
    return;
  }

  if (!newQuestionForm.value.answer) {
    ElMessage.warning('请设置正确答案');
    return;
  }

  if (newQuestionForm.value.type === 'choice') {
    if (newQuestionForm.value.options.some(opt => !opt.trim())) {
      ElMessage.warning('请填写所有选项内容');
      return;
    }
    if (newQuestionForm.value.options.length < 2) {
      ElMessage.warning('选择题至少需要2个选项');
      return;
    }
  }

  // 构造要提交给后端的数据
  const newQuestionData = {
    knowledge: receivedData.value.knowledge || receivedData.value.categoryLevel2 || receivedData.value.categoryLevel1,
    subject: receivedData.value.subject,
    stage: receivedData.value.stage,
    type: newQuestionForm.value.type,
    difficulty: newQuestionForm.value.dificulty,
    questionText: newQuestionForm.value.question.trim(),
    answer: newQuestionForm.value.answer.trim(),
    analysis: newQuestionForm.value.analysis || null,
    // 处理选择题选项：转为|分隔的字符串
    options: newQuestionForm.value.type === 'choice'
        ? newQuestionForm.value.options.map(opt => opt.trim()).join('|')
        : null
  };

  console.log('新增题目数据:', newQuestionData);

  try {
    const response = await fetch(`http://localhost:8080/api/create`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(newQuestionData),
    });

    if (!response.ok) {
      const errorData = await response.json();
      console.error('新增题目失败:', errorData);
      ElMessage.error(`新增失败：${errorData.message || '未知错误'}`);
      throw new Error(`HTTP错误 ${response.status}: ${JSON.stringify(errorData)}`);
    }

    const result = await response.json();
    console.log('后端响应:', result);

    if (result.code === 200) {
      // 创建成功，获取新题目ID并添加到前端列表
      const newQuestion = {
        id: result.data, // 后端返回的新题目ID
        question: newQuestionForm.value.question.trim(),
        answer: newQuestionForm.value.answer.trim(),
        analysis: newQuestionForm.value.analysis || '暂无解析',
        dificulty: newQuestionForm.value.dificulty,
        editing: false,
        type: newQuestionForm.value.type
      };

      // 处理选择题选项
      if (newQuestionForm.value.type === 'choice') {
        newQuestion.option = newQuestionForm.value.options.map((opt, index) => {
          return `${String.fromCharCode(65 + index)}. ${opt.trim()}`;
        });
        newQuestion.options = newQuestionForm.value.options.map(opt => opt.trim()).join('|');
      }

      // 添加到对应题型列表
      if (!responseData.value[newQuestionForm.value.type]) {
        responseData.value[newQuestionForm.value.type] = [];
      }

      responseData.value[newQuestionForm.value.type].push(newQuestion);
      addQuestionDialogVisible.value = false;
      ElMessage.success('题目添加成功');
    } else {
      ElMessage.error(`新增失败：${result.message || '未知错误'}`);
    }
  } catch (error) {
    console.error('调用后端接口失败:', error);
    ElMessage.error('新增失败：' + error.message);
  }
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
const saveQuestion = async (type, index) => {
  const question = responseData.value[type][index];
  console.log("==================question====================");
  console.log(question);

  // 验证数据（包含选项格式）
  const editedQuestion = question.editedQuestion.trim();
  const editedAnswer = question.editedAnswer.trim();

  if (!editedQuestion) {
    ElMessage.warning('题目内容不能为空或仅包含空格');
    return;
  }
  if (!editedAnswer) {
    ElMessage.warning('答案不能为空或仅包含空格');
    return;
  }
  if (!question.dificulty) {
    ElMessage.warning('难度不能为空');
    return;
  }
  if (type === 'choice') {
    if (!question.editedOptions || question.editedOptions.length < 2) {
      ElMessage.warning('选择题至少需要2个选项');
      return;
    }
    if (question.editedOptions.some(option => !option.trim())) {
      ElMessage.warning('选项不能为空');
      return;
    }
  }

  // 构造要提交给后端的数据（options 转为字符串）
  const updatedQuestion = {
    id: Number(question.id),
    questionText: editedQuestion,
    answer: editedAnswer,
    analysis: question.editedAnalyze || null,
    difficulty: question.dificulty,
    type: type,
    options: type === 'choice'
        ? question.editedOptions.map(option => option.trim()).join('|') // 数组转字符串，用 | 分隔
        : null
  };


  console.log('发送的数据:', updatedQuestion);

  try {
    const response = await fetch(`http://localhost:8080/api/update`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(updatedQuestion),
    });

    if (!response.ok) {
      const errorData = await response.json();
      console.error('HTTP错误:', errorData);

      // 处理参数校验错误
      if (errorData.errors) {
        let errorMsg = '参数错误：';
        for (const [field, message] of Object.entries(errorData.errors)) {
          errorMsg += `${field}: ${message}; `;
        }
        ElMessage.error(errorMsg);
      } else {
        ElMessage.error(`保存失败：${errorData.message || '未知错误'}`);
      }

      throw new Error(`HTTP错误 ${response.status}: ${JSON.stringify(errorData)}`);
    }

    const result = await response.json();
    console.log('后端响应:', result);

    if (result.code === 200) {
      // 更新成功，关闭编辑状态
      question.question = editedQuestion;
      question.answer = editedAnswer;
      question.analysis = question.editedAnalyze;
      question.difficulty = question.dificulty;
      if (type === 'choice') {
        question.option = [...question.editedOptions];
      }
      question.editing = false;
      ElMessage.success('题目修改已保存');
    } else {
      ElMessage.error(`保存失败：${result.message || '未知错误'}`);
    }
  } catch (error) {
    console.error('调用后端接口失败:', error);
    ElMessage.error('保存失败：' + error.message);
  }
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
    deleteQuestion(questionId, type, index);
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};

// 删除题目
const deleteQuestion = async (questionId, type, index) => {
  try {
    const response = await fetch(`http://localhost:8080/api/delete/${questionId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || '删除失败');
    }

    const result = await response.json();
    if (result.code === 200) {
      // 从前端列表中移除该题目
      responseData.value[type].splice(index, 1);
      ElMessage.success('题目删除成功');

      // 如果当前页没有题目了且不是第一页，则返回上一页
      if (paginatedQuestions.value.length === 0 && currentPage.value > 1) {
        currentPage.value -= 1;
      }
    } else {
      ElMessage.error(`删除失败：${result.message}`);
    }
  } catch (error) {
    console.error('删除题目失败:', error);
    ElMessage.error('删除失败: ' + error.message);
  }
};

// 查找重复题目
const findDuplicateQuestions = () => {
  try {
    // 重置状态
    resetDuplicateMarks();

    // 获取所有题目
    const allQuestions = [];
    Object.values(responseData.value).forEach(questions => {
      if (Array.isArray(questions)) {
        allQuestions.push(...questions.map(q => ({...q, type: questions === responseData.value.choice ? 'choice' :
              questions === responseData.value.fill ? 'fill' :
                  questions === responseData.value.judge ? 'judge' : 'comprehensive'})));
      }
    });

    // 找出重复题目
    const duplicates = findDuplicates(allQuestions);

    if (duplicates.length === 0) {
      ElMessage.success('没有找到重复题目');
      return;
    }

    // 标记重复题目
    duplicates.forEach(dup => {
      const questionList = responseData.value[dup.type];
      const index = questionList.findIndex(q => q.id === dup.id);
      if (index !== -1) {
        questionList[index].isDuplicate = true;
        questionList[index].duplicateGroup = dup.duplicateGroup;
      }
    });

    duplicateQuestions.value = duplicates;
    hasDuplicates.value = true;

    ElMessage.success(`发现 ${duplicates.length} 道重复题目，已高亮显示`);

  } catch (error) {
    console.error('查找重复题目失败:', error);
    ElMessage.error('查找重复题目失败: ' + error.message);
  }
};

// 重置重复标记
const resetDuplicateMarks = () => {
  Object.values(responseData.value).forEach(questions => {
    if (Array.isArray(questions)) {
      questions.forEach(q => {
        q.isDuplicate = false;
        q.duplicateGroup = null;
      });
    }
  });
  duplicateQuestions.value = [];
  excludedDuplicates.value = [];
  hasDuplicates.value = false;
};

// 增强版查找重复题目
const findDuplicates = (questions) => {
  const seen = new Map();
  const duplicates = [];
  let groupCounter = 1;

  questions.forEach(question => {
    // 创建题目内容的唯一标识
    let key;
    if (question.type === 'choice') {
      // 选择题: 题目+选项+答案
      const options = Array.isArray(question.option) ?
          question.option.join('|') :
          question.option;
      key = `${question.type}-${question.question}-${options}-${question.answer}`;
    } else {
      // 其他题型: 题目+答案
      key = `${question.type}-${question.question}-${question.answer}`;
    }

    if (seen.has(key)) {
      // 如果是第一个重复的题目，也要标记
      if (!seen.get(key).marked) {
        seen.get(key).duplicateGroup = groupCounter;
        duplicates.push({...seen.get(key)});
        seen.get(key).marked = true;
        groupCounter++;
      }
      // 标记当前题目为重复
      question.duplicateGroup = seen.get(key).duplicateGroup;
      duplicates.push({...question});
    } else {
      seen.set(key, {...question});
    }
  });

  return duplicates;
};

// 排除重复题目（用户取消去重）
const excludeDuplicate = (questionId, type) => {
  // 检查是否已经排除过
  if (excludedDuplicates.value.some(item => item.id === questionId && item.type === type)) {
    ElMessage.warning('该题目已取消去重');
    return;
  }

  // 添加到排除列表
  excludedDuplicates.value.push({ id: questionId, type });

  // 更新题目状态
  const questionList = responseData.value[type];
  const index = questionList.findIndex(q => q.id === questionId);

  if (index !== -1) {
    questionList[index].isDuplicate = false;
    ElMessage.success('已取消该题目的去重标记');
  } else {
    ElMessage.error('未找到对应题目');
  }
};

// 执行去重操作
const executeRemoveDuplicates = async () => {
  try {
    // 1. 按重复组整理题目
    const duplicateGroups = {};
    duplicateQuestions.value.forEach(question => {
      if (!excludedDuplicates.value.some(ex => ex.id === question.id && ex.type === question.type)) {
        const groupKey = `group-${question.duplicateGroup}`;
        if (!duplicateGroups[groupKey]) {
          duplicateGroups[groupKey] = [];
        }
        duplicateGroups[groupKey].push(question);
      }
    });

    // 2. 确定要删除的题目（每组保留第一道，删除其余的）
    const toRemove = [];
    Object.values(duplicateGroups).forEach(group => {
      if (group.length > 1) {
        // 保留第一道，其余的加入删除列表
        toRemove.push(...group.slice(1));
      }
    });

    if (toRemove.length === 0) {
      ElMessage.warning('没有需要删除的重复题目');
      return;
    }

    // 3. 确认对话框
    await ElMessageBox.confirm(
        `确定要删除 ${toRemove.length} 道重复题目吗？（每组重复题目将保留一道）`,
        '确认删除重复题目',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );

    // 4. 执行删除
    const deletePromises = toRemove.map(question =>
        fetch(`http://localhost:8080/api/delete/${question.id}`, {
          method: 'DELETE',
          headers: {'Content-Type': 'application/json'}
        })
    );

    const results = await Promise.allSettled(deletePromises);
    const failedDeletes = results.filter(result => result.status === 'rejected' || !result.value.ok);

    if (failedDeletes.length > 0) {
      throw new Error(`部分题目删除失败 (${failedDeletes.length}/${toRemove.length})`);
    }

    // 5. 从前端移除已删除的题目
    toRemove.forEach(question => {
      const questionList = responseData.value[question.type];
      const index = questionList.findIndex(q => q.id === question.id);
      if (index !== -1) {
        questionList.splice(index, 1);
      }
    });

    ElMessage.success(`成功删除 ${toRemove.length} 道重复题目，每组保留了一道题目`);
    resetDuplicateMarks();

  } catch (error) {
    if (error !== 'cancel') {
      console.error('去重操作失败:', error);
      ElMessage.error('去重操作失败: ' + error.message);
    }
  }
};

// 删除重复题目
const deleteDuplicateQuestions = async (duplicates) => {
  try {
    const deletePromises = duplicates.map(question => {
      return fetch(`http://localhost:8080/api/delete/${question.id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });

    const responses = await Promise.all(deletePromises);

    // 检查所有删除操作是否成功
    const failedDeletes = responses.filter(response => !response.ok);
    if (failedDeletes.length > 0) {
      throw new Error(`部分题目删除失败 (${failedDeletes.length}/${duplicates.length})`);
    }

    // 从前端移除重复题目
    duplicates.forEach(question => {
      if (excludedDuplicates.value.some(ex => ex.id === question.id && ex.type === question.type)) {
        return; // 跳过用户排除的题目
      }

      const questionList = responseData.value[question.type];
      const index = questionList.findIndex(q => q.id === question.id);
      if (index !== -1) {
        questionList.splice(index, 1);
      }
    });

  } catch (error) {
    console.error('删除重复题目失败:', error);
    throw error;
  }
};

// 获取题目数据
// 修改fetchQuestions函数，传递selectedMarkLevel参数, 添加对空knowledge的处理
const fetchQuestions = async () => {
  if (!receivedData.value) return;

  isLoading.value = true;
  questionsLoaded.value = false;
  errorOccurred.value = false;
  errorMessage.value = '';

  try {
    // 确定knowledge的值，根据选择的标注级别
    let knowledgeValue = '';
    switch (selectedMarkLevel.value) {
      case 'categoryLevel1':
        knowledgeValue = receivedData.value.categoryLevel1 || '';
        break;
      case 'categoryLevel2':
        knowledgeValue = receivedData.value.categoryLevel2 || '';
        break;
      case 'knowledge':
        knowledgeValue = receivedData.value.knowledge || '';
        break;
      default:
        // 处理无效的标注级别选择
        console.error('无效的标注级别选择:', selectedMarkLevel.value);
        knowledgeValue = receivedData.value.categoryLevel2 || receivedData.value.categoryLevel1 || '';
    }

    // 如果knowledge为空，给出提示
    if (!knowledgeValue || knowledgeValue.trim() === '') {
      throw new Error('所选标注级别下没有设置知识点');
    }

    // 构建请求参数
    const params = {
      knowledge: knowledgeValue,
      subject: receivedData.value.subject,
      stage: receivedData.value.stage,
      number: receivedData.value.number || 5 // 默认5道题
    };

    console.log("请求参数params：",params)

    // 将参数对象转换为URL查询字符串
    const queryString = new URLSearchParams(params).toString();

    // 构建完整的请求URL，包含查询参数
    const requestUrl = `http://localhost:8080/api/detail?${queryString}`;

    console.log("请求URL:", requestUrl);

    // 发送GET请求
    const response = await fetch(requestUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `请求失败，状态码: ${response.status}`);
    }

    const data = await response.json();
    console.log("原始响应数据:", JSON.parse(JSON.stringify(data))); // 打印原始数据结构，不含Proxy

    // 按type字段重组数据，匹配前端期望的格式
    const formattedData = {
      choice: [],
      fill: [],
      judge: [],
      comprehensive: []
    };

    // 记录转换过程中的问题
    const conversionIssues = {
      missingFields: [],
      optionFormatErrors: []
    };

    data.forEach((item, index) => {
      // 映射后端type到前端期望的键名
      let frontEndType;
      switch (item.type) {
        case 'choice':
          frontEndType = 'choice';
          break;
        case 'judge':
          frontEndType = 'judge';
          break;
        case 'fill':
          frontEndType = 'fill';
          break;
        case 'comprehensive':
          frontEndType = 'comprehensive';
          break;
        default:
          console.warn(`未知题型: ${item.type}`, item);
          conversionIssues.missingFields.push({
            index,
            issue: `未知题型 ${item.type}`
          });
          return; // 跳过未知类型
      }

      // 创建一个新对象，避免修改原始数据
      const formattedItem = {
        editing: false, // 添加编辑状态标记
        // 复制所有原始字段
        ...item,
        // 修复字段名不一致 (dificulty vs difficulty)
        dificulty: item.difficulty || item.dificulty || '中等'
      };

      // 验证必要字段
      if (!formattedItem.question && formattedItem.questionText) {
        formattedItem.question = formattedItem.questionText;
      }

      if (!formattedItem.question) {
        conversionIssues.missingFields.push({
          index,
          issue: '缺少question字段'
        });
        return; // 跳过无效题目
      }

      // 处理选择题选项格式
      if (frontEndType === 'choice') {
        if (!item.options && item.option) {
          formattedItem.option = Array.isArray(item.option)
              ? item.option
              : splitOptions(item.option);
        } else if (item.options) {
          formattedItem.option = splitOptions(item.options);
        } else {
          conversionIssues.optionFormatErrors.push({
            index,
            issue: '选择题缺少options/option字段'
          });
          formattedItem.option = []; // 确保有一个空数组
        }

        // 确保答案格式正确
        if (formattedItem.answer && typeof formattedItem.answer === 'string') {
          // 如果答案是类似 "B" 的格式，但选项是 ["A. 选项1", "B. 选项2"]
          // 确保答案与选项格式一致
          if (formattedItem.option.length > 0 && formattedItem.option[0].startsWith('A.')) {
            // 选项是 "A. 内容" 格式，答案只需要字母部分
            formattedItem.answer = formattedItem.answer.charAt(0).toUpperCase();
          }
        }
      }

      // 添加到对应题型数组
      formattedData[frontEndType].push(formattedItem);
    });

    // 打印转换过程中的问题
    if (conversionIssues.missingFields.length > 0 || conversionIssues.optionFormatErrors.length > 0) {
      console.warn("数据转换问题:", conversionIssues);
    }

    // 替换原responseData
    responseData.value = formattedData;
    console.log("格式化后的响应数据:", JSON.parse(JSON.stringify(responseData.value))); // 打印不含Proxy的数据

    questionsLoaded.value = true;



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

    // 开发环境下显示详细错误
    if (import.meta.env.DEV) {
      ElMessageBox.alert(
          `错误详情:\n${error.stack}`,
          '开发环境错误',
          { dangerouslyUseHTMLString: true }
      );
    }

    // 开发模式下使用模拟数据
    if (import.meta.env.DEV) {
      responseData.value = getMockQuestions(receivedData.value);
      questionsLoaded.value = true;
      errorOccurred.value = false;
      ElMessage.warning('使用模拟数据');
    }
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  if (route.query.data) {
    try {
      receivedData.value = JSON.parse(decodeURIComponent(route.query.data));
      console.log("=============receivedData.value===============")
      console.log(receivedData.value)
      // 初始化标注级别选择
      initMarkLevelSelection();
      fetchQuestions();
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
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 15px
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

 /* 知识点选择下拉框样式设置 */
.knowledge-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mark-level-select {
  width: 120px;
  margin-left: 5px;
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


/* 删除按钮样式 */
.question-actions .el-button--danger {
  margin-left: 8px;
}

/* 响应式调整删除按钮 */
@media (max-width: 768px) {
  .question-actions .el-button--danger {
    margin-left: 0;
    margin-top: 5px;
  }
}


/* 添加重复题目高亮样式 */
.question-item.duplicate {
  border-left: 4px solid #e6a23c;
  background-color: #fdf6ec;
}

.duplicate-marker {
  margin-left: 10px;
  color: #e6a23c;
  font-size: 0.8rem;
  font-weight: bold;
}


/* 在 style 部分添加 排除去重按钮 */
.question-actions .el-button--info {
  margin-left: 8px;
  background-color: #f4f4f5;
  border-color: #d3d4d6;
  color: #909399;
}

.question-actions .el-button--info:hover {
  background-color: #e9e9eb;
}
</style>