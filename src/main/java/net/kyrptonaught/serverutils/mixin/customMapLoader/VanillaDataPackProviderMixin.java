package net.kyrptonaught.serverutils.mixin.customMapLoader;

import net.kyrptonaught.serverutils.customMapLoader.CustomMapLoaderMod;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.VanillaDataPackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Arrays;

@Mixin(VanillaDataPackProvider.class)
public class VanillaDataPackProviderMixin {


    @ModifyArgs(method = "createManager(Ljava/nio/file/Path;Lnet/minecraft/util/path/SymlinkFinder;)Lnet/minecraft/resource/ResourcePackManager;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourcePackManager;<init>([Lnet/minecraft/resource/ResourcePackProvider;)V"))
    private static void registerAddonProvider(Args args) {
        ResourcePackProvider[] packs = Arrays.copyOf(((ResourcePackProvider[]) args.get(0)), ((ResourcePackProvider[]) args.get(0)).length + 1);
        packs[packs.length - 1] = CustomMapLoaderMod.ADDON_DATAPACK_PROVIDER;
        args.set(0, packs);
    }
}
