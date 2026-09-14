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
            
            // Collect all custom emoji entity offsets so we can skip them
            java.util.List<int[]> emojiRanges = new java.util.ArrayList<>();
            for (TLRPC.MessageEntity existing : params.entities) {
                if (existing instanceof TLRPC.TL_messageEntityCustomEmoji) {
                    emojiRanges.add(new int[]{existing.offset, existing.offset + existing.length});
                }
            }
            
            if (emojiRanges.isEmpty()) {
                // No custom emojis — wrap the entire message
                final TLRPC.MessageEntity e = entityFor(s);
                if (e == null) return;
                e.offset = 0;
                e.length = text.length();
                params.entities.add(e);
            } else {
                // Sort emoji ranges by offset
                emojiRanges.sort((a, b) -> Integer.compare(a[0], b[0]));
                
                // Add style entities for the gaps between/around emojis
                int currentPos = 0;
                for (int[] range : emojiRanges) {
                    if (currentPos < range[0]) {
                        // There's text before this emoji — wrap it
                        final TLRPC.MessageEntity e = entityFor(s);
                        if (e != null) {
                            e.offset = currentPos;
                            e.length = range[0] - currentPos;
                            params.entities.add(e);
                        }
                    }
                    currentPos = range[1]; // Skip past the emoji
                }
                // Wrap any remaining text after the last emoji
                if (currentPos < text.length()) {
                    final TLRPC.MessageEntity e = entityFor(s);
                    if (e != null) {
                        e.offset = currentPos;
                        e.length = text.length() - currentPos;
                        params.entities.add(e);
                    }
                }
            }
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
