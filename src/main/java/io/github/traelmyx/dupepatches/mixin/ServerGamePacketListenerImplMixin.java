package io.github.traelmyx.dupepatches.mixin;

import io.github.traelmyx.dupepatches.DupePatches;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	/**
	 * Book dupe (1.21.1): a modified client can sign a book with a title of up to 128 characters, but a written book's title
	 * can only be saved up to 32. The player then can't be saved at all and rolls back to their last save on relog,
	 * while anything they put in a chest stays there. Titles that couldn't be saved are refused.
	 */
	@Inject(method = "signBook", at = @At("HEAD"), cancellable = true)
	private void dupepatches$refuseUnsaveableTitle(FilteredText title, List<FilteredText> pages, int slot, CallbackInfo ci) {
		if (title.raw().length() > 32 && DupePatches.BOOK.on(player)) {
			DupePatches.LOGGER.warn("Refused to sign a book for {}: the title is {} characters long, the limit is 32", player.getName().getString(), title.raw().length());
			ci.cancel();
		}
	}
}
