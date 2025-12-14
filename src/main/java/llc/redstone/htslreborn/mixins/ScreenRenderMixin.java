package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.accessors.HandledScreenAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class ScreenRenderMixin implements HandledScreenAccessor {
    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Inject(method = "render", at = @At("HEAD"))
    public void htslreborn$render(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {

    }

    @Override
    public int getGuiTop() {
        return this.y;
    }

    @Override
    public int getXSize() {
        return this.backgroundWidth;
    }
}
