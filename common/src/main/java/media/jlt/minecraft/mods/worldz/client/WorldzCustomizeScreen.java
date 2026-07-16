package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
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

/** World-creation screen for biome, starter-zone, border, and exterior choices. */
final class WorldzCustomizeScreen extends Screen {
    private static final Component TITLE = Component.translatable("jlt_worldz.customize.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.ExteriorSettings overworldExterior;
    private WorldzCustomization.ExteriorSettings netherExterior;
    private StarterLandPlan starterLandPlan;
    private WorldzCustomization.LayoutSettings worldLayout;
    private SpawnStrategy spawnStrategy;
    private Button spawnStrategyButton;
    private MultiLineEditBox allowedBiomes;
    private EditBox starterBiome;
    private EditBox starterRadius;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private String allowedBiomesText;
    private String starterBiomeText;
    private String starterRadiusText;

    WorldzCustomizeScreen(CreateWorldScreen parent, WorldzCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.overworldExterior = initial.overworldExterior();
        this.netherExterior = initial.netherExterior();
        this.starterLandPlan = initial.starterLandPlan();
        this.worldLayout = initial.worldLayout();
        this.spawnStrategy = initial.spawnStrategy();
        this.allowedBiomesText = initial.allowedBiomesText();
        this.starterBiomeText = initial.starterBiome();
        this.starterRadiusText = Integer.toString(initial.starterRadiusBlocks());
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.allowedBiomes = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.customize.allowed_biomes.hint"))
            .build(this.font, FORM_WIDTH, 52, Component.translatable("jlt_worldz.customize.allowed_biomes"));
        this.allowedBiomes.setCharacterLimit(4096);
        this.allowedBiomes.setValue(this.allowedBiomesText);
        this.allowedBiomes.setValueListener(value -> this.allowedBiomesText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.allowedBiomes,
            Component.translatable("jlt_worldz.customize.allowed_biomes")
        ));

        this.starterBiome = textField(Component.translatable("jlt_worldz.customize.starter_biome"), this.starterBiomeText, 256);
        this.starterBiome.setResponder(value -> this.starterBiomeText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.starterBiome,
            Component.translatable("jlt_worldz.customize.starter_biome")
        ));

        this.starterRadius = textField(Component.translatable("jlt_worldz.customize.starter_radius"), this.starterRadiusText, 10);
        this.starterRadius.setResponder(value -> this.starterRadiusText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.starterRadius,
            Component.translatable("jlt_worldz.customize.starter_radius")
        ));

        form.addChild(Button.builder(
            starterLandButtonLabel(this.starterLandPlan.enabled()),
            button -> this.minecraft.gui.setScreen(new WorldzStarterLandScreen(this, this.starterLandPlan))
        ).width(FORM_WIDTH).build());

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

        Tooltip exteriorTooltip = Tooltip.create(Component.translatable("jlt_worldz.customize.exterior.tooltip"));
        LinearLayout exteriorButtons = LinearLayout.horizontal().spacing(10);
        exteriorButtons.addChild(Button.builder(
            exteriorButtonLabel("overworld", this.overworldExterior),
            button -> this.minecraft.gui.setScreen(new WorldzExteriorScreen(this, true, this.overworldExterior))
        ).tooltip(exteriorTooltip).build());
        exteriorButtons.addChild(Button.builder(
            exteriorButtonLabel("nether", this.netherExterior),
            button -> this.minecraft.gui.setScreen(new WorldzExteriorScreen(this, false, this.netherExterior))
        ).tooltip(exteriorTooltip).build());
        form.addChild(exteriorButtons);

        form.addChild(Button.builder(
            layoutButtonLabel(this.worldLayout),
            button -> this.minecraft.gui.setScreen(new WorldzLayoutScreen(this, this.worldLayout))
        ).width(FORM_WIDTH).build());

        this.spawnStrategyButton = Button.builder(spawnStrategyLabel(this.spawnStrategy), button -> cycleSpawnStrategy())
            .width(FORM_WIDTH)
            .build();
        form.addChild(this.spawnStrategyButton);

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

    private void cycleSpawnStrategy() {
        SpawnStrategy[] values = SpawnStrategy.values();
        this.spawnStrategy = values[(this.spawnStrategy.ordinal() + 1) % values.length];
        this.spawnStrategyButton.setMessage(spawnStrategyLabel(this.spawnStrategy));
    }

    private EditBox textField(Component narration, String value, int maximumLength) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(maximumLength);
        field.setValue(value);
        return field;
    }

    private void apply() {
        try {
            WorldzCustomization customization = WorldzCustomization.fromText(
                this.allowedBiomes.getValue(),
                this.starterBiome.getValue(),
                this.starterRadius.getValue(),
                this.starterLandPlan,
                this.overworldBorder,
                this.netherBorder,
                this.overworldExterior,
                this.netherExterior,
                this.worldLayout,
                this.spawnStrategy
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> WorldzPresetEditor.apply(registries, dimensions, customization)
            );
            this.onClose();
        } catch (IllegalArgumentException exception) {
            this.errorMessage.setMessage(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
            this.repositionElements();
        }
    }

    void setBorder(boolean overworld, WorldzCustomization.BorderSettings settings) {
        if (overworld) {
            this.overworldBorder = settings;
        } else {
            this.netherBorder = settings;
        }
    }

    void setStarterLand(StarterLandPlan plan) {
        this.starterLandPlan = plan;
    }

    void setExterior(boolean overworld, WorldzCustomization.ExteriorSettings settings) {
        if (overworld) {
            this.overworldExterior = settings;
        } else {
            this.netherExterior = settings;
        }
    }

    void setLayout(WorldzCustomization.LayoutSettings settings) {
        this.worldLayout = settings;
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

    private static Component borderButtonLabel(String dimension, boolean enabled) {
        return Component.translatable(
            "jlt_worldz.customize." + dimension + "_border",
            Component.translatable(enabled ? "options.on" : "options.off")
        );
    }

    private static Component starterLandButtonLabel(boolean enabled) {
        return Component.translatable(
            "jlt_worldz.customize.starter_land",
            Component.translatable(enabled ? "options.on" : "options.off")
        );
    }

    private static Component exteriorButtonLabel(
        String dimension,
        WorldzCustomization.ExteriorSettings exterior
    ) {
        return Component.translatable(
            "jlt_worldz.customize." + dimension + "_exterior",
            Component.translatable("jlt_worldz.customize.exterior.mode." + exterior.mode().serializedName())
        );
    }

    private static Component layoutButtonLabel(WorldzCustomization.LayoutSettings worldLayout) {
        return Component.translatable(
            "jlt_worldz.customize.layout",
            Component.translatable("jlt_worldz.customize.layout.mode." + worldLayout.mode().serializedName())
        );
    }

    private static Component spawnStrategyLabel(SpawnStrategy strategy) {
        return Component.translatable(
            "jlt_worldz.customize.spawn_strategy",
            Component.translatable("jlt_worldz.customize.spawn_strategy." + strategy.serializedName())
        );
    }
}
