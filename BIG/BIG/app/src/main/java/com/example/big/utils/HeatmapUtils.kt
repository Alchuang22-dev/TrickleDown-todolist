// HeatmapUtils.kt
package com.example.big.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.big.R
import java.text.SimpleDateFormat
import java.util.*

object HeatmapUtils {

    // 获取热力图单元格颜色
    fun getHeatmapColor(intensity: Int): Int {
        return when {
            intensity <= 0 -> Color.parseColor("#ebedf0") // 无活动
            intensity < 30 -> Color.parseColor("#c6e48b") // 低活动
            intensity < 60 -> Color.parseColor("#7bc96f") // 中等活动
            intensity < 120 -> Color.parseColor("#239a3b") // 高活动
            else -> Color.parseColor("#196127") // 非常高活动
        }
    }

    // 创建热力图单元格
// 创建热力图单元格
    fun createHeatmapCell(context: Context, minutes: Int): View {
        val cell = View(context)
        val cellParams = LinearLayout.LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.heatmap_cell_width),
            context.resources.getDimensionPixelSize(R.dimen.heatmap_cell_height)
        )
        cellParams.setMargins(2, 2, 2, 2)
        cell.layoutParams = cellParams
        cell.setBackgroundColor(getHeatmapColor(minutes))

        // 设置点击监听器以显示具体时间
        cell.tag = minutes
        cell.isClickable = true
        cell.setOnClickListener {
            val duration = it.tag as Int
            if (duration > 0) {
                val hours = duration / 60
                val mins = duration % 60
                val timeText = if (hours > 0) {
                    "${hours}小时${mins}分钟"  // 使用字符串模板语法 ${}
                } else {
                    "${mins}分钟"  // 使用字符串模板语法 ${}
                }
                android.widget.Toast.makeText(context, "专注时长: $timeText", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        return cell
    }

    // 获取月份的天数
    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // 获取日期的周数
    fun getWeekOfYear(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.WEEK_OF_YEAR)
    }
}