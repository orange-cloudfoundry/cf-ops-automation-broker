package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;

import java.util.regex.Pattern;

public class VarsFilesYmlFormatter {

    private final Pattern whiteListedPattern = Pattern.compile("[a-zA-Z\\d_\\- ]*");

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    protected String formatAsYml(Object o) throws JsonProcessingException {

        String yml = getMapper().writeValueAsString(o);
        rejectUnsupportedPatterns(yml);
        return yml;
    }

    protected void rejectUnsupportedPatterns(String yml) {
        if (! whiteListedPattern.matcher(yml).matches()) {
            throw new UserFacingRuntimeException("Unsupported characters in: " + yml + ". Not matching " + whiteListedPattern);
        }
    }

    protected ObjectMapper getMapper() {
        return mapper;
    }

}
