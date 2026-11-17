package com.holybuckets.combat;

import com.holybuckets.combat.client.CommonClassClient;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DodgeandSwingMainForgeClient {


    public static void clientInitializeForge() {
        CommonClassClient.initClient();
        //Item challengeChest = ModBlocks.challengeChest.asItem();
       // setBlockEntityRender( challengeChest, ChallengeItemBlockRenderer.CHEST_RENDERER);
    }


}
