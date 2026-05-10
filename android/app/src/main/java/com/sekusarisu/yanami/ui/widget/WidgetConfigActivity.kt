package com.sekusarisu.yanami.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R

class WidgetConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Default: CANCELED so back-press removes the widget
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        val intervals = listOf(5, 15, 30, 60, 180)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.widget_config_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.widget_update_interval),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var selectedMinutes by remember { mutableIntStateOf(30) }
                        val labels = listOf(
                            stringResource(R.string.widget_interval_5m),
                            stringResource(R.string.widget_interval_15m),
                            stringResource(R.string.widget_interval_30m),
                            stringResource(R.string.widget_interval_1h),
                            stringResource(R.string.widget_interval_3h)
                        )

                        Column(modifier = Modifier.selectableGroup()) {
                            intervals.forEachIndexed { index, minutes ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedMinutes == minutes,
                                            onClick = { selectedMinutes = minutes },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedMinutes == minutes,
                                        onClick = null
                                    )
                                    Text(
                                        text = labels[index],
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row {
                            OutlinedButton(
                                onClick = { finish() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            Button(
                                onClick = {
                                    WidgetUpdateWorker.enqueue(
                                        this@WidgetConfigActivity,
                                        intervalMinutes = selectedMinutes,
                                        immediate = true
                                    )
                                    setResult(
                                        RESULT_OK,
                                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    )
                                    finish()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.widget_confirm))
                            }
                        }
                    }
                }
            }
        }
    }
}
