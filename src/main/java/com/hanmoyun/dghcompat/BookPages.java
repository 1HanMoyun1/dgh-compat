package com.hanmoyun.dghcompat;

import java.util.ArrayList;
import java.util.List;

/** Page allocation is atomic: a full book keeps all its previous contents. */
public final class BookPages {
    private BookPages() {}

    public static List<String> addition(int existingPages, String text) {
        var result = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 180, text.length());
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            result.add(text.substring(start, end));
            if (existingPages + result.size() > 100) return null;
            start = end;
        }
        return result;
    }
}
