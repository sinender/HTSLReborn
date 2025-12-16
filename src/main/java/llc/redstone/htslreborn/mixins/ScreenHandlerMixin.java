package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.accessors.HandledScreenAccessor;
import llc.redstone.htslreborn.ui.FileBrowser;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class ScreenHandlerMixin implements HandledScreenAccessor {
    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Inject(method = "render", at = @At("HEAD"))
    public void htslreborn$render(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        FileBrowser.INSTANCE.renderFileBrowser(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method="mouseClicked" , at=@At("HEAD"), cancellable = true)
    public void htslreborn$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (FileBrowser.INSTANCE.click(click, doubled)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void htslreborn$init(CallbackInfo ci) {
        FileBrowser.INSTANCE.onOpen();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void htslreborn$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (FileBrowser.INSTANCE.input(input)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public int getXSize() {
        return this.backgroundWidth;
    }

    @Override
    public int getGuiTop() {
        return this.y;
    }
}
