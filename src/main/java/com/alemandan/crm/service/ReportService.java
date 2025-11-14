package com.alemandan.crm.service;

import com.alemandan.crm.model.Producto;
import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.repository.ProductoRepository;
import com.alemandan.crm.repository.VentaRepository;
import com.alemandan.crm.service.pdf.HeaderFooterEvent;
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
 */
@Service
public class ReportService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /* ------------------ Informe avanzado (completo) ------------------ */

    public byte[] generarReporteVentasPdf(LocalDateTime from, LocalDateTime to, Long productoId) throws Exception {
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

        // Título y periodo
        Paragraph title = new Paragraph("Informe de Ventas", pdfHeaderFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph("Periodo: " + (from != null ? from.toLocalDate() : "") + " - " + (to != null ? to.toLocalDate() : ""), pdfNormalFont);
        period.setSpacingAfter(12);
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);

        // Resumen
        BigDecimal totalVentas = safeBig(ventaRepository.totalVentasBetween(from, to));
        Long count = Optional.ofNullable(ventaRepository.countVentasBetween(from, to)).orElse(0L);

        document.add(new Paragraph("Resumen", pdfHeaderFont));
        PdfPTable resumenTable = new PdfPTable(2);
        resumenTable.setWidths(new int[]{3, 2});
        resumenTable.setWidthPercentage(50);
        resumenTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        resumenTable.addCell(createHeaderCell("Ventas totales (monto)", pdfHeaderFont));
        resumenTable.addCell(createCell(formatSimple(totalVentas), pdfNormalFont));
        resumenTable.addCell(createHeaderCell("Número de ventas", pdfHeaderFont));
        resumenTable.addCell(createCell(String.valueOf(count), pdfNormalFont));
        BigDecimal avg = (count > 0) ? totalVentas.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        resumenTable.addCell(createHeaderCell("Venta promedio", pdfHeaderFont));
        resumenTable.addCell(createCell(formatSimple(avg), pdfNormalFont));
        resumenTable.setSpacingAfter(12f);
        resumenTable.setSpacingBefore(6f);
        document.add(resumenTable);

        // Top productos (tabla + gráfico)
        document.add(new Paragraph("Productos más vendidos", pdfHeaderFont));
        List<Object[]> topProductos = ventaRepository.topProductosBetween(from, to);

        PdfPTable prodTable = new PdfPTable(new float[]{6, 2});
        prodTable.setWidthPercentage(70);
        prodTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        prodTable.addCell(createHeaderCell("Producto", pdfHeaderFont));
        prodTable.addCell(createHeaderCell("Cantidad vendida", pdfHeaderFont));
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        if (topProductos != null) {
            int maxRows = Math.min(topProductos.size(), 10);
            for (int i = 0; i < maxRows; i++) {
                Object[] r = topProductos.get(i);
                String nombre = r[1] == null ? "N/A" : String.valueOf(r[1]);
                Number cantidad = r[2] == null ? 0 : (Number) r[2];
                prodTable.addCell(createCell(nombre, pdfNormalFont));
                prodTable.addCell(createCell(String.valueOf(cantidad.longValue()), pdfNormalFont));
                barDataset.addValue(cantidad.doubleValue(), "Ventas", nombre);
            }
        }
        prodTable.setSpacingAfter(8f);
        prodTable.setSpacingBefore(8f);
        document.add(prodTable);

        if (barDataset.getColumnCount() > 0) {
            JFreeChart barChart = ChartFactory.createBarChart("", "Producto", "Cantidad", barDataset, PlotOrientation.VERTICAL, false, true, false);
            applyCategoryChartStyle(barChart);
            addCenteredChartHighDpi(document, barChart, 900, 320, 2.0);
        }

        // Top vendedores global
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Top vendedores (global)", pdfHeaderFont));
        List<Object[]> topVendedores = ventaRepository.topVendedoresBetween(from, to);
        PdfPTable vendTable = new PdfPTable(new float[]{6, 2});
        vendTable.setWidthPercentage(60);
        vendTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        vendTable.addCell(createHeaderCell("Vendedor", pdfHeaderFont));
        vendTable.addCell(createHeaderCell("Total vendido", pdfHeaderFont));
        DefaultPieDataset pieDatasetGlobal = new DefaultPieDataset();
        if (topVendedores != null) {
            for (Object[] r : topVendedores) {
                String nombre = r[1] == null ? "N/A" : String.valueOf(r[1]);
                Number total = r[2] == null ? 0 : (Number) r[2];
                vendTable.addCell(createCell(nombre, pdfNormalFont));
                vendTable.addCell(createCell(String.valueOf(total.doubleValue()), pdfNormalFont));
                pieDatasetGlobal.setValue(nombre, total.doubleValue());
            }
        }
        vendTable.setSpacingAfter(12f);
        document.add(vendTable);

        if (pieDatasetGlobal.getItemCount() > 0) {
            document.newPage();
            Paragraph repartoTitle = new Paragraph("Repartición por vendedor", pdfHeaderFont);
            repartoTitle.setAlignment(Element.ALIGN_CENTER);
            repartoTitle.setSpacingAfter(8f);
            document.add(repartoTitle);
            JFreeChart pieChartGlobal = ChartFactory.createPieChart("", pieDatasetGlobal, true, true, false);
            applyPieChartStyle(pieChartGlobal);
            addCenteredChartHighDpi(document, pieChartGlobal, 650, 350, 2.0);
        }

        // Stock bajo
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Productos con stock bajo (<=5)", pdfHeaderFont));
        List<Object[]> stockList = productoRepository.stockProductos();
        PdfPTable stockTable = new PdfPTable(new float[]{6, 2});
        stockTable.setWidthPercentage(60);
        stockTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        stockTable.addCell(createHeaderCell("Producto", pdfHeaderFont));
        stockTable.addCell(createHeaderCell("Stock", pdfHeaderFont));
        if (stockList != null) {
            for (Object[] r : stockList) {
                Number stock = r[2] == null ? 0 : (Number) r[2];
                if (stock.intValue() <= 5) {
                    stockTable.addCell(createCell(String.valueOf(r[1]), pdfNormalFont));
                    stockTable.addCell(createCell(String.valueOf(stock.intValue()), pdfNormalFont));
                }
            }
        }
        stockTable.setSpacingAfter(12f);
        stockTable.setSpacingBefore(6f);
        document.add(stockTable);

        // Ventas por día
        List<Object[]> ventasPorDia = ventaRepository.ventasPorDiaBetween(from, to);
        if (ventasPorDia != null && !ventasPorDia.isEmpty()) {
            document.newPage();
            Paragraph ventasDiaTitle = new Paragraph("Ventas por día", pdfHeaderFont);
            ventasDiaTitle.setAlignment(Element.ALIGN_CENTER);
            ventasDiaTitle.setSpacingAfter(8f);
            document.add(ventasDiaTitle);

            DefaultCategoryDataset seriesDataset = new DefaultCategoryDataset();
            for (Object[] r : ventasPorDia) {
                Object diaObj = r[0];
                Number total = r[1] == null ? 0 : (Number) r[1];
                String label = diaObj == null ? "N/A" : diaObj.toString();
                seriesDataset.addValue(total.doubleValue(), "Ventas", label);
            }
            JFreeChart lineChart = ChartFactory.createLineChart("", "Día", "Total", seriesDataset);
            applyCategoryChartStyle(lineChart);
            addCenteredChartHighDpi(document, lineChart, 900, 320, 2.0);

            Paragraph legend = new Paragraph("Ventas", pdfNormalFont);
            legend.setAlignment(Element.ALIGN_CENTER);
            legend.setSpacingBefore(8f);
            document.add(legend);
        }

        document.close();
        return baos.toByteArray();
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

        // Gráfico de ventas por producto (opcional)
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
        if (!ventasPorProducto.isEmpty()) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            ventasPorProducto.forEach((nombre, qty) -> dataset.addValue(qty.longValue(), "Cantidad", nombre));
            JFreeChart chart = ChartFactory.createBarChart("Ventas por producto", "Producto", "Cantidad", dataset, PlotOrientation.VERTICAL, false, true, false);
            applyCategoryChartStyle(chart);
            addCenteredChartHighDpi(document, chart, 700, 300, 2.0);
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
        dateP.setSpacingAfter(6f);
        document.add(dateP);

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

    /* ------------------ Chart styling / image helpers ------------------ */

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