package com.example.gcalendartest.data

import java.util.Date

data class CalendarEvent(
    val title :String,
    val startTime :Date,
    val endTime :Date
)

data class CalendarTask(
    val title : String,
    val dueDate :Date
)