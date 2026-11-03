package app.lawnchair.smartspace

import android.app.ActivityOptions
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import app.lawnchair.smartspace.model.SmartspaceAction
import com.android.launcher3.R
import com.android.launcher3.Utilities

object BcSmartSpaceUtil {
    private const val TAG = "BcSmartSpaceUtil"

    fun getIconDrawable(icon: Icon?, context: Context): Drawable? {
        if (icon == null) return null
        val drawable = icon.loadDrawable(context) ?: return null
        val iconSize =
            context.resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size)
        drawable.setBounds(0, 0, iconSize, iconSize)
        return drawable
    }

    fun setOnClickListener(
        view: View?,
        action: SmartspaceAction?,
        onClickListener: View.OnClickListener? = null,
        str: String?,
    ) {
        val options = ActivityOptions.makeBasic()
        if (Utilities.ATLEAST_U) {
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        if (view == null || action == null) {
            Log.e(str, "No tap action can be set up")
            return
        }
        val tag = str ?: TAG
        view.setOnClickListener {
            if (performAction(view.context, action, options, tag)) {
                onClickListener?.onClick(view)
            }
        }
    }

    /**
     * Tries the action's intent, then its pending intent, then its click callback. A failure is
     * logged and the next option is tried, so a stale intent no longer hides a working pending intent.
     *
     * @return true if one of them ran, or if the action has nothing to run.
     */
    private fun performAction(
        context: Context,
        action: SmartspaceAction,
        options: ActivityOptions,
        tag: String,
    ): Boolean {
        action.intent?.let { intent ->
            val result = runCatching { context.startActivity(intent) }
            if (result.isSuccess) return true
            Log.w(tag, "Failed to start intent $intent", result.exceptionOrNull())
        }
        action.pendingIntent?.let { pendingIntent ->
            val result = runCatching {
                if (Utilities.ATLEAST_U) {
                    pendingIntent.send(options.toBundle())
                } else {
                    pendingIntent.send()
                }
            }
            if (result.isSuccess) return true
            Log.w(tag, "Failed to send pending intent", result.exceptionOrNull())
        }
        action.onClick?.let { onClick ->
            val result = runCatching { onClick.run() }
            if (result.isSuccess) return true
            Log.w(tag, "Click action failed", result.exceptionOrNull())
        }
        return action.intent == null && action.pendingIntent == null && action.onClick == null
    }

    fun getOpenCalendarIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).setData(
            ContentUris.appendId(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time"),
                System.currentTimeMillis(),
            ).build(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
}
