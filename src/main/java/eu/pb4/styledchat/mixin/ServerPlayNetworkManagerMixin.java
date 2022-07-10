package eu.pb4.styledchat.mixin;


import java.util.UUID;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import eu.pb4.styledchat.StyledChatEvents;
import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.StyledChatUtils.MessageActionType;
import eu.pb4.styledchat.config.Config;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;


@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkManagerMixin {
    @Shadow public ServerPlayerEntity player;

    @Redirect(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    private void styledChat_onDisconnectedBroadcast(PlayerManager playerManager, Text text, MessageType type, UUID sender) {
		Config config = ConfigManager.getConfig();

		ChatChannel channel = StyledChatUtils.getChatChannel(null, MessageActionType.LEAVE, this.player.getCommandSource());

		StyledChatUtils.broadcast(
			playerManager,
			config.getLeft(this.player, channel),
			null,
			MessageType.CHAT,
			this.player,
			channel
		);
    }

    @Redirect(method = "handleMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    private void styledChat_replaceChatMessage(PlayerManager playerManager, Text serverMessage, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType playerMessageType, UUID sender, TextStream.Message message) {
        var handlers = StyledChatUtils.getHandlers(this.player);
        Config config = ConfigManager.getConfig();
        var emotes = StyledChatUtils.getEmotes(this.player);

        String rawMessage =  message.getRaw();
        String filteredMessage = message.getFiltered();

		// Get the chat channel:
		ChatChannel channel = StyledChatUtils.getChatChannel(rawMessage, MessageActionType.CHAT, this.player.getCommandSource());
		boolean chatChannelsEnablement = channel != null && config.configData.chatChannelsEnabled;

        // You might say, that it's useless and you would be kinda right
        // However in case of other mods or vanilla implementing these, it should work without any modifications!
        if (rawMessage.equals(filteredMessage)) {
            rawMessage = StyledChatEvents.PRE_MESSAGE_CONTENT_SEND.invoker().onPreMessage(message.getRaw(), player, false);

			// Remove prefix in sent message:
			if (chatChannelsEnablement && rawMessage.startsWith(channel.usagePrefix)) {
				rawMessage = rawMessage.substring(channel.usagePrefix.length());
			}

            rawMessage = StyledChatUtils.formatMessage(rawMessage, handlers);
            Text rawText = config.getChat(this.player,
                    StyledChatEvents.MESSAGE_CONTENT_SEND.invoker().onMessage(handlers.size() > 0
                                    ? PlaceholderAPI.parsePredefinedText(TextParser.parse(rawMessage, handlers), StyledChatUtils.EMOTE_PATTERN, emotes)
                                    : PlaceholderAPI.parsePredefinedText(new LiteralText(message.getRaw()), StyledChatUtils.EMOTE_PATTERN, emotes),
                            player, false),
				channel);

            if (rawText != null) StyledChatUtils.broadcast(playerManager, rawText, null, playerMessageType, playerManager.getPlayer(sender), channel);
        } else {
            rawMessage = StyledChatEvents.PRE_MESSAGE_CONTENT_SEND.invoker().onPreMessage(message.getRaw(), player, false);
            filteredMessage = StyledChatEvents.PRE_MESSAGE_CONTENT_SEND.invoker().onPreMessage(message.getFiltered(), player, true);

			// Remove prefix in sent message:
			if (chatChannelsEnablement && rawMessage.startsWith(channel.usagePrefix)) {
				rawMessage = rawMessage.substring(channel.usagePrefix.length());
				filteredMessage = filteredMessage.substring(channel.usagePrefix.length());
			}

            rawMessage = StyledChatUtils.formatMessage(rawMessage, handlers);
            filteredMessage = StyledChatUtils.formatMessage(filteredMessage, handlers);

            Text rawText = config.getChat(this.player,
                    StyledChatEvents.MESSAGE_CONTENT_SEND.invoker().onMessage(handlers.size() > 0
                                    ? PlaceholderAPI.parsePredefinedText(TextParser.parse(rawMessage, handlers), StyledChatUtils.EMOTE_PATTERN, emotes)
                                    : PlaceholderAPI.parsePredefinedText(new LiteralText(message.getRaw()), StyledChatUtils.EMOTE_PATTERN, emotes),
                            player, false), channel
            );
            Text filteredText = config.getChat(this.player,
                    StyledChatEvents.MESSAGE_CONTENT_SEND.invoker().onMessage(handlers.size() > 0
                                    ? PlaceholderAPI.parsePredefinedText(TextParser.parse(filteredMessage, handlers), StyledChatUtils.EMOTE_PATTERN, emotes)
                                    : PlaceholderAPI.parsePredefinedText(new LiteralText(message.getFiltered()), StyledChatUtils.EMOTE_PATTERN, emotes),
                            player, true), channel
            );

            if (rawText != null && filteredText != null) StyledChatUtils.broadcast(playerManager, rawText, filteredText, playerMessageType, playerManager.getPlayer(sender), channel);
        }
    }
}
