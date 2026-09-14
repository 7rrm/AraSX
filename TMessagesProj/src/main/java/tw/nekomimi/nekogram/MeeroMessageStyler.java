package tw.nekomimi.nekogram;

import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * MeeroX v230 (owner order): «تنسيق الإرسال» - pick a Telegram text style
 * once in Settings > Fonts and every outgoing text is sent with that
 * entity wrapped over its whole length. 0 = default (off). Values mirror
 * the format popup: 1 Bold, 2 Italic, 3 Underline, 4 Strike, 5 Spoiler,
 * 6 Quote, 7 Mono, 8 Code block. Styling is cosmetic and must never block
 * a send, so every failure path returns silently.
 */
public final class MeeroMessageStyler {

    private MeeroMessageStyler() {
    }

    public static int style() {
        try {
            return NekoConfig.meeroSendTextStyle.Int();
        } catch (Throwable ignore) {
            return 0;
        }
    }

    /** Wrap sendMessageParams' text (message or caption) with the entity.
     *  FIX: Don't overlap with custom emoji entities — wrap only the text
     *  segments between/before/after them so premium emojis stay premium. */
    public static void applyTo(SendMessagesHelper.SendMessageParams params) {
        try {
            if (params == null) {
                return;
            }
            final int s = style();
            if (s <= 0) {
                return;
            }
            final String text = params.message != null ? params.message : params.caption;
            if (text == null || text.isEmpty()) {
                return;
            }
            if (params.entities == null) {
                params.entities = new ArrayList<>();
            }

            // Scan the text for high-codepoint characters (custom/premium emojis)
            // These are characters with codepoint > 0xFFFF (surrogate pairs in Java)
            // We create style entities only for the TEXT segments, NOT the emoji segments
            int textStart = -1;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.isHighSurrogate(c) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    // This is a surrogate pair = custom emoji character
                    // First, close the current text segment
                    if (textStart >= 0) {
                        addStyleEntity(params, s, textStart, i - textStart);
                        textStart = -1;
                    }
                    i++; // Skip the low surrogate
                } else {
                    // Regular character - start a new text segment if needed
                    if (textStart < 0) {
                        textStart = i;
                    }
                }
            }
            // Close the last text segment
            if (textStart >= 0) {
                addStyleEntity(params, s, textStart, text.length() - textStart);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void addStyleEntity(SendMessagesHelper.SendMessageParams params, int style, int offset, int length) {
        if (length <= 0) return;
        final TLRPC.MessageEntity e = entityFor(style);
        if (e == null) return;
        e.offset = offset;
        e.length = length;
        params.entities.add(e);
    }

    private static TLRPC.MessageEntity entityFor(int s) {
        switch (s) {
            case 1:
                return new TLRPC.TL_messageEntityBold();
            case 2:
                return new TLRPC.TL_messageEntityItalic();
            case 3:
                return new TLRPC.TL_messageEntityUnderline();
            case 4:
                return new TLRPC.TL_messageEntityStrike();
            case 5:
                return new TLRPC.TL_messageEntitySpoiler();
            case 6:
                return new TLRPC.TL_messageEntityBlockquote();
            case 7:
                return new TLRPC.TL_messageEntityCode();
            case 8: {
                final TLRPC.TL_messageEntityPre pre = new TLRPC.TL_messageEntityPre();
                pre.language = "";
                return pre;
            }
            default:
                return null;
        }
    }
}
