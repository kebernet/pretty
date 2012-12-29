/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;


public class SetUID {
    public static final int OK = 0;
    public static final int ERROR = -1;

    public static int getuid() {
        if (Platform.isWindows()) {
            return -1;
        }

        return CLibrary.INSTANCE.getuid();
    }

    public static int setgid(int gid) {
        if (Platform.isWindows()) {
            return OK;
        }

        return CLibrary.INSTANCE.setgid(gid);
    }

    public static int setuid(int uid) {
        if (Platform.isWindows()) {
            return OK;
        }

        return CLibrary.INSTANCE.setuid(uid);
    }

    public static int setumask(int umask) {
        if (Platform.isWindows()) {
            return OK;
        }

        return CLibrary.INSTANCE.umask(umask);
    }

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), CLibrary.class);

        int geteuid();

        int getuid();

        int setgid(int gid);

        int setuid(int uid);

        int umask(int umask);
    }
}
