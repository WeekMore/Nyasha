package com.nyasha.inject.mixin.net.minecraft.client;


import com.nyasha.mcef.MCEF;
import com.nyasha.mcef.MCEFDownloader;
import com.nyasha.mcef.MCEFPlatform;
import com.nyasha.mcef.MCEFSettings;
import com.nyasha.mcef.internal.MCEFDownloadListener;
import com.nyasha.mcef.internal.MCEFDownloaderMenu;
import com.nyasha.Nyasha;
import com.nyasha.events.EventClientTick;
import com.nyasha.events.EventPreAttack;
import com.nyasha.util.IMinecraftMixin;
import com.nyasha.managers.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;


/**
 * @author yuxiangll
 * @since 2024/8/9 下午8:10
 * IntelliJ IDEA
 */

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraftMixin {

    @Shadow @Nullable
    public Screen currentScreen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);


    @Inject(at = @At("HEAD"), method = "setScreen", cancellable = true)
    public void redirScreen(Screen guiScreen, CallbackInfo ci) {


        if (!MCEF.isInitialized()) {
            if (guiScreen instanceof TitleScreen) {
                // If the download is done and didn't fail
                if (MCEFDownloadListener.INSTANCE.isDone() && !MCEFDownloadListener.INSTANCE.isFailed()) {
                    mc.execute((() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MCEF.initialize();
                    }));
                }
                // If the download is not done and didn't fail
                else if (!MCEFDownloadListener.INSTANCE.isDone() && !MCEFDownloadListener.INSTANCE.isFailed()) {
                    setScreen(new MCEFDownloaderMenu((TitleScreen) guiScreen));
                    ci.cancel();
                }
                // If the download failed
                else if (MCEFDownloadListener.INSTANCE.isFailed()) {
                    MCEF.getLogger().error("MCEF failed to initialize!");
                }
            }
        }
    }




    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci){
        Nyasha.INSTANCE.getEventBus().post(new EventClientTick());
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    void postWindowInit(RunArgs args, CallbackInfo ci) {

    }

    @Unique
    private void setupLibraryPath() throws IOException {
        final File mcefLibrariesDir;

        // Check for development environment
        // TODO: handle eclipse/others
        // i.e. mcef-repo/neoforge/build
        File buildDir = new File("../build");
        if (buildDir.exists() && buildDir.isDirectory()) {
            mcefLibrariesDir = new File(buildDir, "mcef-libraries/");
        } else {
            mcefLibrariesDir = new File("mods/mcef-libraries/");
        }

        mcefLibrariesDir.mkdirs();

        System.setProperty("mcef.libraries.path", mcefLibrariesDir.getCanonicalPath());
        System.setProperty("jcef.path", new File(mcefLibrariesDir, MCEFPlatform.getPlatform().getNormalizedName()).getCanonicalPath());


    }

    @Inject(at = @At("HEAD"), method = "onInitFinished")
    private void sinit(CallbackInfoReturnable<Boolean> cir) {
        try {
            setupLibraryPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread downloadThread = new Thread(() -> {
            String javaCefCommit;

            try {
                javaCefCommit = MCEF.getJavaCefCommit();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            MCEF.getLogger().info("java-cef commit: " + javaCefCommit);

            MCEFSettings settings = MCEF.getSettings();
            MCEFDownloader downloader = new MCEFDownloader(settings.getDownloadMirror(), javaCefCommit, MCEFPlatform.getPlatform());

            boolean downloadJcefBuild;

            // We always download the checksum for the java-cef build
            // We will compare this with mcef-libraries/<platform>.tar.gz.sha256
            // If the contents of the files differ (or it doesn't exist locally), we know we need to redownload JCEF
            try {
                downloadJcefBuild = !downloader.downloadJavaCefChecksum();
            } catch (IOException e) {
                e.printStackTrace();
                MCEFDownloadListener.INSTANCE.setFailed(true);
                return;
            }

            // Ensure the mcef-libraries directory exists
            // If not, we want to try redownloading
            File mcefLibrariesDir = new File(System.getProperty("mcef.libraries.path"));
            downloadJcefBuild |= !mcefLibrariesDir.exists();

            if (downloadJcefBuild && !settings.isSkipDownload()) {
                try {
                    downloader.downloadJavaCefBuild();
                } catch (IOException e) {
                    e.printStackTrace();
                    MCEFDownloadListener.INSTANCE.setFailed(true);
                    return;
                }

                downloader.extractJavaCefBuild(true);
            }

            MCEFDownloadListener.INSTANCE.setDone(true);
        });
        downloadThread.start();
    }






    @Inject(method = "onFinishedLoading", at = @At("TAIL"))
    private void loadingFinished(CallbackInfo ci) {

        FontManager.initialize();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void preTickHook(CallbackInfo ci) {
        Nyasha.INSTANCE.getEventBus().post(new EventClientTick());
    }


    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
        final EventPreAttack event = new EventPreAttack();
        Nyasha.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setOverlay", at = @At("HEAD"), cancellable = true)
    private void setOverlay(Overlay overlay, CallbackInfo ci) {
        if (overlay instanceof SplashOverlay && (currentScreen instanceof LanguageOptionsScreen || currentScreen instanceof CraftingScreen)) ci.cancel();

    }


}