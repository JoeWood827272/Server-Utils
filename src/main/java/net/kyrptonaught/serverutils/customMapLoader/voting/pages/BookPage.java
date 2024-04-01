package net.kyrptonaught.serverutils.customMapLoader.voting.pages;

import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BookPage {
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
        if (!builder.getOrCreateNbt().contains("pages"))
            return 0;
        return builder.getOrCreateNbt().getList("pages", NbtElement.STRING_TYPE).size();
    }
}
