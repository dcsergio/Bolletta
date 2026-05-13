package it.sdc.bolletta;

public class TariffConfig {
    private final double corrispettivoLuceIndexPerKwh;
    private final double contributoConsumoPerKwh;
    private final double dispacciamentoPerKwh;
    private final double trasportoPerKwh;
    private final double oneriAsosPerKwh;
    private final double oneriArimPerKwh;
    private final double impostePerKwh;
    private final double perditePercent;
    private final double ivaPercent;
    private final double quotaFissaBimestrale;

    public TariffConfig(
            double corrispettivoLuceIndexPerKwh,
            double contributoConsumoPerKwh,
            double dispacciamentoPerKwh,
            double trasportoPerKwh,
            double oneriAsosPerKwh,
            double oneriArimPerKwh,
            double impostePerKwh,
            double perditePercent,
            double ivaPercent,
            double quotaFissaBimestrale
    ) {
        this.corrispettivoLuceIndexPerKwh = corrispettivoLuceIndexPerKwh;
        this.contributoConsumoPerKwh = contributoConsumoPerKwh;
        this.dispacciamentoPerKwh = dispacciamentoPerKwh;
        this.trasportoPerKwh = trasportoPerKwh;
        this.oneriAsosPerKwh = oneriAsosPerKwh;
        this.oneriArimPerKwh = oneriArimPerKwh;
        this.impostePerKwh = impostePerKwh;
        this.perditePercent = perditePercent;
        this.ivaPercent = ivaPercent;
        this.quotaFissaBimestrale = quotaFissaBimestrale;
    }

    public double getCorrispettivoLuceIndexPerKwh() {
        return corrispettivoLuceIndexPerKwh;
    }

    public double getContributoConsumoPerKwh() {
        return contributoConsumoPerKwh;
    }

    public double getDispacciamentoPerKwh() {
        return dispacciamentoPerKwh;
    }

    public double getTrasportoPerKwh() {
        return trasportoPerKwh;
    }

    public double getOneriAsosPerKwh() {
        return oneriAsosPerKwh;
    }

    public double getOneriArimPerKwh() {
        return oneriArimPerKwh;
    }

    public double getImpostePerKwh() {
        return impostePerKwh;
    }

    public double getPerditePercent() {
        return perditePercent;
    }

    public double getIvaPercent() {
        return ivaPercent;
    }

    public double getQuotaFissaBimestrale() {
        return quotaFissaBimestrale;
    }
}

