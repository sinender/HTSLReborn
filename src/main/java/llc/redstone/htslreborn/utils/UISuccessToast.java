package llc.redstone.htslreborn.utils;

import io.wispforest.owo.Owo;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//? } else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? }
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class UISuccessToast implements Toast {

    private final List<FormattedCharSequence> successMessage;
    private final Font textRenderer;
    private final int width;

    public UISuccessToast(String message) {
        this.textRenderer = Minecraft.getInstance().font;
        var texts = this.initText(message);
        this.width = Math.min(240, TextOps.width(textRenderer, texts) + 8);
        this.successMessage = this.wrap(texts);
    }

    public static void report(String message) {
        logErrorsDuringInitialLoad();
        //? if <26.2 {
        Minecraft.getInstance().getToastManager().addToast(new UISuccessToast(message));
        //? } else {
         /*Minecraft.getInstance().gui.toastManager().addToast(new UISuccessToast(message));
        *///? }
    }

    private static void logErrorsDuringInitialLoad() {
        if (UIModelLoader.hasCompletedInitialLoad()) return;

        var throwable = new Throwable();
        Owo.LOGGER.error(
                "An owo-ui error has occurred during the initial resource reload (on thread {}). This is likely a bug caused by *some* other mod initializing an owo-config screen significantly too early - please report it at https://github.com/wisp-forest/owo-lib/issues",
                Thread.currentThread().getName(),
                throwable
        );
    }

    private Visibility visibility = Visibility.HIDE;

    @Override
    public void update(ToastManager manager, long time) {
        this.visibility = time > 10000 ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    //? if <26.1 {
    public void render(GuiGraphics context, Font textRenderer, long startTime) {
        var owoContext = OwoUIGraphics.of(context);

        owoContext.fill(0, 0, this.width, this.height(), 0x77000000);
        owoContext.drawRectOutline(0, 0, this.width(), this.height(), 0xA7FF0000);

        int xOffset = this.width() / 2 - this.textRenderer.width(this.successMessage.getFirst()) / 2;
        owoContext.drawString(this.textRenderer, this.successMessage.getFirst(), 4 + xOffset, 4, 0xFFFFFFFF);

        for (int i = 1; i < this.successMessage.size(); i++) {
            owoContext.drawString(this.textRenderer, this.successMessage.get(i), 4, 4 + i * 11, 0xFFFFFFFF, false);
        }
    }
    //? } else {
    /*public void extractRenderState(GuiGraphicsExtractor context, Font textRenderer, long startTime) {
        var owoContext = OwoUIGraphics.of(context);

        owoContext.fill(0, 0, this.width, this.height(), 0x77000000);
        owoContext.drawRectOutline(0, 0, this.width(), this.height(), 0xA7FF0000);

        int xOffset = this.width() / 2 - this.textRenderer.width(this.successMessage.getFirst()) / 2;
        owoContext.text(this.textRenderer, this.successMessage.getFirst(), 4 + xOffset, 4, 0xFFFFFFFF);

        for (int i = 1; i < this.successMessage.size(); i++) {
            owoContext.text(this.textRenderer, this.successMessage.get(i), 4, 4 + i * 11, 0xFFFFFFFF, false);
        }
    }
    *///? }

    @Override
    public int height() {
        return 6 + this.successMessage.size() * 11;
    }

    @Override
    public int width() {
        return this.width;
    }

    private List<Component> initText(String successMessage) {
        final var texts = new ArrayList<Component>();
        texts.add(Component.literal("HTSLReborn success").withStyle(ChatFormatting.GREEN));
        texts.add(Component.literal(" "));
        texts.add(Component.literal(successMessage.substring(0, Math.min(successMessage.length(), 250))).withStyle(ChatFormatting.GRAY));
        return texts;
    }

    private List<FormattedCharSequence> wrap(List<Component> message) {
        var list = new ArrayList<FormattedCharSequence>();
        for (var text : message) list.addAll(this.textRenderer.split(text, this.width() - 8));
        return list;
    }

    @Override
    public Object getToken() {
        return Type.VERY_TYPE;
    }

    enum Type {
        VERY_TYPE
    }
}