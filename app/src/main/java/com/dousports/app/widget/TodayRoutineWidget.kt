package com.dousports.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionLaunchActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dousports.app.MainActivity
import com.dousports.app.data.local.entity.WeeklyScheduleEntity
import com.dousports.app.data.repository.WeeklyScheduleRepository
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar

class TodayRoutineWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val schedule = getTodaySchedule(context)
        provideContent {
            GlanceTheme {
                WidgetContent(context = context, schedule = schedule)
            }
        }
    }

    private suspend fun getTodaySchedule(context: Context): WeeklyScheduleEntity? {
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .weeklyScheduleRepository()
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return repo.getScheduleForDay(dayOfWeek)
    }
}

@Composable
private fun WidgetContent(context: Context, schedule: WeeklyScheduleEntity?) {
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .clickable(actionLaunchActivity(launchIntent)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DouSports · Aujourd'hui",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFF8C00)),
                    fontSize = 11.sp
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            if (schedule != null) {
                Text(
                    text = schedule.routineName,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
                Spacer(GlanceModifier.height(12.dp))
                Box(
                    modifier = GlanceModifier
                        .background(Color(0xFFFF8C00))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable(actionLaunchActivity(launchIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶  Démarrer",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Text(
                    text = "Repos",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Aucune routine planifiée aujourd'hui",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFF757575)),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
