package eu.pb4.brewery.mixin;

import eu.pb4.brewery.item.BookOfBreweryItem;
import eu.pb4.brewery.item.BrewItems;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "handleCommandExecution", at = @At(value = "HEAD"), cancellable = true)
    private void brewery$onCommand(CommandExecutionC2SPacket packet, LastSeenMessageList lastSeenMessages, CallbackInfo ci) {
        if (packet.command().startsWith("brewery$gui") && (player.getMainHandStack().isOf(BrewItems.BOOK_ITEM) || player.getOffHandStack().isOf(BrewItems.BOOK_ITEM))) {
            var id = Identifier.tryParse(packet.command().substring("brewery$gui ".length()));

            if (id != null) {
                this.server.execute(() -> {
                    this.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1);
                    new BookOfBreweryItem.BrewGui(player, id, true,
                            () -> new BookOfBreweryItem.IndexGui(player, player.getMainHandStack().isOf(BrewItems.BOOK_ITEM) ? Hand.MAIN_HAND : Hand.OFF_HAND).open()
                    ).open();
                });
            }
            ci.cancel();
        }
    }
}
