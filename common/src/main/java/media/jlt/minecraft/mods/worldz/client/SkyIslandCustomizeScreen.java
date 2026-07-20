package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandCustomization;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
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

/** Small world-creation screen for the {@code jlt_worldz:sky_island} typed preset (GOALS 05). */
final class SkyIslandCustomizeScreen extends Screen implements
    LimitEditorHosts.BorderEditorHost, LimitEditorHosts.ExteriorEditorHost, LimitEditorHosts.EndBorderEditorHost {
    private static final Component TITLE = Component.translatable("jlt_worldz.sky_island.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private StarterKitTier chestTier;
    private Button chestTierButton;
    private EditBox islandBiome;
    private EditBox radiusBlocks;
    private EditBox shapeAmplitude;
    private EditBox surfaceY;
    private EditBox thicknessBlocks;
    private String islandBiomeText;
    private String radiusBlocksText;
    private String shapeAmplitudeText;
    private String surfaceYText;
    private String thicknessBlocksText;
    private boolean applyToNether;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.EndBorderSettings endBorder;
    private WorldzCustomization.ExteriorSettings netherExterior;
    private boolean floatingIslandsEnabled;
    private EditBox floatingMinRadiusBlocks;
    private EditBox floatingMaxRadiusBlocks;
    private EditBox floatingShapeAmplitude;
    private EditBox floatingCellSizeBlocks;
    private EditBox floatingSpawnChance;
    private boolean floatingBiomeVariety;
    private MultiLineEditBox floatingIslandBiomes;
    private boolean floatingExclusionZoneEnabled;
    private EditBox floatingExclusionZoneRadiusBlocks;
    private String floatingMinRadiusBlocksText;
    private String floatingMaxRadiusBlocksText;
    private String floatingShapeAmplitudeText;
    private String floatingCellSizeBlocksText;
    private String floatingSpawnChanceText;
    private String floatingIslandBiomesText;
    private String floatingExclusionZoneRadiusBlocksText;

    SkyIslandCustomizeScreen(CreateWorldScreen parent, SkyIslandCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.chestTier = initial.chestTier();
        this.islandBiomeText = initial.islandBiome();
        this.radiusBlocksText = Integer.toString(initial.radiusBlocks());
        this.shapeAmplitudeText = Double.toString(initial.shapeAmplitude());
        this.surfaceYText = Integer.toString(initial.surfaceY());
        this.thicknessBlocksText = Integer.toString(initial.thicknessBlocks());
        this.applyToNether = initial.applyToNether();
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.endBorder = initial.endBorder();
        this.netherExterior = initial.netherExterior();
        FloatingIslandsPlan floatingIslands = initial.floatingIslands();
        this.floatingIslandsEnabled = floatingIslands.enabled();
        this.floatingMinRadiusBlocksText = Integer.toString(floatingIslands.minRadiusBlocks());
        this.floatingMaxRadiusBlocksText = Integer.toString(floatingIslands.maxRadiusBlocks());
        this.floatingShapeAmplitudeText = Double.toString(floatingIslands.shapeAmplitude());
        this.floatingCellSizeBlocksText = Integer.toString(floatingIslands.cellSizeBlocks());
        this.floatingSpawnChanceText = Double.toString(floatingIslands.spawnChance());
        this.floatingBiomeVariety = floatingIslands.biomeVariety();
        this.floatingIslandBiomesText = floatingIslands.islandBiomesText();
        this.floatingExclusionZoneEnabled = floatingIslands.exclusionZone().enabled();
        this.floatingExclusionZoneRadiusBlocksText = Integer.toString(floatingIslands.exclusionZone().radiusBlocks());
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

        this.islandBiome = textField(Component.translatable("jlt_worldz.sky_island.island_biome"), this.islandBiomeText);
        this.islandBiome.setResponder(value -> this.islandBiomeText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.islandBiome, Component.translatable("jlt_worldz.sky_island.island_biome")
        ));

        this.radiusBlocks = textField(Component.translatable("jlt_worldz.sky_island.radius"), this.radiusBlocksText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.radiusBlocks, Component.translatable("jlt_worldz.sky_island.radius")
        ));

        this.shapeAmplitude = textField(Component.translatable("jlt_worldz.sky_island.shape_amplitude"), this.shapeAmplitudeText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.shapeAmplitude, Component.translatable("jlt_worldz.sky_island.shape_amplitude")
        ));

        this.surfaceY = textField(Component.translatable("jlt_worldz.sky_island.surface_y"), this.surfaceYText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.surfaceY, Component.translatable("jlt_worldz.sky_island.surface_y")
        ));

        this.thicknessBlocks = textField(Component.translatable("jlt_worldz.sky_island.thickness"), this.thicknessBlocksText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.thicknessBlocks, Component.translatable("jlt_worldz.sky_island.thickness")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.sky_island.apply_to_nether"), this.font)
            .selected(this.applyToNether)
            .onValueChange((checkbox, selected) -> this.applyToNether = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.sky_island.floating_islands_enabled"), this.font)
            .selected(this.floatingIslandsEnabled)
            .onValueChange((checkbox, selected) -> this.floatingIslandsEnabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.floatingMinRadiusBlocks = textField(
            Component.translatable("jlt_worldz.sky_island.floating_min_radius"), this.floatingMinRadiusBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingMinRadiusBlocks, Component.translatable("jlt_worldz.sky_island.floating_min_radius")
        ));

        this.floatingMaxRadiusBlocks = textField(
            Component.translatable("jlt_worldz.sky_island.floating_max_radius"), this.floatingMaxRadiusBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingMaxRadiusBlocks, Component.translatable("jlt_worldz.sky_island.floating_max_radius")
        ));

        this.floatingShapeAmplitude = textField(
            Component.translatable("jlt_worldz.sky_island.floating_shape_amplitude"), this.floatingShapeAmplitudeText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingShapeAmplitude, Component.translatable("jlt_worldz.sky_island.floating_shape_amplitude")
        ));

        this.floatingCellSizeBlocks = textField(
            Component.translatable("jlt_worldz.sky_island.floating_cell_size"), this.floatingCellSizeBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingCellSizeBlocks, Component.translatable("jlt_worldz.sky_island.floating_cell_size")
        ));

        this.floatingSpawnChance = textField(
            Component.translatable("jlt_worldz.sky_island.floating_spawn_chance"), this.floatingSpawnChanceText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingSpawnChance, Component.translatable("jlt_worldz.sky_island.floating_spawn_chance")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.sky_island.floating_biome_variety"), this.font)
            .selected(this.floatingBiomeVariety)
            .onValueChange((checkbox, selected) -> this.floatingBiomeVariety = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.floatingIslandBiomes = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.sky_island.floating_island_biomes.hint"))
            .build(this.font, FORM_WIDTH, 40, Component.translatable("jlt_worldz.sky_island.floating_island_biomes"));
        this.floatingIslandBiomes.setCharacterLimit(4096);
        this.floatingIslandBiomes.setValue(this.floatingIslandBiomesText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingIslandBiomes, Component.translatable("jlt_worldz.sky_island.floating_island_biomes")
        ));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.sky_island.floating_exclusion_zone_enabled"), this.font)
            .selected(this.floatingExclusionZoneEnabled)
            .onValueChange((checkbox, selected) -> this.floatingExclusionZoneEnabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.floatingExclusionZoneRadiusBlocks = textField(
            Component.translatable("jlt_worldz.sky_island.floating_exclusion_zone_radius"), this.floatingExclusionZoneRadiusBlocksText
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.floatingExclusionZoneRadiusBlocks, Component.translatable("jlt_worldz.sky_island.floating_exclusion_zone_radius")
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

    private void cycleChestTier() {
        StarterKitTier[] values = StarterKitTier.values();
        this.chestTier = values[(this.chestTier.ordinal() + 1) % values.length];
        this.chestTierButton.setMessage(chestTierLabel(this.chestTier));
    }

    private static Component chestTierLabel(StarterKitTier tier) {
        return Component.translatable(
            "jlt_worldz.sky_island.chest_tier",
            Component.translatable("jlt_worldz.sky_island.chest_tier." + tier.serializedName())
        );
    }

    private EditBox textField(Component narration, String value) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(256);
        field.setValue(value);
        return field;
    }

    private void apply() {
        try {
            FloatingIslandsPlan floatingIslands = FloatingIslandsPlan.fromText(
                this.floatingIslandsEnabled,
                this.floatingMinRadiusBlocks.getValue(),
                this.floatingMaxRadiusBlocks.getValue(),
                this.floatingShapeAmplitude.getValue(),
                this.floatingCellSizeBlocks.getValue(),
                this.floatingSpawnChance.getValue(),
                this.floatingBiomeVariety,
                this.floatingIslandBiomes.getValue(),
                this.floatingExclusionZoneEnabled,
                this.floatingExclusionZoneRadiusBlocks.getValue()
            );
            SkyIslandCustomization customization = SkyIslandCustomization.fromText(
                this.islandBiome.getValue(),
                this.radiusBlocks.getValue(),
                this.shapeAmplitude.getValue(),
                this.surfaceY.getValue(),
                this.thicknessBlocks.getValue(),
                this.chestTier,
                this.applyToNether,
                this.overworldBorder,
                this.netherBorder,
                this.endBorder,
                this.netherExterior,
                floatingIslands
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> SkyIslandPresetEditor.apply(registries, dimensions, customization)
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
