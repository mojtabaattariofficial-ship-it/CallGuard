package com.callguard;

import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;

/**
 * Runs entirely in the background. The Android system binds to this service on its own
 * whenever a call comes in (only while the app holds the Call-Screening role), so there
 * is no foreground service, no persistent notification, and nothing to launch by hand.
 *
 * For every incoming call it normalizes the caller's number and, if it matches any configured
 * entry, silently rejects it before the phone rings and suppresses the missed-call notification.
 * Each entry may be a prefix (e.g. "0942" blocks every number that starts with it) or a full
 * number (e.g. "09121234567" blocks exactly that caller) — both are handled by the same
 * prefix match, since a full number is simply a complete prefix.
 */
public class BlockCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details details) {
        String raw = null;
        Uri handle = details.getHandle();
        if (handle != null) {
            raw = handle.getSchemeSpecificPart();
        }

        boolean block = shouldBlock(raw);

        CallResponse.Builder resp = new CallResponse.Builder();
        if (block) {
            resp.setDisallowCall(true);      // stop the call from reaching the user
            resp.setRejectCall(true);        // actively hang up (busy) instead of just silencing
            resp.setSkipCallLog(false);      // keep a record so the user can see what was blocked
            resp.setSkipNotification(true);  // no missed-call notification
        }
        // Must always respond, otherwise the call hangs.
        respondToCall(details, resp.build());
    }

    private boolean shouldBlock(String rawNumber) {
        String number = normalize(rawNumber);
        if (number == null) {
            return false;
        }
        String cfg = MainActivity.getPrefixes(this);
        if (cfg == null || cfg.trim().isEmpty()) {
            return false;
        }
        for (String part : cfg.split(",")) {
            String prefix = normalize(part.trim());
            if (prefix != null && !prefix.isEmpty() && number.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reduces any number to a comparable local form:
     *  - keeps only digits (also converts Persian ۰-۹ and Arabic ٠-٩ digits)
     *  - +98 / 0098 / 98 country code becomes a leading 0
     *  - a bare number without a leading 0 gets one
     * So "+98 942 123 4567", "0942-1234567" and "9421234567" all become "09421234567".
     */
    static String normalize(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder d = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                d.append(c);
            } else if (c >= '۰' && c <= '۹') {       // Persian digits ۰-۹
                d.append((char) ('0' + (c - '۰')));
            } else if (c >= '٠' && c <= '٩') {       // Arabic-Indic digits ٠-٩
                d.append((char) ('0' + (c - '٠')));
            }
        }
        String x = d.toString();
        if (x.isEmpty()) {
            return null;
        }
        if (x.startsWith("0098")) {
            x = "0" + x.substring(4);
        } else if (x.startsWith("98") && x.length() >= 11) {
            x = "0" + x.substring(2);
        } else if (!x.startsWith("0")) {
            x = "0" + x;
        }
        return x;
    }
}
