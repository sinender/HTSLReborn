package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.ui.FileExplorer;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "charTyped", at = @At("HEAD"))
    public void htslreborn$charTyped(long window, CharacterEvent input, CallbackInfo ci) {
        if (!FileExplorer.inActionGui()) return;
        FileExplorer.Companion.getINSTANCE().charTyped(input);
    }
}
