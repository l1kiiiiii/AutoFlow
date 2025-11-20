package com.example.autoflow.domain.usecase

import com.example.autoflow.data.SavedLocation
import com.example.autoflow.data.SavedLocationDao
import com.example.autoflow.domain.usecase.location.SaveLocationUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for SaveLocationUseCase
 * Demonstrates testing approach for Use Cases
 */
class SaveLocationUseCaseTest {
    
    @Mock
    private lateinit var mockLocationDao: SavedLocationDao
    
    private lateinit var useCase: SaveLocationUseCase
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = SaveLocationUseCase(mockLocationDao)
    }
    
    @Test
    fun `execute should succeed with valid inputs`() = runTest {
        // Given
        val name = "Home"
        val latitude = 37.7749
        val longitude = -122.4194
        val radius = 100.0
        val expectedId = 1L
        
        `when`(mockLocationDao.insertLocation(any())).thenReturn(expectedId)
        
        // When
        val result = useCase.execute(name, latitude, longitude, radius)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedId, result.getOrNull())
        verify(mockLocationDao).insertLocation(any())
    }
    
    @Test
    fun `execute should fail with blank name`() = runTest {
        // When
        val result = useCase.execute("", 37.0, -122.0)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Location name cannot be empty", result.exceptionOrNull()?.message)
        verify(mockLocationDao, never()).insertLocation(any())
    }
    
    @Test
    fun `execute should fail with invalid latitude`() = runTest {
        // When
        val result = useCase.execute("Test", 91.0, 0.0)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid latitude") == true)
    }
    
    @Test
    fun `execute should fail with invalid longitude`() = runTest {
        // When
        val result = useCase.execute("Test", 0.0, 181.0)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid longitude") == true)
    }
    
    @Test
    fun `execute should fail with negative radius`() = runTest {
        // When
        val result = useCase.execute("Test", 0.0, 0.0, -10.0)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Radius must be positive") == true)
    }
    
    @Test
    fun `execute should trim location name`() = runTest {
        // Given
        val nameWithSpaces = "  Home  "
        `when`(mockLocationDao.insertLocation(any())).thenReturn(1L)
        
        // When
        val result = useCase.execute(nameWithSpaces, 37.0, -122.0)
        
        // Then
        assertTrue(result.isSuccess)
        verify(mockLocationDao).insertLocation(
            argThat { location ->
                location.name == "Home" && !location.name.contains(" ")
            }
        )
    }
}
