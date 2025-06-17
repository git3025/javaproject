<template>
  <div class="placeholder2">
    <h2>合并结果预览</h2>
    <div class="preview-area">
      <img :src="mergedImageUrl" alt="合并后的图片" style="max-width: 100%" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const mergedImageUrl = ref(null)

onMounted(async () => {
  const { id } = route.query
  if (!id) {
    mergedImageUrl.value = ''
    return
  }

  try {
    const res = await api.get(`/merged-image/${id}`)

    mergedImageUrl.value = `data:image/png;base64,${res.data.imageBase64}`
  } catch (err) {
    console.error('获取合并图片失败:', err)
    mergedImageUrl.value = ''
  }
})
</script>

<style scoped>
.placeholder2 {
  padding: 20px;
  text-align: center;
}

.preview-area {
  margin-top: 20px;
}
</style>