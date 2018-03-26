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

package org.talend.dataprep.schema.xls;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.microsoft.POIFSContainerDetector;
import org.apache.tika.parser.pkg.ZipContainerDetector;
import org.talend.dataprep.schema.Detector;
import org.talend.dataprep.schema.Format;
import org.talend.dataprep.schema.FormatUtils;

import java.io.IOException;

/**
 * This class is used as a detector for XLS format. It is an adaptor for the TIKA {@link POIFSContainerDetector} and
 * {@link ZipContainerDetector}.
 */
public class XlsDetector implements Detector {

    /**
     * The XLS MIME type returned by TIKA for XLS format
     */
    private static final String OLD_XLS_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private POIFSContainerDetector poifsContainerDetector = new POIFSContainerDetector();

    private ZipContainerDetector zipContainerDetector = new ZipContainerDetector();

    /**
     * The xls format family
     */
    private XlsFormatFamily xlsFormatFamily = new XlsFormatFamily();

    /**
     * Reads an input stream and checks if it has a XLS format.
     *
     * The general contract of a detector is to not close the specified stream before returning. It is to the
     * responsibility of the caller to close it. The detector should leverage the mark/reset feature of the specified
     * {@see TikaInputStream} in order to let the stream always return the same bytes.
     *
     * @param metadata the specified TIKA {@link Metadata}
     * @param inputStream the specified input stream
     * @return either null or an XLS format
     * @throws IOException
     */
    @Override
    public Format detect(Metadata metadata, TikaInputStream inputStream) throws IOException {
        Format result = null;

        MediaType mediaType = poifsContainerDetector.detect(inputStream, metadata);
        if (mediaType == null || StringUtils.equals(mediaType.toString(), FormatUtils.UNKNOWN_MEDIA_TYPE)) {
            mediaType = zipContainerDetector.detect(inputStream, new Metadata());
        }

        if (mediaType != null) {
            String mediaTypeName = mediaType.toString();
            if (StringUtils.startsWith(mediaTypeName, XlsFormatFamily.MEDIA_TYPE.toString())
                    || StringUtils.equals(mediaTypeName, OLD_XLS_MEDIA_TYPE)) {
                result = new Format(xlsFormatFamily, FormatUtils.DEFAULT_ENCODING);
            }
        }

        return result;
    }

    @Override
    public XlsFormatFamily getFormatFamily() {
        return xlsFormatFamily;
    }
}
