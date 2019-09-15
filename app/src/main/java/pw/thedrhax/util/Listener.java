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

package pw.thedrhax.util;

import java.util.LinkedList;
import java.util.List;

import android.os.SystemClock;

/**
 * Util class used to monitor every change of the stored variable.
 *
 * Main functions:
 *   - Subscribe to already existing Listeners of the same type
 *   - Allow to retrieve and change the value of variable at any time
 *   - Notify about every change using the onChange() callback
 *   - Interruptible delays
 *   - Stack Overflow protection by checking if callback is the master
 *
 * @author Dmitry Karikh <the.dr.hax@gmail.com>
 * @param <T> type of the stored variable
 */
public class Listener<T> {
    private T value;
    private final List<Listener<T>> callbacks = new LinkedList<>();

    public Listener(T initial_value) {
        value = initial_value;
    }

    public final synchronized void set(T new_value) {
        value = new_value;
        onChange(new_value);
        synchronized (callbacks) {
            for (Listener<T> callback : callbacks) {
                if (callback.callbacks.contains(this)) {
                    callback.value = new_value;
                    callback.onChange(new_value);
                } else {
                    callback.set(new_value);
                }
            }
        }
    }

    public final T get() {
        return value;
    }

    /**
     * Delay execution for N milliseconds, but return as quick as possible if stored
     * value has changed.
     * 
     * @param ms Number of milliseconds to delay for.
     * @return Stored value.
     */
    public T sleep(int ms) {
        T initial_value = value;

        while (ms > 0) {
            if (ms > 100) {
                SystemClock.sleep(100);
                ms -= 100;
            } else {
                SystemClock.sleep(ms);
                ms = 0;
            }

            if (value != initial_value) {
                return value;
            }
        }

        return value;
    }

    public void subscribe(Listener<T> master) {
        synchronized (master.callbacks) {
            if (!master.callbacks.contains(this)) {
                master.callbacks.add(this);
            }
        }
        this.value = master.value;
    }

    public void unsubscribe(Listener<T> master) {
        synchronized (master.callbacks) {
            if (master.callbacks.contains(this)) {
                master.callbacks.remove(this);
            }
        }
    }

    public void onChange(T new_value) {

    }
}
