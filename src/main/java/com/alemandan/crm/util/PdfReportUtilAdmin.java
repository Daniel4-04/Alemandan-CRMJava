package com.alemandan.crm.util;

import com.alemandan.crm.model.Venta;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.List;

public class PdfReportUtilAdmin {

    public static void exportVentasPdf(List<Venta> ventas, OutputStream out) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Reporte de Ventas - Administrador", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(6);
        table.addCell("ID");
        table.addCell("Fecha");
        table.addCell("Empleado");
        table.addCell("Método de pago");
        table.addCell("Total");
        table.addCell("Productos");
        for (Venta v : ventas) {
            table.addCell(String.valueOf(v.getId()));
            table.addCell(String.valueOf(v.getFecha()));
            table.addCell(String.valueOf(v.getUsuario().getNombre()));
            table.addCell(String.valueOf(v.getMetodoPago()));
            table.addCell(String.valueOf(v.getTotal()));
            StringBuilder productos = new StringBuilder();
            v.getDetalles().forEach(d -> productos.append(d.getProducto().getNombre())
                    .append(" (").append(d.getCantidad()).append(" x ").append(d.getPrecioUnitario()).append(")\n"));
            table.addCell(productos.toString());
        }
        doc.add(table);
        doc.close();
    }
}