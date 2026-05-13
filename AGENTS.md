# Bolletta - Agent Guidelines

## Project Overview

**Bolletta** is an Android utility app (Italian) for monitoring electricity consumption and calculating bimonthly bills. Users log monthly meter readings and configure tariff prices matching Italian utility bill structure (Quadri A/B/C/D). The app calculates consumption-based charges with loss adjustments, fixed costs, and VAT.

**Language**: Java | **Min SDK**: 31 | **Target SDK**: 34 | **Build System**: Gradle (Kotlin DSL)

## Architecture & Components

### Three-Layer Design

1. **UI & Persistence Layer** (`MainActivity.java`)  
   - Single-activity architecture binding `R.layout.activity_main`
   - All state persisted to `SharedPreferences` (key prefix: "bolletta_prefs")
   - Monthly readings stored as `reading_yyyy-MM` float entries
   - Tariff prices cached with `KEY_PRICE_*` constants (10 configuration values)

2. **Configuration Model** (`TariffConfig.java`)  
   - Immutable data class holding 10 tariff rates + percentages
   - No business logic—purely parametric configuration
   - All fields are doubles (rates in EUR/kWh, percentages for losses/VAT)

3. **Calculation Engine** (`BillingCalculator.java`, utility class)  
   - Static `calculateBimonthlyTotal(consumptionKwh, config)` method
   - Returns `Result` inner class with 10 computed fields
   - Enforces non-negative values via `Math.max(0d, ...)`

### Data Flow

```
User Input (3 meter readings) 
  → MainActivity calculates kWh differences (month1 = mid - start, month2 = end - mid)
  → TariffConfig created from UI form inputs
  → BillingCalculator.calculateBimonthlyTotal()
  → Result formatted & displayed in resultView (TextView)
```

**Critical**: Readings are stored in SharedPreferences; calculations are **not** persisted—results are ephemeral and recalculated on demand.

## Billing Calculation Pattern

The calc follows Italian utility structure with four "Quadri" (sections):

```java
Quadro A = (corrispettivo_index + contributo) × (consumption + losses) + dispacciamento × consumption
Quadro B = trasporto × consumption
Quadro C = (oneri_asos + oneri_arim) × consumption
Quadro D = imposte × consumption

Subtotal = A + B + C + D + quotaFissa (fixed bimonthly charge)
VAT = Subtotal × (ivaPercent / 100)
Total = Subtotal + VAT
```

**Key insight**: Losses (perdite) are added to base consumption **only** for corrispettivo+contributo; dispacciamento, transport, and other charges apply only to base consumption.

## Developer Workflows

### Run Tests
```powershell
cd C:\Users\sergi\AndroidStudioProjects\Bolletta
.\gradlew.bat test
```
Tests live in `app/src/test/java/it/sdc/bolletta/ExampleUnitTest.java`—verify calcs with known tariff configs (see: `bimonthlyCalculation_isCorrect()` for reference values).

### Build Debug APK
```powershell
cd C:\Users\sergi\AndroidStudioProjects\Bolletta
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Unit Test Patterns
- Use `TariffConfig` constructor with fixed test rates (default: 0.11, 0.024, 0.020, ... see ExampleUnitTest)
- Assert with 4-decimal precision: `.getXxx()` methods vs expected values
- Test edge cases: negative consumption (clamped to 0), zero values, percentage-based calculations

## Key Conventions

### Naming & Formatting
- **Month format**: `yyyy-MM` (e.g., "2026-05") via `DateTimeFormatter.ofPattern("yyyy-MM")`
- **Locale handling**: No explicit locale set; relies on device default for parsing commas-as-decimals (line 270: `.replace(',', '.')`)
- **Number display**: `DecimalFormat("0.000")` for 3 decimal places in results

### SharedPreferences Keys
All keys are class constants in MainActivity (`KEY_PRICE_INDEX`, etc.). Monthly readings use dynamic keys: `readingKey(YearMonth)` → `"reading_" + month.format(YEAR_MONTH_FORMAT)`.

### Validation Rules
- **Readings**: Must be ≥ 0 (toast error: "Insert a valid reading")
- **Prices**: All 10 tariff fields required; any null or empty field blocks save
- **Month input**: Must parse as valid `yyyy-MM` or raises `IllegalArgumentException`

## Important Patterns & Edge Cases

### Missing Data Handling
If calculating a bimonthly period and any of the 3 readings (start, mid, end) is missing from SharedPreferences, display error naming the missing months—do **not** estimate or zero-fill.

### Reminder Logic
On app launch, if today is the last day of the current month AND the current month's reading is absent, auto-populate the month field and show a reminder prompt in resultView.

### Decimal Precision
- Use `double` for calculation; convert to `float` for SharedPreferences storage/retrieval
- Loss percentage is applied as: `loss_kwh = consumption × (percent / 100.0)`
- VAT percentage applied the same way to subtotal

## Integration Points & External Dependencies

- **AndroidX AppCompat** (`androidx.appcompat:appcompat:1.7.0`): Base Activity
- **Material Design** (`com.google.android.material:material:1.12.0`): UI components
- **ConstraintLayout** (`androidx.constraintlayout:constraintlayout:2.1.4`): Layout inflation
- **JUnit 4** (`junit:junit:4.13.2`): Unit test runner
- **Gradle AGP 8.13.2**: Build orchestration

No external database, network, or third-party calculation libraries—all logic is embedded.

## Extending This Project

### Adding New Tariff Rates
1. Add new field to `TariffConfig` (constructor, getter, field)
2. Add corresponding `KEY_*` constant to `MainActivity`
3. Add EditText binding in `bindViews()`, `loadTariffs()`, `saveTariffs()`
4. Update the `calculateBimonthlyTotal()` formula in `BillingCalculator`
5. Update test reference values in `ExampleUnitTest.java`

### Modifying Calculation Logic
Always update test assertions in parallel: see `bimonthlyCalculation_isCorrect()` for the expected output structure. Run `.\gradlew.bat test` after changes to verify.

### UI Changes
All strings are in `app/src/main/res/values/strings.xml` (referenced via `getString(R.string.*)`). Layout in `app/src/main/res/layout/activity_main.xml`.

