package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class VarsFilesYmlFormatter {

    private final Validator validator;
    private final Pattern whiteListedPattern = Pattern.compile(CoabVarsFileDto.WHITE_LISTED_PATTERN);


    public VarsFilesYmlFormatter() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    protected String formatAsYml(CoabVarsFileDto o) throws JsonProcessingException {
        validate(o);
        return getMapper().writeValueAsString(o);
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
            if (! whiteListedPattern.matcher(key).matches()) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sb.append("parameter name " + key + " " + CoabVarsFileDto.WHITE_LISTED_MESSAGE);
            }
            Object valueObject = entry.getValue();
            if (
                    ! (valueObject instanceof String)  &&
                    ! (valueObject instanceof Number)  &&
                    ! (valueObject instanceof Boolean)
                    ) {
                //noinspection StringConcatenationInsideStringBufferAppend
                sb.append("parameter " + key + " of unsupported type: " + valueObject.getClass().getName());

            }
            if (valueObject instanceof String) {
                String value = (String) valueObject;
                if (! whiteListedPattern.matcher(value).matches()) {
                    //noinspection StringConcatenationInsideStringBufferAppend
                    sb.append("parameter " + key + " " + CoabVarsFileDto.WHITE_LISTED_MESSAGE + " whereas it has content:" + value);
                }
            }
        }
        if (sb.length() >0) {
            throw new UserFacingRuntimeException("Unsupported characters in input: " + sb.toString());
        }
    }

    protected ObjectMapper getMapper() {
        return mapper;
    }

}
