package eu.pb4.styledchat.config;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.config.data.ChatStyleData;
import eu.pb4.styledchat.config.data.ConfigData;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class Config {
    public final ConfigData configData;
    private final ChatStyle defaultStyle;
    private final List<PermissionStyle> permissionStyle;
    public final Object2BooleanArrayMap<String> defaultFormattingCodes;
    public final Text linkStyle;
    public final Text spoilerStyle;
    private final Text petDeathMessage;

    private final Map<String, Text> emotes;
    private final List<PermissionEmotes> permissionEmotes;

    public Config(ConfigData data) {
        this.configData = data;
        this.defaultStyle = new ChatStyle(data.defaultStyle, new ChatStyle(ChatStyleData.DEFAULT));

        this.permissionStyle = new ArrayList<>();
        this.linkStyle = TextParser.parse(data.linkStyle);
        this.spoilerStyle = TextParser.parse(data.spoilerStyle);

        for (ConfigData.PermissionPriorityStyle entry : data.permissionStyles) {
            this.permissionStyle.add(new PermissionStyle(entry.permission, entry.opLevel, new ChatStyle(entry.style)));
        }

        this.petDeathMessage = StyledChatUtils.parseText(configData.petDeathMessage);

        this.emotes = new HashMap<>();

        for (var entry : data.emoticons.entrySet()) {
            this.emotes.put(entry.getKey(), TextParser.parse(entry.getValue()));
        }

        this.permissionEmotes = new ArrayList<>();
        for (var entry : data.permissionEmoticons) {
            var emotes = PermissionEmotes.of(entry.permission, entry.opLevel);

            for (var emote : entry.emoticons.entrySet()) {
                emotes.emotes().put(emote.getKey(), TextParser.parse(emote.getValue()));
            }
            this.permissionEmotes.add(emotes);
        }

        this.defaultFormattingCodes = new Object2BooleanArrayMap<>(this.configData.defaultEnabledFormatting);
    }

    public Text getDisplayName(ServerPlayerEntity player, Text vanillaDisplayName) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getDisplayName(player, vanillaDisplayName);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getDisplayName(player, vanillaDisplayName);
    }

    public Text getChat(ServerPlayerEntity player, Text message, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getChat(player, message, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getChat(player, message, channel);
    }

    public Text getJoin(ServerPlayerEntity player, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getJoin(player, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getJoin(player, channel);
    }

    public Text getJoinFirstTime(ServerPlayerEntity player, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getJoinFirstTime(player, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        Text text = this.defaultStyle.getJoinFirstTime(player, channel);
        if (text != null) {
            return text;
        }
        return this.getJoin(player, channel);
    }

    public Text getJoinRenamed(ServerPlayerEntity player, String oldName, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getJoinRenamed(player, oldName, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getJoinRenamed(player, oldName, channel);
    }

    public Text getLeft(ServerPlayerEntity player, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getLeft(player, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getLeft(player, channel);
    }

    public Text getDeath(ServerPlayerEntity player, Text vanillaMessage, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getDeath(player, vanillaMessage, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getDeath(player, vanillaMessage, channel);
    }

    public Text getAdvancementTask(ServerPlayerEntity player, Text advancement, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getAdvancementTask(player, advancement, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getAdvancementTask(player, advancement, channel);
    }

    public Text getAdvancementGoal(ServerPlayerEntity player, Text advancement, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getAdvancementGoal(player, advancement, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getAdvancementGoal(player, advancement, channel);
    }

    public Text getAdvancementChallenge(ServerPlayerEntity player, Text advancement, ChatChannel channel) {
        ServerCommandSource source = player.getCommandSource();
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getAdvancementChallenge(player, advancement, channel);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getAdvancementChallenge(player, advancement, channel);
    }

    public Text getSayCommand(ServerCommandSource source, Text message) {
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getSayCommand(source, message);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getSayCommand(source, message);
    }

    public Text getMeCommand(ServerCommandSource source, Text message) {
        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                Text text = entry.style.getMeCommand(source, message);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getMeCommand(source, message);
    }

    public Text getPrivateMessageSent(Text sender, Text receiver, Text message, ServerCommandSource context) {
        Object placeholderContext;

        try {
            placeholderContext = context.getPlayer();
        } catch (Exception e) {
            placeholderContext = context.getServer();
        }

        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(context, entry.permission, entry.opLevel)) {
                Text text = entry.style.getPrivateMessageSent(sender, receiver, message, placeholderContext);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getPrivateMessageSent(sender, receiver, message, placeholderContext);
    }

    public Text getPrivateMessageReceived(Text sender, Text receiver, Text message, ServerCommandSource context) {
        Object placeholderContext;

        try {
            placeholderContext = context.getPlayer();
        } catch (Exception e) {
            placeholderContext = context.getServer();
        }

        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(context, entry.permission, entry.opLevel)) {
                Text text = entry.style.getPrivateMessageReceived(sender, receiver, message, placeholderContext);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getPrivateMessageReceived(sender, receiver, message, placeholderContext);
    }

    public Text getTeamChatSent(Text team, Text displayName, Text message, ServerCommandSource context) {
        Object placeholderContext;

        try {
            placeholderContext = context.getPlayer();
        } catch (Exception e) {
            placeholderContext = context.getServer();
        }

        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(context, entry.permission, entry.opLevel)) {
                Text text = entry.style.getTeamChatSent(team, displayName, message, placeholderContext);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getTeamChatSent(team, displayName, message, placeholderContext);
    }

    public Text getTeamChatReceived(Text team, Text displayName, Text message, ServerCommandSource context) {
        Object placeholderContext;

        try {
            placeholderContext = context.getPlayer();
        } catch (Exception e) {
            placeholderContext = context.getServer();
        }

        for (PermissionStyle entry : this.permissionStyle) {
            if (Permissions.check(context, entry.permission, entry.opLevel)) {
                Text text = entry.style.getTeamChatReceived(team, displayName, message, placeholderContext);
                if (text != null) {
                    return text;
                }
            }
        }
        return this.defaultStyle.getTeamChatReceived(team, displayName, message, placeholderContext);
    }

    public Text getPetDeath(TameableEntity entity, Text vanillaMessage, ChatChannel channel) {
        if (this.petDeathMessage == null) {
            return null;
        }

		Config config = ConfigManager.getConfig();

        return PlaceholderAPI.parsePredefinedText(
                PlaceholderAPI.parseText(StyledChatUtils.parseText((channel == null ? "" : channel.prefix) + config.configData.petDeathMessage), entity.getServer()),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                Map.of("pet", entity.getDisplayName(),
                        "default_message", vanillaMessage)
        );
    }

    public Map<String, Text> getEmotes(ServerCommandSource source) {
        var base = new HashMap<>(this.emotes);

        for (var entry : this.permissionEmotes) {
            if (Permissions.check(source, entry.permission, entry.opLevel)) {
                base.putAll(entry.emotes());
            }
        }

        return base;
    }

    private static record PermissionStyle(String permission, int opLevel, ChatStyle style) {
    }

    private static record PermissionEmotes(String permission, int opLevel, Map<String, Text> emotes) {
        public static PermissionEmotes of(String permission, int opLevel) {
            return new PermissionEmotes(permission, opLevel, new HashMap<>());
        }
    }
}
