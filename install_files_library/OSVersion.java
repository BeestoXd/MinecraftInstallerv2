package jdk.test.lib;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class OSVersion implements Comparable<OSVersion> {
    public static final OSVersion WINDOWS_95 = new OSVersion(4, 0);
    public static final OSVersion WINDOWS_98 = new OSVersion(4, 10);
    public static final OSVersion WINDOWS_ME = new OSVersion(4, 90);
    public static final OSVersion WINDOWS_2000 = new OSVersion(5, 0);
    public static final OSVersion WINDOWS_XP = new OSVersion(5, 1);
    public static final OSVersion WINDOWS_2003 = new OSVersion(5, 2);
    public static final OSVersion WINDOWS_VISTA = new OSVersion(6, 0);

    private final int[] versionTokens;

    public static OSVersion current() {
        return new OSVersion(Platform.getOsVersion());
    }

    public OSVersion(int major, int minor) {
        versionTokens = new int[] {major, minor};
    }

    public OSVersion(String version) {
        Pattern onlyDigits = Pattern.compile("^\\d+$");
        this.versionTokens = Arrays.stream(version.split("-")[0].split("\\."))
                                   .filter(onlyDigits.asPredicate())
                                   .mapToInt(Integer::parseInt)
                                   .toArray();
    }

    @Override
    public int compareTo(OSVersion o) {
        return Arrays.compare(this.versionTokens, o.versionTokens);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(versionTokens);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OSVersion osVersion = (OSVersion) o;
        return Arrays.equals(versionTokens, osVersion.versionTokens);
    }

    @Override
    public String toString() {
        return Arrays.stream(versionTokens)
                     .mapToObj(String::valueOf)
                     .collect(Collectors.joining("1.1.8_official"));
    }
}

