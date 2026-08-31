package de.moritzstaat.launcher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesDragTest {

    private val row = 56f
    private val order = listOf("A", "B", "C", "D")

    @Test
    fun `a short drag does not reorder anything`() {
        val result = applyDrag(order, draggedIndex = 0, offsetPx = 20f, rowHeightPx = row)
        assertEquals(order, result.order)
        assertEquals(0, result.draggedIndex)
        assertEquals(20f, result.offsetPx, 1e-3f)
    }

    @Test
    fun `dragging down by one row swaps with the next entry`() {
        val result = applyDrag(order, draggedIndex = 0, offsetPx = 60f, rowHeightPx = row)
        assertEquals(listOf("B", "A", "C", "D"), result.order)
        assertEquals(1, result.draggedIndex)
        assertEquals(4f, result.offsetPx, 1e-3f)
    }

    @Test
    fun `dragging up by one row swaps with the previous entry`() {
        val result = applyDrag(order, draggedIndex = 2, offsetPx = -60f, rowHeightPx = row)
        assertEquals(listOf("A", "C", "B", "D"), result.order)
        assertEquals(1, result.draggedIndex)
        assertEquals(-4f, result.offsetPx, 1e-3f)
    }

    @Test
    fun `a long drag walks past several rows at once`() {
        val result = applyDrag(order, draggedIndex = 0, offsetPx = 3 * row, rowHeightPx = row)
        assertEquals(listOf("B", "C", "D", "A"), result.order)
        assertEquals(3, result.draggedIndex)
        assertEquals(0f, result.offsetPx, 1e-3f)
    }

    @Test
    fun `dragging past the end stops at the last position`() {
        val result = applyDrag(order, draggedIndex = 3, offsetPx = 10 * row, rowHeightPx = row)
        assertEquals(order, result.order)
        assertEquals(3, result.draggedIndex)
        assertEquals(10 * row, result.offsetPx, 1e-3f)
    }

    @Test
    fun `dragging past the start stops at the first position`() {
        val result = applyDrag(order, draggedIndex = 0, offsetPx = -10 * row, rowHeightPx = row)
        assertEquals(order, result.order)
        assertEquals(0, result.draggedIndex)
        assertEquals(-10 * row, result.offsetPx, 1e-3f)
    }

    @Test
    fun `an invalid drag index leaves everything untouched`() {
        val result = applyDrag(order, draggedIndex = -1, offsetPx = 500f, rowHeightPx = row)
        assertEquals(order, result.order)
        assertEquals(-1, result.draggedIndex)
    }

    @Test
    fun `a degenerate row height does not loop forever`() {
        val result = applyDrag(order, draggedIndex = 1, offsetPx = 500f, rowHeightPx = 0f)
        assertEquals(order, result.order)
        assertEquals(1, result.draggedIndex)
    }
}
