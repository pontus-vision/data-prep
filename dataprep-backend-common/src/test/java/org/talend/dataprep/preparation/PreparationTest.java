package org.talend.dataprep.preparation;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.talend.dataprep.api.preparation.PreparationActions.ROOT_CONTENT;
import static org.talend.dataprep.api.preparation.Step.ROOT_STEP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.preparation.PreparationActions;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.preparation.store.PreparationRepository;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PreparationTest.class)
@Configuration
@ComponentScan(basePackages = "org.talend.dataprep")
@EnableAutoConfiguration
public class PreparationTest {

    @Autowired
    private PreparationRepository repository;

    @Test
    public void testDefaultPreparation() throws Exception {
        final Preparation preparation = Preparation.defaultPreparation("12345");
        assertThat(preparation.id(), is("ec718238e9bfe45f58031313b79501a3cc55b186"));
        assertThat(preparation.getHeadId(), is(ROOT_STEP.id()));
    }

    @Test
    public void rootObjects() throws Exception {
        assertThat(repository.get("cdcd5c9a3a475f2298b5ee3f4258f8207ba10879", PreparationActions.class), notNullValue());
        assertThat(repository.get("cdcd5c9a3a475f2298b5ee3f4258f8207ba10879", Step.class), nullValue());
        assertThat(repository.get("f6e172c33bdacbc69bca9d32b2bd78174712a171", PreparationActions.class), nullValue());
        assertThat(repository.get("f6e172c33bdacbc69bca9d32b2bd78174712a171", Step.class), notNullValue());
    }

    @Test
    public void testTimestamp() throws Exception {
        Preparation preparation = new Preparation("1234", ROOT_STEP.id());
        final long time0 = preparation.getLastModificationDate();
        TimeUnit.MILLISECONDS.sleep(50);
        preparation.updateLastModificationDate();
        final long time1 = preparation.getLastModificationDate();
        assertThat(time0, lessThan(time1));
    }

    @Test
    public void testId_withName() throws Exception {
        // Preparation id with name
        Preparation preparation = new Preparation("1234", ROOT_STEP.id());
        preparation.setName("My Preparation");
        final String id0 = preparation.getId();
        assertThat(id0, is("e34f3448d71dac403df5305a04086fc88054aa15"));
        // Same preparation (but with empty name)
        preparation.setName("");
        final String id1 = preparation.getId();
        assertThat(id1, is("ae242b07084aa7b8341867a8be1707f4d52501d1"));
        // Same preparation (but with null name, null and empty names should be treated all the same)
        preparation.setName(null);
        final String id2 = preparation.getId();
        assertThat(id2, is("ae242b07084aa7b8341867a8be1707f4d52501d1"));
    }

    @Test
    public void initialStep() {
        final List<Action> actions = getSimpleAction("uppercase", "column_name", "lastname");

        final PreparationActions newContent = new PreparationActions(actions);
        repository.add(newContent);

        final Step s = new Step(ROOT_STEP.id(), newContent.id());
        repository.add(s);

        Preparation preparation = new Preparation("1234", s.id());
        repository.add(preparation);

        assertThat(preparation.id(), Is.is("ae242b07084aa7b8341867a8be1707f4d52501d1"));
    }

    @Test
    public void initialStepWithAppend() {
        final List<Action> actions = getSimpleAction("uppercase", "column_name", "lastname");

        final PreparationActions newContent = ROOT_CONTENT.append(actions);
        repository.add(newContent);

        final Step s = new Step(ROOT_STEP.id(), newContent.id());
        repository.add(s);

        final Preparation preparation = new Preparation("1234", s.id());
        repository.add(preparation);

        assertThat(preparation.id(), Is.is("ae242b07084aa7b8341867a8be1707f4d52501d1"));
    }

    @Test
    public void stepsWithAppend() {
        final List<Action> actions = getSimpleAction("uppercase", "column_name", "lastname");

        final PreparationActions newContent1 = ROOT_CONTENT.append(actions);
        repository.add(newContent1);
        final PreparationActions newContent2 = newContent1.append(actions);
        repository.add(newContent2);

        // Steps
        final Step s1 = new Step(ROOT_STEP.id(), newContent1.id());
        repository.add(s1);

        final Step s2 = new Step(s1.id(), newContent2.id());
        repository.add(s2);

        // Preparation
        final Preparation preparation = new Preparation("1234", s2.id());
        repository.add(preparation);

        assertThat(preparation.id(), Is.is("ae242b07084aa7b8341867a8be1707f4d52501d1"));
    }

    @Test
    public void should_merge_from_other() {
        Preparation source = new Preparation();

        Preparation theOtherOne = new Preparation();
        theOtherOne.setAuthor("Joss Stone");
        theOtherOne.setCreationDate(source.getCreationDate() - 1000);
        theOtherOne.setDataSetId("ds#123456");
        theOtherOne.setLastModificationDate(theOtherOne.getCreationDate() + 12345682);
        theOtherOne.setName("my preparation name");
        theOtherOne.setHeadId(ROOT_STEP.id());

        Preparation actual = source.merge(theOtherOne);

        assertEquals(actual, theOtherOne);
    }

    @Test
    public void should_merge_from_source() {
        Preparation theOtherOne = new Preparation();

        Preparation source = new Preparation();
        source.setAuthor("Bloc Party");
        source.setCreationDate(theOtherOne.getCreationDate() - 1000);
        source.setDataSetId("ds#65478");
        source.setLastModificationDate(source.getCreationDate() + 2658483);
        source.setName("banquet");
        source.setHeadId(ROOT_STEP.id());

        Preparation actual = source.merge(theOtherOne);

        assertEquals(actual, source);
    }

    public static List<Action> getSimpleAction(final String actionName, final String paramKey, final String paramValue) {
        final Action action = new Action();
        action.setAction(actionName);
        action.getParameters().put(paramKey, paramValue);

        final List<Action> actions = new ArrayList<>();
        actions.add(action);

        return actions;
    }
}