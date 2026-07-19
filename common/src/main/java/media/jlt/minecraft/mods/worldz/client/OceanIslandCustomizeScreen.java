package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.OceanIslandCustomization;
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

/** Small world-creation screen for the {@code jlt_worldz:ocean_island} typed preset (GOALS 01, 04). */
final class OceanIslandCustomizeScreen extends Screen implements
    LimitEditorHosts.BorderEditorHost, LimitEditorHosts.ExteriorEditorHost, LimitEditorHosts.EndBorderEditorHost {
    private static final Component TITLE = Component.translatable("jlt_worldz.ocean_island.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private EditBox islandBiome;
    private EditBox radiusBlocks;
    private EditBox shapeAmplitude;
    private EditBox shoreWidthBlocks;
    private EditBox oceanShallowWidthBlocks;
    private EditBox oceanDeepenWidthBlocks;
    private EditBox oceanShallowDepthBlocks;
    private EditBox oceanDeepDepthBlocks;
    private EditBox oceanRegionScaleBlocks;
    private EditBox exclusionZoneRadiusBlocks;
    private String islandBiomeText;
    private String radiusBlocksText;
    private String shapeAmplitudeText;
    private String shoreWidthBlocksText;
    private String oceanShallowWidthBlocksText;
    private String oceanDeepenWidthBlocksText;
    private String oceanShallowDepthBlocksText;
    private String oceanDeepDepthBlocksText;
    private String oceanRegionScaleBlocksText;
    private boolean exclusionZoneEnabled;
    private String exclusionZoneRadiusBlocksText;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.EndBorderSettings endBorder;
    private WorldzCustomization.ExteriorSettings netherExterior;

    OceanIslandCustomizeScreen(CreateWorldScreen parent, OceanIslandCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.islandBiomeText = initial.islandBiome();
        this.radiusBlocksText = Integer.toString(initial.radiusBlocks());
        this.shapeAmplitudeText = Double.toString(initial.shapeAmplitude());
        this.shoreWidthBlocksText = Integer.toString(initial.shoreWidthBlocks());
        this.oceanShallowWidthBlocksText = Integer.toString(initial.oceanShallowWidthBlocks());
        this.oceanDeepenWidthBlocksText = Integer.toString(initial.oceanDeepenWidthBlocks());
        this.oceanShallowDepthBlocksText = Integer.toString(initial.oceanShallowDepthBlocks());
        this.oceanDeepDepthBlocksText = Integer.toString(initial.oceanDeepDepthBlocks());
        this.oceanRegionScaleBlocksText = Integer.toString(initial.oceanRegionScaleBlocks());
        this.exclusionZoneEnabled = initial.exclusionZoneEnabled();
        this.exclusionZoneRadiusBlocksText = Integer.toString(initial.exclusionZoneRadiusBlocks());
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.endBorder = initial.endBorder();
        this.netherExterior = initial.netherExterior();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.islandBiome = textField(Component.translatable("jlt_worldz.ocean_island.island_biome"), this.islandBiomeText);
        this.islandBiome.setResponder(value -> this.islandBiomeText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.islandBiome, Component.translatable("jlt_worldz.ocean_island.island_biome")
        ));

        this.radiusBlocks = textField(Component.translatable("jlt_worldz.ocean_island.radius"), this.radiusBlocksText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.radiusBlocks, Component.translatable("jlt_worldz.ocean_island.radius")
        ));

        this.shapeAmplitude = textField(Component.translatable("jlt_worldz.ocean_island.shape_amplitude"), this.shapeAmplitudeText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.shapeAmplitude, Component.translatable("jlt_worldz.ocean_island.shape_amplitude")
        ));

        this.shoreWidthBlocks = textField(Component.translatable("jlt_worldz.ocean_island.shore_width"), this.shoreWidthBlocksText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.shoreWidthBlocks, Component.translatable("jlt_worldz.ocean_island.shore_width")
        ));

        this.oceanShallowWidthBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.ocean_shallow_width"), this.oceanShallowWidthBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.oceanShallowWidthBlocks, Component.translatable("jlt_worldz.ocean_island.ocean_shallow_width")
        ));

        this.oceanDeepenWidthBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.ocean_deepen_width"), this.oceanDeepenWidthBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.oceanDeepenWidthBlocks, Component.translatable("jlt_worldz.ocean_island.ocean_deepen_width")
        ));

        this.oceanShallowDepthBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.ocean_shallow_depth"), this.oceanShallowDepthBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.oceanShallowDepthBlocks, Component.translatable("jlt_worldz.ocean_island.ocean_shallow_depth")
        ));

        this.oceanDeepDepthBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.ocean_deep_depth"), this.oceanDeepDepthBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.oceanDeepDepthBlocks, Component.translatable("jlt_worldz.ocean_island.ocean_deep_depth")
        ));

        this.oceanRegionScaleBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.ocean_region_scale"), this.oceanRegionScaleBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.oceanRegionScaleBlocks, Component.translatable("jlt_worldz.ocean_island.ocean_region_scale")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.ocean_island.exclusion_zone_enabled"), this.font)
            .selected(this.exclusionZoneEnabled)
            .onValueChange((checkbox, selected) -> this.exclusionZoneEnabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.exclusionZoneRadiusBlocks = textField(
            Component.translatable("jlt_worldz.ocean_island.exclusion_zone_radius"), this.exclusionZoneRadiusBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.exclusionZoneRadiusBlocks, Component.translatable("jlt_worldz.ocean_island.exclusion_zone_radius")
        ));

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
            exteriorButtonLabel(this.netherExterior),
            button -> this.minecraft.gui.setScreen(new WorldzExteriorScreen(this, false, this.netherExterior))
        ).tooltip(exteriorTooltip).build());

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

    private EditBox textField(Component narration, String value) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(256);
        field.setValue(value);
        return field;
    }

    private void apply() {
        try {
            OceanIslandCustomization customization = OceanIslandCustomization.fromText(
                this.islandBiome.getValue(),
                this.radiusBlocks.getValue(),
                this.shapeAmplitude.getValue(),
                this.shoreWidthBlocks.getValue(),
                this.oceanShallowWidthBlocks.getValue(),
                this.oceanDeepenWidthBlocks.getValue(),
                this.oceanShallowDepthBlocks.getValue(),
                this.oceanDeepDepthBlocks.getValue(),
                this.oceanRegionScaleBlocks.getValue(),
                this.exclusionZoneEnabled,
                this.exclusionZoneRadiusBlocks.getValue(),
                this.overworldBorder,
                this.netherBorder,
                this.endBorder,
                this.netherExterior
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> OceanIslandPresetEditor.apply(registries, dimensions, customization)
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
        this.netherExterior = settings;
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

    private static Component exteriorButtonLabel(WorldzCustomization.ExteriorSettings exterior) {
        return Component.translatable(
            "jlt_worldz.customize.nether_exterior",
            Component.translatable("jlt_worldz.customize.exterior.mode." + exterior.mode().serializedName())
        );
    }
}
