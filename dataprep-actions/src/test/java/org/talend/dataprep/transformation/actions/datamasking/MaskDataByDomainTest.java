// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.actions.datamasking;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.talend.dataprep.transformation.actions.AbstractMetadataBaseTest.ValueBuilder.value;
import static org.talend.dataprep.transformation.actions.AbstractMetadataBaseTest.ValuesBuilder.builder;
import static org.talend.dataprep.transformation.actions.ActionMetadataTestUtils.getColumn;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.actions.AbstractMetadataBaseTest;
import org.talend.dataprep.transformation.actions.ActionMetadataTestUtils;
import org.talend.dataprep.transformation.actions.category.ActionCategory;
import org.talend.dataprep.transformation.api.action.ActionTestWorkbench;
import org.talend.dataquality.semantic.datamasking.MaskableCategoryEnum;

/**
 * Test class for MaskDataByDomain action.
 *
 * @see MaskDataByDomain
 */
public class MaskDataByDomainTest extends AbstractMetadataBaseTest {

    /** The action to test. */
    private MaskDataByDomain maskDataByDomain;

    private Map<String, String> parameters;

    private static final Logger LOGGER = LoggerFactory.getLogger(MaskDataByDomainTest.class);

    @Before
    public void init() throws IOException {
        maskDataByDomain = new MaskDataByDomain();
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskDataByDomainAction.json"));
    }

    @Test
    public void testCategory() throws Exception {
        assertThat(maskDataByDomain.getCategory(), is(ActionCategory.DATA_MASKING.getDisplayName()));
    }

    @Test
    public void testShouldMaskDatetime() throws IOException {

        // given
        final DataSetRow row = builder() //
                .with(value("2015-09-15") //
                        .type(Type.DATE) //
                        .statistics(MaskDataByDomainTest.class.getResourceAsStream("statistics_datetime.json"))
                ) //
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        final String resultValue = row.values().get("0000").toString();
        assertTrue("The result [" + resultValue + "] should be a masked date but actually not.",
                resultValue.matches("^2015\\-[0-1][0-9]\\-[0-3][0-9]$"));
    }

    @Test
    public void testShouldMaskEmail() {

        // given
        final DataSetRow row = builder() //
                .with(value("azerty@talend.com").type(Type.STRING).domain(MaskableCategoryEnum.EMAIL.name())) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "XXXXXX@talend.com");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void testShouldMaskInteger() {
        // given
        final DataSetRow row = builder() //
                .with(value("12").type(Type.INTEGER)) //
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        int realValueAsInteger = Integer.parseInt((String) row.values().get("0000"));
        LOGGER.info("Row value: {}", realValueAsInteger);
        assertTrue(realValueAsInteger >= 10 && realValueAsInteger <= 14);
    }

    @Test
    public void testShouldMaskDecimal_well_typed() {
        // given
        final DataSetRow row = builder() //
                .with(value("12.5").type(Type.FLOAT)) //
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        float realValueAsFloat = Float.parseFloat((String) row.values().get("0000"));
        LOGGER.info("Row value: {}", realValueAsFloat);
        assertTrue(realValueAsFloat >= 10 && realValueAsFloat <= 14);
    }

    @Test
    public void testShouldMaskDecimal_wrongly_typed() {
        // given
        final DataSetRow row = builder() //
                .with(value("12.5").type(Type.INTEGER)) //
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        float realValueAsFloat = Float.parseFloat((String) row.values().get("0000"));
        LOGGER.info("Row value: {}", realValueAsFloat);
        assertTrue(realValueAsFloat >= 10 && realValueAsFloat <= 14);
    }

    @Test
    public void testShouldMaskInteger_wrongly_typed() {
        // given
        final DataSetRow row = builder() //
                .with(value("12").type(Type.FLOAT)) //
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        int realValueAsInteger = Integer.parseInt((String) row.values().get("0000"));
        LOGGER.info("Row value: {}", realValueAsInteger);
        assertTrue(realValueAsInteger >= 10 && realValueAsInteger <= 14);
    }

    @Test
    public void testShouldIgnoreEmpty() {

        // given
        final DataSetRow row = builder() //
                .with(value(" ").type(Type.STRING).domain(MaskableCategoryEnum.EMAIL.name())) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", " ");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }


    @Test
    public void testShouldUseDefaultMaskingForInvalid() {

        // given
        final DataSetRow row = builder() //
                .with(value("bla bla").type(Type.STRING).domain(MaskableCategoryEnum.EMAIL.name())) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "XXXXXXX");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void testShouldNotMaskUnsupportedDataType() {

        // given
        final DataSetRow row = builder() //
                .with(value("azerty@talend.com").type(Type.ANY)) //
                .build();

        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "azerty@talend.com");

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        assertEquals(expectedValues, row.values());
    }

    @Test
    public void should_accept_column() {
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.STRING)));
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.DATE)));
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.NUMERIC)));
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.INTEGER)));
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.FLOAT)));
        assertTrue(maskDataByDomain.acceptField(getColumn(Type.BOOLEAN)));
    }

    @Test
    public void should_not_accept_column() {
        assertFalse(maskDataByDomain.acceptField(getColumn(Type.ANY)));
    }

    @Test
    public void should_have_expected_behavior() {
        assertEquals(2, maskDataByDomain.getBehavior().size());
        assertTrue(maskDataByDomain.getBehavior().contains(ActionDefinition.Behavior.VALUES_COLUMN));
        assertTrue(maskDataByDomain.getBehavior().contains(ActionDefinition.Behavior.NEED_STATISTICS_INVALID));
    }

    @Test
    public void testKeepFirstMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskKeepFirstFunctionAction.json"));

        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("12345",result.substring(0,5));
        assertEquals("abcdefg",result.substring(9,result.length()));
        assertNotEquals("6789",result.substring(5,9));
    }

    @Test
    public void testKeepLastMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskKeepFirstFunctionAction.json"));
        parameters.replace("masking_function","KEEP_LAST_AND_GENERATE");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertNotEquals("12345",result.substring(0,5));
        assertEquals("abcdefg",result.substring(9,result.length()));
        assertEquals("6789",result.substring(5,9));
    }

    @Test
    public void testReplaceNFirstMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));

        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertNotEquals("1234567",result.substring(0,7));
        assertEquals("89abcdefg",result.substring(7,result.length()));
    }

    @Test
    public void testReplaceNLastMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","REPLACE_LAST_CHARS");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("1234567",result.substring(0,7));
        assertNotEquals("89abcdefg",result.substring(7,result.length()));
    }

    @Test
    public void testReplaceNumericMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","REPLACE_NUMERIC");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertNotEquals("123456789",result.substring(0,9));
        assertEquals("abcdefg",result.substring(9,result.length()));
    }

    @Test
    public void testReplaceCharsMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","REPLACE_CHARACTERS");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abcdefg").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("123456789",result.substring(0,9));
        assertNotEquals("abcdefg",result.substring(9,result.length()));
    }

    @Test
    public void testGenerateBetweenMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","GENERATE_BETWEEN");
        parameters.replace("extra_parameter","4,10");
        // given
        final DataSetRow row = builder() //
                .with(value("1231111").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertTrue(Integer.valueOf(result) >3 && Integer.valueOf(result) <11 );
    }

    @Test
    public void testKeepBetweenMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","BETWEEN_INDEXES_KEEP");
        parameters.replace("extra_parameter","4,10");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abc").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("456789a",result);
    }

    @Test
    public void testRemoveBetweenMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","BETWEEN_INDEXES_REMOVE");
        parameters.replace("extra_parameter","4,10");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abc").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("123bc",result);
    }

    @Test
    public void testReplaceBetweenMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","BETWEEN_INDEXES_REPLACE");
        parameters.replace("extra_parameter","4,10");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abc").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals("123",result.substring(0,3));
        assertNotEquals("456789a",result.substring(4,10));
        assertEquals("bc",result.substring(10,result.length()));
    }

    @Test
    public void testReplaceAllMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","REPLACE_ALL");
        parameters.replace("extra_parameter","A");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abc").type(Type.STRING))
                .build();
        row.getRowMetadata().getById("0000").setDomain("City");
        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertNotEquals("123456789abc",result);
    }

    @Test
    public void testNumericVarianceMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","NUMERIC_VARIANCE");
        parameters.replace("extra_parameter","100");
        // given
        final DataSetRow row = builder() //
                .with(value("123456789abc").type(Type.STRING))
                .build();
        row.getRowMetadata().getById("0000").setDomain("City");
        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertNotEquals("123456789abc",result);
    }

    @Test
    public void testGenerateFromePatternMaskFunction() throws IOException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","GENERATE_FROM_PATTERN");
        parameters.replace("extra_parameter","aaAA99");
        // given
        final DataSetRow row = builder() //
                .with(value("abc123ABC1111").type(Type.STRING))
                .build();

        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));

        // then
        String result = (String) row.values().get("0000");
        assertEquals(6,result.length());
        assertTrue(Character.isLowerCase(result.charAt(0)));
        assertTrue(Character.isLowerCase(result.charAt(1)));
        assertTrue(Character.isUpperCase(result.charAt(2)));
        assertTrue(Character.isUpperCase(result.charAt(3)));
        assertTrue(Character.isDigit(result.charAt(4)));
        assertTrue(Character.isDigit(result.charAt(5)));
    }

    @Test
    public void testKeepYearMaskFunction() throws IOException, ParseException {
        parameters = ActionMetadataTestUtils
                .parseParameters(MaskDataByDomainTest.class.getResourceAsStream("maskReplaceNFirstFunctionAction.json"));
        parameters.replace("masking_function","KEEP_YEAR");
        parameters.replace("extra_parameter","");
        // given
        final DataSetRow row = builder() //
                .with(value("2015-09-15").type(Type.DATE))
                .build();
        // when
        ActionTestWorkbench.test(row, actionRegistry, factory.create(maskDataByDomain, parameters));
        // then
        String result = (String) row.values().get("0000");
        assertEquals("2015-01-01",result);
    }

}
