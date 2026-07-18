package media.jlt.minecraft.mods.worldz.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RadiusUnitTest {
    @Test
    void blocksDisplayTextIsThePersistedValue() {
        assertEquals("512", RadiusUnit.BLOCKS.toDisplayText(512));
    }

    @Test
    void chunksDisplayTextDividesBySixteen() {
        assertEquals("32", RadiusUnit.CHUNKS.toDisplayText(512));
    }

    @Test
    void chunksDisplayTextRoundsAndNeverReachesZero() {
        assertEquals("1", RadiusUnit.CHUNKS.toDisplayText(1));
        assertEquals("1", RadiusUnit.CHUNKS.toDisplayText(8));
        assertEquals("2", RadiusUnit.CHUNKS.toDisplayText(24));
    }

    @Test
    void nextAlternatesBetweenTheTwoUnits() {
        assertSame(RadiusUnit.CHUNKS, RadiusUnit.BLOCKS.next());
        assertSame(RadiusUnit.BLOCKS, RadiusUnit.CHUNKS.next());
    }

    @Test
    void convertSameUnitReturnsTextUnchanged() {
        assertEquals("512", RadiusUnit.BLOCKS.convert("512", RadiusUnit.BLOCKS));
    }

    @Test
    void convertBlocksToChunksAndBack() {
        assertEquals("32", RadiusUnit.BLOCKS.convert("512", RadiusUnit.CHUNKS));
        assertEquals("512", RadiusUnit.CHUNKS.convert("32", RadiusUnit.BLOCKS));
    }

    @Test
    void convertLeavesNonNumericTextUnchanged() {
        assertEquals(RadiusUnit.AUTO, RadiusUnit.BLOCKS.convert(RadiusUnit.AUTO, RadiusUnit.CHUNKS));
        assertEquals("not-a-number", RadiusUnit.CHUNKS.convert("not-a-number", RadiusUnit.BLOCKS));
    }

    @Test
    void toBlocksTextIsANoOpForBlocks() {
        assertEquals("512", RadiusUnit.BLOCKS.toBlocksText("512"));
    }

    @Test
    void toBlocksTextMultipliesChunksBySixteen() {
        assertEquals("512", RadiusUnit.CHUNKS.toBlocksText("32"));
    }

    @Test
    void toBlocksTextPreservesTheAutoSentinelRegardlessOfUnit() {
        assertEquals(RadiusUnit.AUTO, RadiusUnit.CHUNKS.toBlocksText(RadiusUnit.AUTO));
        assertEquals("AUTO", RadiusUnit.CHUNKS.toBlocksText("AUTO"));
    }

    @Test
    void toBlocksTextLeavesUnparsableChunkTextForDownstreamValidationToReject() {
        assertEquals("not-a-number", RadiusUnit.CHUNKS.toBlocksText("not-a-number"));
    }
}
