package com.alemandan.crm.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilidad para exportar el reporte avanzado de ventas a Excel (admin dashboard).
 * Genera un archivo Excel con múltiples hojas: resumen, top productos, top vendedores y stock bajo.
 */
public class ExcelReportUtilAvanzado {

    /**
     * Genera un reporte avanzado en Excel con múltiples pestañas.
     *
     * @param from Fecha inicio del periodo
     * @param to Fecha fin del periodo
     * @param totalVentas Total de ventas en el periodo
     * @param count Número de ventas
     * @param topProductos Lista de top productos [id, nombre, cantidad]
     * @param topVendedores Lista de top vendedores [id, nombre, total]
     * @param stockBajo Lista de productos con stock bajo [id, nombre, stock]
     * @param ventasPorDia Lista de ventas por día [fecha, total]
     * @param os OutputStream donde se escribirá el Excel
     * @throws Exception Si hay error al generar el archivo
     */
    public static void generarReporteAvanzadoExcel(
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal totalVentas,
            Long count,
            List<Object[]> topProductos,
            List<Object[]> topVendedores,
            List<Object[]> stockBajo,
            List<Object[]> ventasPorDia,
            OutputStream os) throws Exception {

        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper createHelper = wb.getCreationHelper();

            // Estilos
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            // Formatear fechas para el título
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String periodo = "Periodo: " + (from != null ? from.toLocalDate().format(dtf) : "") + " - " + (to != null ? to.toLocalDate().format(dtf) : "");

            // Hoja 1: Resumen
            Sheet resumenSheet = wb.createSheet("Resumen");
            int rowIdx = 0;

            // Título
            Row titleRow = resumenSheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Informe de Ventas");
            titleCell.setCellStyle(titleStyle);

            // Periodo
            Row periodRow = resumenSheet.createRow(rowIdx++);
            periodRow.createCell(0).setCellValue(periodo);
            rowIdx++; // Espacio

            // Resumen
            Row resumenHeaderRow = resumenSheet.createRow(rowIdx++);
            Cell resumenTitleCell = resumenHeaderRow.createCell(0);
            resumenTitleCell.setCellValue("Resumen General");
            resumenTitleCell.setCellStyle(headerStyle);
            resumenHeaderRow.createCell(1).setCellStyle(headerStyle);

            Row totalRow = resumenSheet.createRow(rowIdx++);
            totalRow.createCell(0).setCellValue("Ventas totales (monto)");
            totalRow.createCell(1).setCellValue(totalVentas != null ? totalVentas.doubleValue() : 0.0);

            Row countRow = resumenSheet.createRow(rowIdx++);
            countRow.createCell(0).setCellValue("Número de ventas");
            countRow.createCell(1).setCellValue(count != null ? count : 0);

            BigDecimal avg = BigDecimal.ZERO;
            if (count != null && count > 0 && totalVentas != null) {
                avg = totalVentas.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
            }
            Row avgRow = resumenSheet.createRow(rowIdx++);
            avgRow.createCell(0).setCellValue("Venta promedio");
            avgRow.createCell(1).setCellValue(avg.doubleValue());

            // Autosize resumen
            resumenSheet.autoSizeColumn(0);
            resumenSheet.autoSizeColumn(1);

            // Hoja 2: Top Productos
            Sheet productosSheet = wb.createSheet("Top Productos");
            rowIdx = 0;

            Row prodTitleRow = productosSheet.createRow(rowIdx++);
            Cell prodTitleCell = prodTitleRow.createCell(0);
            prodTitleCell.setCellValue("Productos más vendidos");
            prodTitleCell.setCellStyle(titleStyle);
            rowIdx++; // Espacio

            Row prodHeaderRow = productosSheet.createRow(rowIdx++);
            Cell prodHeaderCell1 = prodHeaderRow.createCell(0);
            prodHeaderCell1.setCellValue("Producto");
            prodHeaderCell1.setCellStyle(headerStyle);
            Cell prodHeaderCell2 = prodHeaderRow.createCell(1);
            prodHeaderCell2.setCellValue("Cantidad vendida");
            prodHeaderCell2.setCellStyle(headerStyle);

            if (topProductos != null) {
                int maxRows = Math.min(topProductos.size(), 10);
                for (int i = 0; i < maxRows; i++) {
                    Object[] r = topProductos.get(i);
                    Row dataRow = productosSheet.createRow(rowIdx++);
                    String nombre = r[1] == null ? "N/A" : String.valueOf(r[1]);
                    Number cantidad = r[2] == null ? 0 : (Number) r[2];
                    dataRow.createCell(0).setCellValue(nombre);
                    dataRow.createCell(1).setCellValue(cantidad.longValue());
                }
            }

            productosSheet.autoSizeColumn(0);
            productosSheet.autoSizeColumn(1);

            // Hoja 3: Top Vendedores
            Sheet vendedoresSheet = wb.createSheet("Top Vendedores");
            rowIdx = 0;

            Row vendTitleRow = vendedoresSheet.createRow(rowIdx++);
            Cell vendTitleCell = vendTitleRow.createCell(0);
            vendTitleCell.setCellValue("Top vendedores (global)");
            vendTitleCell.setCellStyle(titleStyle);
            rowIdx++; // Espacio

            Row vendHeaderRow = vendedoresSheet.createRow(rowIdx++);
            Cell vendHeaderCell1 = vendHeaderRow.createCell(0);
            vendHeaderCell1.setCellValue("Vendedor");
            vendHeaderCell1.setCellStyle(headerStyle);
            Cell vendHeaderCell2 = vendHeaderRow.createCell(1);
            vendHeaderCell2.setCellValue("Total vendido");
            vendHeaderCell2.setCellStyle(headerStyle);

            if (topVendedores != null) {
                for (Object[] r : topVendedores) {
                    Row dataRow = vendedoresSheet.createRow(rowIdx++);
                    String nombre = r[1] == null ? "N/A" : String.valueOf(r[1]);
                    Number total = r[2] == null ? 0 : (Number) r[2];
                    dataRow.createCell(0).setCellValue(nombre);
                    dataRow.createCell(1).setCellValue(total.doubleValue());
                }
            }

            vendedoresSheet.autoSizeColumn(0);
            vendedoresSheet.autoSizeColumn(1);

            // Hoja 4: Stock Bajo
            Sheet stockSheet = wb.createSheet("Stock Bajo");
            rowIdx = 0;

            Row stockTitleRow = stockSheet.createRow(rowIdx++);
            Cell stockTitleCell = stockTitleRow.createCell(0);
            stockTitleCell.setCellValue("Productos con stock bajo (<=5)");
            stockTitleCell.setCellStyle(titleStyle);
            rowIdx++; // Espacio

            Row stockHeaderRow = stockSheet.createRow(rowIdx++);
            Cell stockHeaderCell1 = stockHeaderRow.createCell(0);
            stockHeaderCell1.setCellValue("Producto");
            stockHeaderCell1.setCellStyle(headerStyle);
            Cell stockHeaderCell2 = stockHeaderRow.createCell(1);
            stockHeaderCell2.setCellValue("Stock");
            stockHeaderCell2.setCellStyle(headerStyle);

            if (stockBajo != null) {
                for (Object[] r : stockBajo) {
                    Number stock = r[2] == null ? 0 : (Number) r[2];
                    if (stock.intValue() <= 5) {
                        Row dataRow = stockSheet.createRow(rowIdx++);
                        dataRow.createCell(0).setCellValue(String.valueOf(r[1]));
                        dataRow.createCell(1).setCellValue(stock.intValue());
                    }
                }
            }

            stockSheet.autoSizeColumn(0);
            stockSheet.autoSizeColumn(1);

            // Hoja 5: Ventas por día
            if (ventasPorDia != null && !ventasPorDia.isEmpty()) {
                Sheet ventasDiaSheet = wb.createSheet("Ventas por Día");
                rowIdx = 0;

                Row diaTitleRow = ventasDiaSheet.createRow(rowIdx++);
                Cell diaTitleCell = diaTitleRow.createCell(0);
                diaTitleCell.setCellValue("Ventas por día");
                diaTitleCell.setCellStyle(titleStyle);
                rowIdx++; // Espacio

                Row diaHeaderRow = ventasDiaSheet.createRow(rowIdx++);
                Cell diaHeaderCell1 = diaHeaderRow.createCell(0);
                diaHeaderCell1.setCellValue("Día");
                diaHeaderCell1.setCellStyle(headerStyle);
                Cell diaHeaderCell2 = diaHeaderRow.createCell(1);
                diaHeaderCell2.setCellValue("Total");
                diaHeaderCell2.setCellStyle(headerStyle);

                for (Object[] r : ventasPorDia) {
                    Row dataRow = ventasDiaSheet.createRow(rowIdx++);
                    Object diaObj = r[0];
                    Number total = r[1] == null ? 0 : (Number) r[1];
                    String label = diaObj == null ? "N/A" : diaObj.toString();
                    dataRow.createCell(0).setCellValue(label);
                    dataRow.createCell(1).setCellValue(total.doubleValue());
                }

                ventasDiaSheet.autoSizeColumn(0);
                ventasDiaSheet.autoSizeColumn(1);
            }

            // Escribir el workbook al OutputStream
            wb.write(os);
        }
    }
}
