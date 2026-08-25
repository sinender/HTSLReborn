package llc.redstone.htslreborn.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, priority = 1001)
public class ScreenCloseMixin {
    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    public void htslreborn$onCloseScreen(ClientboundContainerClosePacket packet, CallbackInfo ci) {
//        if (HTSLReborn.INSTANCE.getExporting() || HTSLReborn.INSTANCE.getImporting()) {
//            ci.cancel();
//        }
    }
}
