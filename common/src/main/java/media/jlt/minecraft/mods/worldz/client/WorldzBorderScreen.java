package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.RadiusUnit;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;
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

/** Edits one dimension's optional border schedule and progression guarantee. */
final class WorldzBorderScreen extends Screen {
    private static final int FORM_WIDTH = 310;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final LimitEditorHosts.BorderEditorHost parent;
    private final boolean overworld;
    private final WorldzCustomization.BorderSettings initial;
    private boolean enabled;
    private boolean ensureObjective;
    private RadiusUnit unit = RadiusUnit.BLOCKS;
    private Button unitButton;
    private EditBox initialRadius;
    private EditBox finalRadius;
    private EditBox resizeDays;
    private EditBox resizeDelayDays;
    private EditBox resizeRateBlocks;
    private EditBox resizeRateDays;
    private ResizeStyle resizeStyle;
    private Button resizeStyleButton;
    private MultiLineTextWidget errorMessage;

    WorldzBorderScreen(LimitEditorHosts.BorderEditorHost parent, boolean overworld, WorldzCustomization.BorderSettings initial) {
        super(Component.translatable(overworld ? "jlt_worldz.customize.overworld_border.title" : "jlt_worldz.customize.nether_border.title"));
        this.parent = parent;
        this.overworld = overworld;
        this.initial = initial;
        this.enabled = initial.enabled();
        this.ensureObjective = initial.ensureObjective();
        this.resizeStyle = initial.resizeStyle();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout form = this.layout.addToContents(LinearLayout.vertical().spacing(5));
        form.defaultCellSetting().alignHorizontallyCenter();

        form.addChild(Checkbox.builder(Component.translatable("jlt_worldz.customize.border.enabled"), this.font)
            .selected(this.enabled)
            .onValueChange((checkbox, selected) -> this.enabled = selected)
            .build());

        this.unitButton = Button.builder(RadiusUnitLabel.of(this.unit), button -> cycleUnit()).width(FORM_WIDTH).build();
        form.addChild(this.unitButton);

        this.initialRadius = numberField(
            Component.translatable("jlt_worldz.customize.border.initial_radius"),
            this.initial.initialRadiusBlocks(),
            150
        );
        this.finalRadius = numberField(
            Component.translatable("jlt_worldz.customize.border.final_radius"),
            this.initial.finalRadiusBlocks(),
            150
        );
        LinearLayout radiusFields = LinearLayout.horizontal().spacing(10);
        radiusFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.initialRadius,
            Component.translatable("jlt_worldz.customize.border.initial_radius")
        ));
        radiusFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.finalRadius,
            Component.translatable("jlt_worldz.customize.border.final_radius")
        ));
        form.addChild(radiusFields);

        this.resizeDays = numberField(
            Component.translatable("jlt_worldz.customize.border.resize_days"),
            this.initial.resizeDays(),
            150
        );
        this.resizeDelayDays = numberField(
            Component.translatable("jlt_worldz.customize.border.resize_delay_days"),
            this.initial.resizeDelayDays(),
            150
        );
        LinearLayout timingFields = LinearLayout.horizontal().spacing(10);
        timingFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.resizeDays,
            Component.translatable("jlt_worldz.customize.border.resize_days")
        ));
        timingFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.resizeDelayDays,
            Component.translatable("jlt_worldz.customize.border.resize_delay_days")
        ));
        form.addChild(timingFields);

        this.resizeRateBlocks = numberField(
            Component.translatable("jlt_worldz.customize.border.resize_rate_blocks"),
            this.initial.resizeRateBlocks(),
            150
        );
        this.resizeRateDays = numberField(
            Component.translatable("jlt_worldz.customize.border.resize_rate_days"),
            this.initial.resizeRateDays(),
            150
        );
        LinearLayout rateFields = LinearLayout.horizontal().spacing(10);
        rateFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.resizeRateBlocks,
            Component.translatable("jlt_worldz.customize.border.resize_rate_blocks")
        ));
        rateFields.addChild(CommonLayouts.labeledElement(
            this.font,
            this.resizeRateDays,
            Component.translatable("jlt_worldz.customize.border.resize_rate_days")
        ));
        form.addChild(rateFields);

        this.resizeStyleButton = Button.builder(
            ResizeStyleLabel.of(this.resizeStyle),
            button -> cycleResizeStyle()
        ).width(FORM_WIDTH).build();
        form.addChild(this.resizeStyleButton);

        form.addChild(Checkbox.builder(
            Component.translatable(
                this.overworld ? "jlt_worldz.customize.ensure_end_portal" : "jlt_worldz.customize.ensure_blaze_access"
            ),
            this.font
        ).selected(this.ensureObjective)
            .onValueChange((checkbox, selected) -> this.ensureObjective = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.errorMessage = new MultiLineTextWidget(CommonComponents.EMPTY, this.font).setMaxWidth(FORM_WIDTH).setMaxRows(2).setCentered(true);
        form.addChild(this.errorMessage);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.apply()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private EditBox numberField(Component narration, int value, int width) {
        EditBox field = new EditBox(this.font, width, 20, narration);
        field.setMaxLength(10);
        field.setValue(Integer.toString(value));
        return field;
    }

    private void cycleUnit() {
        RadiusUnit next = this.unit.next();
        this.initialRadius.setValue(this.unit.convert(this.initialRadius.getValue(), next));
        this.finalRadius.setValue(this.unit.convert(this.finalRadius.getValue(), next));
        this.resizeRateBlocks.setValue(this.unit.convert(this.resizeRateBlocks.getValue(), next));
        this.unit = next;
        this.unitButton.setMessage(RadiusUnitLabel.of(this.unit));
    }

    private void cycleResizeStyle() {
        this.resizeStyle = this.resizeStyle.next();
        this.resizeStyleButton.setMessage(ResizeStyleLabel.of(this.resizeStyle));
    }

    private void apply() {
        try {
            WorldzCustomization.BorderSettings settings = WorldzCustomization.BorderSettings.fromText(
                this.enabled,
                this.unit.toBlocksText(this.initialRadius.getValue()),
                this.unit.toBlocksText(this.finalRadius.getValue()),
                this.resizeDays.getValue(),
                this.resizeDelayDays.getValue(),
                this.unit.toBlocksText(this.resizeRateBlocks.getValue()),
                this.resizeRateDays.getValue(),
                this.ensureObjective,
                this.resizeStyle.serializedName()
            );
            this.parent.setBorder(this.overworld, settings);
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
