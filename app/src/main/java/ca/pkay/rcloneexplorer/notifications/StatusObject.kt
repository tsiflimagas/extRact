package ca.pkay.rcloneexplorer.notifications

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import ca.pkay.rcloneexplorer.R
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StatusObject(var mContext: Context){

    private val TAG = "StatusObject"
    var notificationPercent: Int = 0
    var notificationContent: String = ""
    var notificationBigText = ArrayList<String>()
    var mErrorList = ArrayList<ErrorObject>()
    var mStats = JSONObject()
    var mLogline = JSONObject()


    fun getSpeed(): String {
        return Formatter.formatFileSize(mContext, mStats.optLong("speed", 0)) + "/s"
    }

    fun getSize(): String {
        return Formatter.formatFileSize(mContext, mStats.optLong("bytes", 0))
    }

    fun getTotalSize(): String {
        return Formatter.formatFileSize(mContext, mStats.optLong("totalBytes", 0))
    }

    fun getPercentage(): Double {
        return mStats.optLong("bytes", 0).toDouble() / mStats.optLong("totalBytes", 0) * 100
    }

    fun getTransfers(): Int {
        return mStats.optInt("transfers", 0)
    }

    fun getTotalTransfers(): Int {
        return mStats.optInt("totalTransfers", 0)
    }

    fun getDeletions(): Int {
        return mStats.optInt("deletes", 0) + mStats.optInt("deletedDirs", 0)
    }

    fun getErrorMessage(): String {
        if(mLogline.has("msg") && mLogline.getString("level") == "error") {
            return mLogline.getString("msg")
        }
        return ""
    }

    fun getErrorObject(): String {
        if(mLogline.has("msg") && mLogline.getString("level") == "error") {
            return mLogline.optString("object", "")
        }
        return ""
    }

    fun parseLoglineToStatusObject(logLine: JSONObject) {
        if(logLine.getString("level") == "error") {
            clearObject()
            mLogline = logLine

            var error = ErrorObject(getErrorObject(), getErrorMessage())
            Log.e(TAG, error.mErrorObject + " - " + error.mErrorMessage)
            mErrorList.add(error)
        }

        if(logLine.has("stats")) {
            clearObject()
            mLogline = logLine
            mStats = mLogline.getJSONObject("stats")

            //available stats:
            //bytes,checks,deletedDirs,deletes,elapsedTime,errors,eta,fatalError,renames,retryError
            //speed,totalBytes,totalChecks,totalTransfers,transferTime,transfers

            //available stats:
            //bytes,checks,deletedDirs,deletes,elapsedTime,errors,eta,fatalError,renames,retryError
            //speed,totalBytes,totalChecks,totalTransfers,transferTime,transfers


            // when we check stuff, dont show the other messages.
            val checks = mStats.optJSONArray("checking")
            if(checks != null) {
                var filename = checks.getString(0)
                if(!filename.equals("")) {
                    notificationBigText.add(
                        String.format(
                            mContext.getString(R.string.sync_notification_elapsed),
                            prettyPrintDuration(mStats.getInt("elapsedTime"))
                        )
                    )

                    notificationBigText.add(
                        String.format(
                            mContext.getString(R.string.sync_notification_file_checking),
                            filename
                        )
                    )
                }
                return
            }


            val speed = getSpeed()
            val size = getSize()
            val allsize = getTotalSize()
            val percent: Double = getPercentage()

            notificationContent = String.format(
                mContext.getString(R.string.sync_notification_short),
                size,
                allsize,
                prettyPrintDuration(mStats.optInt("eta", 0))
            )
            notificationBigText.clear()
            notificationBigText.add(
                String.format(
                    mContext.getString(R.string.sync_notification_transferred),
                    size,
                    allsize
                )
            )

            if (getDeletions() > 0) {
                notificationBigText.add(
                        String.format(
                                mContext.getString(R.string.sync_notification_deletions),
                                getDeletions()
                        )
                )
            }

            notificationBigText.add(
                String.format(
                    mContext.getString(R.string.sync_notification_speed),
                    speed
                )
            )

            var eta = mStats.get("eta")
            if(eta == null) {
                eta = "0";
            }

            notificationBigText.add(
                String.format(
                    mContext.getString(R.string.sync_notification_remaining),
                    prettyPrintDuration(mStats.optInt("eta", 0))
                )
            )
            if (mStats.getInt("errors") > 0) {
                notificationBigText.add(
                    String.format(
                        mContext.getString(R.string.sync_notification_errors),
                        mStats.getInt("errors")
                    )
                )
            }

            notificationBigText.add(
                String.format(
                    mContext.getString(R.string.sync_notification_elapsed),
                    prettyPrintDuration(mStats.getInt("elapsedTime"))
                )
            )

            val transfers = mStats.optJSONArray("transferring")
            if(transfers != null) {
                val transferObject = transfers.getJSONObject(0)
                var filename = transferObject.optString("name", "")
                if(!filename.equals("")) {
                    notificationBigText.add(String.format(
                        mContext.getString(R.string.sync_notification_file_syncing),
                        filename
                    ))
                }
            }

            notificationPercent = percent.toInt()
        }
    }

    fun clearObject() {
        notificationPercent = 0
        notificationContent = ""
        notificationBigText = ArrayList<String>()
        mLogline = JSONObject()
    }

    fun printErrors(){
        mErrorList.forEach {
            Log.e(TAG, it.mErrorObject + " - " + it.mErrorMessage)
        }
    }

    fun getAllErrorMessages(): String{
        var all = ""
        mErrorList.forEach {
            all += it.mErrorMessage + "\n"
            all += mContext.getString(R.string.status_offendingfile) + it.mErrorObject + "\n"
        }
        return all
    }

    override fun toString(): String {
        return "StatusObject(getSpeed=${getSpeed()}, getSize=${getSize()}, getTotalSize=${getTotalSize()}, getTransfers=${getTransfers()}, getTotalTransfers=${getTotalTransfers()}, getErrorMessage=${getErrorMessage()}, getDeletions=${getDeletions()})"
    }


    private fun prettyPrintDuration(secondDuration: Int) : String {

        var duration = secondDuration.toLong()
        val days = TimeUnit.SECONDS.toDays(duration).toInt()
        duration -= (days * 60 * 60 * 24)
        val hours = TimeUnit.SECONDS.toHours(duration).toInt()
        duration -= (hours * 60 * 60)
        val minutes = TimeUnit.SECONDS.toMinutes(duration).toInt()
        duration -= (minutes * 60)
        val seconds = TimeUnit.SECONDS.toSeconds(duration).toInt()

        var daysText = String.format(
            mContext.resources.getQuantityString(R.plurals.modern_prettyprint_duration_d,
                days,
                days
            )
        )

        var hoursText = String.format(
            mContext.resources.getQuantityString(R.plurals.modern_prettyprint_duration_h,
                hours,
                hours
            )
        )

        var minutesText = String.format(
            mContext.resources.getQuantityString(R.plurals.modern_prettyprint_duration_m,
                minutes,
                minutes
            )
        )

        val secondsText = String.format(
            mContext.resources.getQuantityString(R.plurals.modern_prettyprint_duration_s,
                seconds,
                seconds
            )
        )

        if (days > 0) {
            daysText = "$daysText, "
        } else {
            daysText = ""
        }

        if (hours > 0) {
            hoursText = "$hoursText, "
        } else {
            hoursText = ""
        }

        if (minutes > 0) {
            minutesText = "$minutesText, "
        } else {
            minutesText = ""
        }

        return "$daysText$hoursText$minutesText$secondsText"
    }
}