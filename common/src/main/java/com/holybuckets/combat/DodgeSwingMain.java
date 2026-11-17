package com.holybuckets.combat;


import com.holybuckets.combat.config.ModConfig;
import com.holybuckets.combat.core.DashManager;
import com.holybuckets.combat.core.IDashManager;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.combat.config.DodgeSwingConfig;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;

/**
 * Main instance of the mod, initialize this class statically via commonClass
 * This class will init all major Manager instances and events for the mod
 */
public class DodgeSwingMain {
    private static boolean DEV_MODE = false;;
    public static DodgeSwingConfig CONFIG;
    public static ModConfig MOD_CONFIG;
    public static DodgeSwingMain INSTANCE;

    public static IDashManager DASH_MANAGER;

    public DodgeSwingMain()
    {
        super();
        INSTANCE = this;
        init();
        // LoggerProject.logInit( "001000", this.getClass().getName() ); // Uncomment if you have a logging system in place
    }

    private void init()
    {

        /*
        Proxy for external APIs which are platform dependent
        this.portalApi = (PortalApi) Balm.platformProxy()
            .withFabric("com.holybuckets.challengetemple.externalapi.FabricPortalApi")
            .withForge("com.holybuckets.challengetemple.externalapi.ForgePortalApi")
            .build();
            */


        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        DashManager.init(registrar);
        //ChallengeBlockBehavior.init(registrar);


        //register local events
        registrar.registerOnBeforeServerStarted(this::onServerStarting);

    }

    private void onServerStarting(ServerStartingEvent e) {
        CONFIG = Balm.getConfig().getActiveConfig(DodgeSwingConfig.class);
        MOD_CONFIG = ModConfig.initOnServerStart(CONFIG);
    }


}
