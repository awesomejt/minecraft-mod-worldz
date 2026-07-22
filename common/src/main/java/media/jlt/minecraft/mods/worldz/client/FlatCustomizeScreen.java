package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.FlatCustomization;
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

/** Small world-creation screen for the {@code jlt_worldz:flat} typed preset (GOAL 15). */
final class FlatCustomizeScreen extends Screen implements
    LimitEditorHosts.BorderEditorHost, LimitEditorHosts.ExteriorEditorHost, LimitEditorHosts.EndBorderEditorHost {
    private static final Component TITLE = Component.translatable("jlt_worldz.flat.title");
    private static final int FORM_WIDTH = 310;
    private static final int SCROLL_AREA_MIN_HEIGHT = 100;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final CreateWorldScreen parent;
    private final String layersText;
    private final String structureOverridesText;
    private MultiLineEditBox layers;
    private EditBox biome;
    private String biomeText;
    private boolean decoration;
    private MultiLineEditBox structureOverrides;
    private MultiLineTextWidget errorMessage;
    private ScrollableLayout scrollArea;
    private WorldzCustomization.BorderSettings overworldBorder;
    private WorldzCustomization.BorderSettings netherBorder;
    private WorldzCustomization.EndBorderSettings endBorder;
    private WorldzCustomization.ExteriorSettings overworldExterior;
    private WorldzCustomization.ExteriorSettings netherExterior;

    FlatCustomizeScreen(CreateWorldScreen parent, FlatCustomization initial) {
        super(TITLE);
        this.parent = parent;
        this.layersText = initial.layersText();
        this.biomeText = initial.biome();
        this.decoration = initial.decoration();
        this.structureOverridesText = initial.structureOverridesText();
        this.overworldBorder = initial.overworldBorder();
        this.netherBorder = initial.netherBorder();
        this.endBorder = initial.endBorder();
        this.overworldExterior = initial.overworldExterior();
        this.netherExterior = initial.netherExterior();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        LinearLayout form = LinearLayout.vertical().spacing(4);
        form.defaultCellSetting().alignHorizontallyCenter();

        this.layers = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.flat.layers.hint"))
            .build(this.font, FORM_WIDTH, 64, Component.translatable("jlt_worldz.flat.layers"));
        this.layers.setCharacterLimit(4096);
        this.layers.setValue(this.layersText);
        form.addChild(CommonLayouts.labeledElement(this.font, this.layers, Component.translatable("jlt_worldz.flat.layers")));

        this.biome = textField(Component.translatable("jlt_worldz.flat.biome"), this.biomeText);
        this.biome.setResponder(value -> this.biomeText = value);
        form.addChild(CommonLayouts.labeledElement(this.font, this.biome, Component.translatable("jlt_worldz.flat.biome")));

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.flat.decoration"), this.font)
            .selected(this.decoration)
            .onValueChange((checkbox, selected) -> this.decoration = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.structureOverrides = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.flat.structure_overrides.hint"))
            .build(this.font, FORM_WIDTH, 52, Component.translatable("jlt_worldz.flat.structure_overrides"));
        this.structureOverrides.setCharacterLimit(4096);
        this.structureOverrides.setValue(this.structureOverridesText);
        form.addChild(CommonLayouts.labeledElement(
            this.font, this.structureOverrides, Component.translatable("jlt_worldz.flat.structure_overrides")
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
            FlatCustomization customization = FlatCustomization.fromText(
                this.layers.getValue(),
                this.biomeText,
                this.decoration,
                this.structureOverrides.getValue(),
                this.overworldBorder,
                this.netherBorder,
                this.endBorder,
                this.overworldExterior,
                this.netherExterior
            );
            this.parent.getUiState().updateDimensions(
                (registries, dimensions) -> FlatPresetEditor.apply(registries, dimensions, customization)
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
        if (overworld) {
            this.overworldExterior = settings;
        } else {
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
