package tw.nekomimi.nekogram;

import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public final class MeeroMessageStyler {
    private MeeroMessageStyler() {}

    public static int style() {
        try { return NekoConfig.meeroSendTextStyle.Int(); } catch (Throwable ignore) { return 0; }
    }

    public static void applyTo(SendMessagesHelper.SendMessageParams params) {
        try {
            if (params == null) return;
            final int s = style();
            if (s <= 0) return;
            final String text = params.message != null ? params.message : params.caption;
            if (text == null || text.isEmpty()) return;
            final TLRPC.MessageEntity e = entityFor(s);
            if (e == null) return;
            e.offset = 0;
            e.length = text.length();
            if (params.entities == null) params.entities = new ArrayList<>();
            params.entities.add(e);
        } catch (Throwable ignore) {}
    }

    private static TLRPC.MessageEntity entityFor(int s) {
        switch (s) {
            case 1: return new TLRPC.TL_messageEntityBold();
            case 2: return new TLRPC.TL_messageEntityItalic();
            case 3: return new TLRPC.TL_messageEntityUnderline();
            case 4: return new TLRPC.TL_messageEntityStrike();
            case 5: return new TLRPC.TL_messageEntitySpoiler();
            case 6: return new TLRPC.TL_messageEntityBlockquote();
            case 7: return new TLRPC.TL_messageEntityCode();
            case 8: { TLRPC.TL_messageEntityPre pre = new TLRPC.TL_messageEntityPre(); pre.language = ""; return pre; }
            default: return null;
        }
    }
}
