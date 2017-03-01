package org.talend.dataprep.transformation.api.action.metadata.fillinvalid;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils.getColumn;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.ActionTestWorkbench;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils;
import org.talend.dataprep.transformation.api.action.metadata.fill.FillIfEmpty;
import org.talend.dataprep.transformation.api.action.metadata.fill.FillInvalid;

/**
 * Unit test for the FillWithNumericIfInvalid action.
 * 
 * @see FillInvalid
 */
public class FillWithNumericIfInvalidTest {

    /** The action to test. */
    private FillInvalid action;

    /**
     * Default empty constructor.
     */
    public FillWithNumericIfInvalidTest() {
        action = new FillInvalid();
        action = (FillInvalid) action.adapt(ColumnMetadata.Builder.column().type(Type.INTEGER).build());
    }

    @Test
    public void should_fill_non_valid_integer() throws Exception {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0001", "David Bowie");
        values.put("0002", "N");
        values.put("0003", "Something");

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.setColumns(Collections.singletonList(ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0002") //
                .invalidValues(newHashSet("N")) //
                .build()));

        final DataSetRow row = new DataSetRow(rowMetadata, values);

        Map<String, String> parameters = ActionMetadataTestUtils
                .parseParameters(this.getClass().getResourceAsStream("fillInvalidIntegerAction.json"));

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals("25", row.get("0002"));
        assertEquals("David Bowie", row.get("0001"));
    }


    @Test
    public void should_not_fill_non_valid_integer() throws Exception {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0001", "David Bowie");
        values.put("0002", "30");
        values.put("0003", "Something");

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.setColumns(Collections.singletonList(ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0002") //
                .invalidValues(newHashSet("N")) //
                .build()));

        final DataSetRow row = new DataSetRow(rowMetadata, values);

        Map<String, String> parameters = ActionMetadataTestUtils
                .parseParameters(this.getClass().getResourceAsStream("fillInvalidIntegerAction.json"));

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals("30", row.get("0002"));
        assertEquals("David Bowie", row.get("0001"));
    }

    @Test
    public void should_fill_non_valid_integer_not_in_invalid_values() throws Exception {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0001", "David Bowie");
        values.put("0002", "_______wljh_"); // invalid value
        values.put("0003", "Something");

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.setColumns(Collections.singletonList(ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0002") //
                .invalidValues(new HashSet<>()) // no invalid values
                .build()));

        final DataSetRow row = new DataSetRow(rowMetadata, values);

        Map<String, String> parameters = ActionMetadataTestUtils
                .parseParameters(this.getClass().getResourceAsStream("fillInvalidIntegerAction.json"));

        // when
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        assertEquals("25", row.get("0002"));
        assertEquals("David Bowie", row.get("0001"));

        final Set<String> invalidValues = row.getRowMetadata().getById("0002").getQuality().getInvalidValues();
        assertEquals(1, invalidValues.size());
        assertTrue(invalidValues.contains("_______wljh_"));
    }

    @Test
    public void should_fill_empty_integer_other_column() throws Exception {
        // given
        final Map<String, String> values = new HashMap<>();
        values.put("0001", "David Bowie");
        values.put("0002", "A text");
        values.put("0003", "10");

        final RowMetadata rowMetadata = new RowMetadata();
        rowMetadata.addColumn(ColumnMetadata.Builder.column().type(Type.INTEGER).computedId("0002").build());
        rowMetadata.addColumn(ColumnMetadata.Builder.column().type(Type.INTEGER).computedId("0003").build());

        final DataSetRow row = new DataSetRow(rowMetadata, values);

        Map<String, String> parameters = ActionMetadataTestUtils.parseParameters( //
                this.getClass().getResourceAsStream("fillInvalidIntegerAction.json"));

        // when
        parameters.put(FillIfEmpty.MODE_PARAMETER, FillIfEmpty.OTHER_COLUMN_MODE);
        parameters.put(FillIfEmpty.SELECTED_COLUMN_PARAMETER, "0003");
        ActionTestWorkbench.test(row, action.create(parameters).getRowAction());

        // then
        Assert.assertEquals("10", row.get("0002"));
        Assert.assertEquals("David Bowie", row.get("0001"));
    }

    @Test
    public void should_accept_column() {
        assertTrue(action.acceptColumn(getColumn(Type.NUMERIC)));
        assertTrue(action.acceptColumn(getColumn(Type.DOUBLE)));
        assertTrue(action.acceptColumn(getColumn(Type.FLOAT)));
        assertTrue(action.acceptColumn(getColumn(Type.INTEGER)));
    }

    @Test
    public void should_adapt_null() throws Exception {
        assertThat(action.adapt((ColumnMetadata) null), is(action));
    }

    @Test
    public void should_not_accept_column() {
        assertFalse(action.acceptColumn(getColumn(Type.ANY)));
    }

}