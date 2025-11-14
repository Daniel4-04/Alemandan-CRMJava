package com.alemandan.crm.service.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class HeaderFooterEvent extends PdfPageEventHelper {

    private final Image logo;
    private final Font headerFont;
    private final String title;

    public HeaderFooterEvent(Image logo, Font headerFont, String title) {
        this.logo = logo;
        this.headerFont = headerFont;
        this.title = title;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            PdfPTable header = new PdfPTable(2);
            header.setWidths(new int[]{1, 4});
            header.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            header.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            // logo
            PdfPCell imgCell = new PdfPCell(logo, true);
            imgCell.setBorder(Rectangle.NO_BORDER);
            imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            imgCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            header.addCell(imgCell);
            // title
            PdfPCell textCell = new PdfPCell(new Phrase(title, headerFont));
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            textCell.setPaddingLeft(10);
            header.addCell(textCell);
            header.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 10, writer.getDirectContent());

            // footer: page number centered
            String footerText = String.format("Página %d", writer.getPageNumber());
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(footerText, headerFont),
                    (document.getPageSize().getWidth()) / 2,
                    document.bottomMargin() / 2, 0);
        } catch (Exception e) {
            // no interrumpir generación por problemas en header/footer
            e.printStackTrace();
        }
    }
}