package com.alemandan.crm.service;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.repository.VentaRepository;
import com.alemandan.crm.service.pdf.HeaderFooterEvent;
import com.alemandan.crm.util.ExcelReportUtilAvanzado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService: generación de PDFs y utilidades para reportes.
 *
 * Contiene:
 * - generarReporteVentasPdf(...)
 * - generarMisVentasPdfFromList(...)
 * - generarResumenVentasPdf(...)
 * - generarPdfFromChartBase64(...)
 * - generarReciboVentaPdf(Venta)   <-- método de compatibilidad (delegador)
 * - generarReciboVentaPdfEstilo(...) <-- diseño ticket por categorías con header ajustado
 *
 * Nota: mantener ambos métodos (el antiguo nombre, usado por controladores, delega al nuevo estilo).
 *
 * ACTUALIZACIÓN: Los gráficos JFreeChart han sido reemplazados por tablas para evitar 
 * dependencias nativas (libfreetype, libfontmanager.so) que no están disponibles en 
 * entornos de contenedor (Railway, Docker). Los informes ahora incluyen:
 * - Tablas detalladas de ventas por período (diarias o mensuales)
 * - Tablas de participación de productos con porcentajes
 * - Análisis textual mejorado con métricas e insights
 * - Todas las funcionalidades anteriores sin requerir bibliotecas nativas
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /* ------------------ Informe avanzado (completo) ------------------ */
    
    /**
     * Genera un reporte básico de ventas en formato PDF sin análisis avanzado.
     * Incluye solo tablas de datos sin gráficos ni insights.
     * 
     * @param from Fecha de inicio
     * @param to Fecha de fin
     * @param productoId Filtro opcional por producto
     * @return byte array con el PDF básico
     * @throws Exception si hay error en la generación
     */
    private byte[] generarReporteVentasBasicoPdf(LocalDateTime from, LocalDateTime to, Long productoId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        Image logoForHeader = loadLogoIfExists();
        Font pdfNormalFont = getPdfNormalFont();
        Font pdfHeaderFont = getPdfHeaderFont();

        try {
            HeaderFooterEvent event = new HeaderFooterEvent(logoForHeader, pdfHeaderFont, "Informe de Ventas");
            writer.setPageEvent(event);
        } catch (Exception ignored) {}

        document.open();

        // ============ TÍTULO Y PERIODO ============
        Paragraph title = new Paragraph("Informe de Ventas", pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph("Periodo: " + (from != null ? from.toLocalDate() : "") + " - " + (to != null ? to.toLocalDate() : ""), pdfNormalFont);
        period.setSpacingAfter(16);
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);

        // ============ RESUMEN BÁSICO ============
        BigDecimal totalVentas = safeBig(ventaRepository.totalVentasBetween(from, to));
        Long cantidadVentas = Optional.ofNullable(ventaRepository.countVentasBetween(from, to)).orElse(0L);
        BigDecimal ticketPromedio = (cantidadVentas > 0) 
            ? totalVentas.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP) 
            : BigDecimal.ZERO;

        document.add(new Paragraph("RESUMEN", pdfHeaderFont));
        PdfPTable summary = new PdfPTable(2);
        summary.setWidths(new int[]{3, 2});
        summary.setWidthPercentage(60);
        summary.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        summary.addCell(createHeaderCell("Total Ventas (Monto)", pdfHeaderFont));
        summary.addCell(createCell(formatCurrency(totalVentas), pdfNormalFont));
        
        summary.addCell(createHeaderCell("Número de Ventas", pdfHeaderFont));
        summary.addCell(createCell(String.valueOf(cantidadVentas), pdfNormalFont));
        
        summary.addCell(createHeaderCell("Ticket Promedio", pdfHeaderFont));
        summary.addCell(createCell(formatCurrency(ticketPromedio), pdfNormalFont));
        
        summary.setSpacingAfter(16f);
        summary.setSpacingBefore(8f);
        document.add(summary);

        // ============ TABLA: VENTAS POR PRODUCTO ============
        document.add(new Paragraph("VENTAS POR PRODUCTO", pdfHeaderFont));
        List<Object[]> salesByProduct = ventaRepository.salesByProductBetween(from, to);
        
        if (salesByProduct != null && !salesByProduct.isEmpty()) {
            PdfPTable productTable = buildSalesByProductTable(salesByProduct, pdfHeaderFont, pdfNormalFont);
            document.add(productTable);
        } else {
            Paragraph noData = new Paragraph("No hay ventas en el rango seleccionado.", pdfNormalFont);
            noData.setSpacingBefore(8f);
            noData.setSpacingAfter(16f);
            document.add(noData);
        }

        // ============ TABLA: VENTAS POR USUARIO/VENDEDOR ============
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("VENTAS POR VENDEDOR", pdfHeaderFont));
        List<Object[]> salesByUser = ventaRepository.salesByUserBetween(from, to);
        
        if (salesByUser != null && !salesByUser.isEmpty()) {
            PdfPTable userTable = buildSalesByUserTable(salesByUser, pdfHeaderFont, pdfNormalFont);
            document.add(userTable);
        } else {
            Paragraph noData = new Paragraph("No hay ventas por vendedor en el rango seleccionado.", pdfNormalFont);
            noData.setSpacingBefore(8f);
            document.add(noData);
        }

        document.close();
        return baos.toByteArray();
    }

    /**
     * Genera un informe detallado de ventas en formato PDF que incluye:
     * 1. Resumen ejecutivo con métricas clave
     * 2. Tabla de ventas por producto (cantidad y monto)
     * 3. Tabla de ventas por usuario/vendedor
     * 4. Gráfico de barras: ventas por mes
     * 5. Gráfico de torta: top 10 productos
     * 6. Análisis textual con insights
     * 
     * Compatible con entornos headless (Railway) usando fuentes DejaVu.
     * 
     * @param from Fecha de inicio del periodo
     * @param to Fecha de fin del periodo
     * @param productoId ID del producto (opcional, null para todos)
     * @return byte array con el PDF generado
     * @throws Exception si hay error en la generación
     * @deprecated Use {@code generarReporteVentasPdf(LocalDateTime, LocalDateTime, Long, boolean)} instead.
     * This method is kept for backward compatibility and defaults to includeAnalysis=true.
     */
    @Deprecated
    public byte[] generarReporteVentasPdf(LocalDateTime from, LocalDateTime to, Long productoId) throws Exception {
        return generarReporteVentasPdf(from, to, productoId, true);
    }
    
    /**
     * Genera un informe de ventas en formato PDF.
     * 
     * @param from Fecha de inicio del periodo
     * @param to Fecha de fin del periodo
     * @param productoId ID del producto (opcional, null para todos)
     * @param includeAnalysis Si true, incluye análisis completo (gráficos, métricas, insights).
     *                        Si false, genera un reporte básico solo con tablas de datos.
     * @return byte array con el PDF generado
     * @throws Exception si hay error en la generación
     */
    public byte[] generarReporteVentasPdf(LocalDateTime from, LocalDateTime to, Long productoId, boolean includeAnalysis) throws Exception {
        if (!includeAnalysis) {
            // Generate basic report without analysis
            return generarReporteVentasBasicoPdf(from, to, productoId);
        }
        
        // Generate full report with analysis (existing implementation)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        Image logoForHeader = loadLogoIfExists();
        Font pdfNormalFont = getPdfNormalFont();
        Font pdfHeaderFont = getPdfHeaderFont();

        try {
            HeaderFooterEvent event = new HeaderFooterEvent(logoForHeader, pdfHeaderFont, "Informe Detallado de Ventas");
            writer.setPageEvent(event);
        } catch (Exception ignored) {}

        document.open();

        // ============ TÍTULO Y PERIODO ============
        Paragraph title = new Paragraph("Informe Detallado de Ventas", pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph("Periodo: " + (from != null ? from.toLocalDate() : "") + " - " + (to != null ? to.toLocalDate() : ""), pdfNormalFont);
        period.setSpacingAfter(16);
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);

        // ============ 1. RESUMEN EJECUTIVO CON MÉTRICAS CLAVE ============
        Map<String, Object> metrics = computeMetrics(from, to);
        document.add(new Paragraph("RESUMEN EJECUTIVO", pdfHeaderFont));
        PdfPTable executiveSummary = new PdfPTable(2);
        executiveSummary.setWidths(new int[]{3, 2});
        executiveSummary.setWidthPercentage(60);
        executiveSummary.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        executiveSummary.addCell(createHeaderCell("Total Ventas (Monto)", pdfHeaderFont));
        executiveSummary.addCell(createCell(formatCurrency((BigDecimal) metrics.get("totalVentas")), pdfNormalFont));
        
        executiveSummary.addCell(createHeaderCell("Número de Ventas", pdfHeaderFont));
        executiveSummary.addCell(createCell(String.valueOf(metrics.get("cantidadVentas")), pdfNormalFont));
        
        executiveSummary.addCell(createHeaderCell("Ticket Promedio", pdfHeaderFont));
        executiveSummary.addCell(createCell(formatCurrency((BigDecimal) metrics.get("ticketPromedio")), pdfNormalFont));
        
        // Crecimiento porcentual (si es posible calcularlo)
        BigDecimal crecimiento = (BigDecimal) metrics.get("crecimientoPorcentual");
        if (crecimiento != null) {
            executiveSummary.addCell(createHeaderCell("Crecimiento vs Periodo Anterior", pdfHeaderFont));
            String crecimientoStr = formatPercentage(crecimiento);
            executiveSummary.addCell(createCell(crecimientoStr, pdfNormalFont));
        }
        
        executiveSummary.setSpacingAfter(16f);
        executiveSummary.setSpacingBefore(8f);
        document.add(executiveSummary);

        // ============ 2. TABLA: VENTAS POR PRODUCTO ============
        document.add(new Paragraph("VENTAS POR PRODUCTO", pdfHeaderFont));
        List<Object[]> salesByProduct = ventaRepository.salesByProductBetween(from, to);
        PdfPTable productTable = buildSalesByProductTable(salesByProduct, pdfHeaderFont, pdfNormalFont);
        document.add(productTable);

        // ============ 3. TABLA: VENTAS POR USUARIO/VENDEDOR ============
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("VENTAS POR VENDEDOR", pdfHeaderFont));
        List<Object[]> salesByUser = ventaRepository.salesByUserBetween(from, to);
        PdfPTable userTable = buildSalesByUserTable(salesByUser, pdfHeaderFont, pdfNormalFont);
        document.add(userTable);

        // ============ 4. TABLA: VENTAS POR PERIODO (reemplaza gráfico de barras) ============
        document.newPage();
        Paragraph chartTitle1 = new Paragraph("VENTAS POR PERIODO", pdfHeaderFont);
        chartTitle1.setAlignment(Element.ALIGN_CENTER);
        chartTitle1.setSpacingAfter(12f);
        document.add(chartTitle1);
        
        // Generar tabla de ventas por periodo en lugar de gráfico
        addSalesByPeriodTable(document, from, to, pdfHeaderFont, pdfNormalFont);

        // ============ 5. TABLA: PARTICIPACIÓN TOP 10 PRODUCTOS (reemplaza gráfico de torta) ============
        document.newPage();
        Paragraph chartTitle2 = new Paragraph("PARTICIPACIÓN TOP 10 PRODUCTOS", pdfHeaderFont);
        chartTitle2.setAlignment(Element.ALIGN_CENTER);
        chartTitle2.setSpacingAfter(12f);
        document.add(chartTitle2);
        
        // Generar tabla de participación de productos en lugar de gráfico
        addTopProductsParticipationTable(document, salesByProduct, pdfHeaderFont, pdfNormalFont);

        // ============ 6. ANÁLISIS TEXTUAL CON INSIGHTS ============
        document.newPage();
        document.add(new Paragraph("ANÁLISIS E INSIGHTS", pdfHeaderFont));
        String analysis = generateTextualAnalysis(metrics, salesByProduct, salesByUser);
        Paragraph analysisP = new Paragraph(analysis, pdfNormalFont);
        analysisP.setSpacingBefore(8f);
        analysisP.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(analysisP);

        // ============ SECCIÓN ADICIONAL: Stock bajo (mantener compatibilidad) ============
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("PRODUCTOS CON STOCK BAJO (≤5)", pdfHeaderFont));
        List<Object[]> stockList = productoRepository.stockProductos();
        PdfPTable stockTable = new PdfPTable(new float[]{6, 2});
        stockTable.setWidthPercentage(60);
        stockTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        stockTable.addCell(createHeaderCell("Producto", pdfHeaderFont));
        stockTable.addCell(createHeaderCell("Stock", pdfHeaderFont));
        boolean hasLowStock = false;
        if (stockList != null) {
            for (Object[] r : stockList) {
                Number stock = r[2] == null ? 0 : (Number) r[2];
                if (stock.intValue() <= 5) {
                    stockTable.addCell(createCell(String.valueOf(r[1]), pdfNormalFont));
                    stockTable.addCell(createCell(String.valueOf(stock.intValue()), pdfNormalFont));
                    hasLowStock = true;
                }
            }
        }
        if (hasLowStock) {
            stockTable.setSpacingAfter(12f);
            stockTable.setSpacingBefore(6f);
            document.add(stockTable);
        } else {
            Paragraph noLowStock = new Paragraph("No hay productos con stock bajo en este momento.", pdfNormalFont);
            noLowStock.setSpacingBefore(8f);
            document.add(noLowStock);
        }

        document.close();
        return baos.toByteArray();
    }

    /* ------------------ Helper Methods for Enhanced PDF Report ------------------ */

    /**
     * MÉTODO OBSOLETO - Ya no es necesario registrar fuentes para gráficos.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private void registerFallbackFont() {
        try {
            InputStream fontStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("fonts/DejaVuSans.ttf");
            if (fontStream != null) {
                java.awt.Font dejaVuFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontStream);
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(dejaVuFont);
                logger.debug("Fuente DejaVu Sans registrada exitosamente para modo headless");
            } else {
                logger.warn("No se encontró fuente DejaVu Sans en resources/fonts/");
            }
        } catch (Exception e) {
            logger.warn("No se pudo registrar fuente DejaVu Sans (modo headless): {}", e.getMessage());
        }
    }

    /**
     * Calcula métricas clave del periodo de ventas.
     * Retorna un mapa con: totalVentas, cantidadVentas, ticketPromedio, crecimientoPorcentual
     */
    private Map<String, Object> computeMetrics(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Métricas del periodo actual
        BigDecimal totalVentas = safeBig(ventaRepository.totalVentasBetween(from, to));
        Long cantidadVentas = Optional.ofNullable(ventaRepository.countVentasBetween(from, to)).orElse(0L);
        BigDecimal ticketPromedio = (cantidadVentas > 0) 
            ? totalVentas.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP) 
            : BigDecimal.ZERO;
        
        metrics.put("totalVentas", totalVentas);
        metrics.put("cantidadVentas", cantidadVentas);
        metrics.put("ticketPromedio", ticketPromedio);
        
        // Calcular crecimiento comparando con periodo anterior de igual duración
        try {
            long daysDiff = java.time.Duration.between(from, to).toDays();
            LocalDateTime prevFrom = from.minusDays(daysDiff);
            LocalDateTime prevTo = from.minusSeconds(1);
            
            BigDecimal totalPrevio = safeBig(ventaRepository.totalVentasBetween(prevFrom, prevTo));
            BigDecimal crecimiento = null;
            
            if (totalPrevio.compareTo(BigDecimal.ZERO) > 0) {
                crecimiento = totalVentas.subtract(totalPrevio)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalPrevio, 2, RoundingMode.HALF_UP);
            } else if (totalVentas.compareTo(BigDecimal.ZERO) > 0) {
                crecimiento = BigDecimal.valueOf(100); // 100% growth from zero
            }
            
            metrics.put("crecimientoPorcentual", crecimiento);
        } catch (Exception e) {
            logger.warn("No se pudo calcular crecimiento porcentual: {}", e.getMessage());
            metrics.put("crecimientoPorcentual", null);
        }
        
        return metrics;
    }

    /**
     * Construye tabla de ventas por producto con cantidad y monto total.
     * salesByProduct contiene: [productoId, productoNombre, cantidadVendida, totalMonto]
     */
    private PdfPTable buildSalesByProductTable(List<Object[]> salesByProduct, Font headerFont, Font normalFont) {
        PdfPTable table = new PdfPTable(new float[]{6, 2, 3});
        table.setWidthPercentage(80);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        table.addCell(createHeaderCell("Producto", headerFont));
        table.addCell(createHeaderCell("Cantidad", headerFont));
        table.addCell(createHeaderCell("Total Vendido", headerFont));
        
        if (salesByProduct != null && !salesByProduct.isEmpty()) {
            int maxRows = Math.min(salesByProduct.size(), 20); // Top 20 productos
            for (int i = 0; i < maxRows; i++) {
                Object[] row = salesByProduct.get(i);
                String nombre = row[1] == null ? "N/A" : String.valueOf(row[1]);
                Number cantidad = row[2] == null ? 0 : (Number) row[2];
                Number monto = row[3] == null ? 0 : (Number) row[3];
                
                table.addCell(createCell(nombre, normalFont));
                table.addCell(createCell(String.valueOf(cantidad.longValue()), normalFont));
                table.addCell(createCell(formatCurrency(numberToBigDecimal(monto)), normalFont));
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay datos disponibles", normalFont));
            emptyCell.setColspan(3);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        }
        
        table.setSpacingAfter(16f);
        table.setSpacingBefore(8f);
        return table;
    }

    /**
     * Construye tabla de ventas por usuario/vendedor con cantidad de ventas y total.
     * salesByUser contiene: [usuarioId, usuarioNombre, cantidadVentas, totalVendido]
     */
    private PdfPTable buildSalesByUserTable(List<Object[]> salesByUser, Font headerFont, Font normalFont) {
        PdfPTable table = new PdfPTable(new float[]{6, 2, 3});
        table.setWidthPercentage(75);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        table.addCell(createHeaderCell("Vendedor", headerFont));
        table.addCell(createHeaderCell("Nº Ventas", headerFont));
        table.addCell(createHeaderCell("Total Vendido", headerFont));
        
        if (salesByUser != null && !salesByUser.isEmpty()) {
            for (Object[] row : salesByUser) {
                String nombre = row[1] == null ? "N/A" : String.valueOf(row[1]);
                Number cantidad = row[2] == null ? 0 : (Number) row[2];
                Number total = row[3] == null ? 0 : (Number) row[3];
                
                table.addCell(createCell(nombre, normalFont));
                table.addCell(createCell(String.valueOf(cantidad.longValue()), normalFont));
                table.addCell(createCell(formatCurrency(numberToBigDecimal(total)), normalFont));
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay datos disponibles", normalFont));
            emptyCell.setColspan(3);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        }
        
        table.setSpacingAfter(16f);
        table.setSpacingBefore(8f);
        return table;
    }

    /**
     * Genera tabla de ventas por periodo (mensual o diaria según rango de fechas).
     * Reemplaza el gráfico de barras con una tabla detallada.
     * 
     * @param document Documento PDF donde se añadirá la tabla
     * @param from Fecha de inicio
     * @param to Fecha de fin
     * @param headerFont Fuente para encabezados
     * @param normalFont Fuente para contenido
     */
    private void addSalesByPeriodTable(Document document, LocalDateTime from, LocalDateTime to, 
                                       Font headerFont, Font normalFont) throws Exception {
        long daysDiff = java.time.Duration.between(from, to).toDays();
        
        PdfPTable table = new PdfPTable(new float[]{4, 3});
        table.setWidthPercentage(70);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        String periodLabel = daysDiff <= 60 ? "Fecha" : "Mes";
        table.addCell(createHeaderCell(periodLabel, headerFont));
        table.addCell(createHeaderCell("Total Ventas", headerFont));
        
        if (daysDiff <= 60) {
            // Usar datos diarios para rangos cortos
            List<Object[]> ventasPorDia = ventaRepository.ventasPorDiaBetween(from, to);
            if (ventasPorDia != null && !ventasPorDia.isEmpty()) {
                for (Object[] row : ventasPorDia) {
                    Object fecha = row[0];
                    Number total = row[1] == null ? 0 : (Number) row[1];
                    String label = fecha == null ? "N/A" : fecha.toString();
                    
                    table.addCell(createCell(label, normalFont));
                    table.addCell(createCell(formatCurrency(numberToBigDecimal(total)), normalFont));
                }
            } else {
                addNoDataRow(table, 2, normalFont);
            }
        } else {
            // Usar datos mensuales para rangos largos
            List<Object[]> ventasPorMes = ventaRepository.salesByMonthBetween(from, to);
            if (ventasPorMes != null && !ventasPorMes.isEmpty()) {
                for (Object[] row : ventasPorMes) {
                    String mes = row[0] == null ? "N/A" : String.valueOf(row[0]);
                    Number total = row[1] == null ? 0 : (Number) row[1];
                    
                    table.addCell(createCell(mes, normalFont));
                    table.addCell(createCell(formatCurrency(numberToBigDecimal(total)), normalFont));
                }
            } else {
                addNoDataRow(table, 2, normalFont);
            }
        }
        
        table.setSpacingBefore(8f);
        table.setSpacingAfter(16f);
        document.add(table);
        
        // Añadir análisis textual adicional sobre tendencias
        addPeriodAnalysis(document, from, to, normalFont);
    }
    
    /**
     * Genera tabla de participación de top productos con porcentajes.
     * Reemplaza el gráfico de torta con una tabla detallada.
     * 
     * @param document Documento PDF donde se añadirá la tabla
     * @param salesByProduct Datos de ventas por producto
     * @param headerFont Fuente para encabezados
     * @param normalFont Fuente para contenido
     */
    private void addTopProductsParticipationTable(Document document, List<Object[]> salesByProduct,
                                                   Font headerFont, Font normalFont) throws Exception {
        if (salesByProduct == null || salesByProduct.isEmpty()) {
            Paragraph noData = new Paragraph("No hay datos de productos disponibles.", normalFont);
            noData.setSpacingBefore(8f);
            noData.setAlignment(Element.ALIGN_CENTER);
            document.add(noData);
            return;
        }
        
        // Calcular total para porcentajes
        BigDecimal totalGeneral = BigDecimal.ZERO;
        int maxItems = Math.min(salesByProduct.size(), 10);
        for (int i = 0; i < maxItems; i++) {
            Object[] row = salesByProduct.get(i);
            Number monto = row[3] == null ? 0 : (Number) row[3];
            totalGeneral = totalGeneral.add(numberToBigDecimal(monto));
        }
        
        // Crear tabla con columnas: Producto | Monto | Participación %
        PdfPTable table = new PdfPTable(new float[]{5, 3, 2});
        table.setWidthPercentage(80);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        table.addCell(createHeaderCell("Producto", headerFont));
        table.addCell(createHeaderCell("Total Vendido", headerFont));
        table.addCell(createHeaderCell("Participación", headerFont));
        
        for (int i = 0; i < maxItems; i++) {
            Object[] row = salesByProduct.get(i);
            String nombre = row[1] == null ? "N/A" : String.valueOf(row[1]);
            Number monto = row[3] == null ? 0 : (Number) row[3];
            BigDecimal montoBD = numberToBigDecimal(monto);
            
            // Calcular porcentaje de participación
            BigDecimal porcentaje = BigDecimal.ZERO;
            if (totalGeneral.compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = montoBD.multiply(BigDecimal.valueOf(100))
                    .divide(totalGeneral, 1, RoundingMode.HALF_UP);
            }
            
            table.addCell(createCell(nombre, normalFont));
            table.addCell(createCell(formatCurrency(montoBD), normalFont));
            table.addCell(createCell(porcentaje.setScale(1, RoundingMode.HALF_UP) + "%", normalFont));
        }
        
        // Añadir fila de total
        PdfPCell totalLabelCell = createHeaderCell("TOTAL TOP 10", headerFont);
        table.addCell(totalLabelCell);
        table.addCell(createCell(formatCurrency(totalGeneral), normalFont));
        table.addCell(createCell("100.0%", normalFont));
        
        table.setSpacingBefore(8f);
        table.setSpacingAfter(16f);
        document.add(table);
    }
    
    /**
     * Añade análisis textual sobre tendencias del periodo.
     */
    private void addPeriodAnalysis(Document document, LocalDateTime from, LocalDateTime to, Font normalFont) throws Exception {
        long daysDiff = java.time.Duration.between(from, to).toDays();
        
        Paragraph analysis = new Paragraph();
        analysis.setFont(normalFont);
        analysis.setSpacingBefore(8f);
        analysis.setSpacingAfter(12f);
        analysis.setAlignment(Element.ALIGN_JUSTIFIED);
        
        StringBuilder text = new StringBuilder();
        text.append("ANÁLISIS DE TENDENCIAS: ");
        
        if (daysDiff <= 60) {
            List<Object[]> ventasPorDia = ventaRepository.ventasPorDiaBetween(from, to);
            if (ventasPorDia != null && ventasPorDia.size() > 1) {
                // Encontrar día con mayores y menores ventas
                Object[] maxDay = ventasPorDia.get(0);
                Object[] minDay = ventasPorDia.get(0);
                BigDecimal maxAmount = BigDecimal.ZERO;
                BigDecimal minAmount = new BigDecimal("999999999");
                
                for (Object[] row : ventasPorDia) {
                    BigDecimal amount = numberToBigDecimal((Number) row[1]);
                    if (amount.compareTo(maxAmount) > 0) {
                        maxAmount = amount;
                        maxDay = row;
                    }
                    if (amount.compareTo(minAmount) < 0) {
                        minAmount = amount;
                        minDay = row;
                    }
                }
                
                text.append("El día con mayores ventas fue ")
                    .append(maxDay[0])
                    .append(" con ")
                    .append(formatCurrency(maxAmount))
                    .append(". ");
                text.append("El día con menores ventas fue ")
                    .append(minDay[0])
                    .append(" con ")
                    .append(formatCurrency(minAmount))
                    .append(".");
            } else {
                text.append("Período corto sin suficientes datos para análisis de tendencias detallado.");
            }
        } else {
            List<Object[]> ventasPorMes = ventaRepository.salesByMonthBetween(from, to);
            if (ventasPorMes != null && ventasPorMes.size() > 1) {
                // Encontrar mes con mayores y menores ventas
                Object[] maxMonth = ventasPorMes.get(0);
                Object[] minMonth = ventasPorMes.get(0);
                BigDecimal maxAmount = BigDecimal.ZERO;
                BigDecimal minAmount = new BigDecimal("999999999");
                
                for (Object[] row : ventasPorMes) {
                    BigDecimal amount = numberToBigDecimal((Number) row[1]);
                    if (amount.compareTo(maxAmount) > 0) {
                        maxAmount = amount;
                        maxMonth = row;
                    }
                    if (amount.compareTo(minAmount) < 0) {
                        minAmount = amount;
                        minMonth = row;
                    }
                }
                
                text.append("El mes con mayores ventas fue ")
                    .append(maxMonth[0])
                    .append(" con ")
                    .append(formatCurrency(maxAmount))
                    .append(". ");
                text.append("El mes con menores ventas fue ")
                    .append(minMonth[0])
                    .append(" con ")
                    .append(formatCurrency(minAmount))
                    .append(".");
            } else {
                text.append("Datos mensuales disponibles para el período analizado.");
            }
        }
        
        analysis.add(text.toString());
        document.add(analysis);
    }
    
    /**
     * Añade una fila de "No hay datos" a una tabla.
     */
    private void addNoDataRow(PdfPTable table, int colspan, Font normalFont) {
        PdfPCell noDataCell = new PdfPCell(new Phrase("No hay datos disponibles para este período", normalFont));
        noDataCell.setColspan(colspan);
        noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        noDataCell.setPadding(10);
        table.addCell(noDataCell);
    }

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private JFreeChart createMonthlySalesChart(LocalDateTime from, LocalDateTime to) {
        long daysDiff = java.time.Duration.between(from, to).toDays();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        if (daysDiff <= 60) {
            // Usar datos diarios para rangos cortos
            List<Object[]> ventasPorDia = ventaRepository.ventasPorDiaBetween(from, to);
            if (ventasPorDia != null) {
                for (Object[] row : ventasPorDia) {
                    Object fecha = row[0];
                    Number total = row[1] == null ? 0 : (Number) row[1];
                    String label = fecha == null ? "N/A" : fecha.toString();
                    dataset.addValue(total.doubleValue(), "Ventas", label);
                }
            }
        } else {
            // Usar datos mensuales para rangos largos
            List<Object[]> ventasPorMes = ventaRepository.salesByMonthBetween(from, to);
            if (ventasPorMes != null) {
                for (Object[] row : ventasPorMes) {
                    String mes = row[0] == null ? "N/A" : String.valueOf(row[0]);
                    Number total = row[1] == null ? 0 : (Number) row[1];
                    dataset.addValue(total.doubleValue(), "Ventas", mes);
                }
            }
        }
        
        String xAxisLabel = daysDiff <= 60 ? "Fecha" : "Mes";
        JFreeChart chart = ChartFactory.createBarChart(
            "", xAxisLabel, "Total Ventas", dataset, 
            PlotOrientation.VERTICAL, false, true, false
        );
        applyCategoryChartStyle(chart);
        return chart;
    }

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private JFreeChart createTopProductsPieChart(List<Object[]> salesByProduct) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        if (salesByProduct != null && !salesByProduct.isEmpty()) {
            int maxItems = Math.min(salesByProduct.size(), 10);
            for (int i = 0; i < maxItems; i++) {
                Object[] row = salesByProduct.get(i);
                String nombre = row[1] == null ? "N/A" : String.valueOf(row[1]);
                Number monto = row[3] == null ? 0 : (Number) row[3];
                dataset.setValue(nombre, monto.doubleValue());
            }
        }
        
        JFreeChart chart = ChartFactory.createPieChart("", dataset, true, true, false);
        applyPieChartStyle(chart);
        return chart;
    }

    /**
     * Genera análisis textual con insights basados en los datos de ventas.
     */
    private String generateTextualAnalysis(Map<String, Object> metrics, 
                                          List<Object[]> salesByProduct, 
                                          List<Object[]> salesByUser) {
        StringBuilder analysis = new StringBuilder();
        
        // Resumen general
        BigDecimal totalVentas = (BigDecimal) metrics.get("totalVentas");
        Long cantidadVentas = (Long) metrics.get("cantidadVentas");
        BigDecimal ticketPromedio = (BigDecimal) metrics.get("ticketPromedio");
        BigDecimal crecimiento = (BigDecimal) metrics.get("crecimientoPorcentual");
        
        analysis.append("RESUMEN GENERAL\n\n");
        analysis.append(String.format("Durante el periodo analizado se registraron %d ventas por un total de %s, ",
            cantidadVentas, formatCurrency(totalVentas)));
        analysis.append(String.format("con un ticket promedio de %s. ", formatCurrency(ticketPromedio)));
        
        if (crecimiento != null) {
            if (crecimiento.compareTo(BigDecimal.ZERO) > 0) {
                analysis.append(String.format("Las ventas crecieron un %s respecto al periodo anterior. ", 
                    formatPercentage(crecimiento)));
            } else if (crecimiento.compareTo(BigDecimal.ZERO) < 0) {
                analysis.append(String.format("Las ventas disminuyeron un %s respecto al periodo anterior. ", 
                    formatPercentage(crecimiento.abs())));
            } else {
                analysis.append("Las ventas se mantuvieron estables respecto al periodo anterior. ");
            }
        }
        
        analysis.append("\n\n");
        
        // Análisis de productos
        if (salesByProduct != null && !salesByProduct.isEmpty()) {
            analysis.append("PRODUCTOS\n\n");
            Object[] topProduct = salesByProduct.get(0);
            String topProductName = topProduct[1] == null ? "N/A" : String.valueOf(topProduct[1]);
            Number topProductQty = topProduct[2] == null ? 0 : (Number) topProduct[2];
            Number topProductAmount = topProduct[3] == null ? 0 : (Number) topProduct[3];
            
            analysis.append(String.format("El producto más vendido fue '%s' con %d unidades vendidas, ",
                topProductName, topProductQty.longValue()));
            analysis.append(String.format("generando ingresos de %s. ", 
                formatCurrency(numberToBigDecimal(topProductAmount))));
            
            // Productos con baja rotación (bottom 5)
            if (salesByProduct.size() > 5) {
                analysis.append("\n\nProductos con menor rotación: ");
                int startIdx = Math.max(0, salesByProduct.size() - 5);
                for (int i = startIdx; i < salesByProduct.size(); i++) {
                    Object[] row = salesByProduct.get(i);
                    String nombre = row[1] == null ? "N/A" : String.valueOf(row[1]);
                    Number qty = row[2] == null ? 0 : (Number) row[2];
                    analysis.append(String.format("%s (%d unidades)", nombre, qty.longValue()));
                    if (i < salesByProduct.size() - 1) analysis.append(", ");
                    else analysis.append(".");
                }
            }
        }
        
        analysis.append("\n\n");
        
        // Análisis de vendedores
        if (salesByUser != null && !salesByUser.isEmpty()) {
            analysis.append("VENDEDORES\n\n");
            Object[] topSeller = salesByUser.get(0);
            String topSellerName = topSeller[1] == null ? "N/A" : String.valueOf(topSeller[1]);
            Number topSellerSales = topSeller[2] == null ? 0 : (Number) topSeller[2];
            Number topSellerAmount = topSeller[3] == null ? 0 : (Number) topSeller[3];
            
            analysis.append(String.format("El vendedor con mejor desempeño fue %s con %d ventas, ",
                topSellerName, topSellerSales.longValue()));
            analysis.append(String.format("totalizando %s en ventas.", 
                formatCurrency(numberToBigDecimal(topSellerAmount))));
        }
        
        return analysis.toString();
    }

    /**
     * Formatea un BigDecimal como moneda (con símbolo $).
     */
    private String formatCurrency(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value) + " $";
    }

    /**
     * Formatea un BigDecimal como porcentaje.
     */
    private String formatPercentage(BigDecimal value) {
        if (value == null) return "0%";
        return value.setScale(1, RoundingMode.HALF_UP).toString() + "%";
    }

    /* ------------------ Informe avanzado Excel (streaming) ------------------ */

    /**
     * Genera el reporte avanzado en formato Excel (.xlsx) usando Apache POI.
     * Similar al PDF pero sin gráficos (solo datos tabulares).
     * No requiere bibliotecas nativas, funciona en Railway.
     *
     * NOTA: Este método NO genera gráficos, solo usa Apache POI para tablas.
     * Es seguro en entornos sin bibliotecas nativas (Railway).
     *
     * @param from Fecha inicio del periodo
     * @param to Fecha fin del periodo
     * @param productoId Filtro opcional por producto
     * @return Datos del Excel como byte array para streaming
     * @throws Exception Si hay error al generar el Excel
     */
    public byte[] generarReporteVentasExcel(LocalDateTime from, LocalDateTime to, Long productoId) throws Exception {
        logger.info("Generando reporte avanzado Excel: from={}, to={}, productoId={}", from, to, productoId);

        try {
            // Recopilar datos (misma lógica que el PDF)
            BigDecimal totalVentas = safeBig(ventaRepository.totalVentasBetween(from, to));
            Long count = Optional.ofNullable(ventaRepository.countVentasBetween(from, to)).orElse(0L);
            List<Object[]> topProductos = ventaRepository.topProductosBetween(from, to);
            List<Object[]> topVendedores = ventaRepository.topVendedoresBetween(from, to);
            List<Object[]> stockList = productoRepository.stockProductos();
            List<Object[]> ventasPorDia = ventaRepository.ventasPorDiaBetween(from, to);

            // Generar Excel usando la utilidad
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExcelReportUtilAvanzado.generarReporteAvanzadoExcel(
                    from, to, totalVentas, count,
                    topProductos, topVendedores, stockList, ventasPorDia,
                    baos
            );
            logger.info("Reporte avanzado Excel generado exitosamente: {} bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error al generar reporte avanzado Excel: {}", e.getMessage(), e);
            throw e; // Re-throw to allow controller to handle with proper HTTP error response
        }
    }

    /* ------------------ "Mis ventas" export (a partir de LISTA filtrada por la UI) ------------------ */

    public byte[] generarMisVentasPdfFromList(List<Venta> ventas, LocalDateTime from, LocalDateTime to, String empleadoNombre) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 72, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        Image logo = loadLogoIfExists();
        Font pdfNormalFont = getPdfNormalFont();
        Font pdfHeaderFont = getPdfHeaderFont();

        try {
            HeaderFooterEvent event = new HeaderFooterEvent(logo, pdfHeaderFont, "Historial de Ventas");
            writer.setPageEvent(event);
        } catch (Exception ignored) {}

        document.open();

        Paragraph title = new Paragraph("Historial de ventas", pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        DateTimeFormatter dtfShort = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo = "Periodo: " + (from != null ? from.toLocalDate().format(dtfShort) : "") + " - " + (to != null ? to.toLocalDate().format(dtfShort) : "");
        Paragraph period = new Paragraph(periodo, pdfNormalFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(8f);
        document.add(period);

        PdfPTable table = new PdfPTable(new float[]{1, 2, 2, 2, 5});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.addCell(createHeaderCell("ID", pdfHeaderFont));
        table.addCell(createHeaderCell("Fecha", pdfHeaderFont));
        table.addCell(createHeaderCell("Método pago", pdfHeaderFont));
        table.addCell(createHeaderCell("Total", pdfHeaderFont));
        table.addCell(createHeaderCell("Productos", pdfHeaderFont));

        DateTimeFormatter dtfFull = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        boolean odd = true;
        BigDecimal totalSum = BigDecimal.ZERO;
        if (ventas != null) {
            for (Venta v : ventas) {
                PdfPCell idCell = createCell(String.valueOf(v.getId()), pdfNormalFont);
                PdfPCell fechaCell = createCell(v.getFecha() != null ? v.getFecha().format(dtfFull) : "N/A", pdfNormalFont);
                PdfPCell metodoCell = createCell(v.getMetodoPago() != null ? v.getMetodoPago() : "N/A", pdfNormalFont);
                BigDecimal t = numberToBigDecimal(v.getTotal());
                PdfPCell totalCell = createCell(formatPeso(nf, t), pdfNormalFont);

                StringBuilder sb = new StringBuilder();
                if (v.getDetalles() != null) {
                    for (DetalleVenta d : v.getDetalles()) {
                        String prodName = d.getProducto() != null ? d.getProducto().getNombre() : "N/A";
                        BigDecimal price = numberToBigDecimal(d.getPrecioUnitario());
                        long qty = d.getCantidad() == null ? 0L : d.getCantidad().longValue();
                        sb.append(prodName)
                                .append(" (").append(qty).append(" x ").append(formatPeso(nf, price)).append(")")
                                .append("\n");
                    }
                }
                PdfPCell prodCell = createCell(sb.toString(), pdfNormalFont);

                if (odd) {
                    BaseColor alt = new BaseColor(0xF9, 0xF9, 0xF9);
                    idCell.setBackgroundColor(alt);
                    fechaCell.setBackgroundColor(alt);
                    metodoCell.setBackgroundColor(alt);
                    totalCell.setBackgroundColor(alt);
                    prodCell.setBackgroundColor(alt);
                }
                odd = !odd;

                table.addCell(idCell);
                table.addCell(fechaCell);
                table.addCell(metodoCell);
                table.addCell(totalCell);
                table.addCell(prodCell);

                totalSum = totalSum.add(t);
            }
        }

        document.add(table);

        Paragraph suma = new Paragraph("Total ventas: " + formatPeso(nf, totalSum), pdfHeaderFont);
        suma.setAlignment(Element.ALIGN_RIGHT);
        suma.setSpacingBefore(8f);
        document.add(suma);

        // Construir mapa de ventas por producto
        Map<String, Long> ventasPorProducto = new LinkedHashMap<>();
        if (ventas != null) {
            for (Venta v : ventas) {
                if (v.getDetalles() == null) continue;
                for (DetalleVenta d : v.getDetalles()) {
                    String nombre = d.getProducto() == null ? "N/A" : d.getProducto().getNombre();
                    long add = d.getCantidad() == null ? 0L : d.getCantidad().longValue();
                    ventasPorProducto.merge(nombre, add, Long::sum);
                }
            }
        }

        // Tabla de ventas por producto (reemplaza el gráfico anterior)
        if (!ventasPorProducto.isEmpty()) {
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("VENTAS POR PRODUCTO", pdfHeaderFont));
            
            PdfPTable prodTable = new PdfPTable(new float[]{6, 2});
            prodTable.setWidthPercentage(70);
            prodTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            prodTable.addCell(createHeaderCell("Producto", pdfHeaderFont));
            prodTable.addCell(createHeaderCell("Cantidad", pdfHeaderFont));
            
            // Ordenar por cantidad descendente
            ventasPorProducto.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    prodTable.addCell(createCell(entry.getKey(), pdfNormalFont));
                    prodTable.addCell(createCell(String.valueOf(entry.getValue()), pdfNormalFont));
                });
            
            prodTable.setSpacingBefore(8f);
            prodTable.setSpacingAfter(12f);
            document.add(prodTable);
        }

        document.close();
        return baos.toByteArray();
    }

    /* ------------------ Resumen corto (Exportar PDF resumen) ------------------ */

    public byte[] generarResumenVentasPdf(LocalDateTime from, LocalDateTime to,
                                          Long usuarioId, Long productoId, String metodoPago) throws Exception {
        List<Venta> ventasFiltradas = ventaRepository.filtrarAdmin(from, to, usuarioId, productoId, metodoPago);

        BigDecimal totalVentas = ventasFiltradas.stream()
                .map(v -> numberToBigDecimal(v.getTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = ventasFiltradas.size();
        BigDecimal avg = (count > 0) ? totalVentas.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<Long, ProductAgg> productoAgg = new HashMap<>();
        for (Venta v : ventasFiltradas) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                if (d == null || d.getProducto() == null) continue;
                Long pid = d.getProducto().getId();
                ProductAgg pa = productoAgg.computeIfAbsent(pid, k -> new ProductAgg(pid, d.getProducto().getNombre(), 0L));
                long add = d.getCantidad() == null ? 0L : d.getCantidad().longValue();
                pa.cantidad += add;
            }
        }
        List<ProductAgg> topProductos = productoAgg.values().stream()
                .sorted(Comparator.comparingLong(ProductAgg::getCantidad).reversed())
                .collect(Collectors.toList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        Image logo = loadLogoIfExists();
        Font pdfNormalFont = getPdfNormalFont();
        Font pdfHeaderFont = getPdfHeaderFont();
        try {
            HeaderFooterEvent event = new HeaderFooterEvent(logo, pdfHeaderFont, "Resumen de Ventas");
            writer.setPageEvent(event);
        } catch (Exception ignored) {}

        document.open();

        Paragraph title = new Paragraph("Resumen de Ventas", pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph("Periodo: " + (from != null ? from.toLocalDate() : "") + " - " + (to != null ? to.toLocalDate() : ""), pdfNormalFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(10f);
        document.add(period);

        PdfPTable resumenTable = new PdfPTable(2);
        resumenTable.setWidths(new int[]{3, 2});
        resumenTable.setWidthPercentage(50);
        resumenTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        resumenTable.addCell(createHeaderCell("Ventas totales (monto)", pdfHeaderFont));
        resumenTable.addCell(createCell(formatSimple(totalVentas), pdfNormalFont));
        resumenTable.addCell(createHeaderCell("Número de ventas", pdfHeaderFont));
        resumenTable.addCell(createCell(String.valueOf(count), pdfNormalFont));
        resumenTable.addCell(createHeaderCell("Venta promedio", pdfHeaderFont));
        resumenTable.addCell(createCell(formatSimple(avg), pdfNormalFont));
        resumenTable.setSpacingAfter(12f);
        document.add(resumenTable);

        document.add(new Paragraph("Top productos (top 5)", pdfHeaderFont));
        PdfPTable prodTable = new PdfPTable(new float[]{6, 2});
        prodTable.setWidthPercentage(60);
        prodTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        prodTable.addCell(createHeaderCell("Producto", pdfHeaderFont));
        prodTable.addCell(createHeaderCell("Cantidad", pdfHeaderFont));
        int maxRows2 = Math.min(5, topProductos.size());
        for (int i = 0; i < maxRows2; i++) {
            ProductAgg p = topProductos.get(i);
            prodTable.addCell(createCell(p.nombre, pdfNormalFont));
            prodTable.addCell(createCell(String.valueOf(p.cantidad), pdfNormalFont));
        }
        prodTable.setSpacingAfter(12f);
        document.add(prodTable);

        if (!ventasFiltradas.isEmpty()) {
            document.add(new Paragraph("Ventas filtradas", pdfHeaderFont));
            PdfPTable ventasTable = new PdfPTable(new float[]{1, 2, 3, 2, 4});
            ventasTable.setWidthPercentage(95);
            ventasTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            ventasTable.addCell(createHeaderCell("ID", pdfHeaderFont));
            ventasTable.addCell(createHeaderCell("Fecha", pdfHeaderFont));
            ventasTable.addCell(createHeaderCell("Empleado", pdfHeaderFont));
            ventasTable.addCell(createHeaderCell("Método pago", pdfHeaderFont));
            ventasTable.addCell(createHeaderCell("Productos (cantidad x precio)", pdfHeaderFont));
            for (Venta v : ventasFiltradas) {
                ventasTable.addCell(createCell(String.valueOf(v.getId()), pdfNormalFont));
                ventasTable.addCell(createCell(v.getFecha() == null ? "N/A" : v.getFecha().toString(), pdfNormalFont));
                ventasTable.addCell(createCell(v.getUsuario() == null ? "N/A" : v.getUsuario().getNombre(), pdfNormalFont));
                ventasTable.addCell(createCell(v.getMetodoPago() == null ? "N/A" : v.getMetodoPago(), pdfNormalFont));
                StringBuilder sb = new StringBuilder();
                if (v.getDetalles() != null) {
                    for (DetalleVenta d : v.getDetalles()) {
                        sb.append(d.getProducto().getNombre())
                                .append(" (").append(d.getCantidad()).append(" x ").append(d.getPrecioUnitario()).append(")");
                        sb.append("\n");
                    }
                }
                ventasTable.addCell(createCell(sb.toString(), pdfNormalFont));
            }
            ventasTable.setSpacingAfter(12f);
            document.add(ventasTable);
        }

        document.close();
        return baos.toByteArray();
    }

    /* ------------------ Compatibilidad: método antiguo delegado ------------------ */

    /**
     * Mantener la firma antigua para compatibilidad con controladores existentes.
     * Delegamos al nuevo diseño estilo (puedes cambiar el comercioNombre que aparecerá en el header).
     */
    public byte[] generarReciboVentaPdf(Venta venta) throws Exception {
        // Delegamos al nuevo diseño; cambia "ALEMANDAN" por el nombre que prefieras.
        return generarReciboVentaPdfEstilo(venta, "ALEMANDAN");
    }

    /* ------------------ Diseño NUEVO: recibo estilo columnar / secciones por categoría ------------------ */

    public byte[] generarReciboVentaPdfEstilo(Venta venta, String comercioNombre) throws Exception {
        if (venta == null) throw new IllegalArgumentException("Venta es null");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        document.open();

        // Fonts
        Font brandFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLACK);
        Font facturaFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
        Font metaFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
        Font itemFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
        Font priceFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
        Font subtotalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);

        // Header: left logo only, right factura info (uses venta.id as number)
        PdfPTable header = new PdfPTable(new float[]{1f, 2f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(6f);

        Image logo = loadLogoIfExists();
        if (logo != null) {
            PdfPCell logoCell = new PdfPCell(logo, true);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setPadding(4f);
            header.addCell(logoCell);
        } else {
            PdfPCell brandCell = new PdfPCell(new Phrase(comercioNombre == null ? "" : comercioNombre, brandFont));
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            brandCell.setPadding(6f);
            header.addCell(brandCell);
        }

        PdfPTable right = new PdfPTable(1);
        right.setWidthPercentage(100);

        String facturaLabel = "FACTURA";
        String facturaNum = venta.getId() == null ? "-" : String.format("%06d", venta.getId());
        Paragraph pFactura = new Paragraph();
        pFactura.add(new Phrase(facturaLabel + " ", facturaFont));
        pFactura.add(new Phrase("#" + facturaNum, facturaFont));
        PdfPCell facturaCell = new PdfPCell(pFactura);
        facturaCell.setBorder(Rectangle.NO_BORDER);
        facturaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        facturaCell.setPadding(6f);
        right.addCell(facturaCell);

        String cajero = (venta.getUsuario() != null && venta.getUsuario().getNombre() != null) ? venta.getUsuario().getNombre() : "-";
        String metodo = venta.getMetodoPago() == null ? "-" : venta.getMetodoPago();
        PdfPCell cajeroCell = new PdfPCell(new Phrase("Cajero: " + cajero, metaFont));
        cajeroCell.setBorder(Rectangle.NO_BORDER);
        cajeroCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cajeroCell.setPaddingBottom(2f);
        right.addCell(cajeroCell);

        PdfPCell metodoCell = new PdfPCell(new Phrase("Método: " + metodo, metaFont));
        metodoCell.setBorder(Rectangle.NO_BORDER);
        metodoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metodoCell.setPaddingBottom(2f);
        right.addCell(metodoCell);

        header.addCell(right);
        document.add(header);

        // Date row
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fecha = venta.getFecha() != null ? venta.getFecha().format(dtf) : "";
        Paragraph dateP = new Paragraph(fecha, metaFont);
        dateP.setSpacingAfter(2f);
        document.add(dateP);
        
        // Buyer information (if provided)
        if (venta.getCompradorNombre() != null && !venta.getCompradorNombre().trim().isEmpty()) {
            Paragraph buyerP = new Paragraph("Cliente: " + venta.getCompradorNombre(), metaFont);
            buyerP.setSpacingAfter(2f);
            document.add(buyerP);
        }
        if (venta.getCompradorCedula() != null && !venta.getCompradorCedula().trim().isEmpty()) {
            Paragraph cedulaP = new Paragraph("Cédula: " + venta.getCompradorCedula(), metaFont);
            cedulaP.setSpacingAfter(6f);
            document.add(cedulaP);
        } else if (venta.getCompradorNombre() != null && !venta.getCompradorNombre().trim().isEmpty()) {
            // Add spacing if only name was provided
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(6f);
            document.add(spacer);
        }

        // Group detalles by category name
        Map<String, List<DetalleVenta>> byCategory = new LinkedHashMap<>();
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                String cat = "GENERAL";
                if (d.getProducto() != null && d.getProducto().getCategoria() != null && d.getProducto().getCategoria().getNombre() != null) {
                    cat = d.getProducto().getCategoria().getNombre().toUpperCase();
                }
                byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(d);
            }
        }

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        BigDecimal grandSubtotal = BigDecimal.ZERO;
        BigDecimal grandIva = BigDecimal.ZERO;

        for (Map.Entry<String, List<DetalleVenta>> entry : byCategory.entrySet()) {
            String catName = entry.getKey();
            List<DetalleVenta> details = entry.getValue();

            Paragraph secTitle = new Paragraph(catName, sectionFont);
            secTitle.setSpacingBefore(8f);
            secTitle.setSpacingAfter(4f);
            document.add(secTitle);

            PdfPTable items = new PdfPTable(new float[]{4f, 1f});
            items.setWidthPercentage(100);
            BigDecimal sectionSubtotal = BigDecimal.ZERO;
            BigDecimal sectionIva = BigDecimal.ZERO;

            for (DetalleVenta d : details) {
                String nombre = d.getProducto() != null && d.getProducto().getNombre() != null ? d.getProducto().getNombre() : "N/A";
                BigDecimal precioUnit = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                Integer qty = d.getCantidad() == null ? 0 : d.getCantidad();
                BigDecimal qtyBD = BigDecimal.valueOf(qty);
                BigDecimal lineaSubtotal = precioUnit.multiply(qtyBD);
                BigDecimal ivaMonto = d.getIvaMonto() == null ? BigDecimal.ZERO : d.getIvaMonto();

                String leftText = nombre;
                if (qty > 1) leftText += "  x" + qty;
                PdfPCell nameCell = new PdfPCell(new Phrase(leftText, itemFont));
                nameCell.setBorder(Rectangle.NO_BORDER);
                nameCell.setPadding(4f);
                items.addCell(nameCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(nf.format(precioUnit.setScale(2, RoundingMode.HALF_UP)), priceFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setBorder(Rectangle.NO_BORDER);
                priceCell.setPadding(4f);
                items.addCell(priceCell);

                // discounts via reflection
                BigDecimal discountRate = null;
                BigDecimal discountAmount = null;
                try {
                    Method m = d.getClass().getMethod("getDescuentoRate");
                    Object val = m.invoke(d);
                    if (val instanceof BigDecimal) discountRate = (BigDecimal) val;
                    else if (val instanceof Number) discountRate = new BigDecimal(((Number) val).toString());
                } catch (Exception ignored) {}
                try {
                    Method m = d.getClass().getMethod("getDescuentoAmount");
                    Object val = m.invoke(d);
                    if (val instanceof BigDecimal) discountAmount = (BigDecimal) val;
                    else if (val instanceof Number) discountAmount = new BigDecimal(((Number) val).toString());
                } catch (Exception ignored) {}

                if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal disc = lineaSubtotal.multiply(discountRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    PdfPCell dName = new PdfPCell(new Phrase("   -" + discountRate.setScale(0, RoundingMode.HALF_UP).toString() + "%", new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)));
                    dName.setBorder(Rectangle.NO_BORDER);
                    dName.setPaddingLeft(8f);
                    items.addCell(dName);
                    PdfPCell dPrice = new PdfPCell(new Phrase("-" + nf.format(disc.setScale(2, RoundingMode.HALF_UP)), new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)));
                    dPrice.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    dPrice.setBorder(Rectangle.NO_BORDER);
                    items.addCell(dPrice);
                    lineaSubtotal = lineaSubtotal.subtract(disc);
                } else if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) != 0) {
                    PdfPCell dName = new PdfPCell(new Phrase("   -Descuento", new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)));
                    dName.setBorder(Rectangle.NO_BORDER);
                    dName.setPaddingLeft(8f);
                    items.addCell(dName);
                    PdfPCell dPrice = new PdfPCell(new Phrase("-" + nf.format(discountAmount.setScale(2, RoundingMode.HALF_UP)), new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)));
                    dPrice.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    dPrice.setBorder(Rectangle.NO_BORDER);
                    items.addCell(dPrice);
                    lineaSubtotal = lineaSubtotal.subtract(discountAmount);
                }

                sectionSubtotal = sectionSubtotal.add(lineaSubtotal);
                sectionIva = sectionIva.add(ivaMonto);
            }

            items.setSpacingAfter(6f);
            document.add(items);

            PdfPTable subt = new PdfPTable(new float[]{4f, 1f});
            subt.setWidthPercentage(100);
            PdfPCell subLabel = new PdfPCell(new Phrase("SUBTOTAL", subtotalFont));
            subLabel.setBorder(Rectangle.NO_BORDER);
            subLabel.setPaddingTop(6f);
            subLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            subt.addCell(subLabel);
            PdfPCell subValue = new PdfPCell(new Phrase(nf.format(sectionSubtotal.setScale(2, RoundingMode.HALF_UP)), subtotalFont));
            subValue.setBorder(Rectangle.NO_BORDER);
            subValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            subValue.setPaddingTop(6f);
            subt.addCell(subValue);
            subt.setSpacingAfter(10f);
            document.add(subt);

            grandSubtotal = grandSubtotal.add(sectionSubtotal);
            grandIva = grandIva.add(sectionIva);

            document.add(createDashedSeparator());
        }

        // Totales generales
        PdfPTable totals = new PdfPTable(new float[]{4f, 1f});
        totals.setWidthPercentage(100);
        totals.setSpacingBefore(12f);

        PdfPCell stLabel = new PdfPCell(new Phrase("Subtotal:", itemFont));
        stLabel.setBorder(Rectangle.NO_BORDER);
        totals.addCell(stLabel);
        PdfPCell stValue = new PdfPCell(new Phrase(nf.format(grandSubtotal.setScale(2, RoundingMode.HALF_UP)), itemFont));
        stValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        stValue.setBorder(Rectangle.NO_BORDER);
        totals.addCell(stValue);

        PdfPCell ivaLabel = new PdfPCell(new Phrase("IVA:", itemFont));
        ivaLabel.setBorder(Rectangle.NO_BORDER);
        totals.addCell(ivaLabel);
        PdfPCell ivaValue = new PdfPCell(new Phrase(nf.format(grandIva.setScale(2, RoundingMode.HALF_UP)), itemFont));
        ivaValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ivaValue.setBorder(Rectangle.NO_BORDER);
        totals.addCell(ivaValue);

        PdfPCell totalLabel = new PdfPCell(new Phrase("Total:", subtotalFont));
        totalLabel.setBorder(Rectangle.NO_BORDER);
        totals.addCell(totalLabel);
        PdfPCell totalValue = new PdfPCell(new Phrase(nf.format(grandSubtotal.add(grandIva).setScale(2, RoundingMode.HALF_UP)), subtotalFont));
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setBorder(Rectangle.NO_BORDER);
        totals.addCell(totalValue);

        document.add(totals);

        document.close();
        return baos.toByteArray();
    }

    // Helper: create a dashed separator as a paragraph (simple approach)
    private Paragraph createDashedSeparator() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(6f);
        p.setSpacingAfter(6f);
        p.add(new Phrase("────────────────────────────────────────────────────────────────────────────────"));
        return p;
    }

    /* ------------------ Generar PDF desde imagen base64 (Exportar gráfica PDF) ------------------ */

    public byte[] generarPdfFromChartBase64(String imgBase64, String titulo, String fecha) throws Exception {
        if (imgBase64 == null) throw new IllegalArgumentException("imgBase64 es null");
        String base64 = imgBase64;
        if (base64.contains(",")) base64 = base64.substring(base64.indexOf(',') + 1);
        byte[] imageBytes = Base64.getDecoder().decode(base64);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 72, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        Image logoForHeader = loadLogoIfExists();
        Font pdfNormalFont = getPdfNormalFont();
        Font pdfHeaderFont = getPdfHeaderFont();
        try { writer.setPageEvent(new HeaderFooterEvent(logoForHeader, pdfHeaderFont, titulo == null ? "Gráfica" : titulo)); } catch (Exception ignored) {}

        document.open();

        Paragraph title = new Paragraph(titulo == null ? "Gráfica" : titulo, pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        if (fecha != null && !fecha.isBlank()) {
            Paragraph pfecha = new Paragraph(fecha, pdfNormalFont);
            pfecha.setAlignment(Element.ALIGN_CENTER);
            pfecha.setSpacingAfter(8f);
            document.add(pfecha);
        }

        Image img = Image.getInstance(imageBytes);
        float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 40f;
        if (img.getWidth() > maxWidth) {
            float scale = maxWidth / img.getWidth();
            img.scalePercent(scale * 100f);
        }
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.addElement(img);
        t.addCell(c);
        t.setSpacingBefore(12f);
        t.setSpacingAfter(12f);
        document.add(t);

        document.close();
        return baos.toByteArray();
    }

    /* ------------------ Chart styling / image helpers (DEPRECATED - no longer used) ------------------ */

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private void applyCategoryChartStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(0xF5, 0xF5, 0xF5));
        plot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);
        plot.setOutlineVisible(false);
        plot.setInsets(new RectangleInsets(10, 10, 10, 10));

        if (plot.getRenderer() instanceof BarRenderer) {
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setBarPainter(new StandardBarPainter());
            Paint[] palette = new Paint[]{
                    new Color(0xFF6B6B),
                    new Color(0xFFB77A),
                    new Color(0xFFD76B),
                    new Color(0x8ED1A0),
                    new Color(0x6EC6FF),
                    new Color(0x9B8CFF)
            };
            for (int i = 0; i < palette.length; i++) {
                renderer.setSeriesPaint(i, palette[i % palette.length]);
            }
            renderer.setItemMargin(0.1);
            renderer.setMaximumBarWidth(0.08);
            renderer.setDefaultItemLabelGenerator(new org.jfree.chart.labels.StandardCategoryItemLabelGenerator("{2}", java.text.NumberFormat.getInstance()));
            renderer.setDefaultItemLabelFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
            renderer.setDefaultItemLabelsVisible(true);
        }
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private void applyPieChartStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() instanceof PiePlot) {
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(new Color(0xF5, 0xF5, 0xF5));
            plot.setOutlineVisible(false);
            plot.setSimpleLabels(false);
            plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
            plot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
            if (chart.getLegend() != null) {
                chart.getLegend().setFrame(new BlockBorder(Color.WHITE));
                chart.getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);
            }
        }
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private void addCenteredChartHighDpi(Document document, JFreeChart chart, int width, int height, double scale) throws Exception {
        Image chartImg = createHighDpiImageFromChart(chart, width, height, scale);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(90);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        chartImg.setAlignment(Image.ALIGN_CENTER);
        c.addElement(chartImg);
        t.addCell(c);
        t.setSpacingBefore(8f);
        t.setSpacingAfter(12f);
        document.add(t);
    }

    /**
     * MÉTODO OBSOLETO - Mantenido solo por compatibilidad.
     * Los gráficos han sido reemplazados por tablas para evitar dependencias nativas.
     * @deprecated No se usa más. Los reportes ahora usan tablas en lugar de gráficos.
     */
    @Deprecated
    private Image createHighDpiImageFromChart(JFreeChart chart, int width, int height, double scale) throws Exception {
        int w = (int) (width * scale);
        int h = (int) (height * scale);
        BufferedImage img = chart.createBufferedImage(w, h);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        bos.flush();
        byte[] bytes = bos.toByteArray();
        bos.close();
        Image itextImg = Image.getInstance(bytes);
        itextImg.scaleAbsolute(width, height);
        return itextImg;
    }

    /* ------------------ Utilities: logo, fonts, cells, IO ------------------ */

    private Image loadLogoIfExists() {
        try (InputStream logoIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("static/assets/img/Alogo.png")) {
            if (logoIs != null) {
                byte[] logoBytes = readAllBytes(logoIs);
                Image logoForHeader = Image.getInstance(logoBytes);
                logoForHeader.scaleToFit(80, 80);
                return logoForHeader;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Font getPdfNormalFont() {
        try (InputStream regularIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/Roboto-Regular.ttf")) {
            if (regularIs != null) {
                File tmpReg = File.createTempFile("roboto_reg", ".ttf");
                try (OutputStream os = new FileOutputStream(tmpReg)) {
                    os.write(readAllBytes(regularIs));
                }
                com.itextpdf.text.pdf.BaseFont bfReg = com.itextpdf.text.pdf.BaseFont.createFont(tmpReg.getAbsolutePath(), com.itextpdf.text.pdf.BaseFont.IDENTITY_H, com.itextpdf.text.pdf.BaseFont.EMBEDDED);
                Font f = new Font(bfReg, 10);
                tmpReg.deleteOnExit();
                return f;
            }
        } catch (Exception ignored) {}
        return FontFactory.getFont(FontFactory.HELVETICA, 10);
    }

    private Font getPdfHeaderFont() {
        try (InputStream boldIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("fonts/Roboto-Bold.ttf")) {
            if (boldIs != null) {
                File tmpBold = File.createTempFile("roboto_bold", ".ttf");
                try (OutputStream os = new FileOutputStream(tmpBold)) {
                    os.write(readAllBytes(boldIs));
                }
                com.itextpdf.text.pdf.BaseFont bfBold = com.itextpdf.text.pdf.BaseFont.createFont(tmpBold.getAbsolutePath(), com.itextpdf.text.pdf.BaseFont.IDENTITY_H, com.itextpdf.text.pdf.BaseFont.EMBEDDED);
                Font f = new Font(bfBold, 12, Font.BOLD);
                tmpBold.deleteOnExit();
                return f;
            }
        } catch (Exception ignored) {}
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // internal aggregator for product totals
    private static class ProductAgg {
        Long id;
        String nombre;
        long cantidad;
        ProductAgg(Long id, String nombre, long cantidad) { this.id = id; this.nombre = nombre; this.cantidad = cantidad; }
        long getCantidad() { return cantidad; }
    }

    // helper to convert Number (including BigDecimal) to BigDecimal reliably
    private BigDecimal numberToBigDecimal(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal) return (BigDecimal) n;
        try {
            return new BigDecimal(n.toString());
        } catch (Exception e) {
            // fallback
            return BigDecimal.valueOf(n.doubleValue());
        }
    }

    private String formatPeso(NumberFormat nf, BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        try {
            return nf.format(value) + " $";
        } catch (Exception e) {
            return value.setScale(2, RoundingMode.HALF_UP).toString() + " $";
        }
    }

    private PdfPCell createCell(String text, Font f) {
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell createHeaderCell(String text, Font f) {
        Font fh = f != null ? f : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        PdfPCell cell = new PdfPCell(new Phrase(text, fh));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(6);
        return cell;
    }

    // helper para celdas sin borde (usado por recibos)
    private PdfPCell createCellNoBorder(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        return c;
    }

    // small helpers
    private BigDecimal safeBig(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
    private String formatSimple(BigDecimal b) { return b == null ? "0.00" : b.setScale(2, RoundingMode.HALF_UP).toString(); }
    private String vendaIdToString(Long id) { return id == null ? "-" : id.toString(); }
}