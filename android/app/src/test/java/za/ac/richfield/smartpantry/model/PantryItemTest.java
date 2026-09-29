package za.ac.richfield.smartpantry.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for how an amount is written on a line of the pantry.
 *
 * <p>These exist because the list read "12 piece". The rule is not simply "add
 * an s": units that count things take a plural and units that measure them do
 * not, and "250 gs" is as wrong as "12 piece". The two cases are easy to
 * conflate and easy to break again, so both are pinned here.
 *
 * <p>Plain JUnit on the JVM, like the matcher tests: no emulator, so they are
 * cheap enough to run on every build.
 */
public class PantryItemTest {

    private static PantryItem item(double quantity, String unit) {
        return new PantryItem(1L, "Eggs", quantity, unit, null);
    }

    @Test
    public void countingUnitTakesAPluralWhenThereIsMoreThanOne() {
        assertEquals("12 pieces", item(12, "piece").getDisplayAmount());
        assertEquals("3 slices", item(3, "slice").getDisplayAmount());
        assertEquals("2 cans", item(2, "can").getDisplayAmount());
    }

    @Test
    public void countingUnitStaysSingularWhenThereIsExactlyOne() {
        assertEquals("1 piece", item(1, "piece").getDisplayAmount());
        assertEquals("1 clove", item(1, "clove").getDisplayAmount());
    }

    @Test
    public void countingUnitTakesAPluralWhenThereIsLessThanOne() {
        // Half a packet is still "packets" in English, and the comparison is
        // against exactly one rather than against "more than one".
        assertEquals("0.5 packets", item(0.5, "packet").getDisplayAmount());
    }

    @Test
    public void measuringUnitNeverTakesAPlural() {
        assertEquals("250 g", item(250, "g").getDisplayAmount());
        assertEquals("2 kg", item(2, "kg").getDisplayAmount());
        assertEquals("100 tsp", item(100, "tsp").getDisplayAmount());
        assertEquals("500 ml", item(500, "ml").getDisplayAmount());
    }

    @Test
    public void wholeNumbersLoseTheirDecimalPoint() {
        // The API sends NUMERIC, which Gson reads as a double, so an untouched
        // quantity would be written "250.0 g".
        assertEquals("250 g", item(250.0, "g").getDisplayAmount());
        assertEquals("1.5 kg", item(1.5, "kg").getDisplayAmount());
    }

    @Test
    public void unitIsMatchedWhateverItsCase() {
        assertEquals("4 Slices", item(4, "Slice").getDisplayAmount());
    }

    @Test
    public void aMissingUnitLeavesTheQuantityOnItsOwn() {
        assertEquals("3", item(3, null).getDisplayAmount());
        assertEquals("3", item(3, "").getDisplayAmount());
    }
}
