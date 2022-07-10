package eu.pb4.styledchat.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.StyledChatUtils.MessageActionType;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;


@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Redirect(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
	private void styledChat_onDeathBroadcast(PlayerManager playerManager, Text message, MessageType type, UUID sender) {
		ChatChannel channel = StyledChatUtils.getChatChannel(null, MessageActionType.DEATH, ((ServerPlayerEntity) (Object) this).getCommandSource());

		StyledChatUtils.broadcast(
			playerManager,
			ConfigManager.getConfig().getDeath(
				(ServerPlayerEntity) (Object) this,
				message,
				channel
			),
			null,
			type,
			(ServerPlayerEntity) (Object) this,
			channel
		);
	}

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
    private void styledChat_excludeSendingOfHiddenMessages(Text message, MessageType type, UUID sender, CallbackInfo ci) {
        if (message instanceof TranslatableText text && text.getKey().equals(StyledChatUtils.IGNORED_TEXT_KEY)) {
            ci.cancel();
        }
    }
}
