package it.sdc.bolletta;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void bimonthlyCalculation_isCorrect() {
        TariffConfig config = new TariffConfig(
                0.11,
                0.024,
                0.020,
                0.0135,
                0.0297,
                0.0017,
                0.0227,
                10.0,
                10.0,
                0.0
        );

        BillingCalculator.Result result = BillingCalculator.calculateBimonthlyTotal(250, config);

        assertEquals(250.0, result.consumptionKwh(), 0.0001);
        assertEquals(25.0, result.lossKwh(), 0.0001);
        assertEquals(41.85, result.quadroA(), 0.0001);
        assertEquals(3.375, result.quadroB(), 0.0001);
        assertEquals(7.85, result.quadroC(), 0.0001);
        assertEquals(5.675, result.quadroD(), 0.0001);
        assertEquals(58.75, result.subtotalWithoutVat(), 0.0001);
        assertEquals(5.875, result.vatCost(), 0.0001);
        assertEquals(64.625, result.total(), 0.0001);
    }

    @Test
    public void negativeConsumption_isClampedToZero() {
        TariffConfig config = new TariffConfig(
                0.11,
                0.024,
                0.020,
                0.0135,
                0.0297,
                0.0017,
                0.0227,
                10.0,
                10.0,
                3.0
        );

        BillingCalculator.Result result = BillingCalculator.calculateBimonthlyTotal(-20, config);

        assertEquals(0.0, result.consumptionKwh(), 0.0001);
        assertEquals(3.3, result.total(), 0.0001);
    }
}