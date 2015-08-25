package org.talend.dataprep.transformation.api.action.dynamic;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.dataprep.transformation.api.action.dynamic.cluster.ClusterParameters;

/**
 * Unit test for the dynamic type.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DynamicTypeTest.class)
@Configuration
@ComponentScan(basePackages = "org.talend.dataprep")
@EnableAutoConfiguration
public class DynamicTypeTest {

    /** Spring application context. */
    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldReturnNull() {
        assertNull(DynamicType.fromAction(null));
        assertNull(DynamicType.fromAction("blahblah"));
    }

    @Test
    public void shouldReturnCluster() {
        final DynamicType actual = DynamicType.fromAction("textclustering");
        assertEquals(DynamicType.TEXT_CLUSTER, actual);
    }

    @Test
    public void shouldReturnClusterType() {
        final DynamicParameters actual = DynamicType.fromAction("textclustering").getGenerator(context);
        assertTrue(actual instanceof ClusterParameters);
    }

}