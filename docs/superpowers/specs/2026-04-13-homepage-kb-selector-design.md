# 首页添加知识库选择器设计文档

## 目标
在首页 (`HomeView.vue`) 的快捷输入区域添加一个可选的知识库选择器，使用户在创建咨询时可以直接关联知识库。

## 方案
采用方案 A：在输入框下方的 `input-actions` 上方，紧邻"已选择的专家预览"，添加一个紧凑的 `el-select` 下拉框。

## 具体设计

### 位置与样式
- 位于 `.input-box` 内部，在 `selected-agents-preview` 和 `input-actions` 之间
- 使用 `el-select` 组件，宽度自适应，靠右对齐
- placeholder: "关联知识库（可选）"
- clearable: true

### 行为
- **显示条件**：仅当 `userStore.isLoggedIn` 为 true 时显示
- **数据加载**：在 `onMounted` 中调用 `kbApi.getKbs({ page: 1, size: 100 })` 加载知识库列表
- **默认值**：`undefined`（不关联任何知识库）
- **提交**：`handleSubmit` 时把 `selectedKbId` 传入 `taskApi.createTask` 的 `kbId` 字段

### 涉及文件
- `careermind-frontend/src/views/HomeView.vue`

### 接口兼容性
- `taskApi.createTask` 的 `CreateTaskData` 类型已包含 `kbId?: number`
- Java 后端和 RAG 服务的知识库关联逻辑已就绪，无需修改
