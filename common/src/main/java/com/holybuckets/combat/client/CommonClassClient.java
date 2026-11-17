package com.holybuckets.combat.client;

import com.holybuckets.combat.DodgeSwingMain;
import com.holybuckets.foundation.client.ClientBalmEventRegister;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.client.BalmClient;


public class CommonClassClient {

    public static void initClient() {
        ClientEventRegistrar registrar = ClientEventRegistrar.getInstance();
        registrar.registerOnConnectedToServer(e -> regDashManager());
        EventRegistrar.getInstance().registerOnBeforeServerStarted(e -> regDashManager());
        ClientBalmEventRegister.registerEvents();
        ClientDashManager.init(registrar);
        ModRenderers.clientInitialize(BalmClient.getRenderers());
    }

    private static void regDashManager() {
        DodgeSwingMain.DASH_MANAGER = new ClientDashManager();
    }

    /**
     * Description: Run sample tests methods
     */
    public static void sample()
    {

    }


}