// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.dataset.row;

import static java.util.stream.Collectors.joining;
import static org.talend.dataprep.api.dataset.row.FlagNames.INTERNAL_PROPERTY_PREFIX;
import static org.talend.dataprep.api.dataset.row.FlagNames.TDP_INVALID;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.type.Type;

/**
 * A DataSetRow is a row of a dataset. Values in data set row are <b>alphabetically</b> ordered by name.
 */
public class DataSetRow implements Cloneable, Serializable {

    /**
     * <p>
     * Filter for {@link #toArray(Predicate[])} that filters out TDP_ID column in results.
     * </p>
     * <p>
     * Example:<br/>
     * <code>
     *      String[] filteredValues = row.toArray(DataSetRow.SKIP_TDP_ID);
     * </code>
     * </p>
     */
    public static final Predicate<Map.Entry<String, String>> SKIP_TDP_ID = e -> !FlagNames.TDP_ID.equals(e.getKey());

    /** Metadata information (columns...) about this DataSetRow */
    private RowMetadata rowMetadata;

    /** Values of the dataset row. */
    private Map<String, String> values = new TreeMap<>();

    /** True if this row is deleted. */
    private boolean deleted;

    /** the old value used for the diff. */
    private DataSetRow oldValue;

    /** Row id */
    private Long rowId;

    /** A structure to speed up invalid related operations */
    private final Set<String> invalidColumnIds = new HashSet<>();

    /**
     * Constructor with values.
     */
    public DataSetRow(RowMetadata rowMetadata) {
        this.rowMetadata = rowMetadata.clone();
        this.deleted = false;
    }

    /**
     * Constructor with values.
     *
     * @param values the row value.
     */
    public DataSetRow(RowMetadata rowMetadata, Map<String, ?> values) {
        this(rowMetadata);
        initFromValues(values);
    }

    public DataSetRow(Map<String, ?> values) {
        List<ColumnMetadata> columns = values.keySet().stream() //
                .map(columnName -> ColumnMetadata.Builder.column().name(columnName).type(Type.STRING).build()) //
                .collect(Collectors.toList());
        rowMetadata = new RowMetadata(columns);
        initFromValues(values);
    }

    private void initFromValues(Map<String, ?> values) {
        values.forEach((k, v) -> internalSet(k, String.valueOf(v), this));
    }

    /**
     * @return The {@link RowMetadata metadata} that describes the current values.
     */
    public RowMetadata getRowMetadata() {
        return rowMetadata;
    }

    public DataSetRow setRowMetadata(RowMetadata rowMetadata) {
        DataSetRow clone = clone();
        clone.rowMetadata = rowMetadata.clone();
        return clone;
    }

    /**
     * Set an entry in the dataset row
     *
     * @param id - the key
     * @param value - the value
     */
    public DataSetRow set(final String id, final String value) {
        DataSetRow clone = clone();
        internalSet(id, value, clone);
        return clone;
    }

    private static void internalSet(String id, String value, DataSetRow row) {
        if (TDP_INVALID.equals(id)) {
            final List<String> ids = Arrays.asList(value.split(","));
            row.invalidColumnIds.addAll(ids);
        } else if (FlagNames.TDP_ID.equals(id)) {
            row.rowId = Long.parseLong(value);
        } else {
            row.values.put(id, value);
        }
    }

    /**
     * Get the value associated with the provided key
     *
     * @param id the column id.
     * @return - the value as string
     */
    public String get(final String id) {
        if (StringUtils.startsWith(id, INTERNAL_PROPERTY_PREFIX)) {
            return getInternalValues().get(id);
        } else {
            return values.get(id);
        }
    }

    /**
     * Check if the row is deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Set whether the row is deleted
     */
    public DataSetRow setDeleted(boolean deleted) {
        DataSetRow clone = clone();
        clone.deleted = deleted;
        return clone;
    }

    /**
     * Set the old row for diff
     *
     * @param oldRow - the original row
     */
    public DataSetRow diff(final DataSetRow oldRow) {
        DataSetRow clone = clone();
        clone.oldValue = oldRow.clone();
        return clone;
    }

    /**
     * Here we decide the flags to set and write is to the response
     * <ul>
     * <li>flag NEW : deleted by old but not by new</li>
     * <li>flag UPDATED : not deleted at all and value has changed</li>
     * <li>flag DELETED : not deleted by old by is by new</li>
     * </ul>
     */
    public Map<String, Object> values() {

        final Map<String, Object> result = new LinkedHashMap<>(values.size() + 1);

        // put all invalid column ids
        getInternalValues().forEach((key, value) -> {
            if (!StringUtils.isEmpty(value)) {
                result.put(key, value);
            }
        });

        // if not old value, no diff to compute
        if (this.oldValue == null) {
            result.putAll(values);
            return result;
        }

        // row is no more deleted : we write row values with the *NEW* flag
        if (oldValue.isDeleted() && !isDeleted()) {
            result.put(FlagNames.ROW_DIFF_KEY, Flag.NEW.getValue());
            result.putAll(values);
        }
        // row has been deleted : we write row values with the *DELETED* flag
        else if (!oldValue.isDeleted() && isDeleted()) {
            result.put(FlagNames.ROW_DIFF_KEY, Flag.DELETE.getValue());
            result.putAll(oldValue.values());
        }

        // row has been updated : write the new values and get the diff for each value, then write the DIFF_KEY
        // property

        final Map<String, Object> diff = new HashMap<>();
        final Map<String, Object> originalValues = oldValue.values();

        // compute the new value (column is not found in old value)
        values.forEach((key, value) -> {
            if (!originalValues.containsKey(key)) {
                diff.put(key, Flag.NEW.getValue());
            }
        });

        // compute the deleted values (column is deleted)
        originalValues.forEach((key, value) -> {
            if (!values.containsKey(key)) {
                diff.put(key, Flag.DELETE.getValue());
                // put back the original entry so that the value can be displayed
                set(key, (String) value);
            }
        });

        // compute the update values (column is still here but value is different)
        values.forEach((key, value) -> {
            if (originalValues.containsKey(key)) {
                final Object originalValue = originalValues.get(key);
                if (!StringUtils.equals(value, (String) originalValue)) {
                    diff.put(key, Flag.UPDATE.getValue());
                }
            }
        });

        result.putAll(values);
        if (!diff.isEmpty()) {
            result.put(FlagNames.DIFF_KEY, diff);
        }

        return result;
    }

    public Map<String, Object> valuesWithId() {
        final Map<String, Object> temp = values();
        if (getTdpId() != null) {
            temp.put(FlagNames.TDP_ID, getTdpId());
        }
        return temp;
    }

    /**
     * Clear all values in this row and reset state as it was when created (e.g. {@link #isDeleted()} returns
     * <code>false</code>).
     */
    public DataSetRow clear() {
        return new DataSetRow(rowMetadata);
    }

    /**
     * @see Cloneable#clone()
     */
    @Override
    public DataSetRow clone() {
        final DataSetRow clone = new DataSetRow(rowMetadata.clone(), values);
        clone.invalidColumnIds.addAll(invalidColumnIds);
        clone.deleted = deleted;
        clone.rowId = rowId;
        return clone;
    }

    /**
     * Determine if the row should be written
     */
    public boolean shouldWrite() {
        if (this.oldValue == null) {
            return !isDeleted();
        } else {
            return !oldValue.isDeleted() || !isDeleted();
        }
    }

    /**
     * @see Objects#equals(Object, Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DataSetRow that = (DataSetRow) o;
        return Objects.equals(deleted, that.deleted) && Objects.equals(values, that.values) && Objects.equals(rowId, that.rowId);
    }

    /**
     * @see Objects#hash(Object...)
     */
    @Override
    public int hashCode() {
        return Objects.hash(deleted, values);
    }

    @Override
    public String toString() {
        return "DataSetRow{" + //
                "rowMetadata=" + rowMetadata + //
                ", values=" + values + //
                ", deleted=" + deleted + //
                ", oldValue=" + oldValue + //
                ", rowId=" + rowId + //
                '}';
    }

    /**
     * Order values of this data set row according to <code>columns</code>. This method clones the current record, so no
     * need to call {@link #clone()}.
     *
     * @param columns The columns to be used to order values.
     * @return A new data set row for method with values ordered following <code>columns</code>.
     */
    public DataSetRow order(List<ColumnMetadata> columns) {
        if (columns == null) {
            throw new IllegalArgumentException("Columns cannot be null.");
        }
        if (columns.isEmpty()) {
            return this;
        }
        if (columns.size() < values.size() && (!values.containsKey(TDP_INVALID) || columns.size() + 1 < values().size())) {
            throw new IllegalArgumentException("Expected " + values.size() + " columns but got " + columns.size());
        }

        Map<String, String> orderedValues = new LinkedHashMap<>();
        for (ColumnMetadata column : columns) {
            final String id = column.getId();
            orderedValues.put(id, values.get(id));
        }

        final DataSetRow dataSetRow = new DataSetRow(rowMetadata);
        dataSetRow.values = orderedValues;
        return dataSetRow;
    }


    /**
     * Order values of this data set row according to its own <code>columns</code>. This method clones the current
     * record, so no need to call {@link #clone()}.
     *
     * @return A new data set row for method with values ordered following its <code>columns</code>.
     */
    public DataSetRow order() {
        return order(getRowMetadata().getColumns());
    }

    /**
     * Removes the value with the specified id and removes the column metadata if it has not been already removed, and
     * returns <tt>true</tt> if the value has been removed. If this dataset row does not contain the specified it, it
     * is unchanged and returns <tt>false</tt>.
     *
     * @param id the id of the value to be removed
     * @return the modified dataset row?
     */
    public DataSetRow deleteColumnById(String id) {
        DataSetRow clone = clone();
        clone.rowMetadata.deleteColumnById(id);
        clone.values.remove(id);
        return clone;
    }

    /**
     * Returns the current row as an array of Strings.
     *
     * @param filters An optional set of {@link Predicate filters} to be used to filter values. See {@link #SKIP_TDP_ID}
     * for example.
     * @return The current row as array of String eventually with filtered out columns depending on filter.
     */
    @SafeVarargs
    public final String[] toArray(Predicate<Map.Entry<String, String>>... filters) {
        Stream<Map.Entry<String, String>> stream = values.entrySet().stream();
        // Apply filters
        for (Predicate<Map.Entry<String, String>> filter : filters) {
            stream = stream.filter(filter);
        }
        // Get as string array the selected columns
        return stream.map(Map.Entry::getValue) //
                .map(String::valueOf) //
                .toArray(String[]::new);
    }

    public Long getTdpId() {
        return rowId;
    }

    public DataSetRow setTdpId(Long tdpId) {
        DataSetRow clone = clone();
        clone.rowId = tdpId;
        return clone;
    }

    /**
     * @return <code>true</code> if row has no value / or / only contains empty strings / or / null strings.
     * <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return values.isEmpty() || values.values().stream().allMatch(StringUtils::isEmpty);
    }

    public DataSetRow filter(List<ColumnMetadata> filteredColumns) {
        final Set<String> columnsToKeep = filteredColumns.stream().map(ColumnMetadata::getId).collect(Collectors.toSet());
        final Set<String> columnsToDelete = values.entrySet().stream()
                .filter(e -> !columnsToKeep.contains(e.getKey())) //
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        final RowMetadata rowMetadataClone = rowMetadata.clone();
        final LinkedHashMap<String, String> filteredValues = new LinkedHashMap<>(this.values);
        for (String columnId : columnsToDelete) {
            filteredValues.remove(columnId);
            rowMetadataClone.deleteColumnById(columnId);
        }
        final DataSetRow filteredDataSetRow = new DataSetRow(rowMetadataClone, filteredValues);
        filteredDataSetRow.invalidColumnIds.addAll(invalidColumnIds);
        return filteredDataSetRow;
    }

    /**
     * Check if a column has an invalid value in this row.
     *
     * @param columnId A column id in the line.
     * @return <code>true</code> if column is marked as invalid in row, <code>false</code> otherwise or if column does not exist.
     */
    public boolean isInvalid(String columnId) {
        final String currentInvalidColumnIds = get(TDP_INVALID);
        return currentInvalidColumnIds != null && currentInvalidColumnIds.contains(columnId);
    }

    /**
     * Mark column <code>columnId</code> as invalid.
     *
     * @param columnId A column id in the line.
     * @see #unsetInvalid(String)
     */
    public DataSetRow setInvalid(String columnId) {
        DataSetRow clone = clone();
        clone.invalidColumnIds.add(columnId);
        return clone;
    }

    /**
     * Unmark column <code>columnId</code> as invalid.
     *
     * @param columnId A column id in the line.
     * @see #setInvalid(String)
     */
    public DataSetRow unsetInvalid(String columnId) {
        DataSetRow clone = clone();
        clone.invalidColumnIds.remove(columnId);
        return clone;
    }

    /**
     * @return All technical/internal values in this line (values not meant to be displayed as is).
     * @see FlagNames
     */
    public Map<String, String> getInternalValues() {
        final Map<String, String> internalValues = new HashMap<>(1);
        internalValues.put(TDP_INVALID, invalidColumnIds.stream().collect(joining(",")));
        return internalValues;
    }
}
