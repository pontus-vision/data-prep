package org.talend.dataprep.schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.talend.dataprep.schema.csv.CsvDetector;
import org.talend.dataprep.schema.csv.CsvFormatFamily;
import org.talend.dataprep.schema.html.HtmlDetector;
import org.talend.dataprep.schema.html.HtmlFormatFamily;
import org.talend.dataprep.schema.xls.XlsDetector;
import org.talend.dataprep.schema.xls.XlsFormatFamily;

@Configuration
public class SchemaDetectionConfiguration {

    /** The default enclosure character. */
    @Value("${default.import.text.enclosure:\"}")
    private String defaultTextEnclosure;

    /** The default escape character. */
    @Value("${default.import.text.escape:\u0000}")
    private String defaultEscapeChar;

    @Value("${dataset.import.xls.size.column.max:1000}")
    private int maxNumberOfColumns;

    @Bean
    public DataprepSchema dataprepSchema() {
        DataprepSchema dataprepSchema = new DataprepSchema();
        dataprepSchema.registerDetector(xlsDetector());
        dataprepSchema.registerDetector(htmlDetector());
        dataprepSchema.registerDetector(csvDetector());
        return dataprepSchema;
    }

//    @Bean
//    @Order(value = 1)
    public XlsDetector xlsDetector() {
        XlsDetector xlsDetector = new XlsDetector();
        xlsDetector.getFormatFamily().getSchemaGuesser().setMaxNumberOfColumns(maxNumberOfColumns);
        return xlsDetector;
    }

//    @Bean(XlsFormatFamily.BEAN_ID)
    public XlsFormatFamily xlsFormatFamily() {
        return xlsDetector().getFormatFamily();
    }

//    @Bean
//    @Order(value = 2)
    public HtmlDetector htmlDetector() {
        return new HtmlDetector();
    }

//    @Bean(HtmlFormatFamily.BEAN_ID)
    public HtmlFormatFamily htmlFormatFamily() {
        return htmlDetector().getFormatFamily();
    }
//
//    @Bean
//    @Order(value = 3)
    public CsvDetector csvDetector() {
        return new CsvDetector();
    }

//    @Bean(CsvFormatFamily.BEAN_ID)
    public CsvFormatFamily csvFormatFamily() {
        CsvFormatFamily csvFormatFamily = csvDetector().getFormatFamily();
        csvFormatFamily.setDefaultTextEnclosure(defaultTextEnclosure);
        csvFormatFamily.setDefaultEscapeChar(defaultEscapeChar);
        return csvFormatFamily;
    }


}
