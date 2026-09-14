package tw.nekomimi.nekogram;

import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * MeeroX v230 (owner order): «تنسيق الإرسال» - pick a Telegram text style
 * once in Settings > Fonts and every outgoing text is sent with that
 * entity wrapped over its whole length. 0 = default (off). Values mirror
 * the format popup: 1 Bold, 2 Italic, 3 Underline, 4 Strike, 5 Spoiler,
 * 6 Quote, 7 Mono, 8 Code block.
 *
 * FIX v3: When the message contains custom/premium emoji (surrogate pairs
 * in UTF-16), the style is NOT applied at all. This guarantees premium
 * emojis stay premium. When there are no custom emojis, the style is
 * applied to the entire message as before.
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

    /** Check if text contains any surrogate pair (custom/premium emoji). */
    private static boolean hasCustomEmoji(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isHighSurrogate(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Wrap sendMessageParams' text with the entity. */
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

            // FIX: If message contains custom/premium emoji, DON'T apply style
            // This guarantees the emoji stays premium
            if (hasCustomEmoji(text)) {
                return;
            }

            // No custom emoji - apply style to entire message (original behavior)
            final TLRPC.MessageEntity e = entityFor(s);
            if (e == null) {
                return;
            }
            e.offset = 0;
            e.length = text.length();
            if (params.entities == null) {
                params.entities = new ArrayList<>();
            }
            params.entities.add(e);
        } catch (Throwable ignore) {
        }
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
