package com.fasterxml.jackson.datatype.primitive_collections_base.ser.map;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author yawkat
 */
public abstract class RefRefMapSerializer<T> extends StdContainerSerializer<T>
{
    private final JavaType _type;
    private final JavaType _keyType, _valueType;

    protected final ValueSerializer<Object> _keySerializer;
    private final TypeSerializer _valueTypeSerializer;
    protected final ValueSerializer<Object> _valueSerializer;

    /**
     * Set of entries to omit during serialization, if any
     */
    protected final Set<String> _ignoredEntries;

    public RefRefMapSerializer(
            JavaType type, Class<? super T> mapClass,
            ValueSerializer<Object> keySerializer, TypeSerializer vts, ValueSerializer<Object> valueSerializer,
            Set<String> ignoredEntries
    ) {
        super(type, null);
        _type = type;
        // Assumes that the map class has first two type parameters corresponding to the key and the
        // value type.
        JavaType[] typeParameters = type.findTypeParameters(mapClass);
        JavaType keyType = (typeParameters.length > 0) ? typeParameters[0] : TypeFactory.unknownType();
        JavaType valueType = (typeParameters.length > 1) ? typeParameters[1] : TypeFactory.unknownType();
        _keyType = keyType;
        _valueType = valueType;
        _keySerializer = keySerializer;
        _valueTypeSerializer = vts;
        _valueSerializer = valueSerializer;
        _ignoredEntries = ignoredEntries;
    }

    @SuppressWarnings("unchecked")
    protected RefRefMapSerializer(
            RefRefMapSerializer<?> src, BeanProperty property,
            ValueSerializer<?> keySerializer, TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Set<String> ignoredEntries
    ) {
        super(src, property);
        _type = src._type;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _keySerializer = (ValueSerializer<Object>) keySerializer;
        _valueTypeSerializer = vts;
        _valueSerializer = (ValueSerializer<Object>) valueSerializer;
        _dynamicValueSerializers = src._dynamicValueSerializers;
        _ignoredEntries = ignoredEntries;
    }

    protected abstract RefRefMapSerializer<?> withResolved(
            BeanProperty property,
            ValueSerializer<?> keySer, TypeSerializer vts, ValueSerializer<?> valueSer,
            Set<String> ignored
    );

    /*
    /**********************************************************
    /* Post-processing (contextualization)
    /**********************************************************
     */

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
    {
        ValueSerializer<?> valueSer = _valueSerializer;
        if (valueSer == null) { // if type is final, can actually resolve:
            JavaType valueType = getContentType();
            if (valueType.isFinal()) {
                valueSer = provider.findContentValueSerializer(valueType, property);
            }
        } else {
            valueSer = valueSer.createContextual(provider, property);
        }

        final SerializationConfig config = provider.getConfig();
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final AnnotatedMember propertyAcc = (property == null) ? null : property.getMember();
        ValueSerializer<?> keySer = null;

        // First: if we have a property, may have property-annotation overrides
        if (propertyAcc != null && intr != null) {
            Object serDef = intr.findKeySerializer(config, propertyAcc);
            if (serDef != null) {
                keySer = provider.serializerInstance(propertyAcc, serDef);
            }
            serDef = intr.findContentSerializer(config, propertyAcc);
            if (serDef != null) {
                valueSer = provider.serializerInstance(propertyAcc, serDef);
            }
        }
        if (valueSer == null) {
            valueSer = _valueSerializer;
        }
        // [datatype-guava#124]: May have a content converter
        valueSer = findContextualConvertingSerializer(provider, property, valueSer);
        if (valueSer == null) {
            // One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            JavaType valueType = getContentType();
            if (valueType.useStaticType()) {
                valueSer = provider.findContentValueSerializer(valueType, property);
            }
        } else {
            valueSer = provider.handleSecondaryContextualization(valueSer, property);
        }
        if (keySer == null) {
            keySer = _keySerializer;
        }
        if (keySer == null) {
            keySer = provider.findKeySerializer(getKeyType(), property);
        } else {
            keySer = provider.handleSecondaryContextualization(keySer, property);
        }
        // finally, TypeSerializers may need contextualization as well
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(provider, property);
        }

        Set<String> ignored = _ignoredEntries;

        if (intr != null && propertyAcc != null) {
            JsonIgnoreProperties.Value ignorals = intr.findPropertyIgnoralByName(config, propertyAcc);
            if (ignorals != null) {
                Set<String> newIgnored = ignorals.findIgnoredForSerialization();
                if ((newIgnored != null) && !newIgnored.isEmpty()) {
                    ignored = (ignored == null) ? new HashSet<>() : new HashSet<>(ignored);
                    ignored.addAll(newIgnored);
                }
            }
        }
        return withResolved(property, keySer, typeSer, valueSer, ignored);
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return _valueSerializer;
    }

    protected JavaType getKeyType() {
        return _keyType;
    }

    @Override
    public JavaType getContentType() {
        return _valueType;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.writeStartObject(value);
        if (!isEmpty(provider, value)) {
            serializeFields(value, gen, provider);
        }
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(T value, JsonGenerator gen,
            SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        gen.assignCurrentValue(value);
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, ctxt,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        if (!isEmpty(ctxt, value)) {
            serializeFields(value, gen, ctxt);
        }
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    protected abstract void forEachKeyValue(T value, BiConsumer<Object, Object> action);

    private void serializeFields(T value, JsonGenerator gen, SerializerProvider provider) {
        Set<String> ignored = _ignoredEntries;
        forEachKeyValue(value, (key, v) -> {
            // First, serialize key
            if ((ignored != null) && ignored.contains(key)) {
                return;
            }
            if (key == null) {
                provider.findNullKeySerializer(getKeyType(), _property)
                        .serialize(null, gen, provider);
            } else {
                _keySerializer.serialize(key, gen, provider);
            }
            if (v == null) {
                provider.defaultSerializeNullValue(gen);
                return;
            }
            ValueSerializer<Object> valueSer = _valueSerializer;
            if (valueSer == null) {
                valueSer = _findSerializer(provider, v);
            }
            if (_valueTypeSerializer == null) {
                valueSer.serialize(v, gen, provider);
            } else {
                valueSer.serializeWithType(v, gen, provider, _valueTypeSerializer);
            }
        });
    }

    private ValueSerializer<Object> _findSerializer(SerializerProvider ctxt,
        Object value)
    {
        final Class<?> cc = value.getClass();
        ValueSerializer<Object> valueSer = _dynamicValueSerializers.serializerFor(cc);
        if (valueSer != null) {
            return valueSer;
        }
        if (_valueType.hasGenericTypes()) {
            return _findAndAddDynamic(ctxt,
                    ctxt.constructSpecializedType(_valueType, cc));
        }
        return _findAndAddDynamic(ctxt, cc);
    }
}

