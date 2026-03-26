package eu.pb4.brewery.other;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.Nullable;


public record WrappedText(Component text, @Nullable String input) {
    public static final Codec<WrappedText> CODEC = Codec.either(Codec.STRING, ComponentSerialization.CODEC)
            .xmap(x -> x.left().isPresent() ? of(x.left().get()) : of(x.right().get()), x -> x.input != null ? Either.left(x.input) : Either.right(x.text));

    public static WrappedText of(String input) {
        return new WrappedText(TagParser.QUICK_TEXT_WITH_STF.parseComponent(input, ParserContext.of()), input);
    }

    public static WrappedText of(Component text) {
        return new WrappedText(text, null);
    }
}
