<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="展厅名称" prop="showroomName">
        <el-input
          v-model="queryParams.showroomName"
          placeholder="请输入展厅名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属部门" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptOptions"
          :props="deptProps"
          value-key="deptId"
          placeholder="请选择所属部门"
          check-strictly
          clearable
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:showroom:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="showroomList">
      <el-table-column label="展厅编号" align="center" prop="id" width="120" />
      <el-table-column label="展厅名称" align="center" prop="showroomName" min-width="180" show-overflow-tooltip />
      <el-table-column label="经度" align="center" prop="longitude" width="140" />
      <el-table-column label="纬度" align="center" prop="latitude" width="140" />
      <el-table-column label="所属部门" align="center" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          {{ getDeptName(scope.row.deptId) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:showroom:edit']"
          >修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="620px" class="system-management-dialog" append-to-body>
      <el-form ref="showroomRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="展厅名称" prop="showroomName">
          <el-input v-model="form.showroomName" placeholder="请输入展厅名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptOptions"
            :props="deptProps"
            value-key="deptId"
            placeholder="请选择所属部门"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number
                v-model="form.longitude"
                :min="-180"
                :max="180"
                :precision="7"
                :step="0.0000001"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number
                v-model="form.latitude"
                :min="-90"
                :max="90"
                :precision="7"
                :step="0.0000001"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Showroom">
import { addShowroom, getShowroom, listShowroom, updateShowroom } from "@/api/system/showroom"
import { listDept } from "@/api/system/dept"

const { proxy } = getCurrentInstance()

const showroomList = ref([])
const deptOptions = ref([])
const deptNameMap = ref({})
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const deptProps = { value: "deptId", label: "deptName", children: "children" }

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    showroomName: undefined,
    deptId: undefined
  },
  rules: {
    showroomName: [{ required: true, message: "展厅名称不能为空", trigger: "blur" }],
    deptId: [{ required: true, message: "所属部门不能为空", trigger: "change" }],
    longitude: [{ required: true, message: "经度不能为空", trigger: "change" }],
    latitude: [{ required: true, message: "纬度不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询展厅列表 */
function getList() {
  loading.value = true
  listShowroom(queryParams.value).then(response => {
    showroomList.value = response.rows
    total.value = response.total
  }).catch(() => {
    showroomList.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

/** 加载部门选择项 */
function loadDeptOptions() {
  listDept({ status: "0" }).then(response => {
    const depts = response.data || []
    deptNameMap.value = Object.fromEntries(depts.map(dept => [String(dept.deptId), dept.deptName]))
    deptOptions.value = proxy.handleTree(depts, "deptId")
  })
}

function getDeptName(deptId) {
  return deptNameMap.value[String(deptId)] || deptId
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    showroomName: undefined,
    longitude: undefined,
    latitude: undefined,
    deptId: undefined
  }
  proxy.resetForm("showroomRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加展厅"
}

function handleUpdate(row) {
  reset()
  getShowroom(row.id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改展厅"
  })
}

function submitForm() {
  proxy.$refs["showroomRef"].validate(valid => {
    if (!valid) {
      return
    }
    const request = form.value.id == null ? addShowroom(form.value) : updateShowroom(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.id == null ? "新增成功" : "修改成功")
      open.value = false
      getList()
    })
  })
}

loadDeptOptions()
getList()
</script>
