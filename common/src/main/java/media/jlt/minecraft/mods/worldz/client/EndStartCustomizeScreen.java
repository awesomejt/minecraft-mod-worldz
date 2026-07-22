package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.EndStartCustomization;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Small world-creation screen for the {@code jlt_worldz:end_start} typed preset (GOALS 34). */
final class EndStartCustomizeScreen extends Screen implements
    LimitEditorHosts.BorderEditorHost, LimitEditorHosts.EndBorderEditorHost {
    private static final Component TITLE = Component.translatable("jlt_worldz.end_start.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private StarterKitTier chestTier;
    private Button chestTierButton;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.EndBorderSettings endBorder;

    EndStartCustomizeScreen(CreateWorldScreen parent, EndStartCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.chestTier = initial.chestTier();
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.endBorder = initial.endBorder();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.chestTierButton = Button.builder(chestTierLabel(this.chestTier), button -> cycleChestTier())
            .width(FORM_WIDTH)
            .build();
        form.addChild(this.chestTierButton);

        Tooltip borderTooltip = Tooltip.create(Component.translatable("jlt_worldz.customize.border.tooltip"));
        LinearLayout borderButtons = LinearLayout.horizontal().spacing(10);
        borderButtons.addChild(Button.builder(
            borderButtonLabel("overworld", this.overworldBorder.enabled()),
            button -> this.minecraft.gui.setScreen(new WorldzBorderScreen(this, true, this.overworldBorder))
        ).tooltip(borderTooltip).build());
        borderButtons.addChild(Button.builder(
            borderButtonLabel("nether", this.netherBorder.enabled()),
            button -> this.minecraft.gui.setScreen(new WorldzBorderScreen(this, false, this.netherBorder))
        ).tooltip(borderTooltip).build());
        form.addChild(borderButtons);

        form.addChild(Button.builder(
            endBorderButtonLabel(this.endBorder.carryFromOverworld()),
            button -> this.minecraft.gui.setScreen(new EndBorderScreen(this, this.endBorder))
        ).width(FORM_WIDTH).build());

        this.errorMessage = new MultiLineTextWidget(CommonComponents.EMPTY, this.font).setMaxWidth(FORM_WIDTH).setMaxRows(2).setCentered(true);
        form.addChild(this.errorMessage);

        this.scrollArea = new ScrollableLayout(this.minecraft, form, SCROLL_AREA_MIN_HEIGHT);
        this.scrollArea.setMinWidth(FORM_WIDTH);
        content.addChild(this.scrollArea);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.apply()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private void cycleChestTier() {
        StarterKitTier[] values = StarterKitTier.values();
        this.chestTier = values[(this.chestTier.ordinal() + 1) % values.length];
        this.chestTierButton.setMessage(chestTierLabel(this.chestTier));
    }

    private static Component chestTierLabel(StarterKitTier tier) {
        return Component.translatable(
            "jlt_worldz.end_start.chest_tier",
            Component.translatable("jlt_worldz.end_start.chest_tier." + tier.serializedName())
        );
    }

    private void apply() {
        try {
            EndStartCustomization customization = new EndStartCustomization(
                this.chestTier, this.overworldBorder, this.netherBorder, this.endBorder
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> EndStartPresetEditor.apply(registries, dimensions, customization)
            );
            this.onClose();
        } catch (IllegalArgumentException exception) {
            this.errorMessage.setMessage(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
            this.repositionElements();
        }
    }

    @Override
    protected void repositionElements() {
        this.scrollArea.setMaxHeight(SCROLL_AREA_MIN_HEIGHT);
        this.layout.arrangeElements();
        int availableExtraHeight = this.height - this.layout.getFooterHeight() - this.scrollArea.getRectangle().bottom();
        this.scrollArea.setMaxHeight(this.scrollArea.getHeight() + availableExtraHeight);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public Screen asScreen() {
        return this;
    }

    @Override
    public void setBorder(boolean overworld, WorldzCustomization.BorderSettings settings) {
        if (overworld) {
            this.overworldBorder = settings;
        } else {
            this.netherBorder = settings;
        }
    }

    @Override
    public void setEndBorder(WorldzCustomization.EndBorderSettings settings) {
        this.endBorder = settings;
    }

    private static Component borderButtonLabel(String dimension, boolean enabled) {
        return Component.translatable(
            "jlt_worldz.customize." + dimension + "_border",
            Component.translatable(enabled ? "options.on" : "options.off")
        );
    }

    private static Component endBorderButtonLabel(boolean carryFromOverworld) {
        return Component.translatable(
            "jlt_worldz.customize.end_border",
            Component.translatable(carryFromOverworld ? "options.on" : "options.off")
        );
    }
}
