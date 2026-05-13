package it.sdc.bolletta;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "bolletta_prefs";
    private static final String KEY_PRICE_INDEX = "price_index";
    private static final String KEY_PRICE_CONTRIBUTO = "price_contributo";
    private static final String KEY_PRICE_DISPACCIAMENTO = "price_dispacciamento";
    private static final String KEY_PRICE_TRASPORTO = "price_trasporto";
    private static final String KEY_PRICE_ONERI_ASOS = "price_oneri_asos";
    private static final String KEY_PRICE_ONERI_ARIM = "price_oneri_arim";
    private static final String KEY_PRICE_IMPOSTE = "price_imposte";
    private static final String KEY_PERDITE_PERCENT = "perdite_percent";
    private static final String KEY_IVA_PERCENT = "iva_percent";
    private static final String KEY_PRICE_FISSA = "price_fissa";
    private static final String SLOT_MONTH_ONE = "month_one";
    private static final String SLOT_MONTH_TWO = "month_two";

    private static final double DEFAULT_INDEX = 0.132665d;
    private static final double DEFAULT_CONTRIBUTO = 0.024000d;
    private static final double DEFAULT_DISPACCIAMENTO = 0.023170d;
    private static final double DEFAULT_TRASPORTO = 0.014730d;
    private static final double DEFAULT_ONERI_ASOS = 0.028657d;
    private static final double DEFAULT_ONERI_ARIM = 0.001638d;
    private static final double DEFAULT_IMPOSTE = 0.022700d;
    private static final double DEFAULT_PERDITE_PERCENT = 10.0d;
    private static final double DEFAULT_IVA_PERCENT = 10.0d;
    private static final double DEFAULT_FISSA = 0.0d;

    private final DecimalFormat numberFormat = new DecimalFormat("0.000");
    private final DecimalFormat inputFormat = new DecimalFormat("0.######");
    private final DecimalFormat totalFormat = new DecimalFormat("0.00");

    private EditText readingMonthOneInput;
    private EditText readingMonthTwoInput;

    private MonthTariffInputs monthOneTariffInputs;
    private MonthTariffInputs monthTwoTariffInputs;

    private TextView resultView;
    private TextView resultTotalView;
    private SharedPreferences preferences;

    private static final class MonthTariffInputs {
        final EditText priceIndexInput;
        final EditText priceContributoInput;
        final EditText priceDispacciamentoInput;
        final EditText priceTrasportoInput;
        final EditText priceOneriAsosInput;
        final EditText priceOneriArimInput;
        final EditText priceImposteInput;
        final EditText lossesPercentInput;
        final EditText ivaPercentInput;
        final EditText priceFissaInput;

        MonthTariffInputs(
                EditText priceIndexInput,
                EditText priceContributoInput,
                EditText priceDispacciamentoInput,
                EditText priceTrasportoInput,
                EditText priceOneriAsosInput,
                EditText priceOneriArimInput,
                EditText priceImposteInput,
                EditText lossesPercentInput,
                EditText ivaPercentInput,
                EditText priceFissaInput
        ) {
            this.priceIndexInput = priceIndexInput;
            this.priceContributoInput = priceContributoInput;
            this.priceDispacciamentoInput = priceDispacciamentoInput;
            this.priceTrasportoInput = priceTrasportoInput;
            this.priceOneriAsosInput = priceOneriAsosInput;
            this.priceOneriArimInput = priceOneriArimInput;
            this.priceImposteInput = priceImposteInput;
            this.lossesPercentInput = lossesPercentInput;
            this.ivaPercentInput = ivaPercentInput;
            this.priceFissaInput = priceFissaInput;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        bindViews();
        loadTariffs();
        setupActions();
    }

    private void bindViews() {
        readingMonthOneInput = findViewById(R.id.inputReadingMonthOne);
        readingMonthTwoInput = findViewById(R.id.inputReadingMonthTwo);

        monthOneTariffInputs = new MonthTariffInputs(
                findViewById(R.id.inputPriceIndex),
                findViewById(R.id.inputPriceContributo),
                findViewById(R.id.inputPriceDispacciamento),
                findViewById(R.id.inputPriceTrasporto),
                findViewById(R.id.inputPriceOneriAsos),
                findViewById(R.id.inputPriceOneriArim),
                findViewById(R.id.inputPriceImposte),
                findViewById(R.id.inputLossesPercent),
                findViewById(R.id.inputVatPercent),
                findViewById(R.id.inputPriceFissa)
        );

        monthTwoTariffInputs = new MonthTariffInputs(
                findViewById(R.id.inputPriceIndexMonthTwo),
                findViewById(R.id.inputPriceContributoMonthTwo),
                findViewById(R.id.inputPriceDispacciamentoMonthTwo),
                findViewById(R.id.inputPriceTrasportoMonthTwo),
                findViewById(R.id.inputPriceOneriAsosMonthTwo),
                findViewById(R.id.inputPriceOneriArimMonthTwo),
                findViewById(R.id.inputPriceImposteMonthTwo),
                findViewById(R.id.inputLossesPercentMonthTwo),
                findViewById(R.id.inputVatPercentMonthTwo),
                findViewById(R.id.inputPriceFissaMonthTwo)
        );

        resultView = findViewById(R.id.textResult);
        resultTotalView = findViewById(R.id.textResultTotal);
    }

    private void setupActions() {
        Button copyPricesButton = findViewById(R.id.buttonCopyMonthOneToMonthTwo);
        Button savePricesButton = findViewById(R.id.buttonSavePrices);
        Button calculateButton = findViewById(R.id.buttonCalculate);

        copyPricesButton.setOnClickListener(v -> confirmCopyMonthOneToMonthTwo());
        savePricesButton.setOnClickListener(v -> saveTariffsForPeriod());
        calculateButton.setOnClickListener(v -> calculatePeriod());
    }

    private void confirmCopyMonthOneToMonthTwo() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.copy_prices_title)
                .setMessage(R.string.copy_prices_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> copyMonthOneToMonthTwo())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void copyMonthOneToMonthTwo() {
        TariffConfig firstMonthConfig = parseRequiredTariffConfig(monthOneTariffInputs);
        if (firstMonthConfig == null) {
            toast(getString(R.string.error_prices));
            return;
        }

        setTariffInputs(monthTwoTariffInputs, firstMonthConfig);
        toast(getString(R.string.prices_copied));
    }

    private void saveTariffsForPeriod() {
        TariffConfig firstMonthConfig = parseRequiredTariffConfig(monthOneTariffInputs);
        if (firstMonthConfig == null) {
            toast(getString(R.string.error_prices));
            return;
        }

        TariffConfig secondMonthConfig = parseSecondMonthTariffConfig(monthTwoTariffInputs, firstMonthConfig);
        if (secondMonthConfig == null) {
            toast(getString(R.string.error_prices));
            return;
        }

        persistTariffForSlot(SLOT_MONTH_ONE, firstMonthConfig);
        persistTariffForSlot(SLOT_MONTH_TWO, secondMonthConfig);

        toast(getString(R.string.prices_saved));
    }

    private void loadTariffs() {
        TariffConfig firstMonthConfig = tariffConfigForSlot(SLOT_MONTH_ONE);
        if (firstMonthConfig == null) {
            firstMonthConfig = defaultTariffConfig();
        }
        setTariffInputs(monthOneTariffInputs, firstMonthConfig);

        TariffConfig secondMonthConfig = tariffConfigForSlot(SLOT_MONTH_TWO);
        if (secondMonthConfig == null) {
            secondMonthConfig = firstMonthConfig;
        }
        setTariffInputs(monthTwoTariffInputs, secondMonthConfig);
    }

    private void calculatePeriod() {
        Double monthOneKwh = parseDoubleOrNull(readingMonthOneInput.getText().toString());
        Double monthTwoKwh = parseDoubleOrNull(readingMonthTwoInput.getText().toString());
        if (monthOneKwh == null || monthTwoKwh == null || monthOneKwh < 0 || monthTwoKwh < 0) {
            toast(getString(R.string.error_reading));
            return;
        }

        TariffConfig firstMonthConfig = parseRequiredTariffConfig(monthOneTariffInputs);
        if (firstMonthConfig == null) {
            toast(getString(R.string.error_prices));
            return;
        }

        TariffConfig secondMonthConfig = parseSecondMonthTariffConfig(monthTwoTariffInputs, firstMonthConfig);
        if (secondMonthConfig == null) {
            toast(getString(R.string.error_prices));
            return;
        }

        persistTariffForSlot(SLOT_MONTH_ONE, firstMonthConfig);
        persistTariffForSlot(SLOT_MONTH_TWO, secondMonthConfig);

        BillingCalculator.Result firstMonthResult = BillingCalculator.calculateBimonthlyTotal(
                monthOneKwh,
                withHalfFixedCost(firstMonthConfig)
        );
        BillingCalculator.Result secondMonthResult = BillingCalculator.calculateBimonthlyTotal(
                monthTwoKwh,
                withHalfFixedCost(secondMonthConfig)
        );

        double totalConsumption = firstMonthResult.getConsumptionKwh() + secondMonthResult.getConsumptionKwh();
        double totalLoss = firstMonthResult.getLossKwh() + secondMonthResult.getLossKwh();
        double totalQuadroA = firstMonthResult.getQuadroA() + secondMonthResult.getQuadroA();
        double totalQuadroB = firstMonthResult.getQuadroB() + secondMonthResult.getQuadroB();
        double totalQuadroC = firstMonthResult.getQuadroC() + secondMonthResult.getQuadroC();
        double totalQuadroD = firstMonthResult.getQuadroD() + secondMonthResult.getQuadroD();
        double totalFixed = firstMonthResult.getFixedCost() + secondMonthResult.getFixedCost();
        double totalSubtotal = firstMonthResult.getSubtotalWithoutVat() + secondMonthResult.getSubtotalWithoutVat();
        double totalVat = firstMonthResult.getVatCost() + secondMonthResult.getVatCost();
        double total = firstMonthResult.getTotal() + secondMonthResult.getTotal();

        String text = getString(
                R.string.result_template,
                numberFormat.format(monthOneKwh),
                numberFormat.format(monthTwoKwh),
                numberFormat.format(totalConsumption),
                numberFormat.format(totalLoss),
                numberFormat.format(totalQuadroA),
                numberFormat.format(totalQuadroB),
                numberFormat.format(totalQuadroC),
                numberFormat.format(totalQuadroD),
                numberFormat.format(totalFixed),
                numberFormat.format(totalSubtotal),
                numberFormat.format(totalVat),
                numberFormat.format(total)
        );
        resultView.setText(text);
        resultTotalView.setText("Totale bimestrale: " + totalFormat.format(total) + " EUR");
    }

    private TariffConfig parseRequiredTariffConfig(MonthTariffInputs inputs) {
        Double index = parseDoubleOrNull(inputs.priceIndexInput.getText().toString());
        Double contributo = parseDoubleOrNull(inputs.priceContributoInput.getText().toString());
        Double dispacciamento = parseDoubleOrNull(inputs.priceDispacciamentoInput.getText().toString());
        Double trasporto = parseDoubleOrNull(inputs.priceTrasportoInput.getText().toString());
        Double oneriAsos = parseDoubleOrNull(inputs.priceOneriAsosInput.getText().toString());
        Double oneriArim = parseDoubleOrNull(inputs.priceOneriArimInput.getText().toString());
        Double imposte = parseDoubleOrNull(inputs.priceImposteInput.getText().toString());
        Double perditePercent = parseDoubleOrNull(inputs.lossesPercentInput.getText().toString());
        Double ivaPercent = parseDoubleOrNull(inputs.ivaPercentInput.getText().toString());
        Double fissa = parseDoubleOrNull(inputs.priceFissaInput.getText().toString());

        if (index == null || contributo == null || dispacciamento == null || trasporto == null
                || oneriAsos == null || oneriArim == null || imposte == null
                || perditePercent == null || ivaPercent == null || fissa == null) {
            return null;
        }

        return new TariffConfig(
                index,
                contributo,
                dispacciamento,
                trasporto,
                oneriAsos,
                oneriArim,
                imposte,
                perditePercent,
                ivaPercent,
                fissa
        );
    }

    private TariffConfig parseSecondMonthTariffConfig(MonthTariffInputs inputs, TariffConfig fallback) {
        Double index = parseDoubleOrFallback(inputs.priceIndexInput.getText().toString(), fallback.getCorrispettivoLuceIndexPerKwh());
        Double contributo = parseDoubleOrFallback(inputs.priceContributoInput.getText().toString(), fallback.getContributoConsumoPerKwh());
        Double dispacciamento = parseDoubleOrFallback(inputs.priceDispacciamentoInput.getText().toString(), fallback.getDispacciamentoPerKwh());
        Double trasporto = parseDoubleOrFallback(inputs.priceTrasportoInput.getText().toString(), fallback.getTrasportoPerKwh());
        Double oneriAsos = parseDoubleOrFallback(inputs.priceOneriAsosInput.getText().toString(), fallback.getOneriAsosPerKwh());
        Double oneriArim = parseDoubleOrFallback(inputs.priceOneriArimInput.getText().toString(), fallback.getOneriArimPerKwh());
        Double imposte = parseDoubleOrFallback(inputs.priceImposteInput.getText().toString(), fallback.getImpostePerKwh());
        Double perditePercent = parseDoubleOrFallback(inputs.lossesPercentInput.getText().toString(), fallback.getPerditePercent());
        Double ivaPercent = parseDoubleOrFallback(inputs.ivaPercentInput.getText().toString(), fallback.getIvaPercent());
        Double fissa = parseDoubleOrFallback(inputs.priceFissaInput.getText().toString(), fallback.getQuotaFissaBimestrale());

        if (index == null || contributo == null || dispacciamento == null || trasporto == null
                || oneriAsos == null || oneriArim == null || imposte == null
                || perditePercent == null || ivaPercent == null || fissa == null) {
            return null;
        }

        return new TariffConfig(
                index,
                contributo,
                dispacciamento,
                trasporto,
                oneriAsos,
                oneriArim,
                imposte,
                perditePercent,
                ivaPercent,
                fissa
        );
    }

    private Double parseDoubleOrFallback(String text, double fallback) {
        if (TextUtils.isEmpty(text)) {
            return fallback;
        }
        return parseDoubleOrNull(text);
    }

    private TariffConfig defaultTariffConfig() {
        return new TariffConfig(
                DEFAULT_INDEX,
                DEFAULT_CONTRIBUTO,
                DEFAULT_DISPACCIAMENTO,
                DEFAULT_TRASPORTO,
                DEFAULT_ONERI_ASOS,
                DEFAULT_ONERI_ARIM,
                DEFAULT_IMPOSTE,
                DEFAULT_PERDITE_PERCENT,
                DEFAULT_IVA_PERCENT,
                DEFAULT_FISSA
        );
    }

    private void setTariffInputs(MonthTariffInputs inputs, TariffConfig config) {
        inputs.priceIndexInput.setText(inputFormat.format(config.getCorrispettivoLuceIndexPerKwh()));
        inputs.priceContributoInput.setText(inputFormat.format(config.getContributoConsumoPerKwh()));
        inputs.priceDispacciamentoInput.setText(inputFormat.format(config.getDispacciamentoPerKwh()));
        inputs.priceTrasportoInput.setText(inputFormat.format(config.getTrasportoPerKwh()));
        inputs.priceOneriAsosInput.setText(inputFormat.format(config.getOneriAsosPerKwh()));
        inputs.priceOneriArimInput.setText(inputFormat.format(config.getOneriArimPerKwh()));
        inputs.priceImposteInput.setText(inputFormat.format(config.getImpostePerKwh()));
        inputs.lossesPercentInput.setText(inputFormat.format(config.getPerditePercent()));
        inputs.ivaPercentInput.setText(inputFormat.format(config.getIvaPercent()));
        inputs.priceFissaInput.setText(inputFormat.format(config.getQuotaFissaBimestrale()));
    }

    private void persistTariffForSlot(String slot, TariffConfig config) {
        preferences.edit()
                .putLong(slotKey(KEY_PRICE_INDEX, slot), Double.doubleToRawLongBits(config.getCorrispettivoLuceIndexPerKwh()))
                .putLong(slotKey(KEY_PRICE_CONTRIBUTO, slot), Double.doubleToRawLongBits(config.getContributoConsumoPerKwh()))
                .putLong(slotKey(KEY_PRICE_DISPACCIAMENTO, slot), Double.doubleToRawLongBits(config.getDispacciamentoPerKwh()))
                .putLong(slotKey(KEY_PRICE_TRASPORTO, slot), Double.doubleToRawLongBits(config.getTrasportoPerKwh()))
                .putLong(slotKey(KEY_PRICE_ONERI_ASOS, slot), Double.doubleToRawLongBits(config.getOneriAsosPerKwh()))
                .putLong(slotKey(KEY_PRICE_ONERI_ARIM, slot), Double.doubleToRawLongBits(config.getOneriArimPerKwh()))
                .putLong(slotKey(KEY_PRICE_IMPOSTE, slot), Double.doubleToRawLongBits(config.getImpostePerKwh()))
                .putLong(slotKey(KEY_PERDITE_PERCENT, slot), Double.doubleToRawLongBits(config.getPerditePercent()))
                .putLong(slotKey(KEY_IVA_PERCENT, slot), Double.doubleToRawLongBits(config.getIvaPercent()))
                .putLong(slotKey(KEY_PRICE_FISSA, slot), Double.doubleToRawLongBits(config.getQuotaFissaBimestrale()))
                .apply();
    }

    private TariffConfig tariffConfigForSlot(String slot) {
        if (!hasTariffForSlot(slot)) {
            return null;
        }

        try {
            return new TariffConfig(
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_INDEX, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_CONTRIBUTO, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_DISPACCIAMENTO, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_TRASPORTO, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_ONERI_ASOS, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_ONERI_ARIM, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_IMPOSTE, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PERDITE_PERCENT, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_IVA_PERCENT, slot), 0L)),
                    Double.longBitsToDouble(preferences.getLong(slotKey(KEY_PRICE_FISSA, slot), 0L))
            );
        } catch (ClassCastException e) {
            // Handle cases where preferences were stored as Float in previous versions
            return null;
        }
    }

    private boolean hasTariffForSlot(String slot) {
        return preferences.contains(slotKey(KEY_PRICE_INDEX, slot))
                && preferences.contains(slotKey(KEY_PRICE_CONTRIBUTO, slot))
                && preferences.contains(slotKey(KEY_PRICE_DISPACCIAMENTO, slot))
                && preferences.contains(slotKey(KEY_PRICE_TRASPORTO, slot))
                && preferences.contains(slotKey(KEY_PRICE_ONERI_ASOS, slot))
                && preferences.contains(slotKey(KEY_PRICE_ONERI_ARIM, slot))
                && preferences.contains(slotKey(KEY_PRICE_IMPOSTE, slot))
                && preferences.contains(slotKey(KEY_PERDITE_PERCENT, slot))
                && preferences.contains(slotKey(KEY_IVA_PERCENT, slot))
                && preferences.contains(slotKey(KEY_PRICE_FISSA, slot));
    }

    private TariffConfig withHalfFixedCost(TariffConfig config) {
        return new TariffConfig(
                config.getCorrispettivoLuceIndexPerKwh(),
                config.getContributoConsumoPerKwh(),
                config.getDispacciamentoPerKwh(),
                config.getTrasportoPerKwh(),
                config.getOneriAsosPerKwh(),
                config.getOneriArimPerKwh(),
                config.getImpostePerKwh(),
                config.getPerditePercent(),
                config.getIvaPercent(),
                config.getQuotaFissaBimestrale() / 2d
        );
    }

    private Double parseDoubleOrNull(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }

        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String slotKey(String key, String slot) {
        return key + "_" + slot;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}