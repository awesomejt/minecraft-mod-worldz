package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Edits the coordinated world-layout selection. */
final class WorldzLayoutScreen extends Screen {
    private static final int FORM_WIDTH = 310;
    private static final LayoutMode[] MODE_ORDER = {
        LayoutMode.LEGACY, LayoutMode.OCEAN, LayoutMode.SINGLE_BIOME, LayoutMode.VOID
    };

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final WorldzCustomizeScreen parent;
    private final WorldzCustomization.LayoutSettings initial;
    private LayoutMode mode;
    private Button modeButton;
    private MultiLineEditBox biomes;
    private EditBox regionScaleBlocks;
    private EditBox singleBiome;
    private MultiLineEditBox roleOverrides;
    private MultiLineTextWidget errorMessage;

    WorldzLayoutScreen(WorldzCustomizeScreen parent, WorldzCustomization.LayoutSettings initial) {
        super(Component.translatable("jlt_worldz.customize.layout.title"));
        this.parent = parent;
        this.initial = initial;
        this.mode = initial.mode();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout form = this.layout.addToContents(LinearLayout.vertical().spacing(5));
        form.defaultCellSetting().alignHorizontallyCenter();

        this.modeButton = Button.builder(modeLabel(), button -> cycleMode()).width(FORM_WIDTH).build();
        form.addChild(this.modeButton);

        this.biomes = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.customize.layout.biomes.hint"))
            .build(this.font, FORM_WIDTH, 40, Component.translatable("jlt_worldz.customize.layout.biomes"));
        this.biomes.setCharacterLimit(4096);
        this.biomes.setValue(this.initial.biomesText());
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.biomes,
            Component.translatable("jlt_worldz.customize.layout.biomes")
        ));

        this.regionScaleBlocks = numberField(
            Component.translatable("jlt_worldz.customize.layout.region_scale"),
            Integer.toString(this.initial.regionScaleBlocks()),
            150
        );
        this.singleBiome = numberField(
            Component.translatable("jlt_worldz.customize.layout.single_biome"),
            this.initial.singleBiome(),
            150
        );
        LinearLayout otherFields = LinearLayout.horizontal().spacing(10);
        otherFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.regionScaleBlocks,
            Component.translatable("jlt_worldz.customize.layout.region_scale")
        ));
        otherFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.singleBiome,
            Component.translatable("jlt_worldz.customize.layout.single_biome")
        ));
        form.addChild(otherFields);

        this.roleOverrides = MultiLineEditBox.builder()
            .setPlaceholder(Component.translatable("jlt_worldz.customize.layout.role_overrides.hint"))
            .build(this.font, FORM_WIDTH, 32, Component.translatable("jlt_worldz.customize.layout.role_overrides"));
        this.roleOverrides.setCharacterLimit(2048);
        this.roleOverrides.setValue(this.initial.roleOverridesText());
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.roleOverrides,
            Component.translatable("jlt_worldz.customize.layout.role_overrides")
        ));

        this.errorMessage = new MultiLineTextWidget(CommonComponents.EMPTY, this.font)
            .setMaxWidth(FORM_WIDTH)
            .setMaxRows(3)
            .setCentered(true);
        form.addChild(this.errorMessage);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> apply()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private EditBox numberField(Component narration, String value, int width) {
        EditBox field = new EditBox(this.font, width, 20, narration);
        field.setMaxLength(256);
        field.setValue(value);
        return field;
    }

    private void cycleMode() {
        int index = 0;
        for (int i = 0; i < MODE_ORDER.length; i++) {
            if (MODE_ORDER[i] == this.mode) {
                index = i;
                break;
            }
        }
        this.mode = MODE_ORDER[(index + 1) % MODE_ORDER.length];
        this.modeButton.setMessage(modeLabel());
    }

    private Component modeLabel() {
        return Component.translatable(
            "jlt_worldz.customize.layout.mode",
            Component.translatable("jlt_worldz.customize.layout.mode." + this.mode.serializedName())
        );
    }

    private void apply() {
        try {
            WorldzCustomization.LayoutSettings settings = WorldzCustomization.LayoutSettings.fromText(
                this.mode.serializedName(),
                this.biomes.getValue(),
                this.regionScaleBlocks.getValue(),
                this.singleBiome.getValue(),
                this.roleOverrides.getValue()
            );
            this.parent.setLayout(settings);
            this.onClose();
        } catch (IllegalArgumentException exception) {
            this.errorMessage.setMessage(Component.literal(exception.getMessage()).withStyle(ChatFormatting.RED));
            this.repositionElements();
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
