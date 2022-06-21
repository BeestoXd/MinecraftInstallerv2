package jdk.test.lib;

public class Container {
    // Use this property to specify docker location on your system.
    // E.g.: "/usr/local/bin/docker". We define this constant here so
    // that it can be used in VMProps as well which checks docker support
    // via this command
    public static final String ENGINE_COMMAND =
        System.getProperty("jdk.test.container.command", "docker");
}
