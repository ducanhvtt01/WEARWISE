package com.example.dacs3.connectDB

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorParserTest {

    @Test
    fun `parse returns correct message for UnknownHostException`() {
        val exception = UnknownHostException("Unable to resolve host")
        val result = ErrorParser.parse(exception)
        assertEquals("Cannot connect to the server. Please check your Wifi or mobile data connection.", result)
    }

    @Test
    fun `parse returns correct message for SocketTimeoutException`() {
        val exception = SocketTimeoutException("timeout")
        val result = ErrorParser.parse(exception)
        assertEquals("Cannot connect to the server. Please check your Wifi or mobile data connection.", result)
    }

    @Test
    fun `parse returns correct message for invalid login credentials`() {
        val exception = Exception("Invalid login credentials")
        val result = ErrorParser.parse(exception)
        assertEquals("Incorrect email or password. Please try again.", result)
    }

    @Test
    fun `parse returns correct message for unverified email`() {
        val exception = Exception("Email not confirmed")
        val result = ErrorParser.parse(exception)
        assertEquals("This email address is not verified. Please check your inbox to confirm.", result)
    }

    @Test
    fun `parse returns correct message for rate limit`() {
        val exception = Exception("HTTP 429 Too many requests")
        val result = ErrorParser.parse(exception)
        assertEquals("Action too frequent. Please wait a few minutes and try again.", result)
    }

    @Test
    fun `parse returns fallback message for unknown errors`() {
        val exception = Exception("Some weird unknown bug")
        val result = ErrorParser.parse(exception)
        assertEquals("Error: Some weird unknown bug", result)
    }

    @Test
    fun `parse returns general fallback for empty message exception`() {
        val exception = Exception()
        val result = ErrorParser.parse(exception)
        assertEquals("A system error occurred. Please try again later.", result)
    }
}
