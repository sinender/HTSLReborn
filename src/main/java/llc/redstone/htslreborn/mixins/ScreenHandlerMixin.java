package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.HTSLReborn;
import llc.redstone.htslreborn.accessors.HandledScreenAccessor;
import llc.redstone.htslreborn.ui.FileExplorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
 
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

@Mixin(AbstractContainerScreen.class)
public class ScreenHandlerMixin extends Screen implements HandledScreenAccessor {
    @Shadow
    protected int topPos;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int imageWidth;

    protected ScreenHandlerMixin(Component title) {
        super(title);
    }

    //? if <26.1 {
    @Inject(method = "render", at = @At("HEAD"))
    public void htslreborn$render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        FileExplorer.getINSTANCE().render(context, mouseX, mouseY, deltaTicks);
    }
    //? } else {
    /*@Inject(method="extractRenderState", at=@At("HEAD"))
    public void htslreborn$render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        FileExplorer.getINSTANCE().extractRenderState(context, mouseX, mouseY, deltaTicks);
    }
    *///? }

    @Inject(method="mouseClicked" , at=@At("HEAD"), cancellable = true)
    public void htslreborn$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        if (FileExplorer.getINSTANCE().mouseClicked(click, doubled)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void htslreborn$init(CallbackInfo ci) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        FileExplorer.setINSTANCE(new FileExplorer());
        FileExplorer.getINSTANCE().init(this.width, this.height);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void htslreborn$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        if (FileExplorer.getINSTANCE().keyPressed(input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        if (FileExplorer.getINSTANCE().mouseDragged(click, offsetX, offsetY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        if (FileExplorer.getINSTANCE().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    public void htslreborn$close(CallbackInfo ci) {
        if (!FileExplorer.inActionGui() || !HTSLReborn.INSTANCE.getCONFIG().getShowFileExplorer()) return;
        FileExplorer.getINSTANCE().onClose();
    }

    @Override
    public int getXSize() {
        return this.imageWidth;
    }

    @Override
    public int getGuiTop() {
        return this.topPos;
    }

    @Override
    public int getGuiLeft() {
        return this.leftPos;
    }
}
