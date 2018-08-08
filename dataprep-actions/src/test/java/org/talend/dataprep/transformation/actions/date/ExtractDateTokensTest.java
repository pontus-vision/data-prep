// ============================================================================
//
//  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.actions.date;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.actions.ActionMetadataTestUtils;
import org.talend.dataprep.transformation.api.action.ActionTestWorkbench;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.talend.dataprep.transformation.actions.AbstractMetadataBaseTest.ValueBuilder.value;
import static org.talend.dataprep.transformation.actions.AbstractMetadataBaseTest.ValuesBuilder.builder;

public class ExtractDateTokensTest extends BaseDateTest<ExtractDateTokens> {

    private Map<String, String> parameters;

    public ExtractDateTokensTest() {
        super(new ExtractDateTokens());
    }

    @Before
    public void init() throws IOException {
        parameters = ActionMetadataTestUtils.parseParameters(getDateTestJsonAsStream("extractDateTokensAction.json"));
    }

    @Override
    protected CreateNewColumnPolicy getCreateNewColumnPolicy() {
        return CreateNewColumnPolicy.INVISIBLE_ENABLED;
    }

    @Test
    public void test_apply_inplace() throws Exception {
        // Nothing to test, this action is never applied in place
    }

    @Test
    public void test_apply_in_newcolumn() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("04/25/1999").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "04/25/1999");
        expectedValues.put("0003", "1999");
        expectedValues.put("0004", "4");
        expectedValues.put("0005", "0");
        expectedValues.put("0006", "0");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void test_TDP_2480() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("Apr-25-1999").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "Apr-25-1999");
        expectedValues.put("0003", "1999");
        expectedValues.put("0004", "4");
        expectedValues.put("0005", "0");
        expectedValues.put("0006", "0");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void test_TDP_4494() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("Dec-17-2017").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        parameters.put("QUARTER", "true");

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "Dec-17-2017");
        expectedValues.put("0003", "2017");
        expectedValues.put("0004", "4");
        expectedValues.put("0005", "12");
        expectedValues.put("0006", "0");
        expectedValues.put("0007", "0");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void test_TDP_1676() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("Dec-17-2017").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        parameters.put(ExtractDateTokens.DAY_LABEL, "true");
        parameters.put(ExtractDateTokens.MONTH_LABEL, "true");

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "Dec-17-2017");
        expectedValues.put("0003", "2017");
        expectedValues.put("0004", "12");
        expectedValues.put("0005", "December");
        expectedValues.put("0006", "Sunday");
        expectedValues.put("0008", "0");
        expectedValues.put("0007", "0");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void should_process_row_with_time() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("04/25/1999 15:45").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy_HH_mm.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "04/25/1999 15:45");
        expectedValues.put("0003", "1999");
        expectedValues.put("0004", "4");
        expectedValues.put("0005", "15");
        expectedValues.put("0006", "45");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    /**
     * To test with a date that does not match the most frequent pattern, but match another one present in the stats
     */
    @Test
    public void should_process_row_wrong_pattern() throws Exception {
        // given
        final DataSetRow row = builder() //
                .with(value("toto").type(Type.STRING)) //
                .with(value("04-25-09").type(Type.DATE).statistics(
                        getDateTestJsonAsStream("statistics_MM_dd_yyyy.json"))) //
                .with(value("tata").type(Type.STRING)) //
                .build();

        final Map<String, String> expectedValues = new LinkedHashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "04-25-09");
        expectedValues.put("0003", "2009");
        expectedValues.put("0004", "4");
        expectedValues.put("0005", "0");
        expectedValues.put("0006", "0");
        expectedValues.put("0002", "tata");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(action, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

}
