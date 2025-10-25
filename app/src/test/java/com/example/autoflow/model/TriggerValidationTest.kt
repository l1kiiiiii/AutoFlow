package com.example.autoflow.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Trigger validation
 * Tests critical bug fixes and validation logic
 */
class TriggerValidationTest {

    @Test
    fun `test time trigger validation with valid timestamp`() {
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + (60 * 60 * 1000) // 1 hour in future
        
        val trigger = Trigger(
            type = "TIME",
            value = futureTime.toString()
        )
        
        assertTrue("Valid future time should be accepted", trigger.isValid)
        assertNull("Valid trigger should have no error", trigger.validationError)
    }

    @Test
    fun `test time trigger validation with invalid timestamp`() {
        val trigger = Trigger(
            type = "TIME",
            value = "not_a_number"
        )
        
        assertFalse("Invalid timestamp should fail validation", trigger.isValid)
        assertNotNull("Invalid trigger should have error message", trigger.validationError)
    }

    @Test
    fun `test BLE trigger validation with valid MAC address`() {
        val trigger = Trigger(
            type = "BLE",
            value = "00:11:22:33:44:55"
        )
        
        assertTrue("Valid MAC address should pass", trigger.isValid)
        assertNull("Valid trigger should have no error", trigger.validationError)
    }

    @Test
    fun `test location trigger validation with valid coordinates`() {
        val trigger = Trigger(
            type = "LOCATION",
            value = "37.7749,-122.4194,100"  // San Francisco with 100m radius
        )
        
        assertTrue("Valid coordinates should pass", trigger.isValid)
        assertNull("Valid trigger should have no error", trigger.validationError)
    }

    @Test
    fun `test WiFi trigger validation with valid state`() {
        val trigger = Trigger(
            type = "WIFI",
            value = """{"state":"ON"}"""
        )
        
        assertTrue("WiFi ON state should be valid", trigger.isValid)
    }

    @Test
    fun `test empty trigger type should fail`() {
        val trigger = Trigger(
            type = "",
            value = "some_value"
        )
        
        assertFalse("Empty trigger type should fail", trigger.isValid)
        assertEquals("Error message should mention empty type", 
            "Trigger type cannot be empty", 
            trigger.validationError)
    }

    @Test
    fun `test empty trigger value should fail`() {
        val trigger = Trigger(
            type = "TIME",
            value = ""
        )
        
        assertFalse("Empty trigger value should fail", trigger.isValid)
        assertEquals("Error message should mention empty value",
            "Trigger value cannot be empty",
            trigger.validationError)
    }
}
