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

import java.util.HashMap;

public abstract class WaitTask extends NamedTask {
    private Provider p;
    private int tries = 0;
    private int interval = 100;

    public WaitTask(Provider p, String name) {
        super(name);
        this.p = p;
    }

    @Override
    public boolean run(HashMap<String, Object> vars) {
        for (int i = 0; i < tries || tries == 0; i++) {
            if (until(vars)) {
                return true;
            }

            if (!p.running.sleep(interval)) {
                return false;
            }
        }
        return false;
    }

    public WaitTask tries(int tries) {
        this.tries = tries; return this;
    }

    public WaitTask interval(int interval) {
        this.interval = interval; return this;
    }

    public WaitTask timeout(int timeout) {
        tries(timeout / interval); return this;
    }

    protected void stop() {
        tries(-1);
    }

    public abstract boolean until(HashMap<String, Object> vars);
}
