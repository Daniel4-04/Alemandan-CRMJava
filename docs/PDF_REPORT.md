# PDF Sales Report Documentation

## Overview

The Alemandan CRM/POS system includes an enhanced PDF sales report generation feature that provides comprehensive sales analytics with visual charts and detailed insights.

## Features

The enhanced PDF sales report (`/ventas/reporte/pdf`) includes the following sections:

### 1. Executive Summary
- **Total Sales Amount**: Total revenue for the selected period
- **Number of Sales**: Count of all sales transactions
- **Average Ticket**: Average transaction value
- **Growth Percentage**: Comparison with the previous period of equal duration (if applicable)

### 2. Sales by Product Table
- Product name
- Quantity sold
- Total amount sold
- Top 20 products by revenue

### 3. Sales by User/Seller Table
- Seller name
- Number of sales transactions
- Total amount sold
- All sellers ranked by total sales

### 4. Charts

#### Monthly Sales Chart (Bar Chart)
- Shows sales trends over time
- If the date range is ≤ 60 days, displays daily data
- If the date range is > 60 days, displays monthly aggregated data
- Helps identify seasonal patterns and trends

#### Top 10 Products Pie Chart
- Visual representation of product sales distribution
- Shows the top 10 products by revenue
- Displays participation percentage for each product

### 5. Textual Analysis
Programmatically generated insights including:
- Top-selling product with quantity and revenue
- Products with low rotation (bottom 5)
- Best-performing seller with statistics
- Growth trend analysis
- Average ticket insights

### 6. Low Stock Alert
- Lists products with stock ≤ 5 units
- Helps identify restocking needs

## Headless Environment Compatibility (Railway Deployment)

The PDF generation feature is fully compatible with headless environments like Railway, Docker, and other cloud platforms.

### Technical Implementation

1. **Headless Mode**: The application sets `java.awt.headless=true` in the main class (`AlemandanCrmJavaApplication.java`) to prevent native library loading issues.

2. **DejaVu Sans Font**: 
   - Included in `src/main/resources/fonts/DejaVuSans.ttf` and `DejaVuSans-Bold.ttf`
   - Registered at runtime using `Font.createFont()` and `GraphicsEnvironment.registerFont()`
   - Provides fallback font support when native font managers are unavailable

3. **Chart Generation Fallback**:
   - JFreeChart is used for generating charts (bar charts and pie charts)
   - If native libraries (libfreetype, libfontmanager) are missing, chart generation gracefully fails
   - All tabular data is preserved in the PDF even if charts cannot be generated
   - Warning logs are generated instead of errors to maintain export functionality

### Why DejaVu Sans?

DejaVu Sans was chosen because:
- It's an open-source font (free to redistribute)
- Excellent Unicode coverage for international characters
- Works reliably in headless environments
- Widely used in server-side PDF generation
- Available in Regular and Bold weights

## Usage

### From the Admin Dashboard

1. Navigate to **Reportes Avanzados** section
2. Select date range (from/to)
3. Optional: Filter by specific product
4. Click **Exportar PDF**
5. The enhanced PDF report will be downloaded

### Endpoint

```
GET /ventas/reporte/pdf?from=2024-01-01&to=2024-12-31&productoId=123
```

**Parameters:**
- `from` (optional): Start date in ISO format (YYYY-MM-DD). Defaults to 30 days ago.
- `to` (optional): End date in ISO format (YYYY-MM-DD). Defaults to today.
- `productoId` (optional): Filter by specific product ID.

**Response:**
- Content-Type: `application/pdf`
- Disposition: `attachment; filename=reporte_ventas_[dates].pdf`

## Deployment Considerations

### Railway/Docker Deployment

The application is ready for Railway deployment without any additional configuration:

1. **Environment Variables**: No special font-related environment variables are needed.

2. **Dependencies**: All required fonts are bundled in the application JAR file.

3. **Fallback Logic**: If chart generation fails due to missing native libraries, the PDF is still generated with all data tables intact.

### Local Development

For local development, the application works out-of-the-box with the bundled DejaVu fonts. No additional setup is required.

### Testing

Unit tests are included in `ReportServiceTest.java` to verify:
- PDF generation returns non-null byte array
- PDF contains expected content (minimum size validation)
- Empty data is handled gracefully
- Growth calculations work correctly

Run tests with:
```bash
mvn test -Dtest=ReportServiceTest
```

## Dependencies

The PDF report feature uses:
- **iText PDF 5.5.13.3**: PDF generation library
- **JFreeChart 1.5.4**: Chart generation library
- **Apache POI**: Excel export (separate from PDF)

All dependencies are already included in `pom.xml`.

## Troubleshooting

### Charts Not Appearing in PDF

**Symptom**: PDF is generated but charts are missing.

**Cause**: Native libraries for font rendering are not available in the deployment environment.

**Solution**: This is expected behavior in headless environments. The PDF will still contain all tabular data. Check application logs for warnings like:
```
WARN ReportService - No se pudo generar gráfico de ventas mensuales (librerías nativas no disponibles)
```

### Large PDF File Size

**Symptom**: PDF files are larger than expected.

**Cause**: High-DPI chart images are embedded in the PDF for better quality.

**Solution**: This is intentional for better print quality. The charts use 2x scaling (900x380px rendered at 2.0 scale) for crisp visuals.

### Growth Percentage Showing as N/A

**Symptom**: Growth percentage is not displayed in the executive summary.

**Cause**: Either this is the first period being analyzed, or there's insufficient data in the previous period.

**Solution**: This is normal. The system needs at least two comparable periods to calculate growth.

## Future Enhancements

Potential improvements for future versions:
- Configurable chart types (line, area, etc.)
- Multi-period comparison (quarterly, yearly)
- Custom color themes for charts
- Export configuration options (include/exclude sections)
- Scheduled report generation and email delivery

## Support

For issues or questions about the PDF report feature, please:
1. Check the application logs for detailed error messages
2. Verify date range parameters are correct
3. Ensure database contains sales data for the selected period
4. Review this documentation for known issues and solutions

## Version History

- **v1.0 (December 2024)**: Initial enhanced PDF report implementation
  - Added executive summary with growth metrics
  - Implemented sales by product and user tables
  - Added monthly/daily sales bar chart
  - Added top 10 products pie chart
  - Implemented textual analysis generation
  - Added headless environment compatibility with DejaVu fonts
  - Created comprehensive unit tests
