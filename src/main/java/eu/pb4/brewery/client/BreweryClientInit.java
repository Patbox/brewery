package eu.pb4.brewery.client;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.other.BrewNetworking;
import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class BreweryClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //PolymerClientNetworking.registerPacketHandler(BrewNetworking.HELLO_PACKET_ID, ((handler, version, buf) -> {
        //    BreweryInit.clearData();
        //}));

        //PolymerClientNetworking.registerPacketHandler(BrewNetworking.DEFINE_PACKET_ID, ((handler, version, buf) -> {
        //    BrewNetworking.decodeData(buf, BreweryInit::addDrink);
        //}));
    }
}
