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
 * Utilidad para exportar ventas a Excel (vista admin).
 * Contiene dos métodos públicos:
 * - exportVentasExcel(List<Venta>, OutputStream)  <- nombre usado por AdminVentaController
 * - exportVentasExcelAdmin(List<Venta>, OutputStream) <- implementación "principal"
 *
 * Ambos delegan al mismo implementation para mantener compatibilidad.
 */
public class ExcelReportUtilAdmin {

    /**
     * Compatibilidad: método con el nombre que usa el controlador.
     * Acepta cualquier OutputStream (incluye jakarta.servlet.ServletOutputStream).
     */
    public static void exportVentasExcel(List<Venta> ventas, OutputStream os) throws Exception {
        exportVentasExcelAdmin(ventas, os);
    }

    /**
     * Implementación real que genera el workbook y lo escribe en el OutputStream.
     */
    public static void exportVentasExcelAdmin(List<Venta> ventas, OutputStream os) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CreationHelper createHelper = wb.getCreationHelper();
            Sheet sheet = wb.createSheet("Ventas - Admin");

            // Header style
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Fecha");
            header.createCell(2).setCellValue("Usuario");
            header.createCell(3).setCellValue("Método pago");
            header.createCell(4).setCellValue("Subtotal");
            header.createCell(5).setCellValue("IVA");
            header.createCell(6).setCellValue("Total");
            header.createCell(7).setCellValue("Productos");

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

                // Fecha
                Cell cFecha = r.createCell(1);
                if (v.getFecha() != null) cFecha.setCellValue(v.getFecha().format(dtf));
                else cFecha.setCellValue("");

                // Usuario (si existe)
                Cell cUser = r.createCell(2);
                String user = "";
                if (v.getUsuario() != null) {
                    user = v.getUsuario().getNombre() != null ? v.getUsuario().getNombre() : (v.getUsuario().getEmail() == null ? "" : v.getUsuario().getEmail());
                }
                cUser.setCellValue(user);

                // Metodo pago
                Cell cMetodo = r.createCell(3);
                cMetodo.setCellValue(v.getMetodoPago() == null ? "" : v.getMetodoPago());

                // Subtotal, IVA, Total (BigDecimal -> double)
                Cell cSub = r.createCell(4);
                BigDecimal subtotal = v.getSubtotal() == null ? BigDecimal.ZERO : v.getSubtotal();
                cSub.setCellValue(subtotal.doubleValue());

                Cell cIva = r.createCell(5);
                BigDecimal iva = v.getIva() == null ? BigDecimal.ZERO : v.getIva();
                cIva.setCellValue(iva.doubleValue());

                Cell cTotal = r.createCell(6);
                BigDecimal total = v.getTotal() == null ? BigDecimal.ZERO : v.getTotal();
                cTotal.setCellValue(total.doubleValue());

                // Productos
                StringBuilder sb = new StringBuilder();
                if (v.getDetalles() != null) {
                    for (DetalleVenta d : v.getDetalles()) {
                        String prodName = d.getProducto() == null ? "N/A" : d.getProducto().getNombre();
                        BigDecimal price = d.getPrecioUnitario() == null ? BigDecimal.ZERO : d.getPrecioUnitario();
                        Integer qty = d.getCantidad() == null ? 0 : d.getCantidad();
                        sb.append(prodName).append(" (").append(qty).append(" x ").append(price.setScale(2, BigDecimal.ROUND_HALF_UP).toString()).append(")");
                        if (d.getIvaRate() != null && d.getIvaRate().compareTo(BigDecimal.ZERO) > 0) {
                            sb.append(" IVA: ").append(d.getIvaRate().setScale(2, BigDecimal.ROUND_HALF_UP).toString()).append("%");
                        }
                        sb.append("; ");
                    }
                }
                Cell cProd = r.createCell(7);
                cProd.setCellValue(sb.toString());

                totalSum = totalSum.add(total);
            }

            // Autosize
            for (int i = 0; i <= 7; i++) sheet.autoSizeColumn(i);

            // Total general
            int totalRowIdx = rowIdx + 1;
            Row sumaRow = sheet.createRow(totalRowIdx);
            sumaRow.createCell(5).setCellValue("Total ventas:");
            Cell sumaCell = sumaRow.createCell(6);
            sumaCell.setCellValue(totalSum.doubleValue());

            wb.write(os);
        }
    }
}