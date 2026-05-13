package it.sdc.bolletta;

public final class BillingCalculator {

    private BillingCalculator() {
    }

    public static Result calculateBimonthlyTotal(double consumptionKwh, TariffConfig config) {
        double baseConsumption = Math.max(0d, consumptionKwh);
        double lossKwh = baseConsumption * (Math.max(0d, config.getPerditePercent()) / 100d);

        double corrispettivo = config.getCorrispettivoLuceIndexPerKwh() * (baseConsumption + lossKwh);
        double contributo = config.getContributoConsumoPerKwh() * (baseConsumption + lossKwh);
        double dispacciamento = config.getDispacciamentoPerKwh() * baseConsumption;
        double quadroA = corrispettivo + contributo + dispacciamento;

        double quadroB = config.getTrasportoPerKwh() * baseConsumption;
        double quadroC = (config.getOneriAsosPerKwh() + config.getOneriArimPerKwh()) * baseConsumption;
        double quadroD = config.getImpostePerKwh() * baseConsumption;

        double fixedCost = config.getQuotaFissaBimestrale();
        double subtotalWithoutVat = quadroA + quadroB + quadroC + quadroD + fixedCost;
        double vatCost = subtotalWithoutVat * (Math.max(0d, config.getIvaPercent()) / 100d);
        double total = subtotalWithoutVat + vatCost;

        return new Result(baseConsumption, lossKwh, quadroA, quadroB, quadroC, quadroD, fixedCost, subtotalWithoutVat, vatCost, total);
    }

    public static class Result {
        private final double consumptionKwh;
        private final double lossKwh;
        private final double quadroA;
        private final double quadroB;
        private final double quadroC;
        private final double quadroD;
        private final double fixedCost;
        private final double subtotalWithoutVat;
        private final double vatCost;
        private final double total;

        public Result(
                double consumptionKwh,
                double lossKwh,
                double quadroA,
                double quadroB,
                double quadroC,
                double quadroD,
                double fixedCost,
                double subtotalWithoutVat,
                double vatCost,
                double total
        ) {
            this.consumptionKwh = consumptionKwh;
            this.lossKwh = lossKwh;
            this.quadroA = quadroA;
            this.quadroB = quadroB;
            this.quadroC = quadroC;
            this.quadroD = quadroD;
            this.fixedCost = fixedCost;
            this.subtotalWithoutVat = subtotalWithoutVat;
            this.vatCost = vatCost;
            this.total = total;
        }

        public double getConsumptionKwh() {
            return consumptionKwh;
        }

        public double getLossKwh() {
            return lossKwh;
        }

        public double getQuadroA() {
            return quadroA;
        }

        public double getQuadroB() {
            return quadroB;
        }

        public double getQuadroC() {
            return quadroC;
        }

        public double getQuadroD() {
            return quadroD;
        }

        public double getFixedCost() {
            return fixedCost;
        }

        public double getSubtotalWithoutVat() {
            return subtotalWithoutVat;
        }

        public double getVatCost() {
            return vatCost;
        }

        public double getTotal() {
            return total;
        }
    }
}

