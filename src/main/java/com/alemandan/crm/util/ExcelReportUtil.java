package com.alemandan.crm.util;

import com.alemandan.crm.model.DetalleVenta;
import com.alemandan.crm.model.Venta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilidad para exportar ventas a Excel (vista empleado).
 * Corrige el uso de Cell.setCellValue para BigDecimal -> usa double.
 */
public class ExcelReportUtil {

    public static void exportVentasExcel(List<Venta> ventas, OutputStream os) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper createHelper = wb.getCreationHelper();
            Sheet sheet = wb.createSheet("Mis Ventas");

            // Header style
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            // Date style
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Fecha");
            header.createCell(2).setCellValue("Método pago");
            header.createCell(3).setCellValue("Total");
            header.createCell(4).setCellValue("Productos");

            for (Cell c : header) c.setCellStyle(headerStyle);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            int rowIdx = 1;
            BigDecimal totalSum = BigDecimal.ZERO;
            for (Venta v : ventas) {
                Row r = sheet.createRow(rowIdx++);

                // ID
                Cell cId = r.createCell(0);
                if (v.getId() != null) cId.setCellValue(v.getId());
                else cId.setCellValue("");

                // Fecha (string formatted)
                Cell cFecha = r.createCell(1);
                if (v.getFecha() != null) {
                    cFecha.setCellValue(v.getFecha().format(dtf));
                } else {
                    cFecha.setCellValue("");
                }

                // Metodo pago
                Cell cMetodo = r.createCell(2);
                cMetodo.setCellValue(v.getMetodoPago() == null ? "" : v.getMetodoPago());

                // Total (BigDecimal -> double)
                Cell cTotal = r.createCell(3);
                BigDecimal total = v.getTotal() == null ? BigDecimal.ZERO : v.getTotal();
                cTotal.setCellValue(total.doubleValue());

                // Productos: concatenar nombre (cantidad x precio)
                StringBuilder sb = new StringBuilder();
                if (v.getDetalles() != null) {
                    for (DetalleVenta d : v.getDetalles()) {
                        String prodName = d.getProducto() == null ? "N/A" : d.getProducto().getNombre();
                        BigDecimal price = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                        Integer qty = d.getCantidad() == null ? 0 : d.getCantidad();
                        sb.append(prodName).append(" (").append(qty).append(" x ").append(price.setScale(2, BigDecimal.ROUND_HALF_UP).toString()).append(")").append("; ");
                    }
                }
                Cell cProd = r.createCell(4);
                cProd.setCellValue(sb.toString());

                totalSum = totalSum.add(total);
            }

            // Autosize
            for (int i = 0; i <= 4; i++) sheet.autoSizeColumn(i);

            // Totales al final
            int totalRowIdx = rowIdx + 1;
            Row sumaRow = sheet.createRow(totalRowIdx);
            sumaRow.createCell(2).setCellValue("Total ventas:");
            Cell sumaCell = sumaRow.createCell(3);
            sumaCell.setCellValue(totalSum.doubleValue());

            wb.write(os);
        }
    }
}