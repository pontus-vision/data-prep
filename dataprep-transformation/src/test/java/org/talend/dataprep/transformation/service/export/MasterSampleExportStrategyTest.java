package org.talend.dataprep.transformation.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.export.ExportParametersUtil;
import org.talend.dataprep.api.filter.FilterService;
import org.talend.dataprep.api.preparation.PreparationDTO;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.command.preparation.PreparationSummaryGet;
import org.talend.dataprep.dataset.adapter.DatasetClient;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.lock.LockFactory;
import org.talend.dataprep.security.SecurityProxy;
import org.talend.dataprep.transformation.api.transformer.TransformerFactory;
import org.talend.dataprep.transformation.format.FormatRegistrationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MasterSampleExportStrategyTest {

    @InjectMocks
    private MasterSampleExportStrategy masterSampleExportStrategy;

    @Mock
    private CacheKeyGenerator cacheKeyGenerator;

    @Mock
    private ExportParametersUtil exportParametersUtil;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ContentCache contentCache;

    @Mock
    private FilterService filterService;

    /** The format registration service. */
    @Mock
    private FormatRegistrationService formatRegistrationService;

    /** The transformer factory. */
    @Mock
    private TransformerFactory factory;

    /** The lock factory. */
    @Mock
    private LockFactory lockFactory;

    /** The security proxy to use to get the dataset despite the roles/ownership. */
    @Mock
    private SecurityProxy securityProxy;

    @Mock
    private DatasetClient datasetClient;

    @Mock
    private ExportFormat exportFormat;

    private PreparationDTO preparationDTO = new PreparationDTO();

    public void setUp() {
        when(formatRegistrationService.getByName("format")).thenReturn(exportFormat);
        PreparationSummaryGet preparationSummaryGet = mock(PreparationSummaryGet.class);
        preparationDTO.setId("prepId");
        preparationDTO.setHeadId("head ID");
        when(applicationContext.getBean(PreparationSummaryGet.class, preparationDTO.getId(), preparationDTO.getHeadId())).thenReturn(preparationSummaryGet);
        when(preparationSummaryGet.execute()).thenReturn(preparationDTO);
    }

    @Test
    public void execute() throws IOException {
        ExportParameters parameters = new ExportParameters();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        masterSampleExportStrategy.execute(parameters).writeTo(outputStream);

    }
}
