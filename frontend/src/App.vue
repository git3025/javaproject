<template>
  <div id="app">
    <sidebar />
    <!-- 添加 router-view -->
    <router-view />
  </div>
</template>

<script>
import Sidebar from './components/Sidebar.vue';

export default {
  components: {
    Sidebar
  }
};
</script>

<style>
#app {
  display: flex;
}
</style>