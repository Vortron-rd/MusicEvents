package musify;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("Musify")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class MixinLoader implements IFMLLoadingPlugin {

    public MixinLoader() throws ClassNotFoundException {
        try {
            Class<?> mixinLoader = Class.forName("fermiumbooter.FermiumRegistryAPI");
//            FermiumRegistryAPI.enqueueMixin(true, "mixins.musify.roguelike.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.musify.doomlike.json");
        } catch (ClassNotFoundException ignored) {
            System.out.println("====================================MUSIFY====================================");
            System.out.println("FermiumBooter not found. The mod will load but certain features may not work.");
            System.out.println("==============================================================================");
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
