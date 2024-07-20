package com.example.gcalendartest

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionScene.Transition.TransitionOnClick
import androidx.core.content.ContextCompat
import androidx.core.os.persistableBundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.integrity.internal.i
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.tasks.model.Task
import org.w3c.dom.Text
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayEventAdapter(private val items: List<Any>,
                   private val onItemLongClick: (Any) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val TYPE_EVENT = 0
    private val TYPE_TASK = 1

    override fun getItemViewType(position: Int): Int {
        return when(items[position]){
            is Event -> TYPE_EVENT
            is Task -> TYPE_TASK
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_EVENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item,parent,false)
                EventViewHolder(view)
            }
            TYPE_TASK -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item,parent,false)
                TaskViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> {
                val event = items[position] as Event

                holder.bind(event,position,items)
                holder.itemView.setOnClickListener {
                    val intent = Intent(it.context,EventTaskDetailActivity::class.java)
                    intent.putExtra("ID",event.id)
                    it.context.startActivity(intent)
                }
                holder.itemView.setOnLongClickListener{
                    onItemLongClick(event)
                    true
                }
            }
            is TaskViewHolder -> {
                val task = items[position] as Task
                holder.bind(task)
                holder.itemView.setOnLongClickListener {
                    onItemLongClick(task)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class EventViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        private val eventSummary:TextView = itemView.findViewById(R.id.eventSummary)
        private val eventDate: TextView = itemView.findViewById(R.id.eventDate)
        private val eventColor: View = itemView.findViewById(R.id.colorLine)

        fun bind(event: Event,position: Int,items: List<Any>) {
            eventSummary.text = event.summary

            val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.JAPANESE)
            val dateFormat = SimpleDateFormat("yyyy/MM/dd",Locale.JAPANESE)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)

            /*val date = if(position > 0 && items[position -1] is String){
                val headerDateFormat = SimpleDateFormat("MM/dd",Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.time = headerDateFormat.parse(items[position -1] as String)
                calendar.set(Calendar.YEAR,Calendar.getInstance().get(Calendar.YEAR))
                calendar.time
            } else {
                Date(event.start.dateTime?.value ?: event.start.date.value)
            }*/

            val startDateTime = event.start.dateTime
            val endDateTime = event.end.dateTime
            val startDate = Date(event.start.dateTime?.value ?: event.start.date.value)
            val endDate = Date(event.end.dateTime?.value ?: event.end.date.value)


            val startTime = startDateTime?.let { timeFormat.format(Date(it.value)) }
            val endTime = endDateTime?.let { timeFormat.format(Date(it.value)) }

            if(event.start.dateTime == null && event.end.dateTime == null){
                val calendar = Calendar.getInstance()
                calendar.time = endDate
                calendar.add(Calendar.DAY_OF_MONTH, - 1)
                endDate.time = calendar.timeInMillis
            }

            val currentDate = dateFormat.format(Date())
            val eventStartDate = dateFormat.format(startDate)
            val eventEndDate = dateFormat.format(endDate)

            val isMultiDay = dateFormat.format(startDate) != dateFormat.format(endDate)
            val isAllDay = event.start.dateTime == null && event.end.dateTime == null

            eventDate.text = when{
                isAllDay && isMultiDay -> {
                    "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                }
                isAllDay -> {
                    dateFormat.format(startDate)
                }
                isMultiDay -> {
                    "${dateTimeFormat.format(startDate)} - ${dateTimeFormat.format(endDate)}"
                }
                startDateTime != null && endDateTime != null -> {
                    "$startTime - $endTime"
                }
                else -> {
                    dateFormat.format(startDate)
                }
            }


            /*if(startDateTime != null && endDateTime != null){
                val start = Date(startDateTime.value)
                val end = Date(endDateTime.value)
                eventDate.text = "${dateFormat.format(date)} ${timeFormat.format(start)}-${timeFormat.format(end)}"
            } else {
                eventDate.text = dateFormat.format(date)
            }*/

            val colorId = event.colorId?.toIntOrNull()
            colorId?.let {
                val color = ContextCompat.getColor(itemView.context,it)
                val drawable = itemView.background as? GradientDrawable
                drawable?.setStroke(12,color)
                eventColor.setBackgroundColor(color)
            }
        }
        private fun parseDate(dateStr: String):Date{
            return try {
                SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(dateStr)
            } catch (e: ParseException){
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.getDefault()).parse(dateStr)
            }
        }
    }

    class TaskViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        private val title :TextView = itemView.findViewById(R.id.taskSummary)
        private val taskDate :TextView = itemView.findViewById(R.id.taskDate)
        private val taskColor :View = itemView.findViewById(R.id.taskColorLine)

        fun bind(task: Task){
            title.text = task.title
            task.due?.let {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE)
                try{
                    val date = dateFormat.parse(it)
                    val displayFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE)
                    taskDate.text = displayFormat.format(date)
                }catch (e: ParseException){
                    e.printStackTrace()
                    taskDate.text = "Invalid date"
                }
            } ?: run {
                taskDate.text = "No due date"
            }
        }
    }
}