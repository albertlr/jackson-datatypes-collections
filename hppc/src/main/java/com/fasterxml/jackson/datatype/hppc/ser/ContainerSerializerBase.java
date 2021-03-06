package com.fasterxml.jackson.datatype.hppc.ser;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;

/**
 * Base class for various container (~= Collection) serializers.
 */
public abstract class ContainerSerializerBase<T>
    extends StdContainerSerializer<T>
{
    protected final String _schemeElementType;

    protected ContainerSerializerBase(Class<T> type, String schemaElementType)
    {
        super(type);
        _schemeElementType = schemaElementType;
    }

    protected ContainerSerializerBase(JavaType type, String schemaElementType)
    {
        super(type, null);
        _schemeElementType = schemaElementType;
    }

    protected ContainerSerializerBase(ContainerSerializerBase<?> src) {
        super(src._handledType);
        _schemeElementType = src._schemeElementType;
    }

    /*
    /**********************************************************
    /* Simple accessor overrides, defaults
    /**********************************************************
     */

    @Override
    public abstract boolean isEmpty(SerializerProvider provider, T value);

    @Override
    public ValueSerializer<?> getContentSerializer() {
        // We are not delegating, for most part, so while not dynamic claim we don't have it
        return null;
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        // May or may not be supportable, but for now fail loudly, not quietly
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaType getContentType() {
        // Not sure how to efficiently support this; could resolve types of course
        return null;
    }

    @Override
    public abstract void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint);

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.assignCurrentValue(value);
        gen.writeStartArray();
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    protected abstract void serializeContents(T value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException;
    
    @Override
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        gen.assignCurrentValue(value);
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        serializeContents(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */
    
    protected ValueSerializer<?> getSerializer(JavaType type)
    {
        if (_handledType.isAssignableFrom(type.getRawClass())) {
            return this;
        }
        return null;
    }
}
