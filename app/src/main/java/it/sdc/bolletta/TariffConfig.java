package it.sdc.bolletta;

public record TariffConfig(double corrispettivoLuceIndexPerKwh, double contributoConsumoPerKwh,
                           double dispacciamentoPerKwh, double trasportoPerKwh,
                           double oneriAsosPerKwh, double oneriArimPerKwh, double impostePerKwh,
                           double perditePercent, double ivaPercent, double quotaFissaBimestrale) {
}

