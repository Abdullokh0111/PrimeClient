package com.prime.client.api;

/**
 * Common lifecycle for a Prime Client module.
 *
 * Modules register their own tick/render/keybinding hooks inside init() -
 * this interface exists so PrimeClient has one place that knows about every
 * module, instead of a hand-maintained list of `XxxHUD.init()` calls that's
 * easy to forget to update.
 */
public interface Module {
    /** Human-readable name, used in logs. */
    String getName();

    /** Called once during client mod init, after config has been loaded. */
    void init();
}
