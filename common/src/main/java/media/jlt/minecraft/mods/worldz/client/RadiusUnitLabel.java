package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.RadiusUnit;
import net.minecraft.network.chat.Component;

/** Builds the button label for a {@link RadiusUnit} toggle, shared by every border/exterior screen. */
final class RadiusUnitLabel {
    private RadiusUnitLabel() {
    }

    static Component of(RadiusUnit unit) {
        return Component.translatable(
            "jlt_worldz.customize.radius_unit",
            Component.translatable("jlt_worldz.customize.radius_unit." + (unit == RadiusUnit.BLOCKS ? "blocks" : "chunks"))
        );
    }
}
