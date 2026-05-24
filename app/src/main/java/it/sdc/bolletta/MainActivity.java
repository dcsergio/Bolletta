package it.sdc.bolletta;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.Arrays;

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
    private static final String KEY_INHERITED_PREFIX = "inherited_";
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
    private static final int MONTH_TWO_INHERITED_BACKGROUND_COLOR = 0x1FF57C00;

    private final DecimalFormat numberFormat = new DecimalFormat("0.000");
    private final DecimalFormat inputFormat = new DecimalFormat("0.######");
    private final DecimalFormat totalFormat = new DecimalFormat("0.00");
    private final String[] tariffPreferenceKeys = new String[]{
            KEY_PRICE_INDEX,
            KEY_PRICE_CONTRIBUTO,
            KEY_PRICE_DISPACCIAMENTO,
            KEY_PRICE_TRASPORTO,
            KEY_PRICE_ONERI_ASOS,
            KEY_PRICE_ONERI_ARIM,
            KEY_PRICE_IMPOSTE,
            KEY_PERDITE_PERCENT,
            KEY_IVA_PERCENT,
            KEY_PRICE_FISSA
    };

    private EditText readingMonthOneInput;
    private EditText readingMonthTwoInput;

    private MonthTariffInputs monthOneTariffInputs;
    private MonthTariffInputs monthTwoTariffInputs;

    private TextView resultView;
    private TextView resultTotalView;
    private SharedPreferences preferences;
    private boolean isUpdatingMonthTwoField;

    private record MonthTariffInputs(EditText priceIndexInput, EditText priceContributoInput,
                                     EditText priceDispacciamentoInput,
                                     EditText priceTrasportoInput, EditText priceOneriAsosInput,
                                     EditText priceOneriArimInput, EditText priceImposteInput,
                                     EditText lossesPercentInput, EditText ivaPercentInput,
                                     EditText priceFissaInput) {

        EditText[] asArray() {
                return new EditText[]{
                        priceIndexInput,
                        priceContributoInput,
                        priceDispacciamentoInput,
                        priceTrasportoInput,
                        priceOneriAsosInput,
                        priceOneriArimInput,
                        priceImposteInput,
                        lossesPercentInput,
                        ivaPercentInput,
                        priceFissaInput
                };
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
        Button savePricesButton = findViewById(R.id.buttonSavePrices);
        Button calculateButton = findViewById(R.id.buttonCalculate);

        setupFieldNavigation();
        setupMonthOneAutoCopy();
        setupMonthTwoInheritanceTracking();
        savePricesButton.setOnClickListener(v -> saveTariffsForPeriod());
        calculateButton.setOnClickListener(v -> calculatePeriod());
    }

    private void setupFieldNavigation() {
        readingMonthOneInput.setNextFocusForwardId(R.id.inputReadingMonthTwo);
        readingMonthOneInput.setNextFocusDownId(R.id.inputReadingMonthTwo);
        readingMonthTwoInput.setNextFocusForwardId(R.id.inputPriceIndex);
        readingMonthTwoInput.setNextFocusDownId(R.id.inputPriceIndex);

        EditText[] orderedInputs = new EditText[]{
                monthOneTariffInputs.priceIndexInput,
                monthOneTariffInputs.priceContributoInput,
                monthOneTariffInputs.priceDispacciamentoInput,
                monthOneTariffInputs.priceTrasportoInput,
                monthOneTariffInputs.priceOneriAsosInput,
                monthOneTariffInputs.priceOneriArimInput,
                monthOneTariffInputs.priceImposteInput,
                monthOneTariffInputs.lossesPercentInput,
                monthOneTariffInputs.ivaPercentInput,
                monthOneTariffInputs.priceFissaInput,
                monthTwoTariffInputs.priceIndexInput,
                monthTwoTariffInputs.priceContributoInput,
                monthTwoTariffInputs.priceDispacciamentoInput,
                monthTwoTariffInputs.priceTrasportoInput,
                monthTwoTariffInputs.priceOneriAsosInput,
                monthTwoTariffInputs.priceOneriArimInput,
                monthTwoTariffInputs.priceImposteInput,
                monthTwoTariffInputs.lossesPercentInput,
                monthTwoTariffInputs.ivaPercentInput,
                monthTwoTariffInputs.priceFissaInput
        };

        for (int i = 0; i < orderedInputs.length - 1; i++) {
            int nextViewId = orderedInputs[i + 1].getId();
            orderedInputs[i].setNextFocusForwardId(nextViewId);
            orderedInputs[i].setNextFocusDownId(nextViewId);
        }

        EditText lastInput = orderedInputs[orderedInputs.length - 1];
        lastInput.setNextFocusForwardId(R.id.buttonSavePrices);
        lastInput.setNextFocusDownId(R.id.buttonSavePrices);
    }

    private void setupMonthOneAutoCopy() {
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceIndexInput, monthTwoTariffInputs.priceIndexInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceContributoInput, monthTwoTariffInputs.priceContributoInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceDispacciamentoInput, monthTwoTariffInputs.priceDispacciamentoInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceTrasportoInput, monthTwoTariffInputs.priceTrasportoInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceOneriAsosInput, monthTwoTariffInputs.priceOneriAsosInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceOneriArimInput, monthTwoTariffInputs.priceOneriArimInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceImposteInput, monthTwoTariffInputs.priceImposteInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.lossesPercentInput, monthTwoTariffInputs.lossesPercentInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.ivaPercentInput, monthTwoTariffInputs.ivaPercentInput);
        bindAutoCopyOnFocusChange(monthOneTariffInputs.priceFissaInput, monthTwoTariffInputs.priceFissaInput);
    }

    private void bindAutoCopyOnFocusChange(EditText source, EditText target) {
        source.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                return;
            }

            String sourceValue = source.getText() == null ? "" : source.getText().toString();
            String targetValue = target.getText() == null ? "" : target.getText().toString();
            boolean targetInherited = isMonthTwoFieldInherited(target);
            boolean targetIsEmpty = TextUtils.isEmpty(targetValue.trim());

            if (targetInherited || targetIsEmpty) {
                updateMonthTwoField(target, sourceValue);
            }
        });
    }

    private void setupMonthTwoInheritanceTracking() {
        for (EditText input : monthTwoTariffInputs.asArray()) {
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdatingMonthTwoField) {
                        return;
                    }

                    setMonthTwoFieldInherited(input, TextUtils.isEmpty(s == null ? "" : s.toString().trim()));
                }
            });
        }
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
        persistMonthTwoInheritanceState();

        toast(getString(R.string.prices_saved));
    }

    private void loadTariffs() {
        TariffConfig firstMonthConfig = tariffConfigForSlot(SLOT_MONTH_ONE);
        if (firstMonthConfig == null) {
            firstMonthConfig = defaultTariffConfig();
        }
        setTariffInputs(monthOneTariffInputs, firstMonthConfig);

        TariffConfig secondMonthConfig = tariffConfigForSlot(SLOT_MONTH_TWO);
        boolean hasSavedSecondMonthConfig = secondMonthConfig != null;
        if (secondMonthConfig == null) {
            secondMonthConfig = firstMonthConfig;
        }
        setTariffInputs(monthTwoTariffInputs, secondMonthConfig);
        applyLoadedMonthTwoInheritanceState(hasSavedSecondMonthConfig);
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
        persistMonthTwoInheritanceState();

        BillingCalculator.Result firstMonthResult = BillingCalculator.calculateBimonthlyTotal(
                monthOneKwh,
                firstMonthConfig
        );
        BillingCalculator.Result secondMonthResult = BillingCalculator.calculateBimonthlyTotal(
                monthTwoKwh,
                secondMonthConfig
        );

        double totalConsumption = firstMonthResult.consumptionKwh() + secondMonthResult.consumptionKwh();
        double totalLoss = firstMonthResult.lossKwh() + secondMonthResult.lossKwh();
        double totalFixedCost = firstMonthResult.fixedCost() + secondMonthResult.fixedCost();
        double totalSubtotal = firstMonthResult.subtotalWithoutVat() + secondMonthResult.subtotalWithoutVat();
        double totalVat = firstMonthResult.vatCost() + secondMonthResult.vatCost();
        double total = firstMonthResult.total() + secondMonthResult.total();

        String text = getString(
                R.string.result_template,
                numberFormat.format(monthOneKwh),
                numberFormat.format(monthTwoKwh),
                numberFormat.format(totalConsumption),
                numberFormat.format(totalLoss),
                numberFormat.format(totalFixedCost),
                numberFormat.format(totalSubtotal),
                numberFormat.format(totalVat),
                numberFormat.format(total)
        );
        resultView.setText(text);
        resultTotalView.setText(getString(R.string.result_total_format, totalFormat.format(total)));
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
        Double index = parseDoubleOrFallback(inputs.priceIndexInput.getText().toString(), fallback.corrispettivoLuceIndexPerKwh());
        Double contributo = parseDoubleOrFallback(inputs.priceContributoInput.getText().toString(), fallback.contributoConsumoPerKwh());
        Double dispacciamento = parseDoubleOrFallback(inputs.priceDispacciamentoInput.getText().toString(), fallback.dispacciamentoPerKwh());
        Double trasporto = parseDoubleOrFallback(inputs.priceTrasportoInput.getText().toString(), fallback.trasportoPerKwh());
        Double oneriAsos = parseDoubleOrFallback(inputs.priceOneriAsosInput.getText().toString(), fallback.oneriAsosPerKwh());
        Double oneriArim = parseDoubleOrFallback(inputs.priceOneriArimInput.getText().toString(), fallback.oneriArimPerKwh());
        Double imposte = parseDoubleOrFallback(inputs.priceImposteInput.getText().toString(), fallback.impostePerKwh());
        Double perditePercent = parseDoubleOrFallback(inputs.lossesPercentInput.getText().toString(), fallback.perditePercent());
        Double ivaPercent = parseDoubleOrFallback(inputs.ivaPercentInput.getText().toString(), fallback.ivaPercent());
        Double fissa = parseDoubleOrFallback(inputs.priceFissaInput.getText().toString(), fallback.quotaFissaBimestrale());

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
        inputs.priceIndexInput.setText(inputFormat.format(config.corrispettivoLuceIndexPerKwh()));
        inputs.priceContributoInput.setText(inputFormat.format(config.contributoConsumoPerKwh()));
        inputs.priceDispacciamentoInput.setText(inputFormat.format(config.dispacciamentoPerKwh()));
        inputs.priceTrasportoInput.setText(inputFormat.format(config.trasportoPerKwh()));
        inputs.priceOneriAsosInput.setText(inputFormat.format(config.oneriAsosPerKwh()));
        inputs.priceOneriArimInput.setText(inputFormat.format(config.oneriArimPerKwh()));
        inputs.priceImposteInput.setText(inputFormat.format(config.impostePerKwh()));
        inputs.lossesPercentInput.setText(inputFormat.format(config.perditePercent()));
        inputs.ivaPercentInput.setText(inputFormat.format(config.ivaPercent()));
        inputs.priceFissaInput.setText(inputFormat.format(config.quotaFissaBimestrale()));
    }

    private void applyLoadedMonthTwoInheritanceState(boolean hasSavedSecondMonthConfig) {
        boolean[] inheritedStates = restoreMonthTwoInheritanceStates(hasSavedSecondMonthConfig);
        EditText[] monthOneFields = monthOneTariffInputs.asArray();
        EditText[] monthTwoFields = monthTwoTariffInputs.asArray();

        for (int i = 0; i < monthTwoFields.length; i++) {
            if (inheritedStates[i]) {
                String sourceValue = monthOneFields[i].getText() == null ? "" : monthOneFields[i].getText().toString();
                updateMonthTwoField(monthTwoFields[i], sourceValue);
            } else {
                setMonthTwoFieldInherited(monthTwoFields[i], false);
            }
        }
    }

    private boolean[] restoreMonthTwoInheritanceStates(boolean hasSavedSecondMonthConfig) {
        boolean[] inheritedStates = new boolean[tariffPreferenceKeys.length];

        if (!hasSavedSecondMonthConfig) {
            Arrays.fill(inheritedStates, true);
            return inheritedStates;
        }

        if (!hasMonthTwoInheritanceState()) {
            return inheritedStates;
        }

        for (int i = 0; i < tariffPreferenceKeys.length; i++) {
            inheritedStates[i] = preferences.getBoolean(slotKey(inheritedKey(tariffPreferenceKeys[i]), SLOT_MONTH_TWO), false);
        }
        return inheritedStates;
    }

    private boolean hasMonthTwoInheritanceState() {
        for (String key : tariffPreferenceKeys) {
            if (!preferences.contains(slotKey(inheritedKey(key), SLOT_MONTH_TWO))) {
                return false;
            }
        }
        return true;
    }

    private void persistMonthTwoInheritanceState() {
        SharedPreferences.Editor editor = preferences.edit();
        EditText[] monthTwoFields = monthTwoTariffInputs.asArray();

        for (int i = 0; i < tariffPreferenceKeys.length; i++) {
            editor.putBoolean(
                    slotKey(inheritedKey(tariffPreferenceKeys[i]), SLOT_MONTH_TWO),
                    isMonthTwoFieldInherited(monthTwoFields[i])
            );
        }

        editor.apply();
    }

    private void updateMonthTwoField(EditText target, String value) {
        isUpdatingMonthTwoField = true;
        target.setText(value);
        setMonthTwoFieldInherited(target, true);
        isUpdatingMonthTwoField = false;
    }

    private void setMonthTwoFieldInherited(EditText input, boolean inherited) {
        input.setTag(inherited);
        input.setBackgroundColor(inherited ? MONTH_TWO_INHERITED_BACKGROUND_COLOR : Color.TRANSPARENT);
    }

    private boolean isMonthTwoFieldInherited(EditText input) {
        Object tag = input.getTag();
        return tag instanceof Boolean && (Boolean) tag;
    }

    private void persistTariffForSlot(String slot, TariffConfig config) {
        preferences.edit()
                .putLong(slotKey(KEY_PRICE_INDEX, slot), Double.doubleToRawLongBits(config.corrispettivoLuceIndexPerKwh()))
                .putLong(slotKey(KEY_PRICE_CONTRIBUTO, slot), Double.doubleToRawLongBits(config.contributoConsumoPerKwh()))
                .putLong(slotKey(KEY_PRICE_DISPACCIAMENTO, slot), Double.doubleToRawLongBits(config.dispacciamentoPerKwh()))
                .putLong(slotKey(KEY_PRICE_TRASPORTO, slot), Double.doubleToRawLongBits(config.trasportoPerKwh()))
                .putLong(slotKey(KEY_PRICE_ONERI_ASOS, slot), Double.doubleToRawLongBits(config.oneriAsosPerKwh()))
                .putLong(slotKey(KEY_PRICE_ONERI_ARIM, slot), Double.doubleToRawLongBits(config.oneriArimPerKwh()))
                .putLong(slotKey(KEY_PRICE_IMPOSTE, slot), Double.doubleToRawLongBits(config.impostePerKwh()))
                .putLong(slotKey(KEY_PERDITE_PERCENT, slot), Double.doubleToRawLongBits(config.perditePercent()))
                .putLong(slotKey(KEY_IVA_PERCENT, slot), Double.doubleToRawLongBits(config.ivaPercent()))
                .putLong(slotKey(KEY_PRICE_FISSA, slot), Double.doubleToRawLongBits(config.quotaFissaBimestrale()))
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

    private String inheritedKey(String key) {
        return KEY_INHERITED_PREFIX + key;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}