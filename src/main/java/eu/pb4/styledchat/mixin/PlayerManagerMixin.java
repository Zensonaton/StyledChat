package eu.pb4.styledchat.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.StyledChatUtils.MessageActionType;
import eu.pb4.styledchat.config.Config;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import net.minecraft.command.CommandSource;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;


@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Unique private ServerPlayerEntity temporaryPlayer = null;
	private Text connectText = null;

    @Inject(method = "onPlayerConnect", at = @At(value = "HEAD"))
    private void styledChat_storePlayer(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        this.temporaryPlayer = player;
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void styledChat_removeStoredPlayer(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
		Config config = ConfigManager.getConfig();
		ChatChannel channel = null;
		Text message = null;
		CommandSource source = this.temporaryPlayer.getCommandSource();

		if (this.temporaryPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME)) == 0) {
			channel = StyledChatUtils.getChatChannel(null, MessageActionType.JOIN_FIRST_TIME, source);
			message = config.getJoinFirstTime(this.temporaryPlayer, channel);
		}

		Object[] args = ((TranslatableText) this.connectText).getArgs();
        if (args.length == 1) {
			channel = StyledChatUtils.getChatChannel(null, MessageActionType.JOIN, source);
            message = config.getJoin(this.temporaryPlayer, channel);
        } else {
			channel = StyledChatUtils.getChatChannel(null, MessageActionType.JOIN_RENAMED, source);
            message = config.getJoinRenamed(this.temporaryPlayer, (String) args[1], channel);
        }

		StyledChatUtils.broadcast(
			player.getServer().getPlayerManager(),
			message,
			null,
			MessageType.CHAT,
			this.temporaryPlayer,
			channel
		);

		this.temporaryPlayer = null;
    }

	@Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
	private void styledChat_onPlayerConnect(PlayerManager playerManager, Text text, MessageType type, UUID sender) {
		this.connectText = text;
    }

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void styledChat_excludeSendingOfHiddenMessages(Text message, MessageType type, UUID sender, CallbackInfo ci) {
        if (message instanceof TranslatableText text && text.getKey().equals(StyledChatUtils.IGNORED_TEXT_KEY)) {
            ci.cancel();
        }
    }
}
