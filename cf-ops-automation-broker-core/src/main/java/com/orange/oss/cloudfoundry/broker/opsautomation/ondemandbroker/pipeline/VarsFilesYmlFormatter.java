package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;

import java.util.regex.Pattern;

public class VarsFilesYmlFormatter {

    /**
     * \w 	A word character: [a-zA-Z_0-9]
     * \d 	A digit: [0-9]
     */
    private final Pattern whiteListedPattern = Pattern.compile("[\\w\\d _-]*");

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    protected String formatAsYml(Object o) throws JsonProcessingException {

        return getMapper().writeValueAsString(o);
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
