<template>
  <div class="pdf-split-container">
    <el-card>
      <template #header>
        <div class="header">
          <div class="left">
            <el-button @click="$router.push('/dashboard/pdfs')">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <h2>PDF 图片切割</h2>
          </div>
          <div class="right">
            <el-button type="primary" @click="startSplitting" :loading="splitting">
              开始切割
            </el-button>
          </div>
        </div>
      </template>

      <div>
        <!-- 其他组件内容 -->

        <!-- 新增的确认切割按钮 -->
        <button @click="confirmCrop">确认切割</button>
      </div>

      <div class="content">
        <div class="preview-section">
          <div class="preview-container">
            <img
              v-if="currentPage"
              :src="`${api.defaults.baseURL}/images/${currentPage.imagePath}`"
              alt="页面预览"
              class="preview-image"
            />
          </div>
          <div class="page-controls">
            <el-button-group>
              <el-button @click="prevPage" :disabled="currentPageIndex === 0">
                上一页
              </el-button>
              <el-button @click="nextPage" :disabled="currentPageIndex === totalPages - 1">
                下一页
              </el-button>
            </el-button-group>
            <span class="page-info">
              第 {{ currentPageIndex + 1 }} 页 / 共 {{ totalPages }} 页
            </span>
          </div>
        </div>

        <div class="split-options">
          <h3>切割选项</h3>
          <el-form :model="splitOptions" label-width="120px">
            <el-form-item label="切割模式">
              <el-radio-group v-model="splitOptions.mode">
                <el-radio label="auto">自动切割</el-radio>
                <el-radio label="manual">手动选择区域</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="输出格式">
              <el-select v-model="splitOptions.format">
                <el-option label="PNG" value="png" />
                <el-option label="JPEG" value="jpeg" />
              </el-select>
            </el-form-item>

            <el-form-item label="图片质量">
              <el-slider
                v-model="splitOptions.quality"
                :min="1"
                :max="100"
                :format-tooltip="value => `${value}%`"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  methods: {
    async confirmCrop() {
      const imagePath = 'your_image_path_here'; // 替换为实际图片路径
      const x = 100; // 框选区域起始X坐标
      const y = 100; // 框选区域起始Y坐标
      const width = 200; // 框选区域宽度
      const height = 200; // 框选区域高度

      try {
        const response = await fetch('http://localhost:8080/api/pdf-pages/crop', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            imagePath,
            x,
            y,
            width,
            height
          })
        });

        if (response.ok) {
          alert('图片切割成功！');
        } else {
          alert('图片切割失败，请重试。');
        }
      } catch (error) {
        console.error('请求失败:', error);
        alert('请求失败，请检查网络或服务状态。');
      }
    }
  }
}
</script>

<style scoped>
.pdf-split-container {
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
  gap: 16px;
}

.header h2 {
  margin: 0;
}

.content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.preview-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
}

.page-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.page-info {
  color: #606266;
}

.split-options {
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.split-options h3 {
  margin-top: 0;
  margin-bottom: 20px;
}
</style> 