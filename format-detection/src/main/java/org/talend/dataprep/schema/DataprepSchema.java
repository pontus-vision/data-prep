package org.talend.dataprep.schema;

import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * API class following the model of {@link org.apache.tika.Tika}.
 */
public class DataprepSchema {

    private static final byte[] NO_BOM = {};

    private static final byte[] UTF_16_LE_BOM = { (byte) 0xFF, (byte) 0xFE };

    private static final byte[] UTF_16_BE_BOM = { (byte) 0xFE, (byte) 0xFF };

    private static final byte[] UTF_32_LE_BOM = { (byte) 0xFE, (byte) 0xFF, (byte) 0x00, (byte) 0x00 };

    private static final byte[] UTF_32_BE_BOM = { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };

    private static final byte[] UTF_8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private static final byte[][] BOMS = { NO_BOM, UTF_16_LE_BOM, UTF_16_BE_BOM, UTF_32_LE_BOM, UTF_32_BE_BOM, UTF_8_BOM };

    private List<Detector> detectors = new ArrayList<>();

    public Format detect(byte[] inputFirstBytes) throws IOException {
        return detect(inputFirstBytes, detectors);
    }

    private Format detect(byte[] inputFirstBytes, List<Detector> detectors) throws IOException {
        Format result = null;
        for (byte[] bom : BOMS) { // TODO: encoding is only for txt/* contents => CSV/HTML only for us
            for (Detector detector : detectors) {
                // all the depending resources will be closed
                try (TemporaryResources tmp = new TemporaryResources()) {
                    TikaInputStream tis = TikaInputStream.get(addBom(new ByteArrayInputStream(inputFirstBytes), bom), tmp);
                    result = detector.detect(new Metadata(), tis);
                }
                if (result != null)
                    break;
            }
            if (result != null)
                break;
        }
        return result;
    }

    public DeSerializer getDeserializer(MediaType mediaType) {
        Optional<Detector> firstDetector = detectors.stream()
                .filter(d -> d.getFormatFamily().getMediaType().getBaseType().equals(mediaType.getBaseType()))
                .findFirst();
        if (firstDetector.isPresent()) {
            Detector detector = firstDetector.get();
            return detector.getFormatFamily().getDeSerializer();
        } else {
            return null;
        }
    }

    public FormatFamily getFormatFamily(String formatFamilyId) {
        return detectors.stream().map(Detector::getFormatFamily).filter(ff -> formatFamilyId.equals(ff.getBeanId())).findAny().orElse(null);
    }

    public boolean hasFormatFamily(String formatFamilyId) {
        return detectors.stream().map(Detector::getFormatFamily).anyMatch(ff -> formatFamilyId.equals(ff.getBeanId()));
    }

    /**
     * Add a bom (Byte Order Mark) to the given input stream.
     *
     * see https://en.wikipedia.org/wiki/Byte_order_mark.
     *
     * @param stream the stream to prefix with the bom.
     * @param bom the bom to prefix the stream with.
     * @return the given stream prefixed with the given bom.
     */
    private static InputStream addBom(InputStream stream, byte[] bom) {
        ByteArrayInputStream le = new ByteArrayInputStream(bom);
        return new SequenceInputStream(le, stream);
    }

    public void registerDetector(Detector detector) {
        detectors.add(detector);
    }

}
