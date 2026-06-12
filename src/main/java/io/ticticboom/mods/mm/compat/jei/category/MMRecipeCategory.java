package io.ticticboom.mods.mm.compat.jei.category;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WidgetUtils;
import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MMRecipeCategory implements IRecipeCategory<RecipeModel> {

    public static final RecipeType<RecipeModel> RECIPE_TYPE = RecipeType.create(Ref.ID, "recipes", RecipeModel.class);
    private final IJeiHelpers helpers;
    private final IDrawable bgProgressBar;
    @Getter
    private final StructureModel structureModel;
    private final IDrawable fgProgressBar;
    private final RecipeType<RecipeModel> recipeType;
    private final int height;

    @Override
    public ResourceLocation getRegistryName(RecipeModel recipe) {
        return recipe.id();
    }

    public MMRecipeCategory(IJeiHelpers helpers, StructureModel parent, int height) {
        this.helpers = helpers;
        bgProgressBar = helpers.getGuiHelper().createDrawable(Ref.UiTextures.SLOT_PARTS, 26, 0, 24, 17);
        this.structureModel = parent;
        var staticProgressBar = helpers.getGuiHelper().createDrawable(Ref.UiTextures.SLOT_PARTS, 26, 17, 24, 17);
        fgProgressBar = helpers.getGuiHelper().createAnimatedDrawable(staticProgressBar, 16, IDrawableAnimated.StartDirection.LEFT, false);
        if (structureModel != null) {
            recipeType = RecipeType.create("mm", parent.id().getPath() + "_recipe", RecipeModel.class);
        } else {
            recipeType = RECIPE_TYPE;
        }
        this.height = height;
    }

    @Override
    public @NotNull RecipeType<RecipeModel> getRecipeType() {
        return recipeType;
    }

    @Override
    public @NotNull Component getTitle() {
        if (structureModel != null) {
            return Component.literal(this.structureModel.name()).append(Component.literal(" (Recipes)"));
        } else {
            return Component.literal("MM Recipes");
        }
    }

    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return helpers.getGuiHelper().createBlankDrawable(162, height);
    }

    @Override
    public IDrawable getIcon() {
        return helpers.getGuiHelper().createDrawableItemStack(MMRegisters.BLUEPRINT.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, RecipeModel recipe, @NotNull IFocusGroup focuses) {
        int inputRows = (int) Math.ceil(recipe.inputs().inputs().size() / 3.0);
        int outputRows = (int) Math.ceil(recipe.outputs().outputs().size() / 3.0);
        var inGrid = new SlotGrid(20, 20, 3, inputRows, 0, 0);
        var outGrid = new SlotGrid(20, 20, 3, outputRows, 100, 0);
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            input.setRecipe(builder, recipe, focuses, helpers, inGrid);
        }
        for (IRecipeOutputEntry output : recipe.outputs().outputs()) {
            output.setRecipe(builder, recipe, focuses, helpers, outGrid);
        }

        recipe.inputSlots().addAll(inGrid.getSlots());
        recipe.inputSlots().addAll(outGrid.getSlots());
    }

    @Override
    public void draw(RecipeModel recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics gfx, double mouseX, double mouseY) {
        bgProgressBar.draw(gfx, 70, 12);
        fgProgressBar.draw(gfx, 70, 12);
        var seconds = (double) recipe.ticks() / 20;
        var fmt = String.format("%.2f", seconds) + "s";

        if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 70, 12, 24, 17)) {
            gfx.renderTooltip(Minecraft.getInstance().font, Component.literal(fmt), (int) mouseX, (int) mouseY);
        }

        if (structureModel == null) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, 75, 28, 19, 26, 7, 9);
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 75, 28, 7, 9)) {
                gfx.renderTooltip(Minecraft.getInstance().font, Component.literal("Structure: " + recipe.structureId().toString()), (int) mouseX, (int) mouseY);
            }
        }

        for (SlotGridEntry inputSlot : recipe.inputSlots()) {
            if (inputSlot.used()) {
                gfx.blit(Ref.UiTextures.SLOT_PARTS, inputSlot.x, inputSlot.y, 0, 26, 18, 18);
                // draw small 'x' badge bottom-right if slot is marked not used
                if (inputSlot.hasBadgeNotUsed()) {
                    String badge = "x";
                    int badgeX = inputSlot.x + 12;
                    int badgeY = inputSlot.y + 12;
                    gfx.drawString(Minecraft.getInstance().font, badge, badgeX, badgeY, 0xFF5555, true);
                } else if (inputSlot.hasBadgeCount()) {
                    int count = inputSlot.getBadgeCount();
                    var font = Minecraft.getInstance().font;
                    boolean hover = WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, inputSlot.x, inputSlot.y, 18, 18);
                    // show exact for hovered slot, or for all slots if Shift is held
                    String display;
                    if (Screen.hasShiftDown() || hover) {
                        display = Integer.toString(count);
                    } else {
                        display = abbreviateNumber(count);
                    }

                    int textWidth = Math.max(1, font.width(display));
                    // draw numbers centered at the bottom-middle of the item
                    float baseScale = 0.8f; // small text
                    int maxWidth = 20; // max pixel width before scaling down
                    float scale = baseScale;
                    if (textWidth * scale > maxWidth) {
                        scale = Math.max(0.4f, (float) maxWidth / (float) textWidth);
                    }

                    int centerX = inputSlot.x + 9; // slot center (slot width ~18)
                    int drawY = inputSlot.y + 15; // near bottom of slot

                    // draw a semi-transparent dark background behind the text for readability
                    int scaledW = (int) Math.ceil(textWidth * scale);
                    int scaledH = (int) Math.ceil((float) Minecraft.getInstance().font.lineHeight * scale);
                    int padX = Math.max(1, (int) Math.ceil(2f * scale));
                    int padY = Math.max(1, (int) Math.ceil(scale));
                    int left = (int) Math.floor(centerX - (scaledW / 2.0f)) - padX;
                    int top = drawY - padY;
                    int right = (int) Math.ceil(centerX + (scaledW / 2.0f)) + padX;
                    int bottom = drawY + scaledH + padY;
                    // draw background slightly behind the text but above items
                    gfx.pose().pushPose();
                    gfx.pose().translate(0f, 0f, 99f);
                    gfx.fill(left, top, right, bottom, 0x55000000);
                    gfx.pose().popPose();

                    // draw text above the background
                    gfx.pose().pushPose();
                    gfx.pose().translate((float) centerX, (float) drawY, 100f);
                    gfx.pose().scale(scale, scale, 1f);
                    // drawString draws from top-left, so draw at -textWidth/2 to center-align
                    gfx.drawString(font, display, -textWidth / 2, 0, 0xFFFFFFFF, true);
                    gfx.pose().popPose();
                }
            }
        }

    }

    private static String abbreviateNumber(int n) {
        // only abbreviate numbers greater than 9999 (i.e. 10000+)
        if (Math.abs(n) <= 9999) return Integer.toString(n);
        final long abs = Math.abs((long) n);
        final String[] suffix = new String[] {"", "K", "M", "G", "T"};
        int idx = 0;
        double value = abs;
        while (value >= 1000 && idx < suffix.length - 1) {
            value /= 1000.0;
            idx++;
        }
        String fmt;
        if (value < 10 && Math.floor(value) != value) {
            fmt = String.format("%.1f%s", value, suffix[idx]);
        } else {
            fmt = String.format("%.0f%s", Math.floor(value), suffix[idx]);
        }
        return fmt;
    }
}