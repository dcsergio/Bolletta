# Bolletta - Agent Guidelines

## Project Overview

**Bolletta** is an Android utility app (Italian) for estimating bimonthly electricity bills. Users enter 2 monthly consumption values (mese 1 / mese 2) and configure tariff prices matching Italian utility bill structure (Quadri A/B/C/D), with separate tariff columns for the two months when needed. The app calculates consumption-based charges with loss adjustments, fixed costs, and VAT.

**Language**: Java | **Min SDK**: 31 | **Compile SDK**: 37 | **Target SDK**: 37 | **Build System**: Gradle (Kotlin DSL)

## Architecture & Components

### Three-Layer Design

1. **UI & Persistence Layer** (`MainActivity.java`)  
   - Single-activity architecture binding `R.layout.activity_main`
   - Persisted state stored in `SharedPreferences` (key prefix: "bolletta_prefs")
   - Tariff prices cached with `KEY_PRICE_*` constants for two slots: `month_one` and `month_two` (10 configuration values per slot)
   - Uses `MonthTariffInputs` as an inner helper to bind the duplicated month-1/month-2 tariff columns
   - On first load, month 1 is populated from `DEFAULT_*` constants; month 2 mirrors month 1 when no dedicated slot is saved

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
User Input (2 monthly kWh values + 2 tariff columns)
  → MainActivity parses month 1 prices with `parseRequiredTariffConfig()`
  → MainActivity parses month 2 prices with `parseSecondMonthTariffConfig()` (empty fields fall back to month 1)
  → BillingCalculator.calculateBimonthlyTotal() runs once per month
  → MainActivity sums both `Result` objects and displays details in `resultView` plus the final total in `resultTotalView`
```

**Critical**: Only tariff configurations are stored in `SharedPreferences`; the two consumption inputs and all calculated totals are ephemeral and recalculated on demand.

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

In the current UI, the calculator is applied independently to month 1 and month 2, then the two `BillingCalculator.Result` totals are summed in `MainActivity.calculatePeriod()`.

## Developer Workflows

### Run Tests
```powershell
cd C:\Users\sergi\AndroidStudioProjects\Bolletta
.\gradlew.bat test
```
Tests live in `app/src/test/java/it/sdc/bolletta/ExampleUnitTest.java`—verify calcs with known tariff configs (see: `bimonthlyCalculation_isCorrect()` for reference values).

The app module compiles with Java 17 (`sourceCompatibility` / `targetCompatibility` in `app/build.gradle.kts`).

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
- **Numeric parsing**: `parseDoubleOrNull()` trims input and normalizes commas to dots via `.replace(',', '.')`
- **Number display**: `DecimalFormat("0.000")` for result details, `DecimalFormat("0.00")` for the highlighted total, and `DecimalFormat("0.######")` when pre-filling tariff inputs

### SharedPreferences Keys
All keys are class constants in `MainActivity` (`KEY_PRICE_INDEX`, etc.). Tariff entries are namespaced per slot through `slotKey(key, slot)`, e.g. `slotKey(KEY_PRICE_INDEX, SLOT_MONTH_ONE)` → `"price_index_month_one"`.

Tariff values are persisted as raw `double` bits with `putLong(..., Double.doubleToRawLongBits(value))` and restored with `Double.longBitsToDouble(...)`.

### Validation Rules
- **Consumption inputs**: Both month-1 and month-2 kWh fields must parse as numbers ≥ 0 (`error_reading` toast)
- **Prices**: All 10 month-1 tariff fields are required for save/calculate
- **Month 2 prices**: Empty fields are allowed and inherit month-1 values; non-empty invalid numeric input still blocks save/calculate

## Important Patterns & Edge Cases

### Missing Data Handling
There is no persisted 3-reading lookup in the current app. Missing or invalid month-1/month-2 consumption input blocks calculation with `error_reading`; missing month-1 tariff fields block both save and calculate with `error_prices`.

Month-2 tariff inputs are treated differently: `parseSecondMonthTariffConfig()` uses `parseDoubleOrFallback()`, so blank fields inherit the month-1 `TariffConfig` instead of failing validation.

### Reminder Logic
There is no calendar-based reminder or month auto-population flow in the current `MainActivity`. Before any calculation, `textResult` simply shows `@string/result_placeholder`.

### Decimal Precision
- Use `double` for calculation and for persisted tariff values in `SharedPreferences` (stored via raw long bits, not `float`)
- Loss percentage is applied as: `loss_kwh = consumption × (percent / 100.0)`
- VAT percentage applied the same way to subtotal

Backward compatibility note: `tariffConfigForSlot()` catches `ClassCastException` and returns `null` when older preferences still contain `float` values; callers then reload defaults for month 1 or mirror month 1 into month 2.

## Integration Points & External Dependencies

- **AndroidX AppCompat** (`androidx.appcompat:appcompat:1.7.0`): Base Activity
- **Material Design** (`com.google.android.material:material:1.12.0`): UI components
- **AndroidX Activity** (`androidx.activity:activity:1.9.0`): Activity support dependency declared in the app module
- **AndroidX Fragment** (`androidx.fragment:fragment:1.8.9`): Fragment support dependency declared in the app module
- **ConstraintLayout** (`androidx.constraintlayout:constraintlayout:2.1.4`): Layout inflation
- **JUnit 4** (`junit:junit:4.13.2`): Unit test runner
- **AndroidX Test** (`androidx.test.ext:junit:1.2.1`, `androidx.test.espresso:espresso-core:3.6.1`): Instrumentation test dependencies
- **Gradle AGP 9.2.1**: Build orchestration

No external database, network, or third-party calculation libraries—all logic is embedded.

## Extending This Project

### Adding New Tariff Rates
1. Add new field to `TariffConfig` (constructor, getter, field)
2. Add corresponding `KEY_*` constant to `MainActivity`
3. Add both month-1 and month-2 `EditText` bindings via `MonthTariffInputs` in `bindViews()`, plus mapping in `setTariffInputs()`, `parseRequiredTariffConfig()`, and `parseSecondMonthTariffConfig()`
4. Update slot persistence in `persistTariffForSlot()` / `tariffConfigForSlot()` and the `calculateBimonthlyTotal()` formula in `BillingCalculator`
5. Update test reference values in `ExampleUnitTest.java`

### Modifying Calculation Logic
Always update test assertions in parallel: see `bimonthlyCalculation_isCorrect()` for the expected output structure. Run `.\gradlew.bat test` after changes to verify.

### UI Changes
All strings are in `app/src/main/res/values/strings.xml` (referenced via `getString(R.string.*)`). Layout in `app/src/main/res/layout/activity_main.xml`.

