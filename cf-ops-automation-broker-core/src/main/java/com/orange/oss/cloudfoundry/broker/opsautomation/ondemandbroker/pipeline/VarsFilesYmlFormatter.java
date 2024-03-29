package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VarsFilesYmlFormatter {

    private static Logger logger = LoggerFactory.getLogger(VarsFilesYmlFormatter.class.getName());

    public static final int MAX_SERIALIZED_SIZE = 3000;

    private final Validator validator;

    /**
     * Emergency support for turning off input validation when it is observed to perform false positive.
     * Once a coab release fixes the false positive, then this flag should be turned off again.
     */
    private boolean inputValidationDisabled;

    private Pattern relaxedWhiteListedPattern = Pattern.compile(CoabVarsFileDto.RELAXED_WHITE_LISTED_PATTERN);

    private Pattern whiteListedPattern = Pattern.compile(CoabVarsFileDto.WHITE_LISTED_PATTERN);


    public VarsFilesYmlFormatter(boolean inputValidationDisabled) {
        this.inputValidationDisabled = inputValidationDisabled;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    protected String formatAsYml(CoabVarsFileDto o) throws JsonProcessingException {
        validate(o);
        String yml = getMapper().writeValueAsString(o);
        if (yml.length() > MAX_SERIALIZED_SIZE) {
            String message = "Unsupported too long params or context. Size reached " + yml
                .length() + " while max is: " + MAX_SERIALIZED_SIZE + " chars";
            throwUserFacingExceptionUnlessDisabled(message);
        }
        logger.debug("coab-vars.yml content: {}", yml);
        return yml;
    }

    private void throwUserFacingExceptionUnlessDisabled(String message) {
        if (inputValidationDisabled) {
            logger.error("Input validation is disabled as per configuration. Skipping rejecting user input with " +
                "message: " + message);
        } else {
            throw new UserFacingRuntimeException(message);
        }
    }

    protected CoabVarsFileDto parseFromYml(String yaml) throws IOException {
        return getMapper().readValue(yaml, CoabVarsFileDto.class);
    }

    protected CoabVarsFileDto parseFromBoshManifestYml(Path pathToBoshManifestFile) throws IOException {
        BoshDeploymentManifestDTO boshDeploymentManifestDTO = getMapper()
            .readValue(pathToBoshManifestFile.toFile(), BoshDeploymentManifestDTO.class);
        return boshDeploymentManifestDTO.coabCompletionMarker;
    }


    protected void validate(CoabVarsFileDto coabVarsFileDto) {

        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(coabVarsFileDto);
        StringBuilder sb = new StringBuilder();
        if (! constraintViolations.isEmpty()) {
            for (ConstraintViolation<Object> constraintViolation : constraintViolations) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sb.append(constraintViolation.getPropertyPath() +  " " + constraintViolation.getMessage() + " whereas it has content:" + constraintViolation.getInvalidValue() + " ");
            }
        }

        //Can't yet leverage bean validation 2.0 which includes supports for validating containers such as maps.
        //until we bump to springboot 2.0, implementing it manually
        Map<String, Object> parameters = coabVarsFileDto.parameters;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Pattern whiteListedPattern;
            String whiteListedMessage;
            if (CoabVarsFileDto.RELAXED_KEY_NAMES.contains(key)) {
                //Relax pattern for any subkey of "annotations"
                whiteListedPattern = relaxedWhiteListedPattern;
                whiteListedMessage = CoabVarsFileDto.RELAXED_WHITE_LISTED_MESSAGE;
            } else {
                whiteListedPattern = this.whiteListedPattern;
                whiteListedMessage = CoabVarsFileDto.WHITE_LISTED_MESSAGE;
            }
            validateParamsMapEntry(sb, key, entry.getValue(), whiteListedPattern, whiteListedMessage);
        }
        if (sb.length() >0) {
            throwUserFacingExceptionUnlessDisabled("Unsupported characters in input: " + sb.toString());
        }
    }

    private void validateParamsMapEntry(StringBuilder sb, String key, Object value,
        Pattern whiteListedPattern, String whiteListedMessage) {
        if (! whiteListedPattern.matcher(key).matches()) {
            //noinspection StringConcatenationInsideStringBufferAppend
            sb.append("parameter name " + key + " " + whiteListedMessage);
        }
        if (
                ! (value instanceof String)  &&
                ! (value instanceof Number)  &&
                ! (value instanceof Boolean) &&
                ! (value instanceof Map)
                ) {
            //noinspection StringConcatenationInsideStringBufferAppend
            sb.append("parameter value for key=" + key + " of unsupported type: " + value.getClass().getName());

        }
        if (value instanceof String) {
            String stringValue = (String) value;
            if (! whiteListedPattern.matcher(stringValue).matches()) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sb.append("parameter " + key + " " + whiteListedMessage + " whereas it has content:" + stringValue);
            }
        }
        if (value instanceof Map) {
            //noinspection unchecked
            Map<Object,Object> valueMap = (Map<Object, Object>) value;
            for (Map.Entry<Object, Object> valueMapEntry : valueMap.entrySet()) {
                if (!(valueMapEntry.getKey() instanceof String)) {
                    //noinspection StringConcatenationInsideStringBufferAppend
                    sb.append("key map "+ valueMapEntry.getKey() +" from parameter " + key + " " + CoabVarsFileDto.WHITE_LISTED_MESSAGE +
                        " " +
                        "whereas it " +
                        "has " + valueMapEntry.getKey().getClass().getName());
                    continue;
                }
                if (CoabVarsFileDto.RELAXED_KEY_NAMES.contains(valueMapEntry.getKey())) {
                    //Relax pattern for any subkey of "annotations"
                    validateParamsMapEntry(sb, key + "." + valueMapEntry.getKey(), valueMapEntry.getValue(),
                        relaxedWhiteListedPattern,
                        CoabVarsFileDto.RELAXED_WHITE_LISTED_MESSAGE);
                } else {
                    validateParamsMapEntry(sb, key + "."  + valueMapEntry.getKey(), valueMapEntry.getValue(),
                        whiteListedPattern, whiteListedMessage);
                }
            }
        }
    }

    protected ObjectMapper getMapper() {
        return mapper;
    }

}
