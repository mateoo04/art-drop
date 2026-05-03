package hr.tvz.artdrop.artdropapp.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public record FeedCursor(String snapshotId, int offset) {

    public String encode() {
        String raw = snapshotId + "|" + offset;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<FeedCursor> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return Optional.empty();
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            if (sep <= 0 || sep == raw.length() - 1) return Optional.empty();
            String snapshotId = raw.substring(0, sep);
            int offset = Integer.parseInt(raw.substring(sep + 1));
            if (offset < 0) return Optional.empty();
            return Optional.of(new FeedCursor(snapshotId, offset));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
