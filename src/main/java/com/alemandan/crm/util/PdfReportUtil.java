package com.alemandan.crm.util;

import com.alemandan.crm.model.Venta;
import com.alemandan.crm.model.DetalleVenta;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.OutputStream;
import java.util.List;

public class PdfReportUtil {
    public static void exportVentasPdf(List<Venta> ventas, OutputStream os) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, os);
        document.open();

        Font bold = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("Historial de Mis Ventas", bold));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.addCell("Fecha");
        table.addCell("Total");
        table.addCell("Método de pago");
        table.addCell("Productos");

        for (Venta venta : ventas) {
            table.addCell(venta.getFecha().toString());
            table.addCell(String.valueOf(venta.getTotal()));
            table.addCell(venta.getMetodoPago());
            StringBuilder productos = new StringBuilder();
            for (DetalleVenta d : venta.getDetalles()) {
                productos.append(d.getProducto().getNombre())
                        .append(" (").append(d.getCantidad()).append("x").append(d.getPrecioUnitario()).append(")\n");
            }
            table.addCell(productos.toString());
        }
        document.add(table);
        document.close();
    }
}