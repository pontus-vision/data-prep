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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.dataset.statistics.PatternFrequency;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.i18n.ActionsBundle;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.parameters.ParameterType;
import org.talend.dataprep.parameters.SelectParameter;
import org.talend.dataprep.transformation.actions.category.ActionCategory;
import org.talend.dataprep.transformation.actions.common.AbstractActionMetadata;
import org.talend.dataprep.transformation.actions.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.context.ActionContext;
import org.talend.dataprep.transformation.api.action.context.ActionContext.ActionStatus;
import org.talend.dataquality.semantic.datamasking.ValueDataMasker;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Mask sensitive data according to the semantic category.
 */
@Action(AbstractActionMetadata.ACTION_BEAN_PREFIX + MaskDataByDomain.ACTION_NAME)
public class MaskDataByDomain extends AbstractActionMetadata implements ColumnAction {

    /**
     * Action name.
     */
    public static final String ACTION_NAME = "mask_data_by_domain"; //$NON-NLS-1$

    private static final Logger LOGGER = LoggerFactory.getLogger(MaskDataByDomain.class);

    /**
     * Action parameters:
     */
    protected static final String MASKING_FUNCTION = "masking_function"; //$NON-NLS-1$

    private static final String SEMANTIC_MASKING = "semantic_masking"; //$NON-NLS-1$

    private static final String KEEP_CHARACTERS = "BETWEEN_INDEXES_KEEP"; //$NON-NLS-1$

    private static final String GENERATE_FROM_PATTERN = "GENERATE_FROM_PATTERN"; //$NON-NLS-1$

    private static final String REMOVE_CHARACTERS = "BETWEEN_INDEXES_REMOVE"; //$NON-NLS-1$

    private static final String REPLACE_ALL = "REPLACE_ALL"; //$NON-NLS-1$

    private static final String REPLACE_CHARACTERS = "BETWEEN_INDEXES_REPLACE"; //$NON-NLS-1$

    private static final String REPLACE_N_FIRST = "REPLACE_FIRST_CHARS"; //$NON-NLS-1$

    private static final String REPLACE_N_LAST = "REPLACE_LAST_CHARS"; //$NON-NLS-1$

    private static final String REPLACE_ALL_DIGITS = "REPLACE_NUMERIC"; //$NON-NLS-1$

    private static final String REPLACE_ALL_LETTERS = "REPLACE_CHARACTERS"; //$NON-NLS-1$

    private static final String KEEP_N_FIRST = "KEEP_FIRST_AND_GENERATE"; //$NON-NLS-1$

    private static final String KEEP_N_LAST = "KEEP_LAST_AND_GENERATE"; //$NON-NLS-1$

    private static final String NUMERIC_VARIANCE = "NUMERIC_VARIANCE"; //$NON-NLS-1$

    private static final String GENERATE_VALUE= "GENERATE_BETWEEN"; //$NON-NLS-1$

    private static final String DATE_VIARIANCE= "DATE_VARIANCE"; //$NON-NLS-1$

    private static final String KEEP_YEAR = "KEEP_YEAR"; //$NON-NLS-1$

    protected static final String EXTRA_PARAM="extra_parameter"; //$NON-NLS-1$

    /**
     * Key for storing in ActionContext:
     */
    private static final String MASKER = "masker"; //$NON-NLS-1$

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    public String getCategory() {
        return ActionCategory.DATA_MASKING.getDisplayName();
    }

    @Override
    public boolean acceptField(ColumnMetadata column) {
        return Type.STRING.isAssignableFrom(Type.get(column.getType()))
                || Type.NUMERIC.isAssignableFrom(Type.get(column.getType()))
                || Type.DATE.isAssignableFrom(Type.get(column.getType()));
    }

    @Override
    public List<Parameter> getParameters() {
        List<Parameter> parameters = super.getParameters();

        parameters.add(SelectParameter.Builder.builder()
                .name(MASKING_FUNCTION)
                .item(SEMANTIC_MASKING, SEMANTIC_MASKING)
                .item(KEEP_CHARACTERS, KEEP_CHARACTERS,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(GENERATE_FROM_PATTERN, GENERATE_FROM_PATTERN,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REMOVE_CHARACTERS, REMOVE_CHARACTERS,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_ALL, REPLACE_ALL,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_CHARACTERS, REPLACE_CHARACTERS,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_N_FIRST, REPLACE_N_FIRST,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_N_LAST, REPLACE_N_LAST,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_ALL_DIGITS, REPLACE_ALL_DIGITS,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(REPLACE_ALL_LETTERS, REPLACE_ALL_LETTERS,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(KEEP_N_FIRST, KEEP_N_FIRST,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(KEEP_N_LAST, KEEP_N_LAST,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(NUMERIC_VARIANCE, NUMERIC_VARIANCE,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(GENERATE_VALUE, GENERATE_VALUE,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(DATE_VIARIANCE, DATE_VIARIANCE,new Parameter(EXTRA_PARAM, ParameterType.STRING, EMPTY, false, true))
                .item(KEEP_YEAR, KEEP_YEAR)
                .defaultValue(SEMANTIC_MASKING)
                .canBeBlank(false)
                .build());

        return ActionsBundle.attachToAction(parameters, this);
    }
    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        final String columnId = context.getColumnId();
        final String value = row.get(columnId);
        if (StringUtils.isNotBlank(value)) {
            try {
                final ValueDataMasker masker = context.get(MASKER);
                row.set(columnId, masker.maskValue(value));
            } catch (Exception e) {
                // Nothing to do, we let the original value as is
                LOGGER.debug("Unable to process value '{}'.", value, e);
            }
        }
    }

    @Override
    public void compile(ActionContext actionContext) {
        super.compile(actionContext);
        if (actionContext.getActionStatus() == ActionContext.ActionStatus.OK) {
            final RowMetadata rowMetadata = actionContext.getRowMetadata();
            final String columnId = actionContext.getColumnId();
            final ColumnMetadata column = rowMetadata.getById(columnId);
            final String domain = column.getDomain();
            final Type type = Type.get(column.getType());

            final Map<String, String> parameters = actionContext.getParameters();
            final String function = parameters.get(MASKING_FUNCTION);
            final String extraParam = parameters.get(EXTRA_PARAM);

            LOGGER.trace(">>> type: " + type + " metadata: " + column);
            try {
                    switch(function) {
                        case SEMANTIC_MASKING:
                        case REPLACE_ALL:
                        case NUMERIC_VARIANCE:
                        case DATE_VIARIANCE:
                            getSpecialFunction(actionContext, column, domain, type,extraParam);
                            break;
                        default:
                            //use the function selected
                            actionContext.get(MASKER, p -> new ValueDataMasker(function,type.getName(),domain,extraParam));
                    }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                actionContext.setActionStatus(ActionStatus.CANCELED);
            }
        }
    }

    private void getSpecialFunction(ActionContext actionContext, ColumnMetadata column, String domain, Type type, String extraParam) {
        ValueDataMasker valueMasker;
        if (Type.DATE.equals(type)) {
            final List<PatternFrequency> patternFreqList = column.getStatistics().getPatternFrequencies();
            final List<String> dateTimePatternList = patternFreqList.stream() //
                    .map(PatternFrequency::getPattern) //
                    .collect(Collectors.toList());
            valueMasker = new ValueDataMasker(domain, type.getName(), dateTimePatternList);
        } else {
            valueMasker = new ValueDataMasker(domain, type.getName());
        }
        if(StringUtils.isNotEmpty(domain) && StringUtils.isNotEmpty(extraParam)){//when the column has semantic type, use the param set by the user
            valueMasker.resetExtraParameter(extraParam);
        }

        actionContext.get(MASKER, p -> valueMasker);
    }

    @Override
    public Set<Behavior> getBehavior() {
        return EnumSet.of(Behavior.VALUES_COLUMN, Behavior.NEED_STATISTICS_INVALID);
    }

}
