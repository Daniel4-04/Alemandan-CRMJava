package com.alemandan.crm.service;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService PDF generation functionality.
 * 
 * Tests verify that the enhanced PDF report generation works correctly
 * with the new sections: executive summary, product/user tables, charts, and analysis.
 */
class ReportServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test that generarReporteVentasPdf generates a non-null byte array.
     * Updated to reflect table-based approach (no more chart generation).
     */
    @Test
    void testGenerarReporteVentasPdf_ReturnsNonNullByteArray() throws Exception {
        // Setup: Mock repository responses
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.valueOf(15000.50));
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(25L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(createMockSalesByMonth());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(createMockSalesByDay());
        when(productoRepository.stockProductos()).thenReturn(createMockStockProducts());

        // Execute
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null);

        // Verify
        assertNotNull(pdfData, "PDF data should not be null");
        assertTrue(pdfData.length > 0, "PDF data should not be empty");
        
        // Verify repository methods were called
        // totalVentasBetween is called twice (current period + previous period for growth calculation)
        verify(ventaRepository, atLeast(1)).totalVentasBetween(any(), any());
        verify(ventaRepository, atLeast(1)).countVentasBetween(any(), any());
        verify(ventaRepository, times(1)).salesByProductBetween(any(), any());
        verify(ventaRepository, times(1)).salesByUserBetween(any(), any());
        // For 30-day range (≤ 60 days), ventasPorDiaBetween is called instead of salesByMonthBetween
        verify(ventaRepository, atLeast(1)).ventasPorDiaBetween(any(), any());
    }

    /**
     * Test that PDF contains expected minimum size (indicates content was added).
     * Updated to reflect table-based approach.
     */
    @Test
    void testGenerarReporteVentasPdf_ContainsExpectedContent() throws Exception {
        // Setup
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.valueOf(5000.00));
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(10L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(createMockSalesByMonth());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(createMockSalesByDay());
        when(productoRepository.stockProductos()).thenReturn(new ArrayList<>());

        // Execute
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null);

        // Verify
        assertNotNull(pdfData);
        // A PDF with all sections (now with tables instead of charts) should be substantial
        assertTrue(pdfData.length > 10000, 
            "PDF should contain substantial content (actual size: " + pdfData.length + " bytes)");
    }

    /**
     * Test that PDF generation handles empty data gracefully.
     */
    @Test
    void testGenerarReporteVentasPdf_HandlesEmptyData() throws Exception {
        // Setup: Empty results
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(0L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(new ArrayList<>());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(new ArrayList<>());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(new ArrayList<>());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(new ArrayList<>());
        when(productoRepository.stockProductos()).thenReturn(new ArrayList<>());

        // Execute
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null);

        // Verify - should still generate a valid PDF even with no data
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0, "PDF should be generated even with empty data");
    }

    /**
     * Test that PDF generation with growth calculation works.
     */
    @Test
    void testGenerarReporteVentasPdf_CalculatesGrowth() throws Exception {
        // Setup
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        // Current period: 15000
        when(ventaRepository.totalVentasBetween(eq(from), eq(to))).thenReturn(BigDecimal.valueOf(15000));
        when(ventaRepository.countVentasBetween(eq(from), eq(to))).thenReturn(20L);
        
        // Previous period: 10000 (should show 50% growth)
        when(ventaRepository.totalVentasBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenAnswer(invocation -> {
                LocalDateTime argFrom = invocation.getArgument(0);
                LocalDateTime argTo = invocation.getArgument(1);
                if (argTo.isBefore(from)) {
                    return BigDecimal.valueOf(10000); // Previous period
                }
                return BigDecimal.valueOf(15000); // Current period
            });
        
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(new ArrayList<>());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(new ArrayList<>());
        when(productoRepository.stockProductos()).thenReturn(new ArrayList<>());

        // Execute
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null);

        // Verify
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
    }
    
    /**
     * Test that basic PDF (without analysis) can be generated.
     */
    @Test
    void testGenerarReporteVentasPdf_WithoutAnalysis() throws Exception {
        // Setup
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.valueOf(10000.00));
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(15L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());

        // Execute - generate basic report without analysis
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null, false);

        // Verify
        assertNotNull(pdfData, "Basic PDF should not be null");
        assertTrue(pdfData.length > 0, "Basic PDF should not be empty");
        // Basic PDF should contain substantial content even without charts
        assertTrue(pdfData.length > 5000, "Basic PDF should contain tables and summary");
        
        // Verify that only basic queries were called (no monthly/daily data for charts)
        verify(ventaRepository, times(1)).totalVentasBetween(any(), any());
        verify(ventaRepository, times(1)).countVentasBetween(any(), any());
        verify(ventaRepository, times(1)).salesByProductBetween(any(), any());
        verify(ventaRepository, times(1)).salesByUserBetween(any(), any());
        // These should NOT be called for basic report
        verify(ventaRepository, never()).salesByMonthBetween(any(), any());
        verify(ventaRepository, never()).ventasPorDiaBetween(any(), any());
        verify(productoRepository, never()).stockProductos();
    }
    
    /**
     * Test that full PDF (with analysis) generates larger file with all sections.
     */
    @Test
    void testGenerarReporteVentasPdf_WithAnalysis() throws Exception {
        // Setup
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.valueOf(10000.00));
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(15L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(createMockSalesByMonth());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(createMockSalesByDay());
        when(productoRepository.stockProductos()).thenReturn(createMockStockProducts());

        // Execute - generate full report with analysis
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null, true);

        // Verify
        assertNotNull(pdfData, "Full PDF should not be null");
        assertTrue(pdfData.length > 0, "Full PDF should not be empty");
        
        // Verify all repository methods were called for full report
        verify(ventaRepository, atLeast(1)).totalVentasBetween(any(), any());
        verify(ventaRepository, atLeast(1)).countVentasBetween(any(), any());
        verify(ventaRepository, times(1)).salesByProductBetween(any(), any());
        verify(ventaRepository, times(1)).salesByUserBetween(any(), any());
        verify(productoRepository, times(1)).stockProductos();
    }
    
    /**
     * Test backward compatibility - deprecated method still works.
     */
    @Test
    void testGenerarReporteVentasPdf_BackwardCompatibility() throws Exception {
        // Setup
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(ventaRepository.totalVentasBetween(any(), any())).thenReturn(BigDecimal.valueOf(5000.00));
        when(ventaRepository.countVentasBetween(any(), any())).thenReturn(10L);
        when(ventaRepository.salesByProductBetween(any(), any())).thenReturn(createMockSalesByProduct());
        when(ventaRepository.salesByUserBetween(any(), any())).thenReturn(createMockSalesByUser());
        when(ventaRepository.salesByMonthBetween(any(), any())).thenReturn(new ArrayList<>());
        when(ventaRepository.ventasPorDiaBetween(any(), any())).thenReturn(createMockSalesByDay());
        when(productoRepository.stockProductos()).thenReturn(new ArrayList<>());

        // Execute - call old method signature (should default to includeAnalysis=true)
        @SuppressWarnings("deprecation")
        byte[] pdfData = reportService.generarReporteVentasPdf(from, to, null);

        // Verify - should generate full report with all sections
        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
    }

    // Helper methods to create mock data

    private List<Object[]> createMockSalesByProduct() {
        List<Object[]> result = new ArrayList<>();
        // [productoId, productoNombre, cantidadVendida, totalMonto]
        result.add(new Object[]{1L, "Producto A", 100L, BigDecimal.valueOf(5000.00)});
        result.add(new Object[]{2L, "Producto B", 75L, BigDecimal.valueOf(3750.00)});
        result.add(new Object[]{3L, "Producto C", 50L, BigDecimal.valueOf(2500.00)});
        result.add(new Object[]{4L, "Producto D", 25L, BigDecimal.valueOf(1250.00)});
        result.add(new Object[]{5L, "Producto E", 10L, BigDecimal.valueOf(500.00)});
        return result;
    }

    private List<Object[]> createMockSalesByUser() {
        List<Object[]> result = new ArrayList<>();
        // [usuarioId, usuarioNombre, cantidadVentas, totalVendido]
        result.add(new Object[]{1L, "Juan Pérez", 15L, BigDecimal.valueOf(7500.00)});
        result.add(new Object[]{2L, "María García", 10L, BigDecimal.valueOf(5000.00)});
        result.add(new Object[]{3L, "Carlos López", 5L, BigDecimal.valueOf(2500.00)});
        return result;
    }

    private List<Object[]> createMockSalesByMonth() {
        List<Object[]> result = new ArrayList<>();
        // [mes, totalMonto]
        result.add(new Object[]{"2024-01", BigDecimal.valueOf(5000.00)});
        result.add(new Object[]{"2024-02", BigDecimal.valueOf(6000.00)});
        result.add(new Object[]{"2024-03", BigDecimal.valueOf(4500.00)});
        return result;
    }

    private List<Object[]> createMockSalesByDay() {
        List<Object[]> result = new ArrayList<>();
        // [fecha, totalMonto]
        result.add(new Object[]{"2024-03-01", BigDecimal.valueOf(500.00)});
        result.add(new Object[]{"2024-03-02", BigDecimal.valueOf(750.00)});
        result.add(new Object[]{"2024-03-03", BigDecimal.valueOf(600.00)});
        return result;
    }

    private List<Object[]> createMockStockProducts() {
        List<Object[]> result = new ArrayList<>();
        // [productoId, productoNombre, stock]
        result.add(new Object[]{10L, "Producto con stock bajo", 3});
        result.add(new Object[]{11L, "Otro producto bajo", 2});
        return result;
    }
}
