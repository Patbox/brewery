package eu.pb4.brewery.other;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public record ResourcePackRequestFile(List<Entry> entries) {
    public static Codec<ResourcePackRequestFile> CODEC = Entry.CODEC.listOf().xmap(ResourcePackRequestFile::new, ResourcePackRequestFile::entries);

    public static ResourcePackRequestFile create(Collection<DrinkType> types) {
        var list = new ArrayList<ResourcePackRequestFile.Entry>();

        for (var type : types) {
            type.failedVisuals().resourcePackModel().ifPresent(x -> list.add(new Entry(x, type.failedVisuals().defaultModel())));

            for (var entry : type.looks().visualsSelector().entries()) {
                entry.result().resourcePackModel().ifPresent(x -> list.add(new Entry(x, entry.result().defaultModel())));
            }
        }


        return new ResourcePackRequestFile(list);
    }

    public Map<Identifier, Map<Identifier, PolymerModelData>> requestModels() {
        var map = new HashMap<Identifier, Map<Identifier, PolymerModelData>>();

        for (var entry : this.entries) {
            var item = Registries.ITEM.get(entry.baseItem);
            if (item == Items.AIR) {
                item = Items.POTION;
            }

            map.computeIfAbsent(entry.baseItem, x -> new HashMap<>()).put(entry.modelId, PolymerResourcePackUtils.requestModel(item, entry.modelId.withPrefixedPath("item/")));
        }
        return map;
    }

    public record Entry(Identifier modelId, Identifier baseItem) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("model").forGetter(Entry::modelId),
                Identifier.CODEC.fieldOf("base").forGetter(Entry::baseItem)
        ).apply(instance, Entry::new));
    }
}
