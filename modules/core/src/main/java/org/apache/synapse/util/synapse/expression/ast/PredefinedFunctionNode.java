/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.util.synapse.expression.ast;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;
import org.apache.synapse.util.synapse.expression.exception.EvaluationException;
import org.apache.synapse.util.synapse.expression.utils.ExpressionUtils;
import org.jaxen.JaxenException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.List;

/**
 * Represents a node in the AST that holds a predefined function.
 * Ex: toUpper() toLower()
 */
public class PredefinedFunctionNode implements ExpressionNode {

    private final String functionName;
    private final List<ExpressionNode> arguments;

    public PredefinedFunctionNode(ArgumentListNode arguments, String functionName) {
        this.arguments = arguments.getArguments();
        this.functionName = functionName;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context) {
        if (arguments.isEmpty()) {
            return handleNoArgumentFunctions();
        } else if (arguments.size() == 1) {
            return handleSingleArgumentFunctions(context);
        } else if (arguments.size() == 2) {
            return handleDoubleArgumentFunctions(context);
        } else if (arguments.size() == 3) {
            return handleTripleArgumentFunctions(context);
        }
        throw new EvaluationException("Invalid number of arguments: " + arguments.size()
                + " provided for the function: " + functionName);
    }

    private ExpressionResult handleNoArgumentFunctions() {
        if (functionName.equals(SynapseConstants.NOW)) {
            return new ExpressionResult(System.currentTimeMillis());
        }
        throw new EvaluationException("Invalid function: " + functionName + " with no arguments");
    }

    private ExpressionResult handleSingleArgumentFunctions(EvaluationContext context) {
        ExpressionResult result = null;
        // do not evaluate the source for exists function - since we need to catch the exception
        if (!functionName.equals(SynapseConstants.EXISTS)) {
            result = arguments.get(0).evaluate(context);
            checkArguments(result, "source");
        }
        switch (functionName) {
            case SynapseConstants.LENGTH:
                return handleLengthFunction(result);
            case SynapseConstants.TO_LOWER:
                return handleToLowerFunction(result);
            case SynapseConstants.TO_UPPER:
                return handleToUpperFunction(result);
            case SynapseConstants.TRIM:
                return handleTrimFunction(result);
            case SynapseConstants.ABS:
                return handleAbsFunction(result);
            case SynapseConstants.CEIL:
                return handleCeilFunction(result);
            case SynapseConstants.FLOOR:
                return handleFloorFunction(result);
            case SynapseConstants.ROUND:
                return handleRoundFunction(result);
            case SynapseConstants.SQRT:
                return handleSqrtFunction(result);
            case SynapseConstants.B64ENCODE:
                return handleBase64EncodeFunction(result);
            case SynapseConstants.B64DECODE:
                return handleBase64DecodeFunction(result);
            case SynapseConstants.URL_ENCODE:
                return handleUrlEncodeFunction(result);
            case SynapseConstants.URL_DECODE:
                return handleUrlDecodeFunction(result);
            case SynapseConstants.IS_STRING:
                return new ExpressionResult(result.isString());
            case SynapseConstants.IS_NUMBER:
                return new ExpressionResult(result.isInteger() || result.isDouble());
            case SynapseConstants.IS_ARRAY:
                return new ExpressionResult(result.isArray());
            case SynapseConstants.IS_OBJECT:
                return new ExpressionResult(result.isObject());
            case SynapseConstants.STRING:
                return new ExpressionResult(result.asString());
            case SynapseConstants.INTEGER:
                return handleIntegerConversion(result);
            case SynapseConstants.FLOAT:
                return handleFloatConversion(result);
            case SynapseConstants.BOOLEAN:
                return handleBooleanConversion(result);
            case SynapseConstants.REGISTRY:
                return handleRegistryAccess(context, result, null);
            case SynapseConstants.EXISTS:
                return handleExistsCheck(context, arguments.get(0));
            case SynapseConstants.OBJECT:
                return convertToObject(result);
            case SynapseConstants.ARRAY:
                return convertToArray(result);
            case SynapseConstants.XPATH:
                return evaluateXPATHExpression(context, result);
            case SynapseConstants.SECRET:
                return fetchSecretValue(context, result.asString());
            case SynapseConstants.NOT:
                return new ExpressionResult(!result.asBoolean());
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with one argument");
        }
    }

    private ExpressionResult handleDoubleArgumentFunctions(EvaluationContext context) {
        ExpressionResult source = arguments.get(0).evaluate(context);
        ExpressionResult argument1 = arguments.get(1).evaluate(context);
        checkArguments(source, "source");
        checkArguments(argument1, "argument1");
        switch (functionName) {
            case SynapseConstants.SUBSTRING:
                return handleSubstringFunction(source, argument1);
            case SynapseConstants.STARTS_WITH:
                return handleStartsWithFunction(source, argument1);
            case SynapseConstants.ENDS_WITH:
                return handleEndsWithFunction(source, argument1);
            case SynapseConstants.CONTAINS:
                return handleContainsFunction(source, argument1);
            case SynapseConstants.SPLIT:
                return handleSplitFunction(source, argument1);
            case SynapseConstants.POW:
                return handlePowFunction(source, argument1);
            case SynapseConstants.B64ENCODE:
                return handleBase64EncodeFunction(source, argument1);
            case SynapseConstants.URL_ENCODE:
                return handleUrlEncodeFunction(source, argument1);
            case SynapseConstants.REGISTRY:
                return handleRegistryAccess(context, source, argument1);
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with two arguments");
        }
    }

    private ExpressionResult handleTripleArgumentFunctions(EvaluationContext context) {
        ExpressionResult source = arguments.get(0).evaluate(context);
        ExpressionResult argument1 = arguments.get(1).evaluate(context);
        ExpressionResult argument2 = arguments.get(2).evaluate(context);
        checkArguments(source, "source");
        checkArguments(argument1, "argument1");
        checkArguments(argument2, "argument2");
        switch (functionName) {
            case SynapseConstants.SUBSTRING:
                return handleSubstringFunction(source, argument1, argument2);
            case SynapseConstants.REPLACE:
                return handleReplaceFunction(source, argument1, argument2);
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with three arguments");
        }
    }

    private void checkArguments(ExpressionResult result, String argumentName) {
        if (result == null || result.isNull()) {
            throw new EvaluationException("Null " + argumentName + " value provided for the function: " + functionName);
        }
    }

    private ExpressionResult handleLengthFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().length());
        } else if (result.isArray()) {
            return new ExpressionResult(result.asJsonElement().getAsJsonArray().size());
        }
        throw new EvaluationException("Invalid argument provided for length function");
    }

    private ExpressionResult handleToLowerFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().toLowerCase());
        } else if (result.isJsonPrimitive()) {
            return new ExpressionResult(new JsonPrimitive(result.asJsonElement().getAsString().toLowerCase()));
        }
        throw new EvaluationException("Invalid argument provided for toLower function");
    }

    private ExpressionResult handleToUpperFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().toUpperCase());
        } else if (result.isJsonPrimitive()) {
            return new ExpressionResult(new JsonPrimitive(result.asJsonElement().getAsString().toUpperCase()));
        }
        throw new EvaluationException("Invalid argument provided for toUpper function");
    }

    private ExpressionResult handleTrimFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().trim());
        }
        throw new EvaluationException("Invalid argument provided for trim function");
    }

    private ExpressionResult handleAbsFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(Math.abs(result.asInt()));
        } else if (result.isDouble()) {
            return new ExpressionResult(Math.abs(result.asDouble()));
        }
        throw new EvaluationException("Invalid argument provided for abs function");
    }

    private ExpressionResult handleCeilFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        } else if (result.isDouble()) {
            return new ExpressionResult(Math.ceil(result.asDouble()));
        }
        throw new EvaluationException("Invalid argument provided for ceil function");
    }

    private ExpressionResult handleFloorFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        } else if (result.isDouble()) {
            return new ExpressionResult(Math.floor(result.asDouble()));
        }
        throw new EvaluationException("Invalid argument provided for floor function");
    }

    private ExpressionResult handleRoundFunction(ExpressionResult result) {
        if (result.isDouble()) {
            return new ExpressionResult((int) Math.round(result.asDouble()));
        } else if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        }
        throw new EvaluationException("Invalid argument provided for round function");
    }

    private ExpressionResult handleSqrtFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(Math.sqrt(result.asInt()));
        } else if (result.isDouble()) {
            return new ExpressionResult(Math.sqrt(result.asDouble()));
        }
        throw new EvaluationException("Invalid argument provided for sqrt function");
    }

    private ExpressionResult handleBase64EncodeFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(Base64.getEncoder().encodeToString(result.asString().getBytes()));
        }
        throw new EvaluationException("Invalid argument provided for base64Encode function");
    }

    private ExpressionResult handleBase64DecodeFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(new String(Base64.getDecoder().decode(result.asString())));
        }
        throw new EvaluationException("Invalid argument provided for base64Decode function");
    }

    private ExpressionResult handleUrlEncodeFunction(ExpressionResult result) {
        if (result.isString()) {
            try {
                return new ExpressionResult(URLEncoder.encode(result.asString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("unsupported encoding provided for urlEncode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlEncode function");
    }

    private ExpressionResult handleUrlDecodeFunction(ExpressionResult result) {
        if (result.isString()) {
            try {
                return new ExpressionResult(java.net.URLDecoder.decode(result.asString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("unsupported encoding provided for urlDecode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlDecode function");
    }

    private ExpressionResult handleIntegerConversion(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        }
        try {
            return new ExpressionResult(Integer.parseInt(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for integer conversion");
        }
    }

    private ExpressionResult handleFloatConversion(ExpressionResult result) {
        if (result.isDouble()) {
            return new ExpressionResult(result.asDouble());
        }
        try {
            return new ExpressionResult(Double.parseDouble(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for float conversion");
        }
    }

    private ExpressionResult handleBooleanConversion(ExpressionResult result) {
        if (result.isBoolean()) {
            return new ExpressionResult(result.asBoolean());
        }
        try {
            return new ExpressionResult(Boolean.parseBoolean(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for boolean conversion");
        }
    }

    private ExpressionResult handleSubstringFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isInteger()) {
            if (source.asString().length() < argument1.asInt() || argument1.asInt() < 0) {
                throw new EvaluationException("Invalid index for subString: " + argument1.asInt()
                        + ", source string length: " + source.asString().length());
            }
            return new ExpressionResult(source.asString().substring(argument1.asInt()));
        }
        throw new EvaluationException("Invalid argument provided for subString function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleStartsWithFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().startsWith(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for startsWith function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleEndsWithFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().endsWith(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for endsWith function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleContainsFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().contains(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for contains function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleSplitFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            String[] splits = source.asString().split(argument1.asString());
            JsonArray jsonArray = new JsonArray();
            for (String split : splits) {
                jsonArray.add(split);
            }
            return new ExpressionResult(jsonArray);
        }
        throw new EvaluationException("Invalid argument provided for split function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handlePowFunction(ExpressionResult source, ExpressionResult argument1) {
        if ((source.isDouble() || source.isInteger()) && (argument1.isDouble() || argument1.isInteger())) {
            return new ExpressionResult(Math.pow(source.asDouble(), argument1.asDouble()));
        }
        throw new EvaluationException("Invalid argument provided for pow function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleBase64EncodeFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            try {
                return new ExpressionResult(Base64.getEncoder().encodeToString(source.asString()
                        .getBytes(ExpressionUtils.getCharset(argument1.asString()))));
            } catch (UnsupportedCharsetException e) {
                throw new EvaluationException("Invalid charset provided for base64Encode function. Charset: "
                        + argument1.asString());
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("Error encoding the string for base64Encode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for base64Encode function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleUrlEncodeFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            try {
                return new ExpressionResult(URLEncoder.encode(source.asString(), ExpressionUtils.getCharset(argument1.asString())));
            } catch (UnsupportedCharsetException e) {
                throw new EvaluationException("Invalid charset provided for urlEncode function. Charset: "
                        + argument1.asString());
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("Error encoding the string for urlEncode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlEncode function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleSubstringFunction(ExpressionResult source, ExpressionResult argument1, ExpressionResult argument2) {
        if (source.isString() && argument1.isInteger() && argument2.isInteger()) {
            if (argument2.asInt() < 0 || argument1.asInt() < 0 || argument1.asInt() >
                    argument2.asInt() || argument2.asInt() > source.asString().length()) {
                throw new EvaluationException("Invalid subString indices: start=" + argument1.asInt()
                        + ", end=" + argument2.asInt() + ", string length=" + source.asString().length());
            }
            return new ExpressionResult(source.asString().substring(argument1.asInt(), argument2.asInt()));
        }
        throw new EvaluationException("Invalid argument provided for subString function. source: " + source.asString()
                + ", argument1: " + argument1.asString() + ", argument2: " + argument2.asString());
    }

    private ExpressionResult handleReplaceFunction(ExpressionResult source, ExpressionResult argument1,
                                                   ExpressionResult argument2) {
        if (source.isString() && argument1.isString() && argument2.isString()) {
            return new ExpressionResult(source.asString().replace(argument1.asString(), argument2.asString()));
        }
        throw new EvaluationException("Invalid argument provided for replace function. source: " + source.asString()
                + ", argument1: " + argument1.asString() + ", argument2: " + argument2.asString());
    }

    private ExpressionResult handleRegistryAccess(EvaluationContext ctx, ExpressionResult regKey,
                                                  ExpressionResult propKey) {
        if (regKey.isString()) {
            if (propKey != null && propKey.isString()) {
                String prop = ctx.getRegistryResourceProperty(regKey.asString(), propKey.asString());
                if (prop != null) {
                    return new ExpressionResult(prop);
                }
                throw new EvaluationException("Could not find the property: " + propKey.asString()
                        + " in the registry resource: " + regKey.asString());
            } else {
                Object resource;
                try {
                    resource = ctx.getRegistryResource(regKey.asString());
                } catch (UnsupportedEncodingException e) {
                    throw new EvaluationException("Error retrieving the registry resource: " + regKey.asString());
                }
                if (resource != null) {
                    return new ExpressionResult(resource.toString());
                }
                throw new EvaluationException("Could not find the registry resource: " + regKey.asString());
            }
        }
        throw new EvaluationException("Invalid argument provided for registry function. regKey: " + regKey.asString()
                + ", propKey: " + propKey.asString());
    }

    private ExpressionResult handleExistsCheck(EvaluationContext context, ExpressionNode expression) {
        try {
            ExpressionResult result = expression.evaluate(context);
            return result != null ? new ExpressionResult(true) : new ExpressionResult(false);
        } catch (EvaluationException e) {
            // this is the only method we are handling the exceptions
            return new ExpressionResult(false);
        }
    }

    private ExpressionResult convertToObject(ExpressionResult result) {
        if (result.isObject()) {
            return new ExpressionResult(result.asJsonObject());
        }
        throw new EvaluationException("Argument cannot be converted to a JSON object");
    }

    private ExpressionResult convertToArray(ExpressionResult result) {
        if (result.isArray()) {
            return new ExpressionResult(result.asJsonArray());
        }
        throw new EvaluationException("Argument cannot be converted to a JSON array");
    }

    private ExpressionResult evaluateXPATHExpression(EvaluationContext context, ExpressionResult expression) {
        try {
            return new ExpressionResult(context.evaluateXpathExpression(expression.asString()));
        } catch (JaxenException e) {
            throw new EvaluationException("Invalid XPATH expression : " + expression.asString());
        }
    }

    private ExpressionResult fetchSecretValue(EvaluationContext context, String expression) {
        try {
            String result = context.fetchSecretValue(expression);
            // if vault-lookup fails it just return the same expression as the result
            if (result.startsWith(SynapseConstants.VAULT_LOOKUP)) {
                throw new EvaluationException("Error fetching secret value for alias: " + expression);
            }
            return new ExpressionResult(result);
        } catch (JaxenException e) {
            throw new EvaluationException("Error fetching secret value for alias: " + expression);
        }
    }
}
