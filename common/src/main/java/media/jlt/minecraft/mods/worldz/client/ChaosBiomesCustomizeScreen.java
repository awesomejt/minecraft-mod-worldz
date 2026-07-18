package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.ChaosBiomesCustomization;
import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Small world-creation screen for the {@code jlt_worldz:chaos_biomes} typed preset (DESIGN §20.11). */
final class ChaosBiomesCustomizeScreen extends Screen {
    private static final Component TITLE = Component.translatable("jlt_worldz.chaos_biomes.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private SpawnStrategy spawnStrategy;
    private Button spawnStrategyButton;
    private MultiLineEditBox biomes;
    private EditBox regionScaleBlocks;
    private EditBox starterBiome;
    private EditBox starterRadius;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private String biomesText;
    private String regionScaleBlocksText;
    private String starterBiomeText;
    private String starterRadiusText;
    private boolean allowRivers;
    private boolean allowOceans;

    ChaosBiomesCustomizeScreen(CreateWorldScreen parent, ChaosBiomesCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.spawnStrategy = initial.spawnStrategy();
        this.biomesText = initial.biomesText();
        this.regionScaleBlocksText = Integer.toString(initial.regionScaleBlocks());
        this.starterBiomeText = initial.starterBiome();
        this.starterRadiusText = Integer.toString(initial.starterRadiusBlocks());
        this.allowRivers = initial.allowRivers();
        this.allowOceans = initial.allowOceans();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.biomes = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.chaos_biomes.biomes.hint"))
            .build(this.font, FORM_WIDTH, 40, Component.translatable("jlt_worldz.chaos_biomes.biomes"));
        this.biomes.setCharacterLimit(4096);
        this.biomes.setValue(this.biomesText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.biomes, Component.translatable("jlt_worldz.chaos_biomes.biomes")
        ));

        this.regionScaleBlocks = textField(
            Component.translatable("jlt_worldz.chaos_biomes.region_scale"),
            this.regionScaleBlocksText,
            10
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.regionScaleBlocks, Component.translatable("jlt_worldz.chaos_biomes.region_scale")
        ));

        this.starterBiome = textField(Component.translatable("jlt_worldz.chaos_biomes.starter_biome"), this.starterBiomeText, 256);
        this.starterBiome.setResponder(value -> this.starterBiomeText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.starterBiome, Component.translatable("jlt_worldz.chaos_biomes.starter_biome")
        ));

        this.starterRadius = textField(Component.translatable("jlt_worldz.chaos_biomes.starter_radius"), this.starterRadiusText, 10);
        this.starterRadius.setResponder(value -> this.starterRadiusText = value);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.starterRadius, Component.translatable("jlt_worldz.chaos_biomes.starter_radius")
        ));

        this.spawnStrategyButton = Button.builder(spawnStrategyLabel(this.spawnStrategy), button -> cycleSpawnStrategy())
            .width(FORM_WIDTH)
            .build();
        form.addChild(this.spawnStrategyButton);

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.chaos_biomes.allow_rivers"), this.font)
            .selected(this.allowRivers)
            .onValueChange((checkbox, selected) -> this.allowRivers = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.chaos_biomes.allow_oceans"), this.font)
            .selected(this.allowOceans)
            .onValueChange((checkbox, selected) -> this.allowOceans = selected)
            .maxWidth(FORM_WIDTH)
            .build());

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
            ChaosBiomesCustomization customization = ChaosBiomesCustomization.fromText(
                this.biomes.getValue(),
                this.regionScaleBlocks.getValue(),
                this.starterBiome.getValue(),
                this.starterRadius.getValue(),
                this.spawnStrategy,
                this.allowRivers,
                this.allowOceans
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> ChaosBiomesPresetEditor.apply(registries, dimensions, customization)
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

    private static Component spawnStrategyLabel(SpawnStrategy strategy) {
        return Component.translatable(
            "jlt_worldz.customize.spawn_strategy",
            Component.translatable("jlt_worldz.customize.spawn_strategy." + strategy.serializedName())
        );
    }
}
