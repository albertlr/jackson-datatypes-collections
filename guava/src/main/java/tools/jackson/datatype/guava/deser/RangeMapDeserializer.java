package tools.jackson.datatype.guava.deser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.impl.NullsConstantProvider;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.util.ClassUtil;

/**
 * Jackson deserializer for a Guava {@link RangeMap}.
 * <p>
 * Only string serializable ranges are supported at this time.
 *
 * @author mcvayc
 */
public class RangeMapDeserializer<T extends RangeMap<Comparable<?>, Object>>
        extends StdDeserializer<T>
{
    private static final List<String> METHOD_NAMES = ImmutableList.of("copyOf", "create");
    private final MapLikeType type;
    private final KeyDeserializer keyDeserializer;
    private final TypeDeserializer elementTypeDeserializer;
    private final ValueDeserializer<?> elementDeserializer;

    private final NullValueProvider nullProvider;
    private final boolean isImmutable;
    private final boolean skipNullValues;

    /**
     * Since we have to use a method to transform from a known range-map type into actual one, we'll
     * resolve method just once, use it. Note that if this is set to null, we can just construct a
     * {@link TreeRangeMap} instance and be done with it.
     */
    private final Method creatorMethod;

    public RangeMapDeserializer(MapLikeType type, KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer,
            boolean isImmutable
    ) {
        this(type, keyDeserializer, elementTypeDeserializer, elementDeserializer,
                findTransformer(type.getRawClass()), null, isImmutable);
    }

    public RangeMapDeserializer(MapLikeType type, KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer,
            Method creatorMethod, NullValueProvider nvp, boolean isImmutable) {
        super(type);
        this.type = type;
        this.keyDeserializer = keyDeserializer;
        this.elementTypeDeserializer = elementTypeDeserializer;
        this.elementDeserializer = elementDeserializer;
        this.creatorMethod = creatorMethod;
        this.nullProvider = nvp;
        this.isImmutable = isImmutable;
        skipNullValues = (nvp == null) ? false : NullsConstantProvider.isSkipper(nvp);
    }

    private static Method findTransformer(Class<?> rawType) {
        if (rawType == TreeRangeMap.class) {
            return null;
        }

        // First, check type itself for matching methods
        for (String methodName : METHOD_NAMES) {
            try {
                Method m = rawType.getDeclaredMethod(methodName, RangeMap.class);
                if (m != null) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
            }
        }

        // If not working, possibly super types too (should we?)
        for (String methodName : METHOD_NAMES) {
            try {
                Method m = rawType.getMethod(methodName, RangeMap.class);
                if (m != null) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
            }
        }

        return null;
    }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Map;
    }

    /**
     * We need to use this method to properly handle possible contextual variants of key and value
     * deserializers, as well as type deserializers.
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
        throws JacksonException
    {
        KeyDeserializer kd = keyDeserializer;
        if (kd == null) {
            kd = ctxt.findKeyDeserializer(type.getKeyType(), property);
        }
        ValueDeserializer<?> valueDeser = elementDeserializer;
        final JavaType vt = type.getContentType();
        if (valueDeser == null) {
            valueDeser = ctxt.findContextualValueDeserializer(vt, property);
        } else { // if directly assigned, probably not yet contextual, so:
            valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
        }
        // Type deserializer is slightly different; must be passed, but needs to become contextual:
        TypeDeserializer vtd = elementTypeDeserializer;
        if (vtd != null) {
            vtd = vtd.forProperty(property);
        }

        return new RangeMapDeserializer<>(type, kd, vtd, valueDeser, creatorMethod,
                findContentNullProvider(ctxt, property, valueDeser), isImmutable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        RangeMap rangeMap = TreeRangeMap.create();

        JsonToken currToken = p.currentToken();
        if (currToken != JsonToken.PROPERTY_NAME) {
            if (currToken != JsonToken.END_OBJECT) {
                expect(ctxt, JsonToken.START_OBJECT, currToken);
                currToken = p.nextToken();
            }
        }

        for (; currToken == JsonToken.PROPERTY_NAME; currToken = p.nextToken()) {
            final Range<Comparable<?>> key = (Range<Comparable<?>>) keyDeserializer.deserializeKey(p.currentName(), ctxt);

            p.nextToken();
            final Object value;
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                if (skipNullValues) {
                    continue;
                }
                value = nullProvider.getNullValue(ctxt);
            } else if (elementTypeDeserializer != null) {
                value = elementDeserializer.deserializeWithType(p, ctxt, elementTypeDeserializer);
            } else {
                value = elementDeserializer.deserialize(p, ctxt);
            }
            rangeMap.put(key, value);
        }

        if (creatorMethod == null) {
            return (T) rangeMap;
        }
        try {
            T map = (T) creatorMethod.invoke(null, rangeMap);
            return map;
        } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
            T result = (T) ctxt.handleInstantiationProblem(handledType(), rangeMap, e);
            return result;
        }
    }

    private void expect(DeserializationContext context,
            JsonToken expected, JsonToken actual)
    {
        if (actual != expected) {
            context.reportInputMismatch(this, String.format("Problem deserializing %s: expecting %s, found %s",
                    ClassUtil.getTypeDescription(getValueType()), expected, actual));
        }
    }
}
