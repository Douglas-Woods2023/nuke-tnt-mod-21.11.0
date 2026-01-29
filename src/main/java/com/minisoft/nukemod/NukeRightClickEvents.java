package com.minisoft.nukemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = NukeMod.MODID)
public class NukeRightClickEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();

        // 检查右键的方块是否是核弹方块
        if (level.getBlockState(event.getPos()).is(NukeMod.NUKE_BLOCK.get())) { //TODO: 检查26.1的兼容性
            // 新增：检查玩家是否潜行（按下Shift）
            if (!player.isShiftKeyDown()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§6[提示]§r 按住Shift并右键来引爆核弹方块！"),
                        true
                );
                // 如果玩家没有潜行，不触发爆炸，正常进行交互
                return;
            }

            // 如果是客户端，直接返回成功
            if (level.isClientSide()) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            // 服务端逻辑
            if (level instanceof ServerLevel serverLevel) {
                var pos = event.getPos();

                // 移除核弹方块
                serverLevel.removeBlock(pos, false);

                // 创建自定义爆炸破坏计算器
                ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator() {
                    @Override
                    public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter blockGetter, net.minecraft.core.BlockPos blockPos, BlockState blockState, float power) {
                        return !blockState.isAir() && blockState.getDestroySpeed(blockGetter, blockPos) >= 0;
                    }

                    @Override
                    public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
                        return true;
                    }

                    @Override
                    public Optional<Float> getBlockExplosionResistance(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter blockGetter, net.minecraft.core.BlockPos blockPos, BlockState blockState, FluidState fluidState) {
                        return Optional.of(Math.min(blockState.getBlock().getExplosionResistance(), 6.0F));
                    }
                };

                // 产生爆炸
                serverLevel.explode(
                        player,
                        null,
                        damageCalculator,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        295.0F,
                        true,
                        Level.ExplosionInteraction.TNT
                );

                // 给爆炸范围内的所有生物添加中毒II效果
                double explosionRadius = 300.0; // 爆炸半径
                AABB affectedArea = new AABB(
                        pos.getX() - explosionRadius,
                        pos.getY() - explosionRadius,
                        pos.getZ() - explosionRadius,
                        pos.getX() + explosionRadius,
                        pos.getY() + explosionRadius,
                        pos.getZ() + explosionRadius
                );

                // 获取范围内的所有实体
                List<Entity> entitiesInRange = serverLevel.getEntities(null, affectedArea);

                for (Entity entity : entitiesInRange) {
                    if (entity instanceof LivingEntity livingEntity) {
                        // 添加中毒II效果，持续30秒
                        livingEntity.addEffect(new MobEffectInstance(
                                MobEffects.POISON,
                                30 * 20,
                                1,
                                false,
                                true
                        ));

                        // 添加虚弱效果，持续60秒
                        livingEntity.addEffect(new MobEffectInstance(
                                MobEffects.WEAKNESS,
                                60 * 20,
                                0,
                                false,
                                true
                        ));
                    }
                }

                // 火焰粒子效果
                for (int i = 0; i < 100; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.FLAME,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            5,
                            serverLevel.random.nextDouble() * 2 - 1,
                            serverLevel.random.nextDouble() * 2,
                            serverLevel.random.nextDouble() * 2 - 1,
                            0.5
                    );
                }

                // 使用灵魂火焰粒子代替毒雾粒子
                for (int i = 0; i < 30; i++) {
                    serverLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME, // 灵魂火焰粒子
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            3,
                            serverLevel.random.nextDouble() * 2 - 1,
                            serverLevel.random.nextDouble() * 2,
                            serverLevel.random.nextDouble() * 2 - 1,
                            0.5
                    );
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}