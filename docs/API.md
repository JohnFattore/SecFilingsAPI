# API Documentation

This document outlines the available API endpoints for the SecFilingsAPI.

## Admin Endpoints

### Load Tickers
`GET /admin/load`
Downloads the list of Nasdaq and other listed tickers and populates the `assets` and `listings` tables.

### Load Financials
`GET /admin/quarters`
Fetches the latest financial data (Net Income, Revenue, Assets, Liabilities) for all non-fund S&P 500 companies from the SEC EDGAR API.

---

## Data Endpoints

### List Quarters
`GET /quarters?ticker={ticker}`
Returns a list of all stored quarters for a specific ticker.

**Parameters:**
- `ticker` (Required): The stock ticker symbol (e.g., AAPL).

### Company Fact Sheet
`GET /company-fact-sheet?ticker={ticker}`
Provides a summary of the latest financial performance for a company.

**Parameters:**
- `ticker` (Required): The stock ticker symbol (e.g., AAPL).

**Returns:**
- `ticker`: The company ticker.
- `cik`: The Central Index Key.
- `ttmNetIncome`: Trailing Twelve Months Net Income.
- `ttmRevenue`: Trailing Twelve Months Revenue.
- `ttmNetIncomeYoY`: YoY growth of TTM Net Income.
- `ttmRevenueYoY`: YoY growth of TTM Revenue.
- `debtRatio`: Total Liabilities / Total Assets.
- `latestQuarterEnd`: Date of the most recently available data.
- **Income Statement**: `productsNetSales`, `servicesNetSales`, `grossMargin`, `operatingIncome`, `basicEps`, `dilutedEps`.
- **Balance Sheet**: `cashAndEquivalents`, `marketableSecurities`, `totalDebt`, `shareholdersEquity`.
- **Cash Flow**: `operatingCashFlow`.

---

## Utility Endpoints

### Health Check
`GET /test2`
Returns "MAXWELL" if the service is up.
