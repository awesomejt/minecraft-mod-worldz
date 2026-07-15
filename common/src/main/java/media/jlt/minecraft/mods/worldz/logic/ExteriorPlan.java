package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.ExteriorConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

/**
 * Immutable exterior-terrain choices baked into one Worldz world.
 *
 * @param overworld resolved Overworld envelope
 * @param nether resolved Nether envelope
 */
public record ExteriorPlan(DimensionEnvelope overworld, DimensionEnvelope nether) {
    /**
     * Returns a plan that preserves vanilla terrain in both dimensions.
     *
     * @return normal terrain plan
     */
    public static ExteriorPlan normal() {
        return new ExteriorPlan(DimensionEnvelope.normal(), DimensionEnvelope.normal());
    }

    /**
     * Resolves explicit or automatic YAML boundaries.
     *
     * @param config sanitized startup configuration
     * @return immutable resolved plan
     */
    public static ExteriorPlan fromConfig(WorldzConfig config) {
        return new ExteriorPlan(
            DimensionEnvelope.fromConfig(config.overworldExterior, config.overworldBorder),
            DimensionEnvelope.fromConfig(config.netherExterior, config.netherBorder)
        );
    }

    /**
     * One dimension's resolved terrain envelope.
     *
     * @param mode terrain generated outside the natural radius
     * @param boundaryRadiusBlocks outer square half-width
     * @param oceanTransitionWidthBlocks ocean width inside the outer boundary
     */
    public record DimensionEnvelope(ExteriorMode mode, int boundaryRadiusBlocks, int oceanTransitionWidthBlocks) {
        /** Validates persisted envelope values. */
        public DimensionEnvelope {
            if (mode == null) {
                throw new IllegalArgumentException("exterior mode is required");
            }
            if (boundaryRadiusBlocks < 0 || oceanTransitionWidthBlocks < 0) {
                throw new IllegalArgumentException("exterior radii must not be negative");
            }
            if (mode != ExteriorMode.NORMAL && boundaryRadiusBlocks == 0) {
                throw new IllegalArgumentException("non-normal exterior requires a resolved boundary");
            }
            if (mode != ExteriorMode.OCEAN && oceanTransitionWidthBlocks != 0) {
                oceanTransitionWidthBlocks = 0;
            } else if (oceanTransitionWidthBlocks > boundaryRadiusBlocks) {
                oceanTransitionWidthBlocks = boundaryRadiusBlocks;
            }
        }

        /**
         * Returns an envelope that never changes delegated terrain.
         *
         * @return normal terrain envelope
         */
        public static DimensionEnvelope normal() {
            return new DimensionEnvelope(ExteriorMode.NORMAL, 0, 0);
        }

        /**
         * Returns the solid-terrain radius before ocean begins.
         *
         * @return square land half-width
         */
        public int solidRadiusBlocks() {
            return mode == ExteriorMode.OCEAN ? boundaryRadiusBlocks - oceanTransitionWidthBlocks : boundaryRadiusBlocks;
        }

        /**
         * Classifies a block column using overflow-safe square distance.
         *
         * @param blockX block X
         * @param blockZ block Z
         * @return terrain to generate at the column
         */
        public ExteriorMode modeAt(int blockX, int blockZ) {
            if (mode == ExteriorMode.NORMAL) {
                return ExteriorMode.NORMAL;
            }
            long distance = Math.max(Math.abs((long)blockX), Math.abs((long)blockZ));
            int naturalRadius = mode == ExteriorMode.OCEAN ? solidRadiusBlocks() : boundaryRadiusBlocks;
            return distance > naturalRadius ? mode : ExteriorMode.NORMAL;
        }

        private static DimensionEnvelope fromConfig(ExteriorConfig exterior, BorderConfig border) {
            if (exterior.mode == ExteriorMode.NORMAL) {
                return normal();
            }
            int boundary = exterior.boundaryRadiusBlocks;
            if (boundary == 0) {
                boundary = Math.max(border.initialRadiusBlocks, border.finalRadiusBlocks);
            }
            return new DimensionEnvelope(exterior.mode, boundary, exterior.oceanTransitionWidthBlocks);
        }
    }
}
