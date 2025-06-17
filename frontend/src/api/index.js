import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const getImageUrl =(id) =>  {
  return `http://localhost:8080/api/pdf-pages/image/${id}`;
}

// 上传合并后的图片
export const uploadMergedImage = async (base64Image) => {
  const res = await apiClient.post('/merged-images', { image: base64Image })
  return res.data
}

// 获取合并图片数据
export const getMergedImageById = async (id) => {
  const res = await apiClient.get(`/merged-image/${id}`)
  return res.data
}

export const searchDocuments = async (keyword) => {
  try {
    const response = await api.get('/pdf/search', { params: { keyword } });
    return response.data;
  } catch (error) {
    console.error('搜索失败:', error);
    return [];
  }
};
export const getDocuments = async () => {
  try {
    const response = await api.get('/pdf');
    return response.data;
  } catch (error) {
    console.error('获取文档列表失败:', error);
    return [];
  }
};

export const saveCroppedImage = async (base64, isbn, filePath, bookPage, filename) => {
  const res = await apiClient.post('/save-cropped-image', {
    base64,
    isbn,
    filePath,
    bookPage,
    filename
  })

  return res.data
}

export default api 