package com.alemandan.crm.util;

import com.alemandan.crm.model.Venta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.OutputStream;
import java.util.List;

public class ExcelReportUtilAdmin {

    public static void exportVentasExcel(List<Venta> ventas, OutputStream out) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Ventas");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Fecha");
        header.createCell(2).setCellValue("Empleado");
        header.createCell(3).setCellValue("Método de pago");
        header.createCell(4).setCellValue("Total");
        header.createCell(5).setCellValue("Productos");
        int rowIdx = 1;
        for (Venta v : ventas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(v.getId());
            row.createCell(1).setCellValue(String.valueOf(v.getFecha()));
            row.createCell(2).setCellValue(v.getUsuario().getNombre());
            row.createCell(3).setCellValue(v.getMetodoPago());
            row.createCell(4).setCellValue(v.getTotal());
            StringBuilder productos = new StringBuilder();
            v.getDetalles().forEach(d -> productos.append(d.getProducto().getNombre())
                    .append(" (").append(d.getCantidad()).append(" x ").append(d.getPrecioUnitario()).append(")\n"));
            row.createCell(5).setCellValue(productos.toString());
        }
        workbook.write(out);
        workbook.close();
    }
}