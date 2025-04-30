package com.example.big

import java.util.Date

class Task {
    var id: String
        private set
    var title: String
        private set
    var timeRange: String
        private set
    var date: Date
        private set
    var durationMinutes: Int
        private set
    var isImportant: Boolean
        private set
    var description: String? = null
        private set

    //关联用户
    private val user_id = 0

    //倒计时
    var dueDate: Date? = null

    //地点
    var place: String? = null

    //是否完成
    var isFinished: Boolean = false

    //是否拖延
    var isDelayed: Boolean = false

    //与别的任务存在时间重叠的个数
    private val layers = 0

    //从属的类别（如果有）
    var category: String? = null

    constructor(
        id: String,
        title: String,
        timeRange: String,
        date: Date,
        durationMinutes: Int,
        important: Boolean
    ) {
        this.id = id
        this.title = title
        this.timeRange = timeRange
        this.date = date
        this.durationMinutes = durationMinutes
        this.isImportant = important
    }

    // （1）包含 description 的构造函数
    constructor(
        id: String,
        title: String,
        timeRange: String,
        date: Date,
        durationMinutes: Int,
        important: Boolean,
        description: String?
    ) {
        this.id = id
        this.title = title
        this.timeRange = timeRange
        this.date = date
        this.durationMinutes = durationMinutes
        this.isImportant = important
        this.description = description
    }

    // （2）允许 timeRange 和 durationMinutes 使用默认值的构造函数
    constructor(id: String, title: String, date: Date, important: Boolean, description: String?) {
        this.id = id
        this.title = title
        this.date = date
        this.isImportant = important
        this.timeRange = "未设定时间" // 或者你可以用 null / ""
        this.durationMinutes = 0
        this.description = description
    }

    // （3）添加包含 place 参数的构造函数
    constructor(
        id: String,
        title: String,
        timeRange: String,
        date: Date,
        durationMinutes: Int,
        important: Boolean,
        description: String?,
        place: String?
    ) {
        this.id = id
        this.title = title
        this.timeRange = timeRange
        this.date = date
        this.durationMinutes = durationMinutes
        this.isImportant = important
        this.description = description
        this.place = place
    }

    // （4）添加包含 place 和 due_date 参数的构造函数
    constructor(
        id: String,
        title: String,
        timeRange: String,
        date: Date,
        durationMinutes: Int,
        important: Boolean,
        description: String?,
        place: String?,
        due_date: Date?
    ) {
        this.id = id
        this.title = title
        this.timeRange = timeRange
        this.date = date
        this.durationMinutes = durationMinutes
        this.isImportant = important
        this.description = description
        this.place = place
        this.dueDate = due_date
    }

    val all: Map<String, Any?>
        // 获取所有属性的方法
        get() {
            val taskData: MutableMap<String, Any?> =
                HashMap()
            taskData["id"] = id
            taskData["title"] = title
            taskData["timeRange"] = timeRange
            taskData["date"] = date
            taskData["durationMinutes"] = durationMinutes
            taskData["important"] = isImportant
            taskData["description"] = description
            taskData["user_id"] = user_id
            taskData["due_date"] = dueDate
            taskData["place"] = place
            taskData["finished"] = isFinished
            taskData["delayed"] = isDelayed
            taskData["layers"] = layers
            taskData["category"] = category
            return taskData
        }

    // 编辑任务的方法
    fun edit(
        title: String, timeRange: String, date: Date, durationMinutes: Int,
        important: Boolean, description: String?, place: String?, due_date: Date?
    ) {
        this.title = title
        this.timeRange = timeRange
        this.date = date
        this.durationMinutes = durationMinutes
        this.isImportant = important
        this.description = description
        this.place = place
        this.dueDate = due_date

        // 这里可以添加保存到数据库的代码
    }

    // 删除任务的方法
    fun delete(): Boolean {
        // 这里添加从数据库中删除该任务的代码
        // 成功删除返回true，失败返回false
        return true
    }
}