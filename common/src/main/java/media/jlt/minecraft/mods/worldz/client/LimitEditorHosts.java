package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.client.gui.screens.Screen;

/**
 * Small callback interfaces shared by every Customize screen that edits border/exterior/End
 * settings ({@code WorldzCustomizeScreen}, {@code SingleBiomeCustomizeScreen},
 * {@code ChaosBiomesCustomizeScreen}) -- lets {@link WorldzBorderScreen}, {@link WorldzExteriorScreen},
 * and {@link EndBorderScreen} be reused across all of them instead of one screen each (TODO 5.3).
 */
final class LimitEditorHosts {
    private LimitEditorHosts() {
    }

    interface BorderEditorHost {
        Screen asScreen();

        void setBorder(boolean overworld, WorldzCustomization.BorderSettings settings);
    }

    interface ExteriorEditorHost {
        Screen asScreen();

        void setExterior(boolean overworld, WorldzCustomization.ExteriorSettings settings);
    }

    interface EndBorderEditorHost {
        Screen asScreen();

        void setEndBorder(WorldzCustomization.EndBorderSettings settings);
    }
}
