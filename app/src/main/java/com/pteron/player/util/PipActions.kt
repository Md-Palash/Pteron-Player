package com.pteron.player.util

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Rational
import androidx.annotation.RequiresApi
import com.pteron.player.R

/** The play/pause button shown inside the Picture-in-Picture window. */
object PipActions {
    const val ACTION_TOGGLE = "com.pteron.player.PIP_TOGGLE"

    @RequiresApi(26)
    fun params(context: Context, aspect: Float, playing: Boolean): PictureInPictureParams {
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_TOGGLE).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = if (playing) "Pause" else "Play"
        val action = RemoteAction(
            Icon.createWithResource(context, if (playing) R.drawable.ic_pip_pause else R.drawable.ic_pip_play),
            title,
            title,
            intent
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((aspect.coerceIn(0.42f, 2.39f) * 100).toInt(), 100))
            .setActions(listOf(action))
            .build()
    }
}
