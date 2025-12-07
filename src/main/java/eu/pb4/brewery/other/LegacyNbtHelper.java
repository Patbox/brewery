package eu.pb4.brewery.other;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Helper methods for handling NBT.
 */
public final class LegacyNbtHelper {
    private LegacyNbtHelper() {
    }

    // Before 1.21.4 this class serialized using int array
    // In 1.21.5 this was changed to Uuids.CODEC (string without dashes)
    public static final Codec<UUID> UUID_CODEC = Codec.withAlternative(UUIDUtil.CODEC, UUIDUtil.AUTHLIB_CODEC);

    @Nullable
    public static GameProfile toGameProfile(CompoundTag nbt) {
        UUID uUID = nbt.contains("Id") ? nbt.read("Id", UUID_CODEC).orElse(Util.NIL_UUID) : Util.NIL_UUID;
        String string = nbt.getStringOr("Name", "");

        try {
            var map = ImmutableMultimap.<String, com.mojang.authlib.properties.Property>builder();
            if (nbt.contains("Properties")) {
                CompoundTag nbtCompound = nbt.getCompoundOrEmpty("Properties");

                for (String string2 : nbtCompound.keySet()) {
                    ListTag nbtList = nbtCompound.getListOrEmpty(string2);

                    for (int i = 0; i < nbtList.size(); ++i) {
                        CompoundTag nbtCompound2 = nbtList.getCompoundOrEmpty(i);
                        String string3 = nbtCompound2.getStringOr("Value", "");
                        if (nbtCompound2.contains("Signature")) {
                            map.put(string2, new com.mojang.authlib.properties.Property(string2, string3, nbtCompound2.getStringOr("Signature", null)));
                        } else {
                            map.put(string2, new com.mojang.authlib.properties.Property(string2, string3));
                        }
                    }
                }
            }
            return new GameProfile(uUID, string, new PropertyMap(map.build()));
        } catch (Throwable var11) {
            return null;
        }
    }

    public static CompoundTag writeGameProfile(CompoundTag nbt, GameProfile profile) {
        if (!profile.name().isEmpty()) {
            nbt.putString("Name", profile.name());
        }

        if (!profile.id().equals(Util.NIL_UUID)) {
            nbt.store("Id", UUIDUtil.AUTHLIB_CODEC, profile.id());
        }

        if (!profile.properties().isEmpty()) {
            CompoundTag nbtCompound = new CompoundTag();

            for (String string : profile.properties().keySet()) {
                ListTag nbtList = new ListTag();

                for (com.mojang.authlib.properties.Property property : profile.properties().get(string)) {
                    CompoundTag nbtCompound2 = new CompoundTag();
                    nbtCompound2.putString("Value", property.value());
                    String string2 = property.signature();
                    if (string2 != null) {
                        nbtCompound2.putString("Signature", string2);
                    }

                    nbtList.add(nbtCompound2);
                }

                nbtCompound.put(string, nbtList);
            }

            nbt.put("Properties", nbtCompound);
        }

        return nbt;
    }

    public static IntArrayTag fromUuid(UUID uuid) {
        return new IntArrayTag(UUIDUtil.uuidToIntArray(uuid));
    }

    public static UUID toUuid(Tag element) {
        if (element.getType() != IntArrayTag.TYPE) {
            throw new IllegalArgumentException(
                    "Expected UUID-Tag to be of type " + IntArrayTag.TYPE.getName() + ", but found " + element.getType().getName() + "."
            );
        } else {
            int[] is = ((IntArrayTag) element).getAsIntArray();
            if (is.length != 4) {
                throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + is.length + ".");
            } else {
                return UUIDUtil.uuidFromIntArray(is);
            }
        }
    }

    public static BlockPos toBlockPos(CompoundTag nbt) {
        return new BlockPos(nbt.getIntOr("X", 0), nbt.getIntOr("Y", 0), nbt.getIntOr("Z", 0));
    }

    public static CompoundTag fromBlockPos(BlockPos pos) {
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putInt("X", pos.getX());
        nbtCompound.putInt("Y", pos.getY());
        nbtCompound.putInt("Z", pos.getZ());
        return nbtCompound;
    }
}