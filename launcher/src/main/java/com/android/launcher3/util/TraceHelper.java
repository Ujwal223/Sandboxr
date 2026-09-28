package com.android.launcher3.util;

/** Bridge stub for TraceHelper. */
public class TraceHelper {
    public static final TraceHelper INSTANCE = new TraceHelper();
    public SafeCloseable beginSection(String name) { return () -> {}; }
    public void endSection() {}
    public <T> T allowIpcs(String name, java.util.function.Supplier<T> supplier) {
        return supplier.get();
    }
}
