package com.dachjobs.pipeline.classify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Ports the posting_id hash from notebooks/03_silver_clean.py: sha2-256 of
 * title|company|city|first 200 chars of description, all lowercased. This is
 * what dedup and "genuine repost" detection key off, since the same job is
 * often re-listed under a new adzuna_id.
 */
public final class PostingHasher {

    private PostingHasher() {
    }

    public static String hash(String titleRaw, String company, String city, String description) {
        String desc = description == null ? "" : description;
        String truncated = desc.substring(0, Math.min(200, desc.length()));
        String joined = String.join("|",
                lower(titleRaw == null ? "" : titleRaw.trim()),
                lower(company == null ? "" : company),
                city == null ? "" : city,
                lower(truncated));
        return sha256Hex(joined);
    }

    private static String lower(String s) {
        return s.toLowerCase();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
