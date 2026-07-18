package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.RadiusUnit;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Edits one dimension's terrain outside its central square. */
final class WorldzExteriorScreen extends Screen {
    private static final int FORM_WIDTH = 310;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final LimitEditorHosts.ExteriorEditorHost parent;
    private final boolean overworld;
    private final WorldzCustomization.ExteriorSettings initial;
    private ExteriorMode mode;
    private Button modeButton;
    private RadiusUnit unit = RadiusUnit.BLOCKS;
    private Button unitButton;
    private EditBox boundaryRadius;
    private EditBox oceanTransitionWidth;
    private MultiLineTextWidget errorMessage;

    WorldzExteriorScreen(
        LimitEditorHosts.ExteriorEditorHost parent,
        boolean overworld,
        WorldzCustomization.ExteriorSettings initial
    ) {
        super(Component.translatable(
            overworld ? "jlt_worldz.customize.overworld_exterior.title" : "jlt_worldz.customize.nether_exterior.title"
        ));
        this.parent = parent;
        this.overworld = overworld;
        this.initial = initial;
        this.mode = initial.mode();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout form = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        form.defaultCellSetting().alignHorizontallyCenter();

        this.modeButton = Button.builder(modeLabel(), button -> cycleMode()).width(FORM_WIDTH).build();
        form.addChild(this.modeButton);

        this.unitButton = Button.builder(RadiusUnitLabel.of(this.unit), button -> cycleUnit()).width(FORM_WIDTH).build();
        form.addChild(this.unitButton);

        this.boundaryRadius = textField(
            Component.translatable("jlt_worldz.customize.exterior.boundary_radius"),
            this.initial.boundaryRadiusBlocks() == 0 ? RadiusUnit.AUTO : Integer.toString(this.initial.boundaryRadiusBlocks())
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.boundaryRadius,
            Component.translatable("jlt_worldz.customize.exterior.boundary_radius")
        ));

        this.oceanTransitionWidth = textField(
            Component.translatable("jlt_worldz.customize.exterior.ocean_transition"),
            Integer.toString(this.initial.oceanTransitionWidthBlocks())
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.oceanTransitionWidth,
            Component.translatable("jlt_worldz.customize.exterior.ocean_transition")
        ));
        updateOceanField();

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

    private EditBox textField(Component narration, String value) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(10);
        field.setValue(value);
        return field;
    }

    private void cycleMode() {
        this.mode = switch (this.mode) {
            case NORMAL -> this.overworld ? ExteriorMode.OCEAN : ExteriorMode.VOID;
            case OCEAN -> ExteriorMode.VOID;
            case VOID -> ExteriorMode.NORMAL;
        };
        this.modeButton.setMessage(modeLabel());
        updateOceanField();
    }

    private void cycleUnit() {
        RadiusUnit next = this.unit.next();
        this.boundaryRadius.setValue(this.unit.convert(this.boundaryRadius.getValue(), next));
        this.unit = next;
        this.unitButton.setMessage(RadiusUnitLabel.of(this.unit));
    }

    private void updateOceanField() {
        this.oceanTransitionWidth.active = this.mode == ExteriorMode.OCEAN;
    }

    private Component modeLabel() {
        return Component.translatable(
            "jlt_worldz.customize.exterior.mode",
            Component.translatable("jlt_worldz.customize.exterior.mode." + this.mode.serializedName())
        );
    }

    private void apply() {
        try {
            WorldzCustomization.ExteriorSettings settings = WorldzCustomization.ExteriorSettings.fromText(
                this.mode.serializedName(),
                this.unit.toBlocksText(this.boundaryRadius.getValue()),
                this.oceanTransitionWidth.getValue()
            );
            this.parent.setExterior(this.overworld, settings);
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
        this.minecraft.gui.setScreen(this.parent.asScreen());
    }
}
