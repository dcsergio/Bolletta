package it.sdc.bolletta;

public final class BillingCalculator {

    private BillingCalculator() {
    }

    public static Result calculateBimonthlyTotal(double consumptionKwh, TariffConfig config) {
        double baseConsumption = Math.max(0d, consumptionKwh);
        double lossKwh = baseConsumption * (Math.max(0d, config.perditePercent()) / 100d);

        double corrispettivo = config.corrispettivoLuceIndexPerKwh() * (baseConsumption + lossKwh);
        double contributo = config.contributoConsumoPerKwh() * (baseConsumption + lossKwh);
        double dispacciamento = config.dispacciamentoPerKwh() * baseConsumption;
        double quadroA = corrispettivo + contributo + dispacciamento;

        double quadroB = config.trasportoPerKwh() * baseConsumption;
        double quadroC = (config.oneriAsosPerKwh() + config.oneriArimPerKwh()) * baseConsumption;
        double quadroD = config.impostePerKwh() * baseConsumption;

        double fixedCost = config.quotaFissaBimestrale();
        double subtotalWithoutVat = quadroA + quadroB + quadroC + quadroD + fixedCost;
        double vatCost = subtotalWithoutVat * (Math.max(0d, config.ivaPercent()) / 100d);
        double total = subtotalWithoutVat + vatCost;

        return new Result(baseConsumption, lossKwh, quadroA, quadroB, quadroC, quadroD, fixedCost, subtotalWithoutVat, vatCost, total);
    }

    public record Result(double consumptionKwh, double lossKwh, double quadroA, double quadroB,
                         double quadroC, double quadroD, double fixedCost,
                         double subtotalWithoutVat, double vatCost, double total) {
    }
}

