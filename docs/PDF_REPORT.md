# PDF Sales Report Documentation

## Overview

The Alemandan CRM/POS system includes an enhanced PDF sales report generation feature that provides comprehensive sales analytics with detailed tables and textual analysis.

**IMPORTANT UPDATE (December 2024)**: Charts have been replaced with tables to eliminate native library dependencies (libfreetype, libfontmanager) that cause issues in containerized environments like Railway and Docker. The new table-based approach provides the same data in a more portable format.

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

### 4. Sales by Period Table (replaces Monthly Sales Chart)
- **Daily data**: For date ranges ≤ 60 days, shows daily sales breakdown
- **Monthly data**: For date ranges > 60 days, shows monthly aggregated sales
- Includes textual analysis of trends (highest/lowest sales periods)
- Helps identify seasonal patterns and trends

### 5. Top 10 Products Participation Table (replaces Pie Chart)
- Product name
- Total amount sold
- Participation percentage of each product
- Total row showing sum of top 10 products
- Visual representation through percentage values

### 6. Textual Analysis
Programmatically generated insights including:
- Top-selling product with quantity and revenue
- Products with low rotation (bottom 5)
- Best-performing seller with statistics
- Growth trend analysis
- Average ticket insights
- Period trend analysis (highest/lowest sales days/months)

### 7. Low Stock Alert
- Lists products with stock ≤ 5 units
- Helps identify restocking needs

## Container Environment Compatibility

The PDF generation feature is now fully compatible with containerized environments (Railway, Docker, Kubernetes) **without requiring any native libraries**.

### What Changed?

**Before**: The system used JFreeChart to generate visual charts (bar charts, pie charts) which required native libraries:
- libfreetype.so
- libfontmanager.so
- AWT/Swing native components

These libraries were not available in most container environments, causing `UnsatisfiedLinkError` exceptions.

**After**: All charts have been replaced with:
- **Detailed tables** showing the same data in tabular format
- **Enhanced textual analysis** with insights and metrics
- **Percentage-based visualizations** (e.g., participation %)

**Benefits**:
- ✅ Works in all containerized environments without configuration
- ✅ No native library dependencies
- ✅ Smaller PDF file sizes
- ✅ Faster generation (no image rendering)
- ✅ Better accessibility (table data is searchable and screen-reader friendly)
- ✅ Same data, more portable format

## Usage

### From the Admin Dashboard

1. Navigate to **Reportes Avanzados** section
2. Select date range (from/to)
3. Optional: Filter by specific product
4. Click **Exportar PDF**
5. The enhanced PDF report will be downloaded

### Endpoint

```
GET /ventas/reporte/pdf?from=2024-01-01&to=2024-12-31&productoId=123&includeAnalysis=true
```

**Parameters:**
- `from` (optional): Start date in ISO format (YYYY-MM-DD). Defaults to 30 days ago.
- `to` (optional): End date in ISO format (YYYY-MM-DD). Defaults to today.
- `productoId` (optional): Filter by specific product ID.
- `includeAnalysis` (optional): Boolean to include/exclude advanced analysis. Defaults to `true`.
  - `true`: Generates full report with all tables, growth metrics, insights, and low stock alerts
  - `false`: Generates basic report with only summary tables (faster, smaller file)

**Response:**
- Content-Type: `application/pdf`
- Disposition: `attachment; filename=reporte_ventas_[dates].pdf`

### Report Modes

#### Full Report (includeAnalysis=true, default)
The full report includes:
- Executive summary with growth metrics
- Sales by product table (top 20)
- Sales by user/seller table
- Sales by period table (daily or monthly)
- Top 10 products participation table with percentages
- Enhanced textual analysis with insights
- Period trend analysis
- Low stock alerts

**Use case**: Comprehensive business analytics, management reports, strategic planning.

**Example**: `/ventas/reporte/pdf?from=2024-01-01&to=2024-12-31` (analysis included by default)

#### Basic Report (includeAnalysis=false)
The basic report includes:
- Summary with total sales, transaction count, and average ticket
- Sales by product table (top 20)
- Sales by user/seller table

**Use case**: Quick data exports, simpler reports for operational teams, faster generation.

**Example**: `/ventas/reporte/pdf?from=2024-01-01&to=2024-12-31&includeAnalysis=false`

## Deployment Considerations

### Railway/Docker/Kubernetes Deployment

The application is ready for any containerized deployment **without any additional configuration**:

1. **No Native Libraries Required**: The PDF generation no longer depends on system fonts or graphics libraries.

2. **No Environment Variables Needed**: No special configuration is required for PDF generation.

3. **Works Out-of-the-Box**: Deploy and use immediately without worrying about missing dependencies.

### Migration from Chart-Based Reports

If you're upgrading from a previous version that used charts:

- **No action required**: The endpoint and parameters remain the same
- **Data integrity**: All data that was in charts is now in tables
- **Enhanced analysis**: New textual insights provide additional value
- **Backward compatibility**: The `includeAnalysis` parameter continues to work as before

### Local Development

For local development, the application works out-of-the-box. No additional setup is required.

### Testing

Unit tests are included in `ReportServiceTest.java` to verify:
- PDF generation returns non-null byte array
- PDF contains expected content (minimum size validation)
- Empty data is handled gracefully
- Growth calculations work correctly
- Both full and basic report modes work correctly

Run tests with:
```bash
mvn test -Dtest=ReportServiceTest
```

## Dependencies

The PDF report feature uses:
- **iText PDF 5.5.13.3**: PDF generation library (no native dependencies)
- **Apache POI**: Excel export (separate from PDF)

**Removed dependencies**:
- ~~JFreeChart~~ (kept for backward compatibility but no longer used in PDF generation)
- ~~AWT/Swing native libraries~~ (no longer required)

All dependencies are already included in `pom.xml`.

## Troubleshooting

### Tables Instead of Charts

**Observation**: The PDF now contains tables where there used to be charts.

**Explanation**: This is the intended behavior. Charts have been replaced with tables to eliminate native library dependencies and ensure compatibility with containerized environments.

**Benefits**: Tables are more accessible, searchable, and provide the same data in a portable format.

### Large PDF File Size

**Symptom**: PDF files might be smaller than before.

**Explanation**: Tables are more compact than embedded high-resolution chart images, resulting in smaller file sizes while maintaining all the data.

### Growth Percentage Showing as N/A

**Symptom**: Growth percentage is not displayed in the executive summary.

**Cause**: Either this is the first period being analyzed, or there's insufficient data in the previous period.

**Solution**: This is normal. The system needs at least two comparable periods to calculate growth.

## Migration Notes

### Changes from Previous Version

1. **Charts Removed**: All JFreeChart-based visualizations have been replaced with tables
2. **Enhanced Analysis**: New textual analysis sections provide insights that complement the tables
3. **Participation Percentages**: Product participation table now shows percentage values
4. **Trend Analysis**: New section analyzes highest/lowest sales periods

### Code Changes

- Methods `createMonthlySalesChart()`, `createTopProductsPieChart()`, `applyCategoryChartStyle()`, `applyPieChartStyle()`, `addCenteredChartHighDpi()`, and `createHighDpiImageFromChart()` are now deprecated
- New methods: `addSalesByPeriodTable()`, `addTopProductsParticipationTable()`, `addPeriodAnalysis()`
- The `registerFallbackFont()` method is deprecated (no longer needed)

## Future Enhancements

Potential improvements for future versions:
- Configurable table styling and colors
- Multi-period comparison (quarterly, yearly) in side-by-side tables
- Export configuration options (include/exclude specific sections)
- Scheduled report generation and email delivery
- Additional textual insights and recommendations

## Support

For issues or questions about the PDF report feature, please:
1. Check the application logs for detailed error messages
2. Verify date range parameters are correct
3. Ensure database contains sales data for the selected period
4. Review this documentation for known issues and solutions

## Version History

- **v2.0 (December 2024)**: **BREAKING CHANGE** - Replaced charts with tables
  - Removed all JFreeChart dependencies from PDF generation
  - Added sales by period table (daily/monthly)
  - Added top products participation table with percentages
  - Enhanced textual analysis with trend insights
  - Eliminated native library dependencies for container compatibility
  - Improved accessibility and data portability

- **v1.0 (December 2024)**: Initial enhanced PDF report implementation
  - Added executive summary with growth metrics
  - Implemented sales by product and user tables
  - Added monthly/daily sales bar chart
  - Added top 10 products pie chart
  - Implemented textual analysis generation
  - Added headless environment compatibility with DejaVu fonts
  - Created comprehensive unit tests
