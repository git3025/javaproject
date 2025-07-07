<template>
  <div class="sidebar">
    <ul>
      <li>
        <router-link :to="{ name: 'Home' }">PDF文档列表</router-link>
      </li>

      <li>
        <router-link :to="{ name: 'Placeholder1' }">书籍图片列表</router-link>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  data() {
    return {};
  }
};
</script>

<style scoped>
.sidebar {
  width: 250px; /* 增加宽度 */
  background-color: #6193da; /* 与主页背景色一致 */
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); /* 添加阴影效果 */
}

.sidebar ul {
  list-style-type: none;
  padding-left: 0;
}

.sidebar li {
  margin-bottom: 10px;
}

.sidebar a,
.sidebar router-link {
  text-decoration: none;
  color: #333; /* 字体颜色 */
  display: block;
  padding: 10px 16px;
  border-radius: 4px;
  background-color: #eaeff6; /* 链接背景色 */
  transition: background-color 0.3s; /* 平滑过渡效果 */
}

.sidebar a:hover,
.sidebar router-link:hover {
  background-color: #f0f8ff; /* 鼠标悬停时背景色 */
}

.sidebar .router-link-exact-active {
  background-color: #b0d0f3; /* 当前激活链接背景色 */
}
</style>