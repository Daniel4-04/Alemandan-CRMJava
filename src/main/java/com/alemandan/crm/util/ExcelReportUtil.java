package com.alemandan.crm.util;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.OutputStream;
import java.util.List;

public class ExcelReportUtil {
    public static void exportVentasExcel(List<Venta> ventas, OutputStream os) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mis Ventas");

        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("Fecha");
        header.createCell(1).setCellValue("Total");
        header.createCell(2).setCellValue("Método de pago");
        header.createCell(3).setCellValue("Productos");

        for (Venta venta : ventas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(venta.getFecha().toString());
            row.createCell(1).setCellValue(venta.getTotal());
            row.createCell(2).setCellValue(venta.getMetodoPago());
            StringBuilder productos = new StringBuilder();
            for (DetalleVenta d : venta.getDetalles()) {
                productos.append(d.getProducto().getNombre())
                        .append(" (").append(d.getCantidad()).append("x").append(d.getPrecioUnitario()).append(")\n");
            }
            row.createCell(3).setCellValue(productos.toString());
        }
        workbook.write(os);
        workbook.close();
    }
}