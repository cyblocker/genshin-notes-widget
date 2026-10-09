package com.genshin.dailynote.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.genshin.dailynote.worker.NoteSyncWorker

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Trigger background sync
        NoteSyncWorker.enqueueImmediateSync(context)

        // Immediately update widget UI to reflect current local extrapolation and user interaction
        GenshinGlanceWidget().updateAll(context)
    }
}
