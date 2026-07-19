package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
import net.minecraft.network.chat.Component;

/** Builds the button label for a {@link ResizeStyle} toggle, shared by every border screen. */
final class ResizeStyleLabel {
    private ResizeStyleLabel() {
    }

    static Component of(ResizeStyle style) {
        return Component.translatable(
            "jlt_worldz.customize.resize_style",
            Component.translatable("jlt_worldz.customize.resize_style." + style.serializedName())
        );
    }
}
