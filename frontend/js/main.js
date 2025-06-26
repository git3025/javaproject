import Vue from 'vue'
import ElementUI from 'element-ui'
import locale from 'element-ui/lib/locale/lang/zh-CN'
import 'element-ui/lib/theme-chalk/index.css'

Vue.use(ElementUI, { locale })

// 创建Vue实例
new Vue({
    el: '#app',
    data: {
        questions: [],
        selectedQuestion: null,
        searchKeyword: '',
        apiBaseUrl: 'http://localhost:8080' // 后端API基础URL
    },
    methods: {
        // 获取所有题目
        async fetchQuestions() {
            try {
                const response = await axios.get(`${this.apiBaseUrl}/documents`);
                this.questions = response.data;
            } catch (error) {
                this.$message.error('获取题目列表失败');
                console.error('获取题目失败:', error);
            }
        },

        // 显示题目详情
        showQuestionDetail(question) {
            this.selectedQuestion = question;
        },

        // 根据难度返回对应的标签类型
        getDifficultyType(difficulty) {
            const types = {
                '简单': 'success',
                '中等': 'warning',
                '困难': 'danger'
            };
            return types[difficulty] || 'info';
        },

        // 搜索PDF内容
        async searchPdf() {
            if (!this.searchKeyword.trim()) {
                this.$message.warning('请输入搜索关键词');
                return;
            }

            try {
                const response = await axios.get(`${this.apiBaseUrl}/questions/search/pdf-content`, {
                    params: { keyword: this.searchKeyword }
                });
                this.questions = response.data;
                if (this.questions.length === 0) {
                    this.$message.info('未找到相关题目');
                }
            } catch (error) {
                this.$message.error('搜索失败');
                console.error('搜索失败:', error);
            }
        },

        // 下载PDF文件
        downloadPdf(question) {
            if (!question.pdfPath) {
                this.$message.warning('PDF文件不存在');
                return;
            }
            window.open(`${this.apiBaseUrl}/questions/pdf/${question.id}`, '_blank');
        }
    },
    // 页面加载完成后获取题目列表
    mounted() {
        this.fetchQuestions();
    }
});

// 获取所有PDF文档
async function fetchDocuments() {
    try {
        const response = await fetch('http://localhost:8080/documents');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const documents = await response.json();
        displayDocuments(documents);
    } catch (error) {
        console.error('获取PDF文档失败:', error);
        // 在页面上显示错误信息
        const documentsContainer = document.getElementById('documents');
        documentsContainer.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; color: red;">
                    获取文档列表失败，请稍后重试
                </td>
            </tr>
        `;
    }
}

// 显示PDF文档列表
function displayDocuments(documents) {
    const documentsContainer = document.getElementById('documents');
    if (!documents || documents.length === 0) {
        documentsContainer.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center;">
                    暂无文档数据
                </td>
            </tr>
        `;
        return;
    }
    
    documentsContainer.innerHTML = documents.map(doc => `
        <tr>
            <td>${doc.id}</td>
            <td>${doc.fileName || '未设置'}</td>
            <td>${doc.pages || 0}</td>
            <td>${doc.filePath || '未设置'}</td>
            <td>${doc.isbn || '未设置'}</td>
            <td>${new Date(doc.uploadTime).toLocaleString()}</td>
        </tr>
    `).join('');
}

// 将函数暴露到全局作用域
window.fetchDocuments = fetchDocuments;
window.displayDocuments = displayDocuments;

// 页面加载完成后获取PDF文档列表
document.addEventListener('DOMContentLoaded', fetchDocuments); 