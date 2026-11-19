package com.holybuckets.combat.client;

import com.holybuckets.combat.DodgeSwingMain;
import com.holybuckets.combat.config.ModConfig;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.client.ClientBalmEventRegister;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import net.minecraft.client.Minecraft;
import net.blay09.mods.balm.api.client.BalmClient;


public class CommonClassClient {

    public static void initClient() {
        ClientEventRegistrar registrar = ClientEventRegistrar.getInstance();
        registrar.registerOnConnectedToServer(e -> regDashManager());
        registrar.registerOnDisconnectedFromServer(e -> onDisconnectedFromServer());
        registrar.registerOnSimpleMessage(ModConfig.MSG_SYNC, CommonClassClient::onConfigSync);
        EventRegistrar.getInstance().registerOnBeforeServerStarted(e -> regDashManager());
        ClientBalmEventRegister.registerEvents();
        ClientDashManager.init(registrar);
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }

    private static void regDashManager() {
        DodgeSwingMain.DASH_MANAGER = new ClientDashManager();
    }

    private static void onConfigSync(SimpleMessageEvent event) {
        if (GeneralConfig.getInstance().isIntegrated()) return;
        String content = event.getContent();
        Minecraft.getInstance().execute(() -> ModConfig.deserialize(content));
    }

    private static void onDisconnectedFromServer() {
        if (GeneralConfig.getInstance().isIntegrated()) return;
        ModConfig.reset();
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }


}