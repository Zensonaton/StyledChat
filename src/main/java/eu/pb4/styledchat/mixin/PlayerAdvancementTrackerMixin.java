package eu.pb4.styledchat.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.StyledChatUtils.MessageActionType;
import eu.pb4.styledchat.config.Config;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.command.CommandSource;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayerEntity owner;

	@Redirect(method = "grantCriterion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
	private void styledChat_redirectBroadcast(PlayerManager playerManager, Text message, MessageType type, UUID sender) {
		TranslatableText translatableText = (TranslatableText) message;
		Text advancement = (Text) translatableText.getArgs()[1];
		CommandSource source = owner.getCommandSource();
		Config config = ConfigManager.getConfig();

		ChatChannel channel = null;
		Text text = null;

		switch (translatableText.getKey()) {
			case "chat.type.advancement.goal" -> {
				channel = StyledChatUtils.getChatChannel(null, MessageActionType.ADVANCEMENT_GOAL, source);
				text = config.getAdvancementGoal(this.owner, advancement, channel);
			}
			case "chat.type.advancement.challenge" -> {
				channel = StyledChatUtils.getChatChannel(null, MessageActionType.ADVANCEMENT_CHALLENGE, source);
				text = config.getAdvancementChallenge(this.owner, advancement, channel);
			}
			default -> {
				channel = StyledChatUtils.getChatChannel(null, MessageActionType.ADVANCEMENT_TASK, source);
				text = config.getAdvancementTask(this.owner, advancement, channel);
			}
		}

		StyledChatUtils.broadcast(
			playerManager,
			text,
			null,
			type,
			this.owner,
			channel
		);
	}
}
