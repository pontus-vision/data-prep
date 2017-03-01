package org.talend.dataprep.dataset.service.locator;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.talend.dataprep.api.dataset.DataSetLocation;
import org.talend.dataprep.api.dataset.location.HttpLocation;
import org.talend.dataprep.dataset.DataSetBaseTest;

/**
 * Unit test for the HttpDataSetLocator.
 * 
 * @see HttpDataSetLocator
 */
public class HttpDataSetLocatorTest extends DataSetBaseTest {

    /** The dataset locator to test. */
    @Autowired
    HttpDataSetLocator locator;

    /** Jackson builder. */
    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @Test
    public void should_accept_media_type() {
        assertTrue(locator.accept(HttpDataSetLocator.MEDIA_TYPE));
    }

    @Test
    public void should_not_accept_media_type() {
        assertFalse(locator.accept("application/vnd.remote-ds.h"));
        assertFalse(locator.accept(""));
        assertFalse(locator.accept(null));
    }

    @Test
    public void should_parse_location() throws IOException {
        // given
        InputStream location = HttpDataSetLocatorTest.class.getResourceAsStream("http_location_ok.json");
        HttpLocation expected = new HttpLocation();
        expected.setUrl("http://www.lequipe.fr");

        // when
        DataSetLocation actual = locator.getLocation(location);

        // then
        assertEquals(expected, actual);
    }

}