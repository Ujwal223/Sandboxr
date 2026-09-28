package com.android.launcher3.util;

import android.os.UserHandle;
import com.android.users.UserType;

/**
 * Bridge stub for UserIconInfo.
 * Uses delegation instead of inheritance since the Kotlin source is a data class (final).
 */
public class UserIconInfo {
    private final com.sandboxr.launcher.util.UserIconInfo delegate;
    public final UserHandle user;
    public final UserType type;
    public final long userSerial;

    public UserIconInfo(UserHandle user, UserType type, long userSerial) {
        this.delegate = new com.sandboxr.launcher.util.UserIconInfo(user, type, userSerial);
        this.user = user;
        this.type = type;
        this.userSerial = userSerial;
    }

    public UserHandle getUser() { return delegate.user; }
    public UserType getType() { return delegate.type; }
    public long getUserSerial() { return delegate.userSerial; }
    public boolean isMain() { return delegate.isMain(); }
    public boolean isWork() { return delegate.isWork(); }
    public boolean isPrivate() { return delegate.isPrivate(); }
    public boolean isCloned() { return delegate.isCloned(); }
}
