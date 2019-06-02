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

package org.talend.dataprep.schema.csv;

import static org.talend.dataprep.schema.csv.CSVFormatFamily.TEXT_ENCLOSURE_CHAR;

import java.io.*;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.schema.Serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import au.com.bytecode.opencsv.CSVReader;

@Service("serializer#csv")
public class CSVSerializer implements Serializer {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CSVSerializer.class);

    /** The default enclosure character. */
    @Value("${default.import.text.enclosure:\"}")
    private String defaultTextEnclosure;

    /** The default escape character. */
    @Value("${default.import.text.escape:\u0000}")
    private String defaultEscapeChar;

    /** Task executor used to serialize CSV dataset into JSON. */
    @Resource(name = "serializer#csv#executor")
    private TaskExecutor executor;

    @Override
    public InputStream serialize(InputStream rawContent, DataSetMetadata metadata, long limit) {
        try {
            PipedInputStream pipe = new PipedInputStream();
            PipedOutputStream jsonOutput = new PipedOutputStream(pipe);
            // Serialize asynchronously for better performance (especially if caller doesn't consume all, see sampling).
            Runnable r = () -> {
                final Map<String, String> parameters = metadata.getContent().getParameters();
                final String separator = parameters.get(CSVFormatFamily.SEPARATOR_PARAMETER);
                final char actualSeparator = separator.charAt(0);
                final char textEnclosureChar = getFromParameters(parameters, TEXT_ENCLOSURE_CHAR, defaultTextEnclosure);
                final char escapeChar = getFromParameters(parameters, CSVFormatFamily.ESCAPE_CHAR, defaultEscapeChar);

                try (InputStreamReader input = new InputStreamReader(rawContent, metadata.getEncoding());
                        CSVReader reader = new CSVReader(input, actualSeparator, textEnclosureChar, escapeChar)) {

                    JsonGenerator generator = new JsonFactory().createGenerator(jsonOutput);
                    int i = 0;
                    while (i++ < metadata.getContent().getNbLinesInHeader()) {
                        reader.readNext(); // Skip all header lines
                    }
                    generator.writeStartArray();
                    writeLineContent(reader, metadata, generator, separator, limit);
                    generator.writeEndArray();
                    generator.flush();
                } catch (Exception e) {
                    // Consumer may very well interrupt consumption of stream (in case of limit(n) use for sampling).
                    // This is not an issue as consumer is allowed to partially consumes results, it's up to the
                    // consumer to ensure data it consumed is consistent.
                    LOGGER.debug("Unable to continue serialization for {}. Skipping remaining content.", metadata.getId(), e);
                } finally {
                    try {
                        jsonOutput.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to close output", e);
                    }
                }
            };
            executor.execute(r);
            return pipe;
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNABLE_TO_SERIALIZE_TO_JSON, e);
        }
    }

    /**
     * Extract the parameter value from the dataset parameters or return the given default value if not found.
     *
     * @param parameters where to look for the wanted parameter value.
     * @param key the parameter key.
     * @param defaultValue the default value to use if the parameter is not found.
     * @return the parameter value from the dataset parameters or return the given default value if not found.
     */
    private char getFromParameters(Map<String, String> parameters, String key, String defaultValue) {
        final String fromParameters = parameters.get(key);

        // wrong parameter use (empty or more than one character)
        if (fromParameters == null || fromParameters.length() > 1) {
            return StringUtils.isEmpty(defaultValue) ? Character.MIN_VALUE : defaultValue.charAt(0);
        }

        return (fromParameters.length() == 0)  ? Character.MIN_VALUE : fromParameters.charAt(0);
    }

    /**
     * Write the line content.
     *
     * @param reader the csv reader to use as data source.
     * @param metadata the dataset metadata to use to get the columns.
     * @param generator the json generator used to actually write the line content.
     * @param separator the csv separator to use.
     * @param limit The maximum number of lines in the exported content.
     * @throws IOException if an error occurs.
     */
    private void writeLineContent(CSVReader reader, DataSetMetadata metadata, JsonGenerator generator, String separator,
            long limit) throws IOException {
        String[] line;
        int current = 0;

        while ((line = reader.readNext()) != null && withinLimit(limit, current)) {
            // skip empty lines
            if (line.length == 1 && (StringUtils.isEmpty(line[0]) || line[0].charAt(0) == Character.MIN_VALUE)) {
                continue;
            }

            List<ColumnMetadata> columns = metadata.getRowMetadata().getColumns();
            generator.writeStartObject();
            int columnsSize = columns.size();
            for (int i = 0; i < columnsSize; i++) {
                ColumnMetadata columnMetadata = columns.get(i);

                generator.writeFieldName(columnMetadata.getId());

                // deal with additional content (line.length > columns.size)
                if (i == columnsSize - 1 && line.length > columnsSize) {
                    String additionalContent = getRemainingColumns(line, i, separator);
                    generator.writeString(cleanCharacters(additionalContent));
                }
                // deal with fewer content (line.length < columns.size)
                else if (i < line.length && line[i] != null) {
                    generator.writeString(cleanCharacters(line[i]));
                }
                // deal with null
                else {
                    generator.writeNull();
                }
            }
            generator.writeEndObject();
            current++;
        }
    }

    private boolean withinLimit(long limit, int current) {
        return limit < 0 || current < limit;
    }

    private String cleanCharacters(final String value) {
        return StringUtils.remove(value, Character.MIN_VALUE); // unicode null character
    }

    /**
     * Return the remaining raw (with separators) content of the column.
     *
     * @param line the line to parse.
     * @param start where to start in the line.
     * @param separator the separator to append.
     * @return the remaining raw (with separators) content of the column.
     */
    private String getRemainingColumns(String[] line, int start, String separator) {
        StringBuilder buffer = new StringBuilder();
        for (int j = start; j < line.length; j++) {
            buffer.append(line[j]);
            if (j < line.length - 1) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }
}
