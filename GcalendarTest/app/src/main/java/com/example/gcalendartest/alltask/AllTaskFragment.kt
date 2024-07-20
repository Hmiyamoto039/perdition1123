package com.example.gcalendartest.alltask

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gcalendartest.EventAdapter
import com.example.gcalendartest.MainActivity
import com.example.gcalendartest.R
import com.example.gcalendartest.today.TodayTaskFragment
import com.example.gcalendartest.today.TodayTaskFragment.Companion
import com.example.gcalendartest.today.TodayTaskFragment.Companion.REQUEST_AUTHORIZATION
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.play.integrity.internal.o
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.google.api.services.calendar.model.Events
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.Tasks
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class AllTaskFragment:Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventsList = mutableListOf<Any>()

    private val calendarColors = mapOf(
        "1" to R.color.Lavender,
        "2" to R.color.Sage,
        "3" to R.color.Grape,
        "4" to R.color.Flamingo,
        "5" to R.color.Banana,
        "6" to R.color.Tangerine,
        "7" to R.color.Peacock,
        "8" to R.color.Graphite,
        "9" to R.color.Blueberry,
        "10" to R.color.Basil,
        "11" to R.color.Tomato
    )


    private lateinit var yearSpinner: Spinner
    private lateinit var monthSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alltask_fragment,container,false)

        recyclerView = view.findViewById(R.id.allTask_rv)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        eventAdapter = EventAdapter(eventsList){ item ->
            onItemLongClick(item)
        }
        recyclerView.adapter = eventAdapter

        yearSpinner = view.findViewById(R.id.year_Spinner)
        monthSpinner = view.findViewById(R.id.month_Spinner)

        setupSpinners()
        setupSpinnerListeners()

        loadEvents()

        return view
    }

    override fun onStart() {
        super.onStart()
        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun onItemLongClick(item: Any){
        val titleMessage = when(item){
            is Event -> "スケジュールの削除"
            is Task -> "タスクの削除"
            else -> return
        }
        val message = when(item){
            is Event -> "このスケジュールを削除しますか？"
            is Task -> "このタスクを削除しますか？"
            else -> return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(titleMessage)
            .setMessage(message)
            .setPositiveButton("削除"){_,_ -> deleteItem(item)}
            .setNegativeButton("キャンセル",null)
            .show()
    }

    private fun deleteItem(item: Any){
        when(item){
            is Event -> deleteEvent(item)
            is Task -> deleteTask(item)
        }
    }

    private fun deleteEvent(event: Event){
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if(account != null){
            DeleteEventTask(account,event.id).execute()
        }
    }
    private fun deleteTask(task: Task){
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if(account != null){
            DeleteTaskTask(account,task.id).execute()
        }
    }
    private inner class DeleteEventTask(private val account: GoogleSignInAccount,
                                        private val eventId:String) : AsyncTask<Void, Void, Boolean>(){
        override fun doInBackground(vararg p0: Void?): Boolean{
            return try{
                val transport = AndroidHttp.newCompatibleTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singleton(CalendarScopes.CALENDAR)
                )
                credential.selectedAccountName = account.email

                val service = com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("My Calendar App")
                    .build()

                service.events().delete("primary", eventId).execute()
                true
            }catch (e: IOException){
                Log.e(TodayTaskFragment.TAG,"Error deleting event")
                false
            }
        }
        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(requireContext(), "スケジュールを削除しました", Toast.LENGTH_SHORT).show()
                loadEvents()
            } else {
                Toast.makeText(requireContext(), "スケジュールの削除に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class DeleteTaskTask(
        private val account: GoogleSignInAccount,
        private val taskId: String
    ) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                val transport = AndroidHttp.newCompatibleTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singleton(TasksScopes.TASKS)
                )
                credential.selectedAccountName = account.email

                val service = com.google.api.services.tasks.Tasks.Builder(transport, jsonFactory, credential)
                    .setApplicationName("My Tasks App")
                    .build()

                service.tasks().delete("@default", taskId).execute()
                true
            } catch (e: IOException) {
                Log.e(TodayTaskFragment.TAG, "Error deleting task", e)
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(requireContext(), "タスクを削除しました", Toast.LENGTH_SHORT).show()
                loadEvents()
            } else {
                Toast.makeText(requireContext(), "タスクの削除に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinners(){
        val calendar = Calendar.getInstance()

        val years = (1900..2100).toList()
        val yearAdapter = ArrayAdapter(requireContext(),R.layout.ym_spinner_item,years)
        yearAdapter.setDropDownViewResource(R.layout.ym_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter

        val months = (1..12).toList()
        val monthAdapter = ArrayAdapter(requireContext(),R.layout.ym_spinner_item,months)
        monthAdapter.setDropDownViewResource(R.layout.ym_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter

        yearSpinner.setSelection(years.indexOf(calendar.get(Calendar.YEAR)))
        monthSpinner.setSelection(calendar.get(Calendar.MONTH))


    }

    private fun setupSpinnerListeners(){
        val listener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                loadEvents()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        yearSpinner.onItemSelectedListener = listener
        monthSpinner.onItemSelectedListener = listener
    }

    fun loadEvents(){
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if(account != null){
            GetEventsTask(account).execute()
        }else{
            Toast.makeText(requireContext(),"サインインしていません",Toast.LENGTH_SHORT)
                .show()
        }
    }

    private inner class GetEventsTask(private val account: GoogleSignInAccount): AsyncTask<Void, Void, List<Any>>(){
        override fun doInBackground(vararg p0: Void?): List<Any> {
            return try{
                val year = yearSpinner.selectedItem as Int
                val month = monthSpinner.selectedItem as Int
                val events = getEventsFromGoogleCalendar(account,year,month)
                val tasks = getTasksFromGoogleTasks(account, year, month)
                val combinedList = mutableListOf<Any>()
                combinedList.addAll(events)
                combinedList.addAll(tasks)
                combinedList.sortWith(compareBy {
                    when(it){
                        is Event -> it.start.dateTime?.value ?: it.start.date.value
                        is Task -> it.due?.let { dueDate -> parseDate(dueDate).time }
                        else -> null
                    }
                })
                combinedList
            }catch (e: UserRecoverableAuthIOException){
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                emptyList()
            }catch (e: IOException){
                Log.e(TAG,"Error retrieving events from Google Calendar",e)
                emptyList()
            }
        }
        override fun onPostExecute(result: List<Any>) {
            val year = yearSpinner.selectedItem as Int
            val month = monthSpinner.selectedItem as Int
            eventsList.clear()
            if(result.isEmpty()) {
                Toast.makeText(requireContext(),"${year}年${month}月はタスク及びイベントが登録されていません"
                    ,Toast.LENGTH_SHORT).show()
            }else{
                eventsList.addAll(addHeaders(result))
            }
            eventAdapter.notifyDataSetChanged()
        }
    }

    private fun addHeaders(items: List<Any>): List<Any> {
        val result = mutableListOf<Any>()
        var currentDate: String? = null
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val addedHeaders = mutableSetOf<String>()

        for (item in items) {
            val dates = when (item) {
                is Event -> {
                    val isAllDay = item.start.dateTime == null
                    val startDate = if (isAllDay) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(item.start.date.toString())
                    } else {
                        Date(item.start.dateTime?.value ?: item.start.date.value)
                    }
                    val endDate = if (isAllDay) {
                        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(item.end.date.toString())
                        Date(end.time - (24*60*60*1000))
                    } else {
                        Date(item.end.dateTime?.value ?: item.end.date.value)
                    }
                    generateDates(startDate, endDate)
                }
                is Task -> listOf(parseDate(item.due!!))
                else -> continue
            }
            for ((index, date) in dates.withIndex()) {
                val dateString = dateFormat.format(date)

                if(!result.contains(dateString)){
                    result.add(dateString)
                }

                val copyItem = when (item) {
                    is Event -> {
                        if(dates.size > 1){
                            item.clone().apply {
                                summary = "${item.summary} (${index + 1}/${dates.size}日目)"
                            }
                        } else {
                            item
                        }
                    }
                    is Task -> item
                    else -> continue
                }
                val insertionIndex = result.indexOf(dateString) + 1
                result.add(insertionIndex,copyItem)
            }
        }
        return result
    }

    private fun generateDates(startDate :Date,endDate :Date):List<Date>{
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance().apply { time = startDate }
        while (calendar.time <= endDate){
            dates.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH,1)
        }
        return dates
    }


    private fun getTasksFromGoogleTasks(account: GoogleSignInAccount,year: Int,month: Int): List<Task>{
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(),Collections.singleton(TasksScopes.TASKS)
        )
        credential.selectedAccountName = account.email

        val service = com.google.api.services.tasks.Tasks.Builder(transport,jsonFactory,credential)
            .setApplicationName("My Tasks App")
            .build()

        val taskLists = service.tasklists().list().execute().items
        val allTasks =  mutableListOf<Task>()

        taskLists?.forEach{ taskList ->
            val tasks = service.tasks().list(taskList.id).execute().items
            tasks.forEach { task ->
                val taskDueDate = parseDate(task.due ?: "")
                if(taskDueDate != null){
                    val calendar = Calendar.getInstance()
                    calendar.time = taskDueDate
                    if(calendar.get(Calendar.YEAR) == year &&
                        calendar.get(Calendar.MONTH) +1 == month){
                        allTasks.add(task)
                    }
                }
            }
        }

        return allTasks
    }

    private fun getEventsFromGoogleCalendar(account: GoogleSignInAccount,year:Int,month:Int): List<Event>{
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), Collections.singleton(CalendarScopes.CALENDAR)
        )
        credential.selectedAccountName = account.email

        val service = com.google.api.services.calendar.Calendar.Builder(transport,jsonFactory,credential)
            .setApplicationName("My Calendar App")
            .build()

        val calendar = java.util.Calendar.getInstance()

        calendar.set(year,month -1,1,0,0,0)
        calendar.set(Calendar.MILLISECOND,0)
        val startOfMonth = DateTime(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = DateTime(calendar.time)


        val events: Events = service.events().list("primary")
            .setTimeMin(startOfMonth)
            .setTimeMax(endOfMonth)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()

        events.items.forEach{ event ->
            val colorId = event.colorId
            val colorResId = calendarColors[colorId] ?: R.color.default_color
            event.setColorId(colorResId.toString())
        }

        return events.items
    }

    private fun parseDate(dateStr: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(dateStr)!!
        } catch (e: ParseException) {
            Date()
        }
    }

    companion object{
        const val REQUEST_AUTHORIZATION =1001
        const val TAG  = "AllTaskFragment"

        fun addEventToGoogleCalendar(context: Context,account: GoogleSignInAccount, task: String, date: String,endDate: String,
                                     colorId:String?,isAllDay: Boolean,repeatRule: String?,notificationTimes: List<String>) {
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(CalendarScopes.CALENDAR)
            )
            credential.selectedAccountName = account.email

            val service = com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("My Calendar App")
                .build()

            val event = Event()
                .setSummary(task)
                .setDescription(task)
                .setColorId(colorId)

            //終日の場合
            if(isAllDay){
                val startDateTime = DateTime(date)
                val start = EventDateTime().setDate(startDateTime)
                event.start = start

                val endCalendar = Calendar.getInstance().apply {
                    time = SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(endDate)
                    add(Calendar.DAY_OF_MONTH,1)
                }
                val endDateString = SimpleDateFormat("yyyy-MM-dd").format(endCalendar.time)
                val endDateTime = DateTime(endDateString)
                val end = EventDateTime().setDate(endDateTime)
                event.end = end

            //通常のイベント
            } else {
                val startDateTime = com.google.api.client.util.DateTime(date)
                val start = EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("Asia/Tokyo")
                event.start = start

                val endDateTime = com.google.api.client.util.DateTime(endDate)  // Adjust this to your requirement
                val end = EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("Asia/Tokyo")
                event.end = end
            }

            //繰り返し処理
            if(repeatRule != null){
                val recurrenceRule = "RRULE:FREQ=$repeatRule"
                event.recurrence = listOf(recurrenceRule)
            }

            //通知の追加
            if(notificationTimes.isNotEmpty()){
                val reminders = notificationTimes.map { time ->
                    val minutes = when(time){
                        "5分前" -> 5
                        "10分前" -> 10
                        "15分前" -> 15
                        "30分前" -> 30
                        "1時間前" -> 60
                        "1日前" -> 1440
                        else -> 0
                    }
                    EventReminder().setMethod("popup").setMinutes(minutes)
                }
                event.reminders = Event.Reminders().setUseDefault(false).setOverrides(reminders)
            }

            try {
                val calendarId = "primary"
                val createdEvent = service.events().insert(calendarId, event).execute()
                Log.d(TAG, "Event created: ${createdEvent.htmlLink}")
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }
        }
    }
}