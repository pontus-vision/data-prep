package org.talend.dataprep.schema;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.talend.dataprep.schema.csv.CsvDetector;
import org.talend.dataprep.schema.html.HtmlDetector;
import org.talend.dataprep.schema.xls.XlsDetector;

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
        XlsDetector xlsDetector = new XlsDetector();
        xlsDetector.getFormatFamily().getSchemaGuesser().setMaxNumberOfColumns(maxNumberOfColumns);
        dataprepSchema.registerDetector(xlsDetector);
        dataprepSchema.registerDetector(new HtmlDetector());
        CsvDetector detector = new CsvDetector();
        detector.getFormatFamily().setDefaultEscapeChar(defaultEscapeChar);
        detector.getFormatFamily().setDefaultTextEnclosure(defaultTextEnclosure);
        dataprepSchema.registerDetector(detector);
        return dataprepSchema;
    }

}
