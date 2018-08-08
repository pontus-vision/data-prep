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

package org.talend.dataprep.transformation.actions.date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.action.Action;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.parameters.SelectParameter;
import org.talend.dataprep.transformation.actions.Providers;
import org.talend.dataprep.transformation.actions.common.ActionsUtils;
import org.talend.dataprep.transformation.actions.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

import javax.annotation.Nonnull;
import java.text.DateFormatSymbols;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.talend.dataprep.parameters.Parameter.parameter;
import static org.talend.dataprep.parameters.ParameterType.BOOLEAN;
import static org.talend.dataprep.parameters.SelectParameter.selectParameter;

/**
 * Change the date pattern on a 'date' column.
 */
@Action(ExtractDateTokensOrdered.ACTION_NAME)
public class ExtractDateTokensOrdered extends AbstractDate implements ColumnAction {

    /** Action name. */
    public static final String ACTION_NAME = "extract_date_tokens_ordered"; //$NON-NLS-1$

    /** Separator. */
    private static final String SEPARATOR = "_";

    /** Month constant value. */
    protected static final String MONTH_LABEL = "MONTH_LABEL";

    /** Day label constant value. */
    protected static final String DAY_LABEL = "DAY_LABEL";

    /** Language name parameter */
    public static final String LANGUAGE_PARAMETER = "LANGUAGE";

    /** Key for the locale value in context */
    private static String LOCALE_PARAMETER = "LOCALE";

    static final DateFieldMapping SECOND = new DateFieldMapping("SECOND", ChronoField.SECOND_OF_MINUTE);

    static final DateFieldMapping MINUTE = new DateFieldMapping("MINUTE", ChronoField.MINUTE_OF_HOUR);

    static final DateFieldMapping HOUR_24 = new DateFieldMapping("HOUR_24", ChronoField.HOUR_OF_DAY);

    static final DateFieldMapping AM_PM = new DateFieldMapping("AM_PM", ChronoField.AMPM_OF_DAY);

    static final DateFieldMapping HOUR_12 = new DateFieldMapping("HOUR_12", ChronoField.HOUR_OF_AMPM);

    static final DateFieldMapping DAY_OF_WEEK_LABEL =
            new DateFieldMapping(DAY_LABEL, ChronoField.DAY_OF_WEEK, ExtractDateTokensOrdered::getLabelDay);

    static final DateFieldMapping DAY_OF_WEEK_ID = new DateFieldMapping("DAY_OF_WEEK", ChronoField.DAY_OF_WEEK);

    static final DateFieldMapping DAY_OF_MONTH = new DateFieldMapping("DAY", ChronoField.DAY_OF_MONTH);

    static final DateFieldMapping DAY_OF_YEAR = new DateFieldMapping("DAY_OF_YEAR", ChronoField.DAY_OF_YEAR);

    static final DateFieldMapping WEEK_OF_YEAR = new DateFieldMapping("WEEK_OF_YEAR", ChronoField.ALIGNED_WEEK_OF_YEAR);

    static final DateFieldMapping MONTH_OF_YEAR_LABEL =
            new DateFieldMapping(MONTH_LABEL, ChronoField.MONTH_OF_YEAR, ExtractDateTokensOrdered::getLabelMonth);

    static final DateFieldMapping MONTH_OF_YEAR_ID = new DateFieldMapping("MONTH", ChronoField.MONTH_OF_YEAR);

    static final DateFieldMapping QUARTER_OF_YEAR = new DateFieldMapping("QUARTER", IsoFields.QUARTER_OF_YEAR);

    static final DateFieldMapping YEAR_MAPPING = new DateFieldMapping("YEAR", ChronoField.YEAR);

    private static final List<DateFieldMapping> DATE_FIELDS = Arrays.asList(//
            SECOND, //
            MINUTE, //
            HOUR_24, //
            AM_PM, //
            HOUR_12, //
            DAY_OF_WEEK_LABEL, //
            DAY_OF_WEEK_ID, //
            DAY_OF_MONTH, //
            DAY_OF_YEAR, //
            WEEK_OF_YEAR, //
            MONTH_OF_YEAR_LABEL, //
            MONTH_OF_YEAR_ID, //
            QUARTER_OF_YEAR, //
            YEAR_MAPPING //
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractDateTokensOrdered.class);

    private static final boolean CREATE_NEW_COLUMN_DEFAULT = true;

    @Override
    @Nonnull
    public List<Parameter> getParameters(Locale locale) {
        final List<Parameter> parameters = super.getParameters(locale);
        parameters.add(parameter(locale).setName(YEAR_MAPPING.key).setType(BOOLEAN).setDefaultValue(true).build(this));
        parameters.add(parameter(locale).setName(QUARTER_OF_YEAR.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(MONTH_OF_YEAR_ID.key).setType(BOOLEAN).setDefaultValue(true).build(this));
        parameters.add(parameter(locale).setName(MONTH_OF_YEAR_LABEL.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(WEEK_OF_YEAR.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(DAY_OF_YEAR.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(DAY_OF_MONTH.key).setType(BOOLEAN).setDefaultValue(true).build(this));
        parameters.add(parameter(locale).setName(DAY_OF_WEEK_ID.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(DAY_OF_WEEK_LABEL.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(HOUR_12.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(AM_PM.key).setType(BOOLEAN).setDefaultValue(false).build(this));
        parameters.add(parameter(locale).setName(HOUR_24.key).setType(BOOLEAN).setDefaultValue(true).build(this));
        parameters.add(parameter(locale).setName(MINUTE.key).setType(BOOLEAN).setDefaultValue(true).build(this));
        parameters.add(parameter(locale).setName(SECOND.key).setType(BOOLEAN).setDefaultValue(false).build(this));

        List<Locale> locales = Stream
                .of(Locale.CHINESE, Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN, //
                        Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN, Locale.forLanguageTag("es")) //
                .sorted(Comparator.comparing(o -> o.getDisplayLanguage(locale))) //
                .collect(Collectors.toList());

        SelectParameter.SelectParameterBuilder builder = selectParameter(locale).name(LANGUAGE_PARAMETER);
        for (Locale currentLocale : locales) {
            builder = builder.constant(currentLocale.getLanguage(), currentLocale.getDisplayLanguage(locale));
        }
        builder = builder.defaultValue(Locale.ENGLISH.getLanguage());

        parameters.add(builder.build(this));
        return parameters;
    }

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    public void compile(ActionContext context) {
        super.compile(context);
        if (ActionsUtils.doesCreateNewColumn(context.getParameters(), CREATE_NEW_COLUMN_DEFAULT)) {
            final List<ActionsUtils.AdditionalColumn> additionalColumns = new ArrayList<>();
            for (DateFieldMapping dateField : getDateFields()) {
                if (Boolean.valueOf(context.getParameters().get(dateField.key))) {
                    additionalColumns.add(ActionsUtils.additionalColumn()
                            .withKey(dateField.key)
                            .withName(context.getColumnName() + SEPARATOR + dateField.key)
                            .withType(Type.INTEGER));
                }
            }
            ActionsUtils.createNewColumn(context, additionalColumns);
            context.get(LOCALE_PARAMETER, p -> new Locale(context.getParameters().get(LANGUAGE_PARAMETER)));
        }
    }

    protected List<DateFieldMapping> getDateFields() {
        return DATE_FIELDS;
    }

    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        final String columnId = context.getColumnId();
        final Map<String, String> parameters = context.getParameters();

        // Get the most used pattern formatter and parse the date
        final String value = row.get(columnId);
        if (value == null) {
            return;
        }
        TemporalAccessor temporalAccessor = null;
        try {
            temporalAccessor = Providers.get().parse(value, context.getRowMetadata().getById(columnId));
        } catch (DateTimeException e) {
            // temporalAccessor is left null, this will be used bellow to set empty new value for all fields
            LOGGER.debug("Unable to parse date {}.", value, e);
        }

        // insert new extracted values
        for (final DateFieldMapping dateField : getDateFields()) {
            if (Boolean.valueOf(parameters.get(dateField.key))) {
                String newValue;
                if (temporalAccessor != null && // may occurs if date can not be parsed with pattern
                        temporalAccessor.isSupported(dateField.field)) {
                    newValue = dateField.format(temporalAccessor, context.get(LOCALE_PARAMETER));
                } else {
                    newValue = StringUtils.EMPTY;
                }
                row.set(ActionsUtils.getTargetColumnIds(context).get(dateField.key), newValue);
            }
        }
    }

    /**
     * Get the day label of the week day.
     */
    private static String getLabelDay(TemporalAccessor temporalAccessor, Locale locale) {
        int numDayOfWeek = temporalAccessor.get(ChronoField.DAY_OF_WEEK);
        String[] label = DateFormatSymbols.getInstance(locale).getWeekdays();
        if (numDayOfWeek >= 0 && numDayOfWeek < 7) {
            return label[numDayOfWeek + 1];
        } else if (numDayOfWeek == 7){
            return label[1];
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Get the label of the month of the year.
     */
    private static String getLabelMonth(TemporalAccessor temporalAccessor, Locale locale) {
        int numMonth = temporalAccessor.get(ChronoField.MONTH_OF_YEAR);
        String[] label = DateFormatSymbols.getInstance(locale).getMonths();
        if (numMonth >= 1 && numMonth <= 12) {
            return label[numMonth - 1];
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public Set<Behavior> getBehavior() {
        return EnumSet.of(Behavior.METADATA_CREATE_COLUMNS, Behavior.NEED_STATISTICS_PATTERN);
    }

    protected static class DateFieldMapping {

        private final String key;

        private final TemporalField field;

        private final BiFunction<TemporalAccessor, Locale, String> formatter;

        public DateFieldMapping(String key, TemporalField field) {
            this(key, field, null);
        }

        public DateFieldMapping(String key, TemporalField field, BiFunction<TemporalAccessor, Locale, String> formatter) {
            this.key = key;
            this.field = field;
            this.formatter = formatter;
        }

        public String format(TemporalAccessor temporalAccessor, Locale locale) {
            return formatter == null ? String.valueOf(temporalAccessor.get(field)) : formatter.apply(temporalAccessor, locale);
        }
    }
}
