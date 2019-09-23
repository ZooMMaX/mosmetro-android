/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.thedrhax.mosmetro.authenticator;

import java.io.IOException;

import android.content.Context;
import pw.thedrhax.mosmetro.httpclient.Client;
import pw.thedrhax.mosmetro.httpclient.ParsedResponse;
import pw.thedrhax.mosmetro.httpclient.clients.OkHttp;
import pw.thedrhax.util.Listener;
import pw.thedrhax.util.Logger;
import pw.thedrhax.util.Randomizer;
import pw.thedrhax.util.Util;

public class Gen204 {
    /**
     * Unreliable generate_204 endpoints (might be intercepted by provider)
     */
    protected static final String[] URL_DEFAULT = {
            // "www.google.com/generate_204",
            // "www.google.com/gen_204",
            "connectivitycheck.gstatic.com/generate_204",
            "www.gstatic.com/generate_204",
            "connectivitycheck.android.com/generate_204"
    };

    /**
     * Reliable generate_204 endpoints (confirmed to not be intercepted)
     */
    protected static final String[] URL_RELIABLE = {
            "www.google.ru/generate_204",
            "www.google.ru/gen_204",
            "google.com/generate_204",
            "gstatic.com/generate_204",
            "clients1.google.com/generate_204",
            "maps.google.com/generate_204",
            "mt0.google.com/generate_204",
            "mt1.google.com/generate_204",
            "mt2.google.com/generate_204",
            "mt3.google.com/generate_204",
            "play.googleapis.com/generate_204"
    };

    private static final ParsedResponse EMPTY = new ParsedResponse("");

    private final Context context;
    private final Listener<Boolean> running = new Listener<Boolean>(true);
    private final Client client;
    private final Randomizer random;
    private final int pref_retry_count;

    public Gen204(Context context, Listener<Boolean> running) {
        this.context = context;
        this.running.subscribe(running);

        client = new OkHttp(context)
                .customDnsEnabled(true)
                .followRedirects(false)
                .setRunningListener(this.running);

        random = new Randomizer(context);
        pref_retry_count = Util.getIntPreference(context, "pref_retry_count", 3);
    }

    /**
     * Perform logged request to specified URL.
     */
    private ParsedResponse request(String url) throws IOException {
        ParsedResponse res;
        try {
            res = client.get(url, null, pref_retry_count);
            Logger.log(this, url + " | " + res.getResponseCode());
        } catch (IOException ex) {
            Logger.log(this, url + " | " + ex.toString());
            throw ex;
        }
        return res;
    }

    public ParsedResponse check(boolean false_negatives) {
        ParsedResponse unrel, rel_https, rel_http;

        // Unreliable HTTP check (needs to be rechecked by HTTPS)
        try {
            unrel = request("http://" + random.choose(URL_DEFAULT));
        } catch (IOException ex) {
            // network is most probably unreachable
            return EMPTY;
        }

        // Reliable HTTPS check
        try {
            rel_https = request("https://" + random.choose(URL_RELIABLE));
        } catch (IOException ex) {
            rel_https = null;
        }

        if (unrel.getResponseCode() == 204) {
            if (rel_https == null || rel_https.getResponseCode() != 204) {
                // Reliable HTTP check
                try {
                    rel_http = request("http://" + random.choose(URL_RELIABLE));
                } catch (IOException ex) {
                    rel_http = null;
                }

                if (rel_http == null) {
                    return EMPTY; // error
                } else if (rel_http.getResponseCode() != 204) {
                    Logger.log(this, "False positive detected");
                    return rel_http; // false positive
                }
            } else {
                return rel_https; // confirmed positive
            }
        } else {
            if (rel_https == null) {
                return unrel; // confirmed negative
            } else if (rel_https.getResponseCode() == 204) {
                Logger.log(this, "False negative detected");
                return false_negatives ? unrel : rel_https; // false negative
            }
        }

        Logger.log(this, "Unexpected state");
        return EMPTY;
    }
}