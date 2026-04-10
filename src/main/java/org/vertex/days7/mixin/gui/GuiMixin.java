package org.vertex.days7.mixin.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vertex.days7.client.ClientBlockHPManager;

@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderExperienceBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        ci.cancel();
        var th = (GuiAccessor) this;
        var minecraft = th.days$getMinecraft();

        minecraft.getProfiler().push("expBar");

        int xpNeeded = minecraft.player.getXpNeededForNextLevel();
        if (xpNeeded > 0) {
            int barWidth = 182;
            int barHeight = 3;
            int filledWidth = (int) (minecraft.player.experienceProgress * barWidth);
            int y = th.days$getScreenHeight() - 32 + 3;

            // Цвета
            int backgroundColor = 0xFF1A0A2E;  // Тёмно-фиолетовый фон
            int foregroundColor = 0xFF9B30FF;  // Яркий фиолетовый

            int borderColor = 0xFF2D1B4E;
            guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, borderColor);
            // Фон (пустая часть бара)
            guiGraphics.fill(x, y, x + barWidth, y + barHeight, backgroundColor);

            // Заполненная часть
            if (filledWidth > 0) {
                guiGraphics.fill(x, y, x + filledWidth, y + barHeight, foregroundColor);
            }
        }

        minecraft.getProfiler().pop();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderExperienceBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        var th = (GuiAccessor) this;
        var minecraft = th.days$getMinecraft();
        var screenWidth = th.days$getScreenWidth();
        var screenHeight = th.days$getScreenHeight();
        var GUI_ICONS_LOCATION = GuiAccessor.getGUI_ICONS_LOCATION();

        Options options = minecraft.options;
        if (options.getCameraType().isFirstPerson()) {
            if (minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
                if (options.renderDebug && !options.hideGui && !minecraft.player.isReducedDebugInfo() && !options.reducedDebugInfo().get()) {
                    Camera camera = minecraft.gameRenderer.getMainCamera();
                    PoseStack poseStack = RenderSystem.getModelViewStack();
                    poseStack.pushPose();
                    poseStack.mulPoseMatrix(guiGraphics.pose().last().pose());
                    poseStack.translate((float) (screenWidth / 2), (float) (screenHeight / 2), 0.0F);
                    poseStack.mulPose(Axis.XN.rotationDegrees(camera.getXRot()));
                    poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot()));
                    poseStack.scale(-1.0F, -1.0F, -1.0F);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.renderCrosshair(10);
                    poseStack.popPose();
                    RenderSystem.applyModelViewMatrix();
                } else {
                    int crosshairSize = 15;
                    {
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                                GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ZERO
                        );
                        int crosshairX = (screenWidth - crosshairSize) / 2;
                        int crosshairY = (screenHeight - crosshairSize) / 2;
                        guiGraphics.blit(GUI_ICONS_LOCATION, crosshairX, crosshairY, 0, 0, crosshairSize, crosshairSize);
                    }

                    BlockHitResult hitResult = days7$getTargetBlock(minecraft.player, minecraft.level);
                    if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
                        return;
                    }

                    BlockPos pos = hitResult.getBlockPos();
                    BlockState state = minecraft.level.getBlockState(pos);
                    if (!state.isAir()) {
                        var hpData = ClientBlockHPManager.getInstance().getBlockHP(pos);
                        if (hpData == null) return;

                        int currentHP = hpData[0];
                        int maxHP = hpData[1];

                        // Неразрушаемый блок
                        if (maxHP == -1) {
                            // Можно отобразить иконку или текст "Indestructible"
                            String text = "Indestructible";
                            int textWidth = minecraft.font.width(text);
                            int textX = screenWidth / 2 - textWidth / 2;
                            int textY = screenHeight / 2 + crosshairSize / 2 + 4;
                            guiGraphics.drawString(minecraft.font, text, textX, textY, 0xFFFF4444);
                            RenderSystem.defaultBlendFunc();
                            return;
                        }

                        // Если HP полное - не показываем бар
                        if (currentHP >= maxHP) return;

                        int barWidth = 60;
                        int barHeight = 5;
                        int barX = screenWidth / 2 - barWidth / 2;
                        int barY = screenHeight / 2 + crosshairSize / 2 + 4;

                        // Фон
                        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA333333);

                        // HP бар
                        if (maxHP > 0) {
                            int filledWidth = (int) ((float) currentHP / maxHP * barWidth);
                            guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xAA00FF00);
                        }

                        // Рамка
                        guiGraphics.renderOutline(barX, barY, barWidth, barHeight, 0xFFFFFFFF);

                        // Текст
                        String progressText = currentHP + "/" + maxHP;
                        int textWidth = minecraft.font.width(progressText);
                        int textX = screenWidth / 2 - textWidth / 2;
                        int textY = barY - minecraft.font.lineHeight - 1;
                        guiGraphics.drawString(minecraft.font, progressText, textX, textY, 0xFFFFFFFF);

                        RenderSystem.defaultBlendFunc();
                    }
                }
            }
        }
    }

    @Unique
    private static BlockHitResult days7$getTargetBlock(LocalPlayer player, Level level) {
        double reachDistance = player.getAttributeValue(
                net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        ClipContext context = new ClipContext(
                eyePos, endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        return level.clip(context);
    }
}
