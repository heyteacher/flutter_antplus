package me.heyteacher.flutter_antplus.logging

import me.heyteacher.flutter_antplus.AbstractHostApi
import me.heyteacher.flutter_antplus.logging.pigeons.AntplusLogLevel
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.MessageFormatter

class OnLogDataStreamLogger(val onLogDataStreamHandler: OnLogDataStreamHandlerImpl, val loggerName:String) : Logger {

    override fun getName(): String {
        return loggerName
    }

    private fun add(level: AntplusLogLevel, msg: String?, argArray: Array<Any?>? = null, throwable: Throwable? = null) {
        AbstractHostApi.log(
            name,
            level,
            "${format(msg, argArray)}",
            throwable,
            onLogDataStreamHandler)
    }

    private fun format(messagePattern: String?, argArray: Array<Any?>?): String? {
        return if (argArray == null)  messagePattern else MessageFormatter.arrayFormat(messagePattern, argArray).message
    }

    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?) {
        add(AntplusLogLevel.VERBOSE, msg)
    }

    override fun trace(format: String?, arg: Any?) {
        add(AntplusLogLevel.VERBOSE, format, arrayOf(arg))
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogLevel.VERBOSE, format, arrayOf(arg1, arg2))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        add(AntplusLogLevel.VERBOSE, format, arguments.toList().toTypedArray())
    }

    override fun trace(msg: String?, t: Throwable?) {
        add(AntplusLogLevel.VERBOSE, msg, throwable = t)
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
        add(AntplusLogLevel.DEBUG, msg)
    }

    override fun debug(format: String?, arg: Any?) {
        add(AntplusLogLevel.DEBUG, format, arrayOf(arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogLevel.DEBUG, format, arrayOf(arg1, arg2))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        add(AntplusLogLevel.DEBUG, format, arguments.toList().toTypedArray())
    }

    override fun debug(msg: String?, t: Throwable?) {
        add(AntplusLogLevel.DEBUG, msg, throwable = t)
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
        add(AntplusLogLevel.INFO, msg)
    }

    override fun info(format: String?, arg: Any?) {
        add(AntplusLogLevel.INFO, format, arrayOf(arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogLevel.INFO, format, arrayOf(arg1,arg2))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        add(AntplusLogLevel.INFO, format, arguments.toList().toTypedArray())
    }

    override fun info(msg: String?, t: Throwable?) {
        add(AntplusLogLevel.INFO, msg, throwable = t)
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
        add(AntplusLogLevel.WARNING, msg)

    }

    override fun warn(format: String?, arg: Any?) {
        add(AntplusLogLevel.WARNING, format(format?:"", arrayOf(arg)))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        add(AntplusLogLevel.WARNING, format, arguments.toList().toTypedArray())
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogLevel.WARNING, format(format?:"", arrayOf(arg1,arg2)))
    }

    override fun warn(msg: String?, t: Throwable?) {
        add(AntplusLogLevel.WARNING, msg, throwable = t)
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
        add(AntplusLogLevel.ERROR, msg)
    }

    override fun error(format: String?, arg: Any?) {
        add(AntplusLogLevel.ERROR, format, arrayOf(arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        add(AntplusLogLevel.ERROR, format, arrayOf(arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        add(AntplusLogLevel.ERROR, format, arguments.toList().toTypedArray())
    }

    override fun error(msg: String?, t: Throwable?) {
        add(AntplusLogLevel.ERROR, msg, throwable = t)
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