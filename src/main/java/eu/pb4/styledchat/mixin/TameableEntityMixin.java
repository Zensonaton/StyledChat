package eu.pb4.styledchat.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import eu.pb4.styledchat.StyledChatUtils;
import eu.pb4.styledchat.StyledChatUtils.MessageActionType;
import eu.pb4.styledchat.config.Config;
import eu.pb4.styledchat.config.ConfigManager;
import eu.pb4.styledchat.config.data.ConfigData.ChatChannel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;

@Mixin(TameableEntity.class)
public class TameableEntityMixin {
    @Redirect(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;sendSystemMessage(Lnet/minecraft/text/Text;Ljava/util/UUID;)V"))
    private void styledChat_onTameableDeath(LivingEntity diedEntity, Text text, UUID uuid) {
		Config config = ConfigManager.getConfig();

		ChatChannel channel = StyledChatUtils.getChatChannel(null, MessageActionType.TAMEABLE_DEATH);

		StyledChatUtils.broadcast(
			diedEntity.getServer().getPlayerManager(),
			config.getPetDeath((TameableEntity) (Object) this, text, channel),
			null,
			MessageType.CHAT,
			(TameableEntity) (Object) this,
			channel
		);
    }
}
