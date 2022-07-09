package eu.pb4.styledchat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import eu.pb4.placeholders.util.GeneralUtils;
import eu.pb4.placeholders.util.TextParserUtils;
import eu.pb4.styledchat.config.Config;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public final class StyledChatUtils {
    public static final String IGNORED_TEXT_KEY = "styled.chat.ignored.text.if.you.see.it.some.mod.is.bad";
    public static final TranslatableText IGNORED_TEXT = new TranslatableText(IGNORED_TEXT_KEY);

    public static final String URL_REGEX = "(https?:\\/\\/[-a-zA-Z0-9@:%._\\+~#=]+\\.[^ ]+)";

    public static final String ITEM_TAG = "item";
    public static final String POS_TAG = "pos";
    public static final String SPOILER_TAG = "spoiler";
    public static final String LINK_TAG = "sc-link";

    public static final TextParser.TextFormatterHandler SPOILER_TAG_HANDLER = (tag, data, input, handlers, endAt) -> {
        var out = TextParserUtils.recursiveParsing(input, handlers, endAt);
        var config = ConfigManager.getConfig();

        return new GeneralUtils.TextLengthPair(
                ((MutableText) PlaceholderAPI.parsePredefinedText(config.spoilerStyle,
                        PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                        Map.of("spoiler", new LiteralText(config.configData.spoilerSymbol.repeat(out.text().getString().length())))
                )).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, out.text()))),
                out.length()
        );
    };


    public static final String FORMAT_PERMISSION_BASE = "styledchat.format.";
    public static final Pattern EMOTE_PATTERN = Pattern.compile("[:](?<id>[^:]+)[:]");

    public static Text parseText(String input) {
        return !input.isEmpty() ? TextParser.parse(input) : IGNORED_TEXT;
    }

    public static Map<String, TextParser.TextFormatterHandler> getHandlers(ServerPlayerEntity player) {
        HashMap<String, TextParser.TextFormatterHandler> handlers = new HashMap<>();
        ServerCommandSource source = player.getCommandSource();
        Config config = ConfigManager.getConfig();

        for (Map.Entry<String, TextParser.TextFormatterHandler> entry : TextParser.getRegisteredSafeTags().entrySet()) {
            if (config.defaultFormattingCodes.getBoolean(entry.getKey())
                    || Permissions.check(source, FORMAT_PERMISSION_BASE + entry.getKey(), 2)) {
                handlers.put(entry.getKey(), entry.getValue());
            }
        }

        if (config.defaultFormattingCodes.getBoolean(SPOILER_TAG)
                || Permissions.check(source, FORMAT_PERMISSION_BASE + SPOILER_TAG, 2)) {
            handlers.put(SPOILER_TAG, SPOILER_TAG_HANDLER);
        }

        if (handlers.containsKey("light_purple")) {
            handlers.put("pink", handlers.get("light_purple"));
        }

        if (config.defaultFormattingCodes.getBoolean(POS_TAG) ||
                Permissions.check(source, FORMAT_PERMISSION_BASE + POS_TAG, 2)) {
            handlers.put(POS_TAG, (tag, data, input, buildInHandlers, endAt) ->
                    new GeneralUtils.TextLengthPair(new LiteralText(String.format("%.2f %.2f %.2f", player.getX(), player.getY(), player.getZ())), 0));
        }

        if (config.configData.parseLinksInChat
                || Permissions.check(source, "styledchat.links", 2)) {
            handlers.put(LINK_TAG, (tag, data, input, buildInHandlers, endAt) -> {
                String url = TextParserUtils.cleanArgument(data);
                return new GeneralUtils.TextLengthPair(
                        (MutableText) PlaceholderAPI.parsePredefinedText(
                                config.linkStyle,
                                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                                Map.of("link", new LiteralText(url).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))))
                        ), 0);
            });
        }

        StyledChatEvents.FORMATTING_CREATION_EVENT.invoker().onFormattingBuild(player, handlers::put);

        return handlers;
    }

    /**
     * I need to rework things within Placeholder API, but I'm too lazy to do it rn.
     * Whatever I will do it this thing will be replaced with proper thing (I might keep it for sake of backwards compatibility)
     */
    public static Map<String, Text> getEmotes(ServerPlayerEntity player) {
        return new FakeMapPlayer(player, ConfigManager.getConfig().getEmotes(player.getCommandSource()));
    }

    public static Map<String, Text> getEmotes(MinecraftServer server) {
        return new FakeMapServer(server, ConfigManager.getConfig().getEmotes(server.getCommandSource()));
    }

    public static Text formatFor(ServerPlayerEntity player, String input) {
        return PlaceholderAPI.parsePredefinedText(
                TextParser.parse(StyledChatUtils.formatMessage(input, getHandlers(player))),
                EMOTE_PATTERN, getEmotes(player)
        );
    }

    public static Text formatFor(MinecraftServer server, String input) {
        return PlaceholderAPI.parsePredefinedText(
                TextParser.parse(StyledChatUtils.formatMessage(input, TextParser.getRegisteredTags())),
                EMOTE_PATTERN, getEmotes(server)
        );
    }

    public static String formatMessage(String input, Map<String, TextParser.TextFormatterHandler> handlers) {
        var config = ConfigManager.getConfig();
        if (handlers.containsKey(StyledChatUtils.LINK_TAG)) {
            input = input.replaceAll(StyledChatUtils.URL_REGEX, "<" + StyledChatUtils.LINK_TAG + ":'$1'>");
        }

        if (config.configData.legacyChatFormatting) {
            for (Formatting formatting : Formatting.values()) {
                if (handlers.get(formatting.getName()) != null) {
                    input = input.replace(String.copyValueOf(new char[]{'&', formatting.getCode()}), "<" + formatting.getName() + ">");
                }
            }
        }

        try {
            if (config.configData.enableMarkdown) {
                if (handlers.containsKey(SPOILER_TAG)) {
                    input = input.replaceAll(getMarkdownRegex("||", "\\|\\|"), "<spoiler>$2</spoiler>");
                }

                if (handlers.containsKey("bold")) {
                    input = input.replaceAll(getMarkdownRegex("**", "\\*\\*"), "<bold>$2</bold>");
                }

                if (handlers.containsKey("underline")) {
                    input = input.replaceAll(getMarkdownRegex("__", "__"), "<underline>$2</underline>");
                }

                if (handlers.containsKey("strikethrough")) {
                    input = input.replaceAll(getMarkdownRegex("~~", "~~"), "<strikethrough>$2</strikethrough>");
                }

                if (handlers.containsKey("italic")) {
                    input = input.replaceAll(getMarkdownRegex("*", "\\*"), "<italic>$2</italic>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return input;
    }

    private static String getMarkdownRegex(String base, String sides) {
        return "(" + sides + ")(?<id>[^" + base +"]+)(" + sides + ")";
    }

    public static abstract class FakeMap extends AbstractMap<String, Text> {
        private final Map<String, Text> texts;
        private final Map<String, Text> cache = new HashMap<>();

        public FakeMap(Map<String, Text> texts) {
            this.texts = texts;
        }

        @Override
        public Text get(Object key) {
            var text = this.cache.get(key);
            if (text != null) {
                return text;
            }

            text = this.texts.get(key);

            if (text != null) {
                var out = this.getParsed(text);
                this.cache.put((String) key, out);
                return out;
            }
            return null;
        }

        @NotNull
        @Override
        public Set<Entry<String, Text>> entrySet() {
            return texts.entrySet();
        }

        public abstract Text getParsed(Text text);
    }


    public static class FakeMapPlayer extends FakeMap {

        private final ServerPlayerEntity player;

        public FakeMapPlayer(ServerPlayerEntity player, Map<String, Text> texts) {
            super(texts);
            this.player = player;
        }

        @Override
        public Text getParsed(Text text) {
            return PlaceholderAPI.parseText(text, this.player);
        }
    }

    public static class FakeMapServer extends FakeMap {
        private final MinecraftServer server;

        public FakeMapServer(MinecraftServer server, Map<String, Text> texts) {
            super(texts);
            this.server = server;
        }

        @Override
        public Text getParsed(Text text) {
            return PlaceholderAPI.parseText(text, this.server);
        }
    }

	/**
	 * Attempts to get chat channel for sending messages.
	 * @param text Original text message of the sent message, used for searching by "usage prefix".
	 * @return Channel, in which message should be sent.
	 * @param actionType Any of next ones: <code>chat, death, tameable_death, advancement_challenge, advancement_task, advancement_goal, leave, join, join_first_time, join_renamed</code>.
	 */
	public static ChatChannel getChatChannel(String text, MessageActionType actionType) {
		Config config = ConfigManager.getConfig();
		ChatChannel defaultChannel = null;

		if (!config.configData.chatChannelsEnabled) return null;

		if (text == null) text = "";

		for (var channel : config.configData.chatChannels) {
			if (!channel.enabled) continue;

			if (!channel.messageTypesIncluded.contains(actionType.getValue())) continue;

			if (channel.isDefault) defaultChannel = channel;

			if (text.startsWith(channel.usagePrefix)) return channel;
		}

		return defaultChannel;
	}

	public enum MessageActionType {
		CHAT("chat"),
		DEATH("death"),
		TAMEABLE_DEATH("tameable_death"),
		ADVANCEMENT_CHALLENGE("advancement_challenge"),
		ADVANCEMENT_TASK("advancement_task"),
		ADVANCEMENT_GOAL("advancement_goal"),
		LEAVE("leave"),
		JOIN("join"),
		JOIN_FIRST_TIME("join_first_time"),
		JOIN_RENAMED("join_renamed");

		private final String name;

		MessageActionType(String name) {
			this.name = name;
		}

		public String getValue() {
			return name;
		}
	}

	/**
	 * Broadcasts message to players in the server and server console.
	 * @param playerManager
	 * @param text
	 * @param filteredText
	 * @param messageType
	 * @param sender
	 * @param channel
	 */
	public static void broadcast(PlayerManager playerManager, Text text, Text filteredText, MessageType messageType, Entity sender, ChatChannel channel) {
        playerManager.getServer().sendSystemMessage(text, sender.getUuid());

        for (ServerPlayerEntity reciever : playerManager.getPlayerList()) {
			boolean filtered = false;
			if (channel != null && sender != null) {
				// We have a channel specified, this means that we can show message to specific players.

				if (channel.onlyInSameDimension && !sender.getWorld().getDimension().equals(reciever.getWorld().getDimension())) continue;
				if (channel.radius > 0 && sender.distanceTo(reciever) > channel.radius) continue;
			}

			StyledChatEvents.MESSAGE_TO_SEND.invoker().onMessageTo(filtered ? filteredText : text, sender, reciever, filtered);

            reciever.sendMessage(text, messageType, sender.getUuid());
        }
	}
}
