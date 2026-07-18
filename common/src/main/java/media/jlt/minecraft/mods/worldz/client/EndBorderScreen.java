package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.RadiusUnit;
import media.jlt.minecraft.mods.worldz.logic.WorldzCustomization;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Edits whether the Overworld's eventual border radius is carried into the End (GOALS 17). */
final class EndBorderScreen extends Screen {
    private static final int FORM_WIDTH = 310;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final LimitEditorHosts.EndBorderEditorHost parent;
    private final WorldzCustomization.EndBorderSettings initial;
    private boolean carryFromOverworld;
    private RadiusUnit unit = RadiusUnit.BLOCKS;
    private Button unitButton;
    private EditBox minimumRadius;
    private MultiLineTextWidget errorMessage;

    EndBorderScreen(LimitEditorHosts.EndBorderEditorHost parent, WorldzCustomization.EndBorderSettings initial) {
        super(Component.translatable("jlt_worldz.customize.end_border.title"));
        this.parent = parent;
        this.initial = initial;
        this.carryFromOverworld = initial.carryFromOverworld();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout form = this.layout.addToContents(LinearLayout.vertical().spacing(5));
        form.defaultCellSetting().alignHorizontallyCenter();

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.customize.end_border.carry"), this.font)
            .selected(this.carryFromOverworld)
            .onValueChange((checkbox, selected) -> this.carryFromOverworld = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.unitButton = Button.builder(RadiusUnitLabel.of(this.unit), button -> cycleUnit()).width(FORM_WIDTH).build();
        form.addChild(this.unitButton);

        this.minimumRadius = numberField(
            Component.translatable("jlt_worldz.customize.end_border.minimum_radius"),
            this.initial.minimumRadiusBlocks()
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.minimumRadius,
            Component.translatable("jlt_worldz.customize.end_border.minimum_radius")
        ));

        this.errorMessage = new MultiLineTextWidget(CommonComponents.EMPTY, this.font).setMaxWidth(FORM_WIDTH).setMaxRows(2).setCentered(true);
        form.addChild(this.errorMessage);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.apply()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private EditBox numberField(Component narration, int value) {
        EditBox field = new EditBox(this.font, 150, 20, narration);
        field.setMaxLength(10);
        field.setValue(Integer.toString(value));
        return field;
    }

    private void cycleUnit() {
        RadiusUnit next = this.unit.next();
        this.minimumRadius.setValue(this.unit.convert(this.minimumRadius.getValue(), next));
        this.unit = next;
        this.unitButton.setMessage(RadiusUnitLabel.of(this.unit));
    }

    private void apply() {
        try {
            WorldzCustomization.EndBorderSettings settings = WorldzCustomization.EndBorderSettings.fromText(
                this.carryFromOverworld,
                this.unit.toBlocksText(this.minimumRadius.getValue())
            );
            this.parent.setEndBorder(settings);
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
