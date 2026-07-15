package media.jlt.minecraft.mods.worldz.client;

import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;
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

/** Edits the optional natural terrain guarantee beneath the starter biome. */
final class WorldzStarterLandScreen extends Screen {
    private static final int FORM_WIDTH = 310;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 40);
    private final WorldzCustomizeScreen parent;
    private final StarterLandPlan initial;
    private boolean enabled;
    private EditBox transitionWidth;
    private EditBox foundationDepth;
    private MultiLineTextWidget errorMessage;

    WorldzStarterLandScreen(WorldzCustomizeScreen parent, StarterLandPlan initial) {
        super(Component.translatable("jlt_worldz.customize.starter_land.title"));
        this.parent = parent;
        this.initial = initial;
        this.enabled = initial.enabled();
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        LinearLayout form = this.layout.addToContents(LinearLayout.vertical().spacing(8));
        form.defaultCellSetting().alignHorizontallyCenter();

        form.addChild(Checkbox.builder(
            Component.translatable("jlt_worldz.customize.starter_land.enabled"), this.font
        ).selected(this.enabled)
            .onValueChange((checkbox, selected) -> this.enabled = selected)
            .maxWidth(FORM_WIDTH)
            .build());

        this.transitionWidth = numberField(
            Component.translatable("jlt_worldz.customize.starter_land.transition"),
            this.initial.transitionWidthBlocks()
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.transitionWidth,
            Component.translatable("jlt_worldz.customize.starter_land.transition")
        ));

        this.foundationDepth = numberField(
            Component.translatable("jlt_worldz.customize.starter_land.foundation"),
            this.initial.foundationDepthBlocks()
        );
        form.addChild(CommonLayouts.labeledElement(
            this.font,
            this.foundationDepth,
            Component.translatable("jlt_worldz.customize.starter_land.foundation")
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

    private EditBox numberField(Component narration, int value) {
        EditBox field = new EditBox(this.font, FORM_WIDTH, 20, narration);
        field.setMaxLength(10);
        field.setValue(Integer.toString(value));
        return field;
    }

    private void apply() {
        try {
            StarterLandPlan plan = StarterLandPlan.fromText(
                this.enabled,
                this.transitionWidth.getValue(),
                this.foundationDepth.getValue()
            );
            this.parent.setStarterLand(plan);
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
