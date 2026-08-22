package io.github.no1qq.uagc.api;

public interface UagcApi {

    String API_VERSION = "1.0";

    String apiVersion();

    boolean isEnabled();

    UagcIntegration integration(String pluginName);

    UagcQuery query();
}
