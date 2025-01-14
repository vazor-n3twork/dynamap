/*
    Copyright 2017 N3TWORK INC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.n3twork.dynamap.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableDefinition {

    private static final String DEFAULT_SCHEMA_VERSION_FIELD = "_schv";

    private final String tableName;
    private final String description;
    private final String packageName;
    private final String type;
    private final String hashKey;
    private final String rangeKey;
    private final int version;
    private final List<Type> types;
    private final List<Index> globalSecondaryIndexes;
    private final List<Index> localSecondaryIndexes;
    private final boolean optimisticLocking;
    private final String schemaVersionField;
    private final boolean enableMigrations;
    private final List<PersistAsFieldItem> persistAsFieldItems;
    private final List<CompressCollectionItem> compressCollectionItems;

    @JsonCreator
    public TableDefinition(@JsonProperty("table") String tableName, @JsonProperty("description") String description, @JsonProperty("package") String packageName, @JsonProperty("type") String type, @JsonProperty("hashKey") String hashKey, @JsonProperty("rangeKey") String rangeKey,
                           @JsonProperty("version") int version, @JsonProperty("types") List<Type> types, @JsonProperty("globalSecondaryIndexes") List<Index> globalSecondaryIndexes, @JsonProperty("localSecondaryIndexes") List<Index> localSecondaryIndexes, @JsonProperty("optimisticLocking") boolean optimisticLocking,
                           @JsonProperty("schemaVersionField") String schemaVersionField, @JsonProperty("enableMigrations") Boolean enableMigrations) {
        this.tableName = tableName;
        this.description = description;
        this.packageName = packageName;
        this.type = type;
        this.hashKey = hashKey;
        this.rangeKey = rangeKey;
        this.version = version;
        this.types = types;
        this.globalSecondaryIndexes = globalSecondaryIndexes;
        this.localSecondaryIndexes = localSecondaryIndexes;
        this.optimisticLocking = optimisticLocking;
        this.schemaVersionField = schemaVersionField == null ? DEFAULT_SCHEMA_VERSION_FIELD : schemaVersionField;
        this.enableMigrations = enableMigrations == null ? Boolean.TRUE : enableMigrations;
        this.persistAsFieldItems = buildPersistAsListFields();
        this.compressCollectionItems = buildCompressFields();
    }

    public String getTableName() {
        return tableName;
    }

    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public String getTableName(String prefix) {
        return getTableName(prefix, null);
    }

    @JsonIgnore
    public String getTableName(String prefix, String suffix) {
        String fullTableName = "";
        if (prefix != null) {
            fullTableName = prefix + tableName;
        }
        if (suffix != null) {
            fullTableName += suffix;
        }

        return fullTableName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getType() {
        return type;
    }

    public String getHashKey() {
        return hashKey;
    }

    public String getRangeKey() {
        return rangeKey;
    }

    public int getVersion() {
        return version;
    }

    public List<Type> getTypes() {
        return types;
    }

    public List<Index> getGlobalSecondaryIndexes() {
        return globalSecondaryIndexes;
    }

    public List<Index> getLocalSecondaryIndexes() {
        return localSecondaryIndexes;
    }

    public boolean isOptimisticLocking() {
        return optimisticLocking;
    }

    public String getSchemaVersionField() {
        return schemaVersionField;
    }

    public boolean isEnableMigrations() {
        return enableMigrations;
    }

    public Field getField(String fieldName) {
        Type tableType = getTypes().stream().filter(t -> t.getName().equals(getType())).findFirst().get();
        return tableType.getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().get();
    }

    public Type getFieldType(String type) {
        return getTypes().stream().filter(t -> t.getName().equals(type)).findFirst().get();

    }

    /**
     * @return The TTL field for this table, or Optional.EMPTY if none is defined.
     */
    public Optional<Field> getTtlField() {
        Type tableType = getTypes().stream().filter(t -> t.getName().equals(getType())).findFirst().get();
        return tableType.getFields().stream().filter(Field::isTtl).findFirst();
    }

    @JsonIgnore
    public List<PersistAsFieldItem> getPersistAsFieldItems() {
        return persistAsFieldItems;
    }

    @JsonIgnore
    public List<CompressCollectionItem> getCompressCollectionItems() {
        return compressCollectionItems;
    }

    private List<PersistAsFieldItem> buildPersistAsListFields() {
        List<PersistAsFieldItem> paths = new ArrayList<>();
        Set<String> typeNames = getTypes().stream().map(t -> t.getName()).collect(Collectors.toSet());

        Type rootType = getTypes().stream().filter(t -> t.getName().equals(getType())).findFirst().get();
        paths.addAll(getPersistAsFieldItem(rootType, null));

        for (Field field : rootType.getFields()) {
            if (typeNames.contains(field.getType())) {
                paths.addAll(getPersistAsFieldItem(getFieldType(field.getType()), field.getDynamoName()));
            }
        }
        return paths;
    }

    private List<CompressCollectionItem> getCompressFieldItem(Type type, String parentKey) {
        List<CompressCollectionItem> items = new ArrayList<>();
        for (Field field : type.getFields()) {
            if (field.isCompressCollection()) {
                items.add(new CompressCollectionItem(parentKey, field.getDynamoName()));
            }
        }
        return items;
    }

    private List<CompressCollectionItem> buildCompressFields() {
        List<CompressCollectionItem> paths = new ArrayList<>();
        Set<String> typeNames = getTypes().stream().map(t -> t.getName()).collect(Collectors.toSet());

        Type rootType = getTypes().stream().filter(t -> t.getName().equals(getType())).findFirst().get();
        paths.addAll(getCompressFieldItem(rootType, null));

        for (Field field : rootType.getFields()) {
            if (typeNames.contains(field.getType())) {
                paths.addAll(getCompressFieldItem(getFieldType(field.getType()), field.getDynamoName()));
            }
        }
        return paths;
    }

    private List<PersistAsFieldItem> getPersistAsFieldItem(Type type, String parentKey) {
        List<PersistAsFieldItem> items = new ArrayList<>();
        for (Field field : type.getFields()) {
            if (field.isSerializeAsList()) {
                items.add(new PersistAsFieldItem(parentKey, field.getDynamoName(), field.getSerializeAsListElementId()));
            }
        }
        return items;
    }

    public static class PersistAsFieldItem {

        public PersistAsFieldItem(String parentKey, String itemKey, String idKey) {
            this.parentKey = parentKey;
            this.itemKey = itemKey;
            this.idKey = idKey;
        }

        public final String parentKey;
        public final String itemKey;
        public final String idKey;
    }

    public static class CompressCollectionItem {

        public CompressCollectionItem(String parentKey, String itemKey) {
            this.parentKey = parentKey;
            this.itemKey = itemKey;
        }

        public final String parentKey;
        public final String itemKey;
    }

    // Ideally, we would do validation using a strict JSON Schema. But until we add something like that, it's
    // important to catch critical errors in code.
    public void validate() {
        List<Field> ttlFields = types.stream().map(t -> t.getFields()).flatMap(List::stream).filter(Field::isTtl).collect(Collectors.toList());
        if (ttlFields.size() > 1) {
            String msg = String.format("Table %s has %d ttl fields defined. At most one is allowed.", this.getTableName(), ttlFields.size());
            throw new IllegalArgumentException(msg);
        }

        types.forEach(t -> {
            Set<String> validFields = t.getFields().stream().map(Field::getName).collect(Collectors.toSet());

            if (null != t.getEqualsFields()) {
                Set<String> missingEqualsFields = t.getEqualsFields().stream().filter(f -> !validFields.contains(f)).collect(Collectors.toSet());
                if (!missingEqualsFields.isEmpty()) {
                    String msg = String.format("Table %s, type %s, has invalid equals fields: %s.", this.getTableName(), t.getName(), missingEqualsFields);
                    throw new IllegalArgumentException(msg);
                }
            }

            if (null != t.getHashCodeFields()) {
                Set<String> missingHashCodeFields = t.getHashCodeFields().stream().filter(f -> !validFields.contains(f)).collect(Collectors.toSet());
                if (!missingHashCodeFields.isEmpty()) {
                    String msg = String.format("Table %s, type %s, has invalid hashCode fields: %s.", this.getTableName(), t.getName(), missingHashCodeFields);
                    throw new IllegalArgumentException(msg);
                }
            }
        });
    }
}
