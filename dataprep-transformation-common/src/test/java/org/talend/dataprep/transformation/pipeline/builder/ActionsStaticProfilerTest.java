package org.talend.dataprep.transformation.pipeline.builder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.actions.ActionDefinition;
import org.talend.dataprep.transformation.actions.ActionRegistry;
import org.talend.dataprep.transformation.actions.category.ScopeCategory;
import org.talend.dataprep.transformation.actions.common.ImplicitParameters;
import org.talend.dataprep.transformation.actions.common.RunnableAction;

@RunWith(MockitoJUnitRunner.class)
public class ActionsStaticProfilerTest {

    @Mock
    private ActionRegistry actionRegistry;
    private List<ColumnMetadata> columns;
    private RunnableAction action;
    private ActionDefinition definition;
    private ColumnMetadata column1;
    private ColumnMetadata column2;

    @Before
    public void setUp() throws Exception {
        //
        columns = new ArrayList<>();
        column1 = ColumnMetadata.Builder.column() //
                .type(Type.STRING) //
                .computedId("0000") //
                .build();
        column2 = ColumnMetadata.Builder.column() //
                .type(Type.INTEGER) //
                .computedId("0001") //
                .build();
        columns.add(column1);
        columns.add(column2);

        //
        action = new RunnableAction() {
            @Override
            public String getName() {
                return "MyAction";
            }

            @Override
            public Map<String, String> getParameters() {
                return Collections.singletonMap(ImplicitParameters.COLUMN_ID.getKey(), "0000");
            }
        };

        //
        definition = mock(ActionDefinition.class);
        when(actionRegistry.get("MyAction")).thenReturn(definition);
    }

    @Test
    public void shouldProfileWhenOneColumnChanged() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.VALUES_COLUMN));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertTrue(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertTrue(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertTrue(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.VALUES_COLUMN, profile.getBehavior(action).iterator().next());

        // Then
        assertNull(profile.getBehavior(new RunnableAction()));
    }

    @Test
    public void shouldProfileWhenAllColumnChanged() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.VALUES_ALL));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertTrue(profile.getFilterForFullAnalysis().test(column1));
        assertTrue(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertTrue(profile.getFilterForInvalidAnalysis().test(column1));
        assertTrue(profile.getFilterForInvalidAnalysis().test(column2));
        assertTrue(profile.getFilterForPatternAnalysis().test(column1));
        assertTrue(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.VALUES_ALL, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenColumnTypeChanged() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.METADATA_CHANGE_TYPE));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertTrue(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertTrue(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertTrue(profile.getFilterForPatternAnalysis().test(column1));
        assertEquals(ActionDefinition.Behavior.METADATA_CHANGE_TYPE, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenMultipleColumnChanged() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.VALUES_MULTIPLE_COLUMNS));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertTrue(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertTrue(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertTrue(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.VALUES_MULTIPLE_COLUMNS, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenColumnCreated() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.METADATA_CREATE_COLUMNS));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertFalse(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertFalse(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertFalse(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.METADATA_CREATE_COLUMNS, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenColumnCopied() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.METADATA_COPY_COLUMNS));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertTrue(profile.needFullAnalysis());
        assertFalse(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertFalse(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertFalse(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.METADATA_COPY_COLUMNS, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenColumnDeleted() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.METADATA_DELETE_COLUMNS));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertFalse(profile.needFullAnalysis());
        assertFalse(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertFalse(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertFalse(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.METADATA_DELETE_COLUMNS, profile.getBehavior(action).iterator().next());
    }

    @Test
    public void shouldProfileWhenColumnNameChanged() {
        // Given
        final List<RunnableAction> actions = Collections.singletonList(action);
        final ActionsStaticProfiler profiler = new ActionsStaticProfiler(actionRegistry);
        when(definition.getBehavior()).thenReturn(EnumSet.of(ActionDefinition.Behavior.METADATA_CHANGE_NAME));
        when(definition.adapt(any(ScopeCategory.class))).thenReturn(definition);

        // When
        final ActionsProfile profile = profiler.profile(columns, actions);

        // Then
        assertFalse(profile.needFullAnalysis());
        assertFalse(profile.getFilterForFullAnalysis().test(column1));
        assertFalse(profile.getFilterForFullAnalysis().test(column2));
        assertFalse(profile.needOnlyInvalidAnalysis());
        assertFalse(profile.getFilterForInvalidAnalysis().test(column1));
        assertFalse(profile.getFilterForInvalidAnalysis().test(column2));
        assertFalse(profile.getFilterForPatternAnalysis().test(column1));
        assertFalse(profile.getFilterForPatternAnalysis().test(column2));
        assertEquals(ActionDefinition.Behavior.METADATA_CHANGE_NAME, profile.getBehavior(action).iterator().next());
    }

}