<template>
  <div class="full-page-container">
    <el-button type="primary" @click="goBack">返回</el-button>
    <el-button :type="isDrawingEnabled ? 'info' : 'success'" @click="toggleDrawingMode">
      {{ isDrawingEnabled ? '正在等待框选...' : '开启框选' }}
    </el-button>

    <el-button type="primary" @click="confirmCrop">确认切割</el-button>

    <el-button type="warning" @click="mergeSelectedBoxes">合并选中区域</el-button>

    <!-- 删除按钮 -->
    <el-button type="danger" @click="deleteSelectedBoxes">删除选中框选</el-button>

    <div class="image-gallery">
      <div
          v-for="(url, index) in imageUrls"
          :key="index"
          class="image-wrapper"
          :ref="(el) => setImageWrapperRef(el, index)"
      >
        <img
            :src="url"
            style="max-width: 800px; height: auto;"
            @mousedown="startDrawing(index)"
            @mousemove="drawBox(index)"
            @mouseup="endDrawing(index)"
            @mouseleave="endDrawing(index)"
        />
        <!-- 当前绘制的矩形 -->
        <div
            class="selection-box"
            :ref="(el) => setSelectionBoxRef(el, index)"
        ></div>
        <!-- 在 .saved-selection 内部添加可拖拽的角 -->
        <div
            v-for="(box, idx) in selections[index]"
            :key="`${index}-${idx}`"
            class="saved-selection"
            :class="{ selected: isSelected(index, idx) }"
            :style="{
      left: `${box.x}px`,
      top: `${box.y}px`,
      width: `${box.width}px`,
      height: `${box.height}px`,
      transform: 'translateZ(0)' /* 启用硬件加速 */
    }"
            @click.stop="toggleSelection(index, idx)"
        >
          <!-- 四个可拖拽调整的角 -->
          <div class="resize-handle resize-top-left" @mousedown.stop="startResize($event, index, idx, 'top-left')"></div>
          <div class="resize-handle resize-top-right" @mousedown.stop="startResize($event, index, idx, 'top-right')"></div>
          <div class="resize-handle resize-bottom-left" @mousedown.stop="startResize($event, index, idx, 'bottom-left')"></div>
          <div class="resize-handle resize-bottom-right" @mousedown.stop="startResize($event, index, idx, 'bottom-right')"></div>
        </div>


      </div>
    </div>
    <!-- 合并弹窗 -->
    <el-dialog title="选择要合并的区域" v-model="mergeDialogVisible" width="70%">
      <div class="merge-dialog-content">
        <div v-for="(item, idx) in allBoxes" :key="idx" class="merge-item">
          <input type="checkbox" v-model="item.selected" />
          <img :src="item.thumbnail" alt="Thumbnail" style="width: 100px;" />
        </div>
      </div>
      <template #footer>
        <el-button @click="mergeSelectedInDialog">确认合并</el-button>
      </template>
    </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getImageUrl, uploadMergedImage, saveCroppedImage } from '../api'
import html2canvas from 'html2canvas'
import axios from 'axios'


const startX = ref(0);
const startY = ref(0);
const drawing = ref(false);
const route = useRoute()
const router = useRouter()
//  goBack 方法
const goBack = () => {
  router.back()
}

// 新增：存储检测坐标信息
const detectionBoxes = ref([])





const imageUrls = ref([])
// 图片容器和框选区域的 DOM 引用
const imageWrappers = ref({})
const selectionBoxes = ref({})

// 新增合并相关状态
const mergedBoxes = ref([]); // 存储已合并图信息 { imageIndex, boxIndices: [], imagePath }
const tempMergedDir = "zc"; // 临时目录名

// 存储每个图片上的框选痕迹
const selections = ref([])


// 是否允许框选（每次点击按钮才允许一次）
const isDrawingEnabled = ref(false)


let isDragging = ref(false)

let isResizing = ref(false)
let resizeCorner = ref(null)


// 替换原 currentResizeBox 定义，初始化为对象而非 null
let currentResizeBox = ref({
  imageIndex: -1,
  boxIndex: -1,
  original: { x: 0, y: 0, width: 0, height: 0 }
});




const startDrag = (imageIndex, boxIndex) => {
  if (isResizing.value) return; // 防止拖拽和调整同时进行

  isDragging.value = true
  currentDragBox.value = { imageIndex, boxIndex }
  startX.value = event.clientX
  startY.value = event.clientY

  const box = selections.value[imageIndex][boxIndex]
  currentDragBox.value.original = { ...box }

  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', endDrag)
}

// 优化后的调整逻辑
const startResize = (event, imageIndex, boxIndex, corner) => {
  event.preventDefault();
  event.stopPropagation();

  if (isResizing.value) return;
  isResizing.value = true;

  // 获取当前框选的DOM元素
  const boxElement = event.target.closest('.saved-selection');
  if (!boxElement) return;

  // 记录初始状态
  currentResizeBox.value = {
    imageIndex,
    boxIndex,
    corner,
    startX: event.clientX,
    startY: event.clientY,
    original: { ...selections.value[imageIndex][boxIndex] },
    boxElement
  };

  // 添加样式类
  boxElement.classList.add('resizing');

  // 绑定事件
  document.addEventListener('mousemove', handleResizeMove);
  document.addEventListener('mouseup', endResize);
  document.addEventListener('mouseleave', endResize);
};



const handleResizeMove = (event) => {
  if (!isResizing.value || !currentResizeBox.value) return;

  const { imageIndex, boxIndex, corner, startX, startY, original, boxElement } = currentResizeBox.value;
  const dx = event.clientX - startX;
  const dy = event.clientY - startY;

  let { x, y, width, height } = original;

  // 根据调整角计算新尺寸
  switch (corner) {
    case 'top-left':
      x = Math.max(0, original.x + dx);
      y = Math.max(0, original.y + dy);
      width = Math.max(10, original.width - dx);
      height = Math.max(10, original.height - dy);
      break;
    case 'top-right':
      y = Math.max(0, original.y + dy);
      width = Math.max(10, original.width + dx);
      height = Math.max(10, original.height - dy);
      break;
    case 'bottom-left':
      x = Math.max(0, original.x + dx);
      width = Math.max(10, original.width - dx);
      height = Math.max(10, original.height + dy);
      break;
    case 'bottom-right':
      width = Math.max(10, original.width + dx);
      height = Math.max(10, original.height + dy);
      break;
  }

  // 限制在图片范围内
  const wrapper = imageWrappers.value[imageIndex];
  if (wrapper) {
    const img = wrapper.querySelector('img');
    if (img) {
      const imgRect = img.getBoundingClientRect();
      width = Math.min(width, imgRect.width - x);
      height = Math.min(height, imgRect.height - y);
    }
  }

  // 直接更新DOM元素样式（避免响应式更新导致的延迟）
  boxElement.style.left = `${x}px`;
  boxElement.style.top = `${y}px`;
  boxElement.style.width = `${width}px`;
  boxElement.style.height = `${height}px`;

  // 存储最新状态
  currentResizeBox.value.latest = { x, y, width, height };
};





const endResize = () => {
  if (!isResizing.value || !currentResizeBox.value) return;

  const { imageIndex, boxIndex, latest } = currentResizeBox.value;

  // 更新响应式数据
  if (latest) {
    selections.value[imageIndex][boxIndex] = { ...latest };
    selections.value[imageIndex] = [...selections.value[imageIndex]]; // 触发响应式更新
  }

  // 清除样式类
  const boxElement = currentResizeBox.value.boxElement;
  if (boxElement) {
    boxElement.classList.remove('resizing');
  }

  // 清理状态
  isResizing.value = false;
  currentResizeBox.value = null;

  // 移除事件监听
  document.removeEventListener('mousemove', handleResizeMove);
  document.removeEventListener('mouseup', endResize);
  document.removeEventListener('mouseleave', endResize);
};









// 辅助函数：获取框选 DOM
const getSavedSelectionEl = (imageIndex, boxIndex) => {
  const wrapper = imageWrappers.value[imageIndex];
  return wrapper?.querySelector(`[key="${imageIndex}-${boxIndex}"]`);
};


// 多选删除相关
const selectedBoxes = ref([]) // 格式：{ imageIndex: 0, boxIndex: 1 }

const forceUpdate = () => {
  selections.value = selections.value.map(boxes => [...boxes])
}

// 合并弹窗控制
const mergeDialogVisible = ref(false)
const allBoxes = ref([])

// 合并后的图片
const mergedImageBase64 = ref(null)

// 新增：渲染检测框到图片上
const renderDetectionBoxes = (imageIndex) => {
  const boxes = detectionBoxes.value[imageIndex]
  if (!boxes || boxes.length === 0) return

  const wrapper = imageWrappers.value[imageIndex]
  if (!wrapper) return

  const img = wrapper.querySelector('img')
  if (!img) return

  // 清空已有的检测框
  const existingBoxes = wrapper.querySelectorAll('.detection-box')
  existingBoxes.forEach(box => box.remove())

  // 创建新的检测框
  boxes.forEach((box, boxIndex) => {
    const detectionBox = document.createElement('div')
    detectionBox.className = 'detection-box'
    detectionBox.style.position = 'absolute'
    detectionBox.style.left = `${box.x}px`
    detectionBox.style.top = `${box.y}px`
    detectionBox.style.width = `${box.width}px`
    detectionBox.style.height = `${box.height}px`
    detectionBox.style.border = '2px solid green'
    detectionBox.style.opacity = '0.7'
    detectionBox.style.pointerEvents = 'none'
    detectionBox.style.zIndex = '4' // 位于用户绘制的框下方

    wrapper.appendChild(detectionBox)
  })
}




onMounted(async () => {
  const ids = route.query.ids?.split(',') || []
  if (ids.length === 0) {
    alert('没有选中图片')
    router.back()
    return
  }

  imageUrls.value = ids.map(id => getImageUrl(id))
  selections.value = Array(imageUrls.value.length).fill(null).map(() => [])
  selectedBoxes.value = []
  // 新增：读取所有图片的检测坐标
  await loadAllDetectionBoxes(ids)

})


// 新增：加载所有图片的检测坐标
const loadAllDetectionBoxes = async (ids) => {
  detectionBoxes.value = Array(ids.length).fill(null).map(() => [])

  for (let i = 0; i < ids.length; i++) {
    const imageId = ids[i]
    const imageInfoRes = await fetch(`http://localhost:8080/api/pdf-pages/images/${imageId}`);
    const imageInfo = await imageInfoRes.json();
    // 获取 objectDetection 的值
    const objectDetectionValue = imageInfo.objectDetection;
    console.log("objectDetection:", objectDetectionValue);  // 这将输出: 1
    if (objectDetectionValue==0){
      await loadDetectionBoxesForImage(i, imageId)
    }else {
      console.log(` isbn为：${imageInfo.isbn} 的第 ${imageInfo.bookPage} 未进行目标检测`);
    }

  }
}


// 新增：计算图片缩放比例（原始尺寸 / 显示尺寸）
const getImageScale = (imageIndex) => {
  const wrapper = imageWrappers.value[imageIndex];
  if (!wrapper) return { scaleX: 1, scaleY: 1 };
  const img = wrapper.querySelector('img');
  if (!img) return { scaleX: 1, scaleY: 1 };
  // 原始尺寸 / 显示尺寸
  return {
    scaleX: img.naturalWidth / img.clientWidth,
    scaleY: img.naturalHeight / img.clientHeight
  };
};




// 加载单张图片的检测坐标
const loadDetectionBoxesForImage = async (imageIndex, imageId) => {
  console.log("加载图片检测坐标:", imageId);
  try {
    const imageInfoRes = await fetch(`http://localhost:8080/api/pdf-pages/images/${imageId}`);
    const imageInfo = await imageInfoRes.json();
    const imagePath = `D:/pdf/${imageInfo.isbn}/page/${imageInfo.bookPage}.png`;
    const jsonPath = imagePath.replace('/page/', '/labelstxt/').replace('.png', '.json');
    const jsonContent = await readTxtFile(jsonPath);
    if (!jsonContent) return;
    const boxes = parseDetectionCoordinates(jsonContent);
    if (boxes.length > 0) {
      const { scaleX, scaleY } = getImageScale(imageIndex);
      // 转换为前端显示尺寸的坐标
      const scaledBoxes = boxes.map(box => ({
        x: box.x / scaleX,
        y: box.y / scaleY,
        width: box.width / scaleX,
        height: box.height / scaleY
      }));
      selections.value[imageIndex] = scaledBoxes;
      forceUpdate();
      console.log(`成功加载图片 ${imageId} 的 ${boxes.length} 个检测框`);
    }
  } catch (error) {
    console.error(`加载图片 ${imageId} 的检测坐标失败`, error);
  }
};



// 读取JSON文件内容
const readTxtFile = async (filePath) => {
  try {
    const response = await axios.get(
        `http://localhost:8080/api/files/read?path=${encodeURIComponent(filePath)}`,
        {
          responseType: 'text' // 确保返回文本类型
        }
    );

    console.log("文件内容:", response.data);
    return response.data;
  } catch (error) {
    console.warn(`无法读取坐标文件: ${filePath}`, error);
    return null;
  }
};


// 解析检测坐标数据（处理JSON格式）
const parseDetectionCoordinates = (txtContent) => {
  if (!txtContent) return [];
  try {
    const data = JSON.parse(txtContent);
    if (data && data.boxes) {
      return data.boxes;
    }
    return [];
  } catch (jsonError) {
    console.error("JSON解析失败:", jsonError);
    return [];
  }
};


// 新增：在图片上展示检测框
watch([imageUrls, detectionBoxes], () => {
  if (imageUrls.value.length === 0 || detectionBoxes.value.length === 0) return

  imageUrls.value.forEach((_, index) => {
    renderDetectionBoxes(index)
  })
})







// 设置图片容器引用
const setImageWrapperRef = (el, index) => {
  if (el) {
    imageWrappers.value[index] = el;
    console.log(`Set image wrapper ref for index ${index}`, el); // 调试输出
  }
};

// 设置矩形框引用
const setSelectionBoxRef = (el, index) => {
  if (el) {
    selectionBoxes.value[index] = el;
    console.log(`Set selection box ref for index ${index}`, el); // 调试输出
  }
};

// 开启/关闭框选模式
const toggleDrawingMode = () => {
  isDrawingEnabled.value = true
  drawing.value = false
}

// 开始绘制矩形（鼠标按下）
const startDrawing = (index) => {
  if (!isDrawingEnabled.value) return

  const wrapper = imageWrappers.value[index]
  const img = wrapper.querySelector('img')

  if (!img) {
    console.error(`Image not found for index ${index}`)
    return
  }

  const rect = img.getBoundingClientRect()

  drawing.value = true
  startX.value = event.clientX - rect.left
  startY.value = event.clientY - rect.top

  console.log(`[Start Drawing] Index: ${index}, StartX: ${startX.value}, StartY: ${startY.value}`)
}

// 绘制矩形过程（鼠标移动）
const drawBox = (index) => {
  if (!drawing.value || !isDrawingEnabled.value) return

  const wrapper = imageWrappers.value[index]
  const box = selectionBoxes.value[index]
  const img = wrapper.querySelector('img')

  if (!img || !box) {
    console.error(`Missing DOM elements for index ${index}`) // 调试输出
    return
  }

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

// 结束绘制并保存痕迹（鼠标释放）
const endDrawing = (index) => {
  if (!isDrawingEnabled.value) return;

  const wrapper = imageWrappers.value[index];
  const box = selectionBoxes.value[index];

  if (!drawing.value || !wrapper || !box)  {
    console.error(`Missing DOM elements in endDrawing for index ${index}`,{// 调试输出
      wrapper: !!wrapper,
      box: !!box
  });
    return;
  }

  const x = parseInt(box.style.left)
  const y = parseInt(box.style.top)
  const width = parseInt(box.style.width)
  const height = parseInt(box.style.height)

  if (!isNaN(x) && !isNaN(y) && !isNaN(width) && !isNaN(height)) {
    selections.value[index].push({ x, y, width, height })
  }

  // ✅ 清除绘制状态
  drawing.value = false;
  isDrawingEnabled.value = false;

  // ✅ 隐藏 .selection-box（即当前绘制的矩形）
  box.style.display = 'none';

  console.log(`[End Drawing] Index: ${index}, Box:`, { x, y, width, height })
}

















// 确认切割按钮方法
const confirmCrop = async () => {
  if (selectedBoxes.value.length === 0 && mergedBoxes.value.length === 0) {
    alert('请先选择框选区域或合并图');
    return;
  }

  const ids = route.query.ids.split(',');

  // 构建所有要裁剪的区域（含合并图）
  let allRegions = [...selectedBoxes.value];
  mergedBoxes.value.forEach(mergedBox => {
    allRegions.push({
      imageIndex: mergedBox.imageIndex,
      boxIndex: -1,
      isMerged: true,
      path: mergedBox.imagePath,
      x: mergedBox.firstX
    });
  });

// 按 x 坐标排序
  allRegions.sort((a, b) => {
    const aX = a.isMerged ? a.x : (selections.value[a.imageIndex]?.[a.boxIndex]?.x || 0);
    const bX = b.isMerged ? b.x : (selections.value[b.imageIndex]?.[b.boxIndex]?.x || 0);
    return aX - bX;
  });

// 分组处理每页
  const groupedByImage = allRegions.reduce((acc, item) => {
    const idx = item.imageIndex;
    if (!acc[idx]) acc[idx] = [];
    acc[idx].push(item);
    return acc;
  }, {});

  let globalIndex = 0;

  Object.keys(groupedByImage).forEach(imageIndex => {
    groupedByImage[imageIndex].sort((a, b) => {
      const aX = a.isMerged ? a.x : (selections.value[a.imageIndex]?.[a.boxIndex]?.x || 0);
      const bX = b.isMerged ? b.x : (selections.value[b.imageIndex]?.[b.boxIndex]?.x || 0);
      return aX - bX;
    });

    // 为每个元素分配全局编号
    groupedByImage[imageIndex].forEach(item => {
      item.globalIndex = globalIndex++;
    });
  });

  let Aisbn = 1;

  // 收集需要删除的框索引（按 imageIndex 分组，使用 Set 去重）
  const toDelete = new Map(); // key: imageIndex → value: Set(boxIndex[])

  // 遍历每一页进行处理
  for (let imageIndex in groupedByImage) {
    const items = groupedByImage[imageIndex];
    const imageId = ids[imageIndex];

    const imgEl = imageWrappers.value[imageIndex]?.querySelector('img');
    if (!imgEl) continue;

    const scaleX = imgEl.naturalWidth / imgEl.clientWidth;
    const scaleY = imgEl.naturalHeight / imgEl.clientHeight;

    const pageRes = await fetch(`http://localhost:8080/api/pdf-pages/${imageId}`);
    if (!pageRes.ok) {
      alert('Failed to fetch page data');
      return;
    }
    const pageData = await pageRes.json();
    console.log('Page Data:', pageData); // 调试输出

    const isbn = pageData.isbn;
    Aisbn = isbn;
    if (!isbn) {
      alert('ISBN not found in page data');
      return;
    }

    // 构建最终保存路径
    const questionDir = `D:/pdf/${isbn}/question/page${parseInt(imageIndex) + 1}`;
// 创建目标目录（如果不存在）
    try {
      await fetch(`http://localhost:8080/api/pdf-pages/create-dir?dir=${encodeURIComponent(questionDir)}`, {
        method: 'POST'
      });
    } catch (e) {
      alert('创建目标目录失败');
      return;
    }

    for (let i = 0; i < items.length; i++) {
      const item = items[i];

      if (item.isMerged) {
        const srcPath = item.path; // 来自 zc 目录的 merged_x.png
        const destPath = `${questionDir}/${i + 1}.png`;

        // 调用后端 copy-file 接口复制合并图
        const res = await fetch("http://localhost:8080/api/pdf-pages/copy-file", {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ srcPath, destPath })
        });

        if (!res.ok) {
          try {
            const data = await res.json(); // 先尝试解析为 JSON
            alert(`复制合并图失败：${data.error || '未知错误'}`);
          } catch (e) {
            const text = await res.text(); // 如果不是 JSON，改为读取纯文本
            alert(`复制合并图失败：${text}`);
          }
        }

        // 在此处检查是否为合并图并清空缓存
        if (item.isMerged) {
          // 清空合并缓存 & 删除 zc 文件夹
          mergedBoxes.value = [];
          await fetch(`http://localhost:8080/api/pdf-pages/clear-temp-merged?isbn=${Aisbn}`, {
            method: 'POST'
          });
        }

        continue;
      }

      const boxes = selections.value[item.imageIndex];

      if (!boxes || item.boxIndex >= boxes.length) {
        console.error('Box not found', item, { boxes });
        continue;
      }

      const box = boxes[item.boxIndex];
      if (!box) {
        console.error('Box not found', item);
        continue;
      }

      const x = Math.round(box.x * scaleX);
      const y = Math.round(box.y * scaleY);
      const width = Math.round(box.width * scaleX);
      const height = Math.round(box.height * scaleY);

      const order = item.globalIndex + 1; // 使用全局排序后的编号

      const params = new URLSearchParams({
        x: x.toString(),
        y: y.toString(),
        width: width.toString(),
        height: height.toString(),
        imageId: imageId,
        order: order.toString()
      });

      const url = `http://localhost:8080/api/pdf-pages/crop?${params.toString()}`;
      const res = await fetch(url);

      let data;
      try {
        data = await res.json(); // 尝试解析为 JSON
      } catch (e) {
        data = { error: await res.text() }; // 如果不是 JSON，转为 error 字段
      }

      if (!res.ok) {
        alert(`裁剪失败：${data.error || '未知错误'}`);
      } else {
        // 记录要删除的框选区域（自动去重）
        if (!toDelete.has(item.imageIndex)) {
          toDelete.set(item.imageIndex, new Set());
        }
        toDelete.get(item.imageIndex).add(item.boxIndex);
      }
    }

    // 所有该页的裁剪完成后统一删除对应框选痕迹（降序删除，防止索引错乱）
    toDelete.forEach((boxIndices, imageIndex) => {
      const boxes = selections.value[imageIndex];
      if (!boxes) return;

      // 将 Set 转成数组并降序排列，防止 splice 索引错乱
      const sortedIndices = [...boxIndices].sort((a, b) => b - a);
      sortedIndices.forEach(boxIndex => {
        if (boxIndex >= 0 && boxIndex < boxes.length) {
          boxes.splice(boxIndex, 1);
        }
      });

      // 更新响应式数组
      selections.value[imageIndex] = [...boxes];
    });

    forceUpdate(); // 强制刷新视图
  }

  // 清空合并缓存 & 删除 zc 文件夹
  mergedBoxes.value = [];
  await fetch(`http://localhost:8080/api/pdf-pages/clear-temp-merged?isbn=${Aisbn}`, {
    method: 'POST'
  });

  selectedBoxes.value = []; // 清空当前选中状态
  alert('所有图片已按顺序保存');
};



const uploadCroppedImage = async (imageDataUrl) => {
  const formData = new FormData();
  const blob = dataURItoBlob(imageDataUrl);
  formData.append("file", blob, "cropped.png");

  const response = await fetch("http://localhost:8080/api/pdf-pages/merge-cropped-images", {
    method: "POST",
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      images: imageDataUrls,
      baseName: selectedBoxes.value[0].boxIndex // 使用第一个框选区域的 boxIndex 作为基础名
    })
  });

  if (response.ok) {
    const result = await response.json();
    return result.filePath;
  } else {
    throw new Error("Upload failed");
  }
};

// 将 base64 转换为 Blob
function dataURItoBlob(dataURI) {
  const byteString = atob(dataURI.split(',')[1]);
  const mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];
  const ab = new ArrayBuffer(byteString.length);
  const ia = new Uint8Array(ab);
  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i);
  }
  return new Blob([ab], {type: mimeString});
}

const mergeSelectedBoxes = async () => {
  if (selectedBoxes.value.length < 2) {
    alert('请至少选择两个框选区域');
    return;
  }

  const imageDataUrls = [];

  for (const { imageIndex, boxIndex } of selectedBoxes.value) {
    const wrapper = imageWrappers.value[imageIndex];
    const box = selections.value[imageIndex][boxIndex];

    const target = wrapper.querySelector('img');

    const canvas = await html2canvas(target, {
      x: box.x,
      y: box.y,
      width: box.width,
      height: box.height,
      useCORS: true,
      backgroundColor: null
    });

    const imageDataUrl = canvas.toDataURL('image/png');
    imageDataUrls.push(imageDataUrl);
  }

  // 获取当前图像 ID 和 pageIndex
  const ids = route.query.ids.split(',');
  const imageIndex = selectedBoxes.value[0].imageIndex;
  const imageId = ids[imageIndex];

  // 发送请求进行合并
  const response = await fetch("http://localhost:8080/api/pdf-pages/merge-cropped-images", {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      images: imageDataUrls,
      baseName: selectedBoxes.value[0].boxIndex,
      selectedImageId: imageId,
      pageIndex: imageIndex + 1
    })
  });

  if (response.ok) {
    const result = await response.json();
    console.log(result.outputPath); // 输出文件路径

    // 保存合并图信息
    mergedBoxes.value.push({
      imageIndex,
      boxIndices: selectedBoxes.value.map(b => b.boxIndex),
      imagePath: result.outputPath,
      firstX: selections.value[imageIndex][selectedBoxes.value[0].boxIndex].x
    });

    // ✅ 完全重构痕迹清理逻辑 —— 支持跨页删除
    const updatedSelections = selections.value.map((boxes, idx) => {
      // 找出该页上有哪些框需要删除
      const toDeleteForThisPage = selectedBoxes.value
          .filter(sb => sb.imageIndex === idx)
          .map(sb => sb.boxIndex);

      // 去重（防止 splice 错误）
      const uniqueToDelete = [...new Set(toDeleteForThisPage)];

      // 如果该页没有要删除的痕迹，直接返回原数组
      if (uniqueToDelete.length === 0) return boxes;

      // 删除指定索引的痕迹
      return boxes.filter((_, boxIdx) => !uniqueToDelete.includes(boxIdx));
    });


    // 更新响应式数据
    selections.value = updatedSelections;
    selectedBoxes.value = [];

    // 强制更新视图
    forceUpdate();

    alert('合并成功！');
  } else {
    alert('合并失败，请重试');
  }
};




// 判断是否选中
const isSelected = (imageIndex, boxIndex) => {
  return selectedBoxes.value.some(b => b.imageIndex === imageIndex && b.boxIndex === boxIndex)
}


// 切换选中状态
const toggleSelection = (imageIndex, boxIndex) => {
  //console.log('toggleSelection called', {imageIndex, boxIndex}) // 调试输出

  const exists = selectedBoxes.value.some(
      b => b.imageIndex === imageIndex && b.boxIndex === boxIndex
  );

  if (exists) {
    selectedBoxes.value = selectedBoxes.value.filter(
        b => !(b.imageIndex === imageIndex && b.boxIndex === boxIndex)
    );
  } else {
    selectedBoxes.value.push({imageIndex, boxIndex});
  }

  // 打印当前选中痕迹的坐标
  //selectedBoxes.value.forEach(({imageIndex, boxIndex}) => {
  //const box = selections.value[imageIndex]?.[boxIndex];
  // if (box) {
  //console.log('当前选中痕迹坐标:', {
  //imageIndex,
  // boxIndex,
  // x: box.x,
  // y: box.y,
  // width: box.width,
  // height: box.height
  // });
  //  }
  // });
  console.log('当前选中痕迹:', selectedBoxes.value);
};


// 删除选中的多个框选痕迹
// 删除选中框的方法
const deleteSelectedBoxes = () => {
  if (selectedBoxes.value.length === 0) {
    alert('请至少选择一个框选区域');
    return;
  }

  console.log('开始删除选中痕迹...', selectedBoxes.value);

  selectedBoxes.value.forEach(({imageIndex, boxIndex}) => {
    const boxes = selections.value[imageIndex];

    if (!boxes || !Array.isArray(boxes)) {
      console.error(`Invalid boxes for image index ${imageIndex}`, boxes);
      return;
    }

    // 使用 filter 创建新数组，保证响应式更新
    selections.value[imageIndex] = boxes.filter((_, idx) => idx !== boxIndex);
  });

  // 强制更新整个 selections 数组（可选）
  selections.value = [...selections.value];

  // 清空选中状态
  selectedBoxes.value = [];

  console.log('当前所有痕迹:', selections.value);

  forceUpdate()
};

// 合并弹窗中的选中项
const mergeSelectedInDialog = async () => {
  if (selectedItems.length === 0) {
    alert('请至少选择一个区域')
    return;
  }

  const container = document.createElement('div')
  container.style.display = 'flex'
  container.style.flexWrap = 'wrap'
  container.style.gap = '10px'
  container.style.padding = '10px'
  container.style.backgroundColor = '#fff'

  // 创建临时 img 标签插入 container
  selectedItems.forEach(item => {
    const img = new Image()
    img.src = item.thumbnail
    img.style.width = 'auto'
    img.style.height = 'auto'
    img.style.marginRight = '10px'
    img.style.marginBottom = '10px'
    container.appendChild(img)
  })

  // 隐藏容器用于截图
  container.style.position = 'absolute'
  container.style.top = '-9999px'
  container.style.left = '-9999px'
  document.body.appendChild(container)

  // 合并截图
  const combinedCanvas = await html2canvas(container, {useCORS: true, scale: 2})
  document.body.removeChild(container)

  const mergedImage = combinedCanvas.toDataURL('image/png')
  mergedImageBase64.value = mergedImage

// 上传到后端
  try {
    const res = await uploadMergedImage(mergedImage)
    const mergedId = res.data.id

    // 跳转到 Placeholder2 并携带合并后的图片 ID
    router.push({path: '/placeholder2', query: {id: mergedId}})
  } catch (err) {
    console.error('上传失败:', err)
    alert('上传失败，请重试')
  }
  mergeDialogVisible.value = false

  // 清空选中状态
  selectedBoxes.value = [];

  forceUpdate();
};




</script>

<style scoped>
/* 新增检测框样式 */
.detection-box {
  position: absolute;
  border: 2px solid green;
  pointer-events: none;
  z-index: 4;
  opacity: 0.7;
}


img {
  max-width: 100%; /* 替代 max-width: 800px，让图片自适应容器 */
  height: auto;
  display: block; /* 消除底部间隙 */
}

.full-page-container {
  padding: 40px;
  background-color: #f9f9f9;
}


.image-gallery {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
}

.image-wrapper {
  position: relative;
  display: inline-block;
  /* 可选：限制容器大小，避免溢出 */
  max-width: 800px;
  overflow: hidden;
}

.selection-box {
  position: absolute;
  border: 2px solid red;
  pointer-events: none;
  top: 0;
  left: 0;
  display: none;
  z-index: 10;
}
.saved-selection {
  position: absolute;
  border: 2px solid red;
  pointer-events: auto;
  top: 0;
  left: 0;
  z-index: 5;
  cursor: move;
}
.resize-handle {
  position: absolute;
  width: 8px;
  height: 8px;
  background-color: blue;
  z-index: 10;
  cursor: nwse-resize;
}

.resize-top-left {
  top: -4px;
  left: -4px;
  cursor: nwse-resize;
}

.resize-top-right {
  top: -4px;
  right: -4px;
  cursor: nesw-resize;
}

.resize-bottom-left {
  bottom: -4px;
  left: -4px;
  cursor: nesw-resize;
}

.resize-bottom-right {
  bottom: -4px;
  right: -4px;
  cursor: nwse-resize;
}

.saved-selection.selected {
  border-color: blue;
  background-color: rgba(0, 0, 255, 0.1);
}

.merge-dialog-content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.merge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 10px;
}


/* 添加以下样式 */
.saved-selection {
  will-change: transform, width, height; /* 提示浏览器优化 */
  transform: translateZ(0); /* 启用硬件加速 */
  transition: none; /* 禁用不必要的过渡效果 */
}

.saved-selection.resizing {
  pointer-events: none; /* 防止调整过程中触发其他事件 */
}

.resize-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background-color: blue;
  z-index: 10;
  border-radius: 50%; /* 圆形更易点击 */
  transform: translateZ(0); /* 启用硬件加速 */
}
</style>