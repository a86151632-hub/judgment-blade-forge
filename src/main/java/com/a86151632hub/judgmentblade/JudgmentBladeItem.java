package com.a86151632hub.judgmentblade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class JudgmentBladeItem extends SwordItem {
    private static final Vector3f CRIMSON_COLOR = new Vector3f(1.0f, 0.13f, 0.27f);
    private static final Vector3f GOLD_COLOR = new Vector3f(1.0f, 0.84f, 0.0f);

    public JudgmentBladeItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (player.isShiftKeyDown()) {
                // [Shift + 우클릭] 차원 단절 베기 (Dimensional Severance)
                castDimensionalSeverance(serverLevel, player);
                player.getCooldowns().addCooldown(this, 50); // 2.5초 쿨타임
            } else {
                // [우클릭] 단죄의 결계 & 천벌 강림 (Domain of Judgment)
                castDomainOfJudgment(serverLevel, player);
                player.getCooldowns().addCooldown(this, 80); // 4초 쿨타임
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 스킬 1 [우클릭]: 단죄의 결계 (Domain of Judgment)
     */
    private void castDomainOfJudgment(ServerLevel level, Player caster) {
        Vec3 center = caster.position();
        double radius = 11.0;

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4f, 0.7f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.3f);

        DustParticleOptions crimsonDust = new DustParticleOptions(CRIMSON_COLOR, 1.8f);
        DustParticleOptions goldDust = new DustParticleOptions(GOLD_COLOR, 1.5f);

        // 1. 바닥 이중 마법진 전개
        for (int i = 0; i < 120; i++) {
            double angle = (2 * Math.PI * i) / 120.0;
            double outerX = center.x + Math.cos(angle) * radius;
            double outerZ = center.z + Math.sin(angle) * radius;
            level.sendParticles(crimsonDust, outerX, center.y + 0.15, outerZ, 2, 0.05, 0.05, 0.05, 0.0);

            double innerX = center.x + Math.cos(angle) * (radius * 0.65);
            double innerZ = center.z + Math.sin(angle) * (radius * 0.65);
            level.sendParticles(goldDust, innerX, center.y + 0.25, innerZ, 1, 0.02, 0.02, 0.02, 0.0);
        }

        // 2. 결계 외곽 8개의 천벌 빛기둥 소환
        for (int p = 0; p < 8; p++) {
            double pillarAngle = (2 * Math.PI * p) / 8.0;
            double px = center.x + Math.cos(pillarAngle) * (radius * 0.85);
            double pz = center.z + Math.sin(pillarAngle) * (radius * 0.85);
            for (double yOff = 0; yOff <= 16.0; yOff += 0.6) {
                level.sendParticles(ParticleTypes.END_ROD, px, center.y + yOff, pz, 2, 0.08, 0.08, 0.08, 0.01);
                level.sendParticles(crimsonDust, px, center.y + yOff, pz, 1, 0.15, 0.1, 0.15, 0.0);
            }
        }

        // 3. 범위 내 적 속박, 공중 부양 및 뇌격 처형
        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            target.setDeltaMovement(0, 0.85, 0);
            target.hurtMarked = true;

            for (double h = 0; h <= 12.0; h += 0.5) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + h, target.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(target.getX(), target.getY(), target.getZ());
                lightning.setVisualOnly(true);
                level.addFreshEntity(lightning);
            }

            target.hurt(level.damageSources().playerAttack(caster), 28.0f);
        }
    }

    /**
     * 스킬 2 [Shift + 우클릭]: 차원 단절 베기 (Dimensional Severance)
     */
    private void castDimensionalSeverance(ServerLevel level, Player caster) {
        Vec3 start = caster.position();
        Vec3 look = caster.getLookAngle().normalize();
        double dashDist = 14.0;

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.5f);

        DustParticleOptions crimsonDust = new DustParticleOptions(CRIMSON_COLOR, 2.0f);

        Vec3 safeTargetPos = start;
        for (double d = 1.0; d <= dashDist; d += 0.5) {
            Vec3 step = start.add(look.scale(d));
            level.sendParticles(crimsonDust, step.x, step.y + 1.0, step.z, 4, 0.25, 0.25, 0.25, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, step.x, step.y + 1.0, step.z, 2, 0.15, 0.15, 0.15, 0.02);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, step.x, step.y + 1.0, step.z, 1, 0.3, 0.1, 0.3, 0.0);

            AABB slashBox = new AABB(step.x - 2.2, step.y - 1.0, step.z - 2.2, step.x + 2.2, step.y + 2.5, step.z + 2.2);
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, slashBox,
                    e -> e != caster && e.isAlive());
            for (LivingEntity victim : victims) {
                victim.hurt(level.damageSources().playerAttack(caster), 22.0f);
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            }

            if (level.isEmptyBlock(BlockPos.containing(step))) {
                safeTargetPos = step;
            }
        }

        caster.teleportTo(safeTargetPos.x, safeTargetPos.y, safeTargetPos.z);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 4));
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, safeTargetPos.x, safeTargetPos.y + 1.0, safeTargetPos.z, 1, 0, 0, 0, 0);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new DustParticleOptions(GOLD_COLOR, 1.6f),
                    target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.6, 0.6, 0.6, 0.05);
            attacker.heal(2.5f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("하늘의 심판을 대행하는 신화급 대검").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("[우클릭] 단죄의 결계: 이중 마법진 전개 및 8개의 천벌 빛기둥 폭격").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("[Shift + 우클릭] 차원 단절 베기: 전방 14m 초고속 일섬 돌진").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("[기본 공격] 성흔 흡수: 타격 시 황금 충격파 및 체력 회복").withStyle(ChatFormatting.YELLOW));
    }
}
