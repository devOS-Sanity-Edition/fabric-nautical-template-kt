package one.devos.nautical.template.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

/**
 * This entrypoint is suitable for setting up Data Generation
 *
 * See [Fabric Data Generation Setup](https://docs.fabricmc.net/develop/data-generation/setup)
 */
object TemplateModDataGenerator : DataGeneratorEntrypoint {

    /**
     * This code runs on the Data Generator, which will start up a Minecraft Client, and starts processing, creating,
     * and modifying data files
     *
     * To add a provider, create a Provider, and then register it below with
     * `pack.addProvider(::TemplateModThingProvider)`, ie, `pack.addProvider(::TemplateModModelProvider)`
     */
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

//        pack.addProvider(::TemplateModModelProvider)
    }
}