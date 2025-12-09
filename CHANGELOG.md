# Changelog

All notable changes to the Alemandan CRM/POS system will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed - PDF Report Generation Overhaul (December 2024)

**BREAKING CHANGE**: PDF sales reports now use tables instead of charts to eliminate native library dependencies.

#### What Changed
- **Removed** all JFreeChart-based chart generation from PDF reports
- **Replaced** bar chart (monthly/daily sales) with detailed sales by period table
- **Replaced** pie chart (top products) with participation table showing percentages
- **Enhanced** textual analysis with additional trend insights
- **Improved** container compatibility by eliminating native library dependencies

#### Migration Impact
- **Endpoint**: No changes - same URLs and parameters work as before
- **Data**: All data previously shown in charts is now in tables
- **Features**: New insights and trend analysis sections added
- **Compatibility**: Reports now work in all containerized environments (Railway, Docker, Kubernetes) without configuration

#### Technical Details
- Deprecated methods: `createMonthlySalesChart()`, `createTopProductsPieChart()`, `applyCategoryChartStyle()`, `applyPieChartStyle()`, `addCenteredChartHighDpi()`, `createHighDpiImageFromChart()`, `registerFallbackFont()`
- New methods: `addSalesByPeriodTable()`, `addTopProductsParticipationTable()`, `addPeriodAnalysis()`
- Updated: `generarReporteVentasPdf()` to use table-based rendering
- Updated: `generarMisVentasPdfFromList()` to replace chart with product sales table

#### Why This Change?
JFreeChart requires native libraries (libfreetype.so, libfontmanager.so) that are not available in containerized environments. This caused PDF generation to fail with `UnsatisfiedLinkError` on platforms like Railway and Docker. The new table-based approach:
- ✅ Works everywhere without native dependencies
- ✅ Provides the same data in a more portable format
- ✅ Generates smaller, faster PDFs
- ✅ Improves accessibility (tables are searchable and screen-reader friendly)
- ✅ Adds enhanced textual analysis and insights

#### Affected Files
- `src/main/java/com/alemandan/crm/service/ReportService.java` - Main implementation changes
- `src/test/java/com/alemandan/crm/service/ReportServiceTest.java` - Updated tests
- `docs/PDF_REPORT.md` - Updated documentation

#### User Impact
Users will notice that:
1. PDF reports now show tables where charts used to appear
2. Reports work reliably in all deployment environments
3. New trend analysis sections provide additional insights
4. Product participation is shown as percentage values in a table
5. PDF file sizes may be smaller

For detailed information, see `docs/PDF_REPORT.md`.

---

## [1.0.0] - 2024-12-01

### Added
- Initial enhanced PDF sales report with charts
- Executive summary with growth metrics
- Sales by product and user tables
- Monthly/daily sales bar chart
- Top 10 products pie chart
- Textual analysis generation
- Headless environment compatibility with DejaVu fonts
- Comprehensive unit tests
