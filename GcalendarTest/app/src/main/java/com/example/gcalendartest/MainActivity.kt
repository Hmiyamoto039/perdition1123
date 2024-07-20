package com.example.gcalendartest

import android.app.DatePickerDialog
import android.app.ProgressDialog.show
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.gcalendartest.alltask.AllTaskFragment
import com.example.gcalendartest.alltask.AllTaskFragment.Companion.addEventToGoogleCalendar
import com.example.gcalendartest.today.TodayTaskFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.Events
import com.google.api.services.tasks.TasksScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.apache.http.HttpHeaders.IF
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var selectedDate: java.util.Calendar? = null
    private var selectedEndDate: java.util.Calendar? = null

    private val colorMap = mapOf(
        "既定の色" to "0",
        "ブルー" to "9",
        "ターコイズ" to "7",
        "ボルドーブルー" to "9",
        "レッド" to "11",
        "ピンク" to "4",
        "パープル" to "3",
        "イエロー" to "5",
        "オレンジ" to "6",
        "グリーン" to "2",
        "ボルドーグリーン" to "10",
        "グレー" to "8"
    )

    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: ViewPagerHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //viewpager2
        pagerAdapter = ViewPagerHolder(this)
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter

        firebaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR), Scope(TasksScopes.TASKS))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)


        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account == null){
            signIn()
        } else{
            loadInitialEvents()
        }

        //FABの動作、タスク追加ダイアログへアクセス
        val ftb = findViewById<FloatingActionButton>(R.id.ftb_btn)
        ftb.setOnClickListener{
            showAddTaskDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewPager.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val fragment = pagerAdapter.getFragment(position)
                if(fragment is AllTaskFragment){
                    fragment.loadEvents()
                }
            }
        })
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSingInResult(task)
        }
    }
    private fun handleSingInResult(task: Task<GoogleSignInAccount>){
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        }catch (e: ApiException){
            Toast.makeText(this,"サインインに失敗しました",Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this){task ->
                if(task.isSuccessful){
                    Toast.makeText(this,"サインイン成功",Toast.LENGTH_SHORT)
                        .show()
                    loadInitialEvents()
                }else{
                    Toast.makeText(this,"サインイン失敗",Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun loadInitialEvents() {
        viewPager.post{
            val fragment = pagerAdapter.getFragment(viewPager.currentItem)
            if(fragment is AllTaskFragment){
                fragment.loadEvents()
            }else if(fragment is TodayTaskFragment){
                fragment.loadEvents()
            }
        }
    }

    fun refreshAllFragments(){
        val fragment = supportFragmentManager.findFragmentById(R.id.pager) as? AllTaskFragment
        fragment?.loadEvents()

        val todayFragmentPosition =  pagerAdapter.getFragmentPosition(TodayTaskFragment::class.java)
        if(todayFragmentPosition != -1){
            val todayTaskFragment = pagerAdapter.getFragment(todayFragmentPosition) as? TodayTaskFragment
            todayTaskFragment?.loadEvents()
        }
    }

    private fun showAddTaskDialog(){
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task,null)
        val taskInput = dialogView.findViewById<EditText>(R.id.taskInput)
        val allDayLinearLayout = dialogView.findViewById<LinearLayout>(R.id.allDayCheck)
        val setAllDay = dialogView.findViewById<Switch>(R.id.setAllDaySwitch)
        val dateInput = dialogView.findViewById<TextView>(R.id.dateInput)
        val dateTimeInput = dialogView.findViewById<TextView>(R.id.startTimeInput)
        val endDateInput = dialogView.findViewById<TextView>(R.id.endDateInput)
        val endTimeInput = dialogView.findViewById<TextView>(R.id.endTimeInput)
        val setRepeat = dialogView.findViewById<LinearLayout>(R.id.setRepeat)
        val repeatText = dialogView.findViewById<TextView>(R.id.repeatText)

        //通知
        val addNotification = dialogView.findViewById<LinearLayout>(R.id.notificationSet)
        val notificationContainer = dialogView.findViewById<LinearLayout>(R.id.notificationContainer)

        val colorSpinner = dialogView.findViewById<Spinner>(R.id.color_Spinner)

        var repeatRule: String? = null

        val notificationTimes = mutableListOf("30分前")
        notificationContainer.removeAllViews()
        notificationTimes.forEach{ addNotificationView(notificationContainer,it,notificationTimes)}

        val colorNames = colorMap.keys.toList()
        val colorAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,colorNames)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = colorAdapter

        val now = java.util.Calendar.getInstance()
        val initialYear = now.get(java.util.Calendar.YEAR)
        val initialMonth = now.get(java.util.Calendar.MONTH)
        val initialDay = now.get(java.util.Calendar.DAY_OF_MONTH)
        val initialHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val initialMinute = if(now.get(java.util.Calendar.MINUTE) < 30) 0 else 30

        val initialDate = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE).format(now.time)
        dateInput.text = initialDate
        endDateInput.text = initialDate

        val initialTime = SimpleDateFormat("HH:mm",Locale.JAPANESE).format(now.apply {
            set(java.util.Calendar.MINUTE,0) }.time)
        dateTimeInput.text = initialTime

        //デフォルト終了時刻(デフォルトの開始時刻の1時間後)
        val endTimeCalendar  = now.clone() as java.util.Calendar
        endTimeCalendar.add(java.util.Calendar.HOUR_OF_DAY,1)
        val endTime = SimpleDateFormat("HH:mm",Locale.JAPANESE).format(endTimeCalendar.time)
        endTimeInput.text = endTime

        //selectedDate及びselectedEndDateに当日の日付を代入
        selectedDate = now.clone() as java.util.Calendar
        selectedEndDate = now.clone() as java.util.Calendar

        allDayLinearLayout.setOnClickListener {
            setAllDay.isChecked = !setAllDay.isChecked
        }
        //終日のスイッチ
        setAllDay.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                dateTimeInput.visibility = View.GONE
                endTimeInput.visibility = View.GONE
            } else {
                dateTimeInput.visibility = View.VISIBLE
                endTimeInput.visibility = View.VISIBLE
            }
        }

        dateInput.setOnClickListener{
            DatePickerDialog(this,{_,year,month,dayOfMonth ->
                selectedDate = java.util.Calendar.getInstance().apply {
                    set(year,month,dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
                dateInput.text = sdf.format(selectedDate!!.time)

                //終了日の初期値を開始日と同日にする
                selectedEndDate = (selectedDate?.clone() as java.util.Calendar)
                endDateInput.text = sdf.format(selectedEndDate!!.time)
            },initialYear,initialMonth,initialDay).show()
        }

        dateTimeInput.setOnClickListener {
            TimePickerDialog(this,{_,hourOfDay,minute ->
                selectedDate?.apply {
                    set(java.util.Calendar.HOUR_OF_DAY,hourOfDay)
                    set(java.util.Calendar.MINUTE,minute)
                }
                val sdf = SimpleDateFormat("HH:mm", Locale.JAPANESE)
                dateTimeInput.text = sdf.format(selectedDate!!.time)

                //終了時刻を選択した開始時刻の1時間後にする
                selectedEndDate = (selectedDate?.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.HOUR_OF_DAY,1)
                }
                endTimeInput.text = sdf.format(selectedEndDate!!.time)
            },initialHour,initialMinute,true).show()
        }

        endDateInput.setOnClickListener{
            DatePickerDialog(this,{_,year,month,dayOfMonth ->
                selectedEndDate = java.util.Calendar.getInstance().apply {
                    set(year,month,dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
                endDateInput.text = sdf.format(selectedEndDate!!.time)
            },initialYear,initialMonth,initialDay).show()
        }

        endTimeInput.setOnClickListener {
            TimePickerDialog(this,{_,hourOfDay,minute ->
                selectedEndDate?.apply {
                    set(java.util.Calendar.HOUR_OF_DAY,hourOfDay)
                    set(java.util.Calendar.MINUTE,minute)
                }
                val sdf = SimpleDateFormat("HH:mm", Locale.JAPANESE)
                endTimeInput.text = sdf.format(selectedEndDate!!.time)
            },initialHour,initialMinute,true).show()
        }

        //繰り返し処理
        setRepeat.setOnClickListener {
            showRepeatDialog(repeatRule){ selectedRepeatRule ->
                repeatRule = selectedRepeatRule
                repeatText.text = when(selectedRepeatRule){
                    null -> "繰り返さない"
                    "DAILY" -> "毎日"
                    "WEEKLY" -> "毎週"
                    "MONTHLY" -> "毎月"
                    "YEARLY" -> "毎年"
                    else -> ""
                }
            }
        }

        //通知の追加
        addNotification.setOnClickListener {
            showNotificationDialog(notificationTimes) { selectedNotification ->
                if(!notificationTimes.contains(selectedNotification)){
                    notificationTimes.add(selectedNotification)
                    addNotificationView(notificationContainer,selectedNotification,notificationTimes)
                }
            }
        }


        val dialog = AlertDialog.Builder(this)
            .setTitle("タスクを追加")
            .setView(dialogView)
            .setPositiveButton("追加"){_,_ ->
                val task = taskInput.text.toString()
                val date = "${dateInput.text} ${dateTimeInput.text}"
                val endDate = "${endDateInput.text} ${endTimeInput.text}"
                val selectedColorName = colorSpinner.selectedItem.toString()
                val selectedColorId = colorMap[selectedColorName]

                if(task.isNotEmpty() && date.isNotEmpty() && endDate.isNotEmpty()){
                    val formattedDate = convertToRFC3339(date)
                    val formattedEndDate = convertToRFC3339(endDate)
                    if(formattedDate != null && formattedEndDate != null) {
                        val account = GoogleSignIn.getLastSignedInAccount(this)
                        if (account != null) {
                            if(setAllDay.isChecked){
                                val formatedAllDate = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
                                    .format(selectedDate?.time)
                                val formatedEndAllDate = SimpleDateFormat("yyyy-MM-dd",Locale.JAPANESE)
                                    .format(selectedEndDate?.time)
                                addEventToGoogleCalendar(account, task, formatedAllDate, formatedEndAllDate, selectedColorId, true,repeatRule,notificationTimes)
                            } else {
                                val startDateTime = "$date ${dateTimeInput.text}"
                                val endDateTime = "$endDate ${endTimeInput.text}"
                                val formattedStartDateTime = convertToRFC3339(startDateTime)
                                val formattedEndDateTime = convertToRFC3339(endDateTime)
                                if (formattedStartDateTime != null && formattedEndDateTime != null) {
                                    addEventToGoogleCalendar(account, task, formattedStartDateTime, formattedEndDateTime, selectedColorId, false,repeatRule,notificationTimes)
                                } else {
                                    Toast.makeText(this,"日付の形式が正しくありません",Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    } else {
                            Toast.makeText(this, "サインインしていません", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                    Toast.makeText(this,"タスクと日付を入力してください",Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("キャンセル",null)
            .create()
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
    }

    private fun showRepeatDialog(currentRepeatRule: String?,onRepeatSelected: (String?) -> Unit){
        val repeatOptions = arrayOf("繰り返さない","毎日","毎週","毎月","毎年")
        val repeatRuleOptions = arrayOf(null,"DAILY","WEEKLY","MONTHLY","YEARLY")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_repeat,null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.repeatRadioGroup)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        repeatOptions.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                tag = repeatRuleOptions[index]
                textSize = 18f
                setPadding(0,24,0,24)
            }
            radioGroup.addView(radioButton)

            if(repeatRuleOptions[index] == currentRepeatRule){
                radioButton.isChecked = true
            }

            radioButton.setOnClickListener {
                val selectedRepeatRule = radioButton.tag as String?
                onRepeatSelected(selectedRepeatRule)
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
    }

    private fun showNotificationDialog(existingNotificationTimes:List<String>,onNotificationSelected: (String) -> Unit){
        val notificationOptions = arrayOf("5分前","10分前","15分前","30分前","1時間前","1日前")
        val notificationMinutes = arrayOf(5,10,15,30,60,1440)
        val availableOptions = notificationOptions.filter { !existingNotificationTimes.contains(it) }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification,null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.notificationRadioGroup)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        availableOptions.forEach { option ->
            val radioButton = RadioButton(this).apply {
                text = option
                tag = option
                textSize = 18f
                setPadding(0,24,0,24)

                setOnClickListener {
                    onNotificationSelected(option)
                    dialog.dismiss()
                }
            }
            radioGroup.addView(radioButton)
        }
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_border)
    }

    private fun addNotificationView(notificationContainer: LinearLayout,notificationText: String,notificationTimes: MutableList<String>){
        val notificationView = LinearLayout(this)
        notificationView.orientation = LinearLayout.HORIZONTAL
        notificationView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,8,0,0)
        }
        val textView = TextView(this).apply {
            text = notificationText
            textSize = 16f
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.baseline_close_24)
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                notificationContainer.removeView(notificationView)
                notificationTimes.remove(notificationText)
            }
        }

        notificationView.addView(textView)
        notificationView.addView(imageView)

        notificationContainer.addView(notificationView)
    }

    private fun addEventToGoogleCalendar(account: GoogleSignInAccount, task: String, startDate: String, endDate: String,
                                         colorId:String?,isAllDay: Boolean,repeatRule: String?,notificationTimes: List<String>) {
        AddEventTask(account, task, startDate, endDate,colorId,isAllDay,repeatRule,notificationTimes).execute()
    }

    private inner class AddEventTask(
        private val account: GoogleSignInAccount,
        private val task :String,
        private val date :String,
        private val endDate: String,
        private val colorId: String?,
        private val isAllDay: Boolean,
        private val repeatRule: String?,
        private val notificationTimes :List<String>):AsyncTask<Void,Void,Boolean>() {
        override fun doInBackground(vararg parmas: Void?): Boolean {
            return try {
                AllTaskFragment.addEventToGoogleCalendar(applicationContext,account,task,date,endDate,colorId, isAllDay,repeatRule,notificationTimes)
                true
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, AllTaskFragment.REQUEST_AUTHORIZATION)
                false
            } catch (e: IOException) {
                Log.e(AllTaskFragment.TAG, "Error adding event to Google Calendar", e)
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(this@MainActivity, "イベントがGoogleカレンダーに追加されました", Toast.LENGTH_SHORT)
                    .show()
                loadInitialEvents()
            } else {
                Toast.makeText(this@MainActivity, "イベントの追加に失敗しました", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private inner class DeleteEventTask(private val account: GoogleSignInAccount,private val item:Any) : AsyncTask<Void,Void,Boolean>(){
        override fun doInBackground(vararg p0: Void?): Boolean {
            return try {
                if (item is Event) {
                    deleteEventFromGoogleCalendar(item)
                } else if (item is com.google.api.services.tasks.model.Task) {
                    deleteTaskFromGoogleTasks(item)
                }
                true
            }catch (e: IOException){
                Log.e(AllTaskFragment.TAG,"Error deleting item from Google Calendar/Tasks",e)
                false
            }
        }
        override fun onPostExecute(result: Boolean) {
            if(result){
                Toast.makeText(this@MainActivity,"アイテムがGoogleカレンダーから削除されました",Toast.LENGTH_SHORT)
                    .show()
                loadInitialEvents()
            } else {
                Toast.makeText(this@MainActivity,"アイテムの削除に失敗しました",Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun deleteEventFromGoogleCalendar(event: Event){
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null){
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val credential = GoogleAccountCredential.usingOAuth2(
                this,Collections.singleton(CalendarScopes.CALENDAR)
            )
            credential.selectedAccountName = account.email

            val service = Calendar.Builder(transport,jsonFactory,credential)
                .setApplicationName("My Calendar App")
                .build()

            service.events().delete("primary",event.id).execute()
        }
    }
    private fun deleteTaskFromGoogleTasks(task: com.google.api.services.tasks.model.Task){
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(CalendarScopes.CALENDAR)
            )
            credential.selectedAccountName = account.email

            val service = Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("My Task App")
                .build()

            service.events().delete("primary", task.id).execute()
        }
    }


    private fun convertToRFC3339(dateStr: String): String?{
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE)
        return try{
            val date = sdf.parse(dateStr)
            val rfc3339Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            rfc3339Format.format(date)
        }catch (e: ParseException){
            e.printStackTrace()
            null
        }
    }
    companion object {
        const val RC_SIGN_IN = 9001
        const val TAG = "MainActivity"
    }
}