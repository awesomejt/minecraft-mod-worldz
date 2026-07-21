package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.CaveCustomization;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Small world-creation screen for the {@code jlt_worldz:cave} typed preset (GOALS 25-26). */
final class CaveCustomizeScreen extends Screen implements
    LimitEditorHosts.BorderEditorHost, LimitEditorHosts.ExteriorEditorHost, LimitEditorHosts.EndBorderEditorHost {
    private static final Component TITLE = Component.translatable("jlt_worldz.cave.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private final String spawnDepthYText;
    private final String sealedSurfaceYText;
    private final String cavernRadiusBlocksText;
    private final String cavernHeightBlocksText;
    private EditBox spawnDepthY;
    private boolean sealedSurface;
    private EditBox sealedSurfaceY;
    private boolean cavernEnabled;
    private EditBox cavernRadiusBlocks;
    private EditBox cavernHeightBlocks;
    private boolean chestEnabled;
    private StarterKitTier chestTier;
    private Button chestTierButton;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.EndBorderSettings endBorder;
    private WorldzCustomization.ExteriorSettings netherExterior;

    CaveCustomizeScreen(CreateWorldScreen parent, CaveCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.sealedSurface = initial.sealedSurface();
        this.cavernEnabled = initial.cavernEnabled();
        this.chestEnabled = initial.chestEnabled();
        this.chestTier = initial.chestTier();
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.endBorder = initial.endBorder();
        this.netherExterior = initial.netherExterior();
        this.spawnDepthYText = Integer.toString(initial.spawnDepthY());
        this.sealedSurfaceYText = Integer.toString(initial.sealedSurfaceY());
        this.cavernRadiusBlocksText = Integer.toString(initial.cavernRadiusBlocks());
        this.cavernHeightBlocksText = Integer.toString(initial.cavernHeightBlocks());
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.spawnDepthY = textField(Component.translatable("jlt_worldz.cave.spawn_depth_y"), this.spawnDepthYText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.spawnDepthY, Component.translatable("jlt_worldz.cave.spawn_depth_y")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.cave.sealed_surface"), this.font)
            .selected(this.sealedSurface)
            .onValueChange((checkbox, selected) -> this.sealedSurface = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.sealedSurfaceY = textField(Component.translatable("jlt_worldz.cave.sealed_surface_y"), this.sealedSurfaceYText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.sealedSurfaceY, Component.translatable("jlt_worldz.cave.sealed_surface_y")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.cave.cavern_enabled"), this.font)
            .selected(this.cavernEnabled)
            .onValueChange((checkbox, selected) -> this.cavernEnabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.cavernRadiusBlocks = textField(
            Component.translatable("jlt_worldz.cave.cavern_radius"), this.cavernRadiusBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.cavernRadiusBlocks, Component.translatable("jlt_worldz.cave.cavern_radius")
        ));

        this.cavernHeightBlocks = textField(
            Component.translatable("jlt_worldz.cave.cavern_height"), this.cavernHeightBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.cavernHeightBlocks, Component.translatable("jlt_worldz.cave.cavern_height")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.cave.chest_enabled"), this.font)
            .selected(this.chestEnabled)
            .onValueChange((checkbox, selected) -> this.chestEnabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

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

        Tooltip exteriorTooltip = Tooltip.create(Component.translatable("jlt_worldz.customize.exterior.tooltip"));
        form.addChild(Button.builder(
            exteriorButtonLabel("nether", this.netherExterior),
            button -> this.minecraft.gui.setScreen(new WorldzExteriorScreen(this, false, this.netherExterior))
        ).tooltip(exteriorTooltip).width(FORM_WIDTH).build());

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
            "jlt_worldz.cave.chest_tier",
            Component.translatable("jlt_worldz.cave.chest_tier." + tier.serializedName())
        );
    }

    private EditBox textField(Component narration, String value) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(10);
        field.setValue(value);
        return field;
    }

    private void apply() {
        try {
            CaveCustomization customization = CaveCustomization.fromText(
                this.spawnDepthY.getValue(),
                this.sealedSurface,
                this.sealedSurfaceY.getValue(),
                this.cavernEnabled,
                this.cavernRadiusBlocks.getValue(),
                this.cavernHeightBlocks.getValue(),
                this.chestEnabled,
                this.chestTier,
                this.overworldBorder,
                this.netherBorder,
                this.endBorder,
                this.netherExterior
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> CavePresetEditor.apply(registries, dimensions, customization)
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

    @Override
    public void setExterior(boolean overworld, WorldzCustomization.ExteriorSettings settings) {
        if (!overworld) {
            this.netherExterior = settings;
        }
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

    private static Component exteriorButtonLabel(String dimension, WorldzCustomization.ExteriorSettings exterior) {
        return Component.translatable(
            "jlt_worldz.customize." + dimension + "_exterior",
            Component.translatable("jlt_worldz.customize.exterior.mode." + exterior.mode().serializedName())
        );
    }
}
