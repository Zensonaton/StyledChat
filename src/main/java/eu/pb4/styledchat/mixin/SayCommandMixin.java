package eu.pb4.styledchat.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.brigadier.context.CommandContext;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.config.ConfigManager;
import net.minecraft.server.command.SayCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Mixin(SayCommand.class)
public class SayCommandMixin {
    @ModifyVariable(method = "method_13563", at = @At("STORE"), ordinal = 1)
    private static Text styledChat_formatText(Text inputX, CommandContext<ServerCommandSource> context) {
        var input = (Text) ((TranslatableText) inputX).getArgs()[1];

        var inputAsString = input.getString();

        var config = ConfigManager.getConfig();
        var source = context.getSource();

        Text message;
        Map<String, TextParser.TextFormatterHandler> formatting;
        Map<String, Text> emotes;

        try {
            var player = source.getPlayer();
            formatting = StyledChatUtils.getHandlers(player);
            emotes = StyledChatUtils.getEmotes(player);
        } catch (Exception e) {
            formatting = TextParser.getRegisteredSafeTags();
            emotes = StyledChatUtils.getEmotes(context.getSource().getServer());
        }

        if (formatting.size() != 0) {
            var formattedMessage = StyledChatUtils.formatMessage(inputAsString, formatting);
            var tmpMessage = TextParser.parse(formattedMessage, formatting);

            if (!tmpMessage.getString().equals(inputAsString)) {
                message = tmpMessage;
            } else {
                message = input;
            }
        } else {
            message = input;
        }

        if (emotes.size() != 0) {
            message = PlaceholderAPI.parsePredefinedText(message, StyledChatUtils.EMOTE_PATTERN, emotes);
        }

        return config.getSayCommand(source, message);
    }
}
