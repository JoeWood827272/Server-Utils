package net.kyrptonaught.serverutils.customMapLoader.voting.pages;

import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.Collections;

public class BookPage {
    private static final WrittenBookContentComponent DEFAULT_WRITTEN_COMPONENT = new WrittenBookContentComponent(RawFilteredPair.of(""), "", 0, Collections.emptyList(), false);

    protected final BookElementBuilder builder;

    public BookPage(String title, String author) {
        builder = new BookElementBuilder()
                .setTitle(title)
                .setAuthor(author);
    }

    public BookGui build(ServerPlayerEntity player) {
        return new BookGui(player, builder);
    }

    public BookPage addPage(Text... lines) {
        builder.addPage(lines);
        return this;
    }

    public int size() {
        WrittenBookContentComponent component = builder.asStack().getOrDefault(DataComponentTypes.WRITTEN_BOOK_CONTENT, DEFAULT_WRITTEN_COMPONENT);
        return component.pages().size();
    }
}
