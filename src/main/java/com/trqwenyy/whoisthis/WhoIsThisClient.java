package com.trqwenyy.whoisthis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class WhoIsThisClient implements ClientModInitializer {
    private static final Identifier HUD_ID = Identifier.of("whoisthis", "hovered_name");
    private static final KeyBinding SHOW_HOVERED_NAME_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.whoisthis.hovered_name",
            InputUtil.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            KeyBinding.MISC_CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_ID, this::renderHoveredName);
    }

    private void renderHoveredName(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (!SHOW_HOVERED_NAME_KEY.isPressed()) {
            return;
        }

        PlayerEntity player = getLookedAtPlayer(client, tickCounter.getTickProgress(true));
        if (player == null) {
            return;
        }

        Text name = player.getName();
        int x = client.getWindow().getScaledWidth() / 2;
        int y = client.getWindow().getScaledHeight() / 2 - 16;
        context.fill(x - client.textRenderer.getWidth(name) / 2 - 4, y - 4, x + client.textRenderer.getWidth(name) / 2 + 4, y + 9, 0x80000000);
        context.drawCenteredTextWithShadow(client.textRenderer, name, x, y, 0xFFFFFFFF);
    }

    private static PlayerEntity getLookedAtPlayer(MinecraftClient client, float tickDelta) {
        if (client.targetedEntity instanceof PlayerEntity player) {
            return player;
        }

        if (client.crosshairTarget instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof PlayerEntity player) {
            return player;
        }

        Vec3d start = client.player.getCameraPosVec(tickDelta);
        Vec3d look = client.player.getRotationVec(tickDelta).normalize();
        double reach = Math.max(6.0D, 5.0D + client.player.getEntityInteractionRange());
        PlayerEntity bestPlayer = null;
        double bestScore = Double.MAX_VALUE;

        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == client.player || !candidate.isAlive()) {
                continue;
            }

            Vec3d toPlayer = candidate.getBoundingBox().getCenter().subtract(start);
            double distanceSquared = toPlayer.lengthSquared();
            if (distanceSquared < 0.0001D || distanceSquared > reach * reach) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double alignment = look.dotProduct(toPlayer.normalize());
            if (alignment < 0.985D) {
                continue;
            }

            double score = (1.0D - alignment) * 1000.0D + distance;
            if (score < bestScore) {
                bestScore = score;
                bestPlayer = candidate;
            }
        }

        return bestPlayer;
    }
}
