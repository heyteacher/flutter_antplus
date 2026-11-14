package me.heyteacher.flutter_antplus.logging

import android.os.Handler
import android.os.Looper
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogData
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogEvent
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.MessageFormatter
import java.io.PrintWriter
import java.io.StringWriter

class OnLogDataListenerLogger(val onLogDataListener: OnLogDataListener) : Logger {

    override fun getName(): String {
        return "j-antplus"
    }

    private fun add(level: AntplusLogEvent, msg: String?, argArray: Array<Any?>? = null, throwable: Throwable? = null) {
        val sw = StringWriter()
        throwable?.printStackTrace(PrintWriter(sw))
        Handler(Looper.getMainLooper()).post {
            onLogDataListener.add(
                AntplusLogData(
                    level,
                    name,
                    "${format(msg, argArray)} $sw")
            )
        }
    }

    private fun format(messagePattern: String?, argArray: Array<Any?>?): String? {
        return if (argArray == null)  messagePattern else MessageFormatter.arrayFormat(messagePattern, argArray).message
    }

    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?) {
        add(AntplusLogEvent.VERBOSE, msg)
    }

    override fun trace(format: String?, arg: Any?) {
        add(AntplusLogEvent.VERBOSE, format, arrayOf(arg))
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogEvent.VERBOSE, format, arrayOf(arg1, arg2))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        add(AntplusLogEvent.VERBOSE, format, arguments.toList().toTypedArray())
    }

    override fun trace(msg: String?, t: Throwable?) {
        add(AntplusLogEvent.VERBOSE, msg, throwable = t)
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        vararg argArray: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?) {
        add(AntplusLogEvent.DEBUG, msg)
    }

    override fun debug(format: String?, arg: Any?) {
        add(AntplusLogEvent.DEBUG, format, arrayOf(arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogEvent.DEBUG, format, arrayOf(arg1, arg2))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        add(AntplusLogEvent.DEBUG, format, arguments.toList().toTypedArray())
    }

    override fun debug(msg: String?, t: Throwable?) {
        add(AntplusLogEvent.DEBUG, msg, throwable = t)
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(msg: String?) {
        add(AntplusLogEvent.INFO, msg)
    }

    override fun info(format: String?, arg: Any?) {
        add(AntplusLogEvent.INFO, format, arrayOf(arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogEvent.INFO, format, arrayOf(arg1,arg2))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        add(AntplusLogEvent.INFO, format, arguments.toList().toTypedArray())
    }

    override fun info(msg: String?, t: Throwable?) {
        add(AntplusLogEvent.INFO, msg, throwable = t)
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }


    override fun isWarnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(msg: String?) {
        add(AntplusLogEvent.WARNING, msg)

    }

    override fun warn(format: String?, arg: Any?) {
        add(AntplusLogEvent.WARNING, format(format?:"", arrayOf(arg)))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        add(AntplusLogEvent.WARNING, format, arguments.toList().toTypedArray())
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogEvent.WARNING, format(format?:"", arrayOf(arg1,arg2)))
    }

    override fun warn(msg: String?, t: Throwable?) {
        add(AntplusLogEvent.WARNING, msg, throwable = t)
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?) {
        add(AntplusLogEvent.ERROR, msg)
    }

    override fun error(format: String?, arg: Any?) {
        add(AntplusLogEvent.ERROR, format, arrayOf(arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogEvent.ERROR, format, arrayOf(arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        add(AntplusLogEvent.ERROR, format, arguments.toList().toTypedArray())
    }

    override fun error(msg: String?, t: Throwable?) {
        add(AntplusLogEvent.ERROR, msg, throwable = t)
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }
}