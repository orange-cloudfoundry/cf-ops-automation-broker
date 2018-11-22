package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public abstract class GitContext {
    public abstract Map<String, Object> getKeys();
}
