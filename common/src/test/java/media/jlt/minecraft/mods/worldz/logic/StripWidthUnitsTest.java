package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripWidthUnitsTest {
    @Test
    void sixtyFiveBlocksRoundTripsLosslesslyThroughExactChunkDisplay() {
        String chunks = StripWidthUnits.convert("65", RadiusUnit.BLOCKS, RadiusUnit.CHUNKS);

        assertEquals("4.0625", chunks);
        assertEquals("65", StripWidthUnits.convert(chunks, RadiusUnit.CHUNKS, RadiusUnit.BLOCKS));
        assertEquals("65", StripWidthUnits.toBlocksText(chunks, RadiusUnit.CHUNKS));
    }

    @Test
    void chunkAlignedWidthsStayCoherentAndEditable() {
        assertEquals("4", StripWidthUnits.toDisplayText(64, RadiusUnit.CHUNKS));
        assertEquals("4", StripWidthUnits.convert("64", RadiusUnit.BLOCKS, RadiusUnit.CHUNKS));
        assertEquals("64", StripWidthUnits.convert("4", RadiusUnit.CHUNKS, RadiusUnit.BLOCKS));
        assertEquals("80", StripWidthUnits.toBlocksText("5", RadiusUnit.CHUNKS));
    }

    @Test
    void fractionalChunksThatDoNotResolveToWholeBlocksRemainInvalidForDownstreamValidation() {
        assertEquals("4.1", StripWidthUnits.toBlocksText("4.1", RadiusUnit.CHUNKS));
        assertEquals("not-a-number", StripWidthUnits.convert("not-a-number", RadiusUnit.CHUNKS, RadiusUnit.BLOCKS));
    }
}
