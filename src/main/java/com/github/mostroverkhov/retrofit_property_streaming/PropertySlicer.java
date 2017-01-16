package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.github.mostroverkhov.retrofit_property_streaming.model.PropType;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 13.06.2016.
 */
public class PropertySlicer<T> {

    private final Type propTarget;
    private final Gson gson;
    private final JsonReader jsonStream;

    private final PropertyContext propertyContext;

    public PropertySlicer(Type propTarget, Gson gson, JsonReader jsonStream) throws IOException {
        this.propTarget = propTarget;
        this.gson = gson;
        this.jsonStream = jsonStream;

        this.propertyContext = new PropertyContext();
        this.propertyContext.setRoot(true);
    }

    /**
     * @return next property. It is an error to call {@link #nextProp()} after property with
     * type {@link PropType#DOC_END} was returned
     */
    public Prop<T> nextProp() {
        try {
            return getNextProp();
        } catch (IOException e) {
            throw new IllegalStateException("Error while parsing json stream ", e);
        }
    }

    private PropertyContext context() {
        return propertyContext;
    }

    private Prop<T> getNextProp() throws IOException {

        PropertyContext context = context();
        boolean isDocEnd = context.isDocEnd();
        if (isDocEnd) {
            throw new IllegalStateException("getNextProp() called DOC_END notification");
        }
        boolean isRoot = context.isRoot();
        propertyContext.setRoot(false);
        TokenAndName tokenAndName = advanceJsonStream(jsonStream, isRoot);
        if (tokenAndName.isEmpty()) {
            return getNextProp();
        } else {
            ResolveResult<T> resolveResult = resolveProperty(tokenAndName);
            if (resolveResult.isEnd()) {
                context.setDocEnd(true);
                return new Prop<>(PropType.DOC_END);
            }
            /*proceed to next prop if there is no field with given name in target type*/
            if (resolveResult.hasProperty()) {
                return resolveResult.getProp();
            } else {
                return getNextProp();
            }
        }
    }

    private ResolveResult<T> resolveProperty(TokenAndName tokenAndName) throws IOException {
        JsonToken propValue = tokenAndName.getToken();
        String propName = tokenAndName.getName();

        PropertyConsumer propertyConsumer;
        switch (propValue) {
            case BEGIN_ARRAY:
                propertyConsumer = PropertyConsumer.ARR_START;
                propertyContext.setArrayType();
                break;
            case END_ARRAY:
                propertyConsumer = PropertyConsumer.ARR_END;
                propertyContext.setPropType();
                break;
            case BEGIN_OBJECT:
            case NUMBER:
            case BOOLEAN:
            case STRING:
            case NULL:
                propertyConsumer = propertyContext.isArray()
                        ? PropertyConsumer.ARR
                        : PropertyConsumer.OBJECT_OR_PRIMITIVE;
                break;
            case END_DOCUMENT:
                return ResolveResult.end();
            default:
                throw new IllegalStateException("Unexpected token: " + propValue);
        }
        return ResolveResult.prop(propertyConsumer.resolve(propName, propTarget, this));
    }

    /**
     * only tokens interesting for clients (defined in {@link PropType}) are returned
     */
    private TokenAndName advanceJsonStream(JsonReader jsonStream, boolean isRoot) throws IOException {

        JsonToken token = jsonStream.peek();
        /*property with name*/
        if (token == JsonToken.NAME) {
            String propName = jsonStream.nextName();
            token = jsonStream.peek();
            return TokenAndName.tokenAndName(token, propName);
            /*proceed to next if root object; if root is array, it is consumed by ARR_START prop consumer*/
        } else if (token == JsonToken.BEGIN_OBJECT && isRoot) {
            jsonStream.beginObject();
            return TokenAndName.empty();
            /*proceed to next - clients are not interested in END_OBJECT events*/
        } else if (token == JsonToken.END_OBJECT) {
            jsonStream.endObject();
            return TokenAndName.empty();
        } else {
            return TokenAndName.token(token);
        }
    }

    private static class ResolveResult<T> {
        private final Prop<T> prop;
        private final boolean isEnd;

        private ResolveResult(Prop<T> prop, boolean isEnd) {
            this.prop = prop;
            this.isEnd = isEnd;
        }

        public static <T> ResolveResult<T> prop(Prop<T> prop) {
            return new ResolveResult<T>(prop, false);
        }

        public static <T> ResolveResult<T> end() {
            return new ResolveResult<>(null, true);
        }

        public Prop<T> getProp() {
            return prop;
        }

        public boolean isEnd() {
            return isEnd;
        }

        public boolean hasProperty() {
            return prop != null;
        }
    }

    private static class TokenAndName {
        private static final TokenAndName EMPTY = new TokenAndName(null, "");
        private final JsonToken token;
        private final String propName;

        public static TokenAndName tokenAndName(JsonToken toekn, String propName) {
            return new TokenAndName(toekn, propName);
        }

        public static TokenAndName token(JsonToken token) {
            return new TokenAndName(token, "");
        }

        public static TokenAndName empty() {
            return EMPTY;
        }

        private TokenAndName(JsonToken token, String propName) {
            this.token = token;
            this.propName = propName;
        }

        public JsonToken getToken() {
            return token;
        }

        public String getName() {
            return propName;
        }

        public boolean isEmpty() {
            return token == null && Utils.isEmpty(propName);
        }
    }

    private static class PropertyContext {

        private volatile boolean isArray;
        private volatile ContextState contextState = ContextState.empty();
        private volatile boolean isRoot;
        private volatile boolean isDocEnd;

        public void clearNameAndType() {
            contextState = ContextState.empty();
        }

        public void setItemNameAndType(String itemName, Type itemType) {
            contextState = ContextState.state(itemName, itemType);
        }

        public boolean isDocEnd() {
            return isDocEnd;
        }

        public void setDocEnd(boolean docEnd) {
            isDocEnd = docEnd;
        }

        public boolean isRoot() {
            return isRoot;
        }

        public void setRoot(boolean root) {
            isRoot = root;
        }

        public void setArrayType() {
            isArray = true;
        }

        public void setPropType() {
            isArray = false;
        }

        public boolean isArray() {
            return isArray;
        }

        public String getItemName() {
            return contextState.getItemName();
        }

        public Type getItemType() {
            return contextState.getItemType();
        }
    }

    private static class ContextState {

        private static final ContextState CONTEXT_STATE = new ContextState(null, null);

        private final String itemName;
        private final Type itemType;

        public static ContextState empty() {
            return CONTEXT_STATE;
        }

        public static ContextState state(String itemName, Type itemType) {
            return new ContextState(itemName, itemType);
        }

        private ContextState(String itemName, Type itemType) {
            this.itemName = itemName;
            this.itemType = itemType;
        }

        public String getItemName() {
            return itemName;
        }

        public Type getItemType() {
            return itemType;
        }
    }

    private Gson gson() {
        return gson;
    }

    private JsonReader jsonStream() {
        return jsonStream;
    }

    private enum PropertyConsumer {

        OBJECT_OR_PRIMITIVE {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                Gson gson = calculator.gson();
                JsonReader jsonStream = calculator.jsonStream();
                Type fieldType = getFieldTypeForTarget(targetType, propName);
                /*field is not present in target type*/
                if (fieldType == null) {
                    jsonStream.skipValue();
                    return null;
                } else {
                    return new Prop<>(propName, gson.fromJson(jsonStream, fieldType), PropType.PROPERTY);
                }
            }
        },

        ARR_START {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {

                JsonReader jsonStream = calculator.jsonStream();

                Type listParamType;
                /*root is array*/
                if (propName == null) {
                    listParamType = getListParameterTypeOrNull(targetType);
                /*property is array*/
                } else {
                    Class<?> rootRawType = Utils.getRawType(targetType);
                    try {
                        Type fieldType = rootRawType.getDeclaredField(propName).getGenericType();
                        listParamType = getListParameterTypeOrNull(fieldType);
                    } catch (NoSuchFieldException e) {
                        /*no field: skip array and return no property special value*/
                        jsonStream.skipValue();
                        return null;
                    }
                }

                if (listParamType == null) {
                    throw new IllegalArgumentException("Target type is expected to " +
                            "be parametrized and implement Collection<T>");
                }
                PropertyContext context = calculator.context();
                context.setItemNameAndType(propName, listParamType);

                jsonStream.beginArray();
                return new Prop<>(propName, null, PropType.ARR_START);
            }

            private Type getListParameterTypeOrNull(Type targetType) {
                if (targetType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) targetType;
                    Type rawType = parameterizedType.getRawType();
                    Type[] genericArgs = parameterizedType.getActualTypeArguments();
                    if (Collection.class.isAssignableFrom(asClass(rawType))) {
                        return genericArgs.length == 0 ? null : genericArgs[0];
                    }
                }
                return null;
            }

            private Class<?> asClass(Type rawType) {
                return (Class<?>) rawType;
            }
        },

        ARR_END {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                PropertyContext context = calculator.context();
                String name = context.getItemName();
                context.clearNameAndType();

                calculator.jsonStream().endArray();
                return new Prop<>(name, null, PropType.ARR_END);
            }
        },

        ARR {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                PropertyContext context = calculator.context();
                Type itemType = context.getItemType();
                String itemName = context.getItemName();
                if (itemName == null || itemType == null) {
                    throw new AssertionError("Internal error: itemName and itemType were not set for array item:ARR");
                }
                Object item = calculator.gson().fromJson(calculator.jsonStream(), itemType);

                return new Prop<>(itemName, item, PropType.ARR_PROPERTY);
            }
        };

        private static Type getFieldTypeForTarget(Type targetType, String name) {
            Class<?> rootRawType = Utils.getRawType(targetType);
            try {
                return rootRawType.getDeclaredField(name).getType();
            } catch (NoSuchFieldException e) {
                return null;
            }
        }

        /**
         * @param propName   property name for target type
         * @param targetType type for which properties are resolved
         * @return property, or null if target type does not contain field with name propName
         * @throws IOException
         */
        abstract <T> Prop<T> resolve(String propName,
                                     Type targetType,
                                     PropertySlicer<T> calculator) throws IOException;

    }
}
