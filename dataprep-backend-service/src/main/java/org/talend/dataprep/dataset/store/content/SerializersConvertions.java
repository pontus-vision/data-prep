package org.talend.dataprep.dataset.store.content;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSetLocation;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.conversions.BeanConversionService;
import org.talend.dataprep.processor.BeanConversionServiceWrapper;
import org.talend.dataprep.schema.SheetContent;
import org.talend.dataprep.schema.csv.CsvFormatFamily;
import org.talend.dataprep.schema.xls.XlsFormatFamily;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.talend.dataprep.conversions.BeanConversionService.fromBean;

@Component
public class SerializersConvertions extends BeanConversionServiceWrapper {

    @Override
    public BeanConversionService doWith(BeanConversionService conversionService, String beanName,
            ApplicationContext applicationContext) {
        conversionService.register(fromBean(DataSetMetadata.class).toBeans(SheetContent.class)
                .using(SheetContent.class, SerializersConvertions::getSchemaFromDataSetMetadata)
                .build());
        return conversionService;
    }

    private static SheetContent getSchemaFromDataSetMetadata(DataSetMetadata dataSetMetadata, SheetContent content) {
        content.setName(dataSetMetadata.getSheetName());
        content.setParameters(new HashMap<>(dataSetMetadata.getContent().getParameters()));
        int nbLinesInHeader = dataSetMetadata.getContent().getNbLinesInHeader();
        Map<String, String> parameters = content.getParameters();
        if (!parameters.containsKey(CsvFormatFamily.HEADER_NB_LINES_PARAMETER)) {
            parameters.put(CsvFormatFamily.HEADER_NB_LINES_PARAMETER, Integer.toString(nbLinesInHeader));
        }
        if (!parameters.containsKey(XlsFormatFamily.HEADER_NB_LINES_PARAMETER)) {
            parameters.put(XlsFormatFamily.HEADER_NB_LINES_PARAMETER, Integer.toString(nbLinesInHeader));
        }
        DataSetLocation location = dataSetMetadata.getLocation();
        if (location != null) {
            parameters.putAll(location.additionalParameters());
        }
        content.setColumnMetadatas(dataSetMetadata.getRowMetadata()
                .getColumns()
                .stream()
                .map(cm -> new SheetContent.ColumnMetadata.Builder().id(Integer.parseInt(cm.getId()))
                        .headerSize(cm.getHeaderSize())
                        .name(cm.getName())
                        .build())
                .collect(Collectors.toList()));
        return content;
    }

}
