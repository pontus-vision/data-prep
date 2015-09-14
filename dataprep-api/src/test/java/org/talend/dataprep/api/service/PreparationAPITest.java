package org.talend.dataprep.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.dataprep.api.Application;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.ContentCacheKey;
import org.talend.dataprep.preparation.store.PreparationRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;
import static org.talend.dataprep.api.preparation.Step.ROOT_STEP;
import static org.talend.dataprep.test.SameJSONFile.sameJSONAsFile;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest
public class PreparationAPITest extends DataPrepTest {

    @Value("${local.server.port}")
    public int port;

    @Autowired
    PreparationRepository preparationRepository;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    ContentCache cache;

    @Autowired
    protected Jackson2ObjectMapperBuilder builder;

    @Before
    public void setUp() {
        RestAssured.port = port;

        // Overrides connection information with random port value
        MockPropertySource connectionInformation = new MockPropertySource()
                .withProperty("dataset.service.url", "http://localhost:" + port)
                .withProperty("transformation.service.url", "http://localhost:" + port)
                .withProperty("preparation.service.url", "http://localhost:" + port);
        environment.getPropertySources().addFirst(connectionInformation);
    }

    @After
    public void clean() {
        preparationRepository.clear();
        cache.clear();
    }

    //------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------GETTER-------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Test
    public void testEmptyPreparationList() throws Exception {
        assertThat(when().get("/api/preparations").asString(), sameJSONAs("[]"));
        assertThat(when().get("/api/preparations/?format=short").asString(), sameJSONAs("[]"));
        assertThat(when().get("/api/preparations/?format=long").asString(), sameJSONAs("[]"));
    }

    @Test
    public void testPreparationsList() throws Exception {
        // given
        createPreparationFromDataset("1234", "testPreparation");

        // when : short format
        final JsonPath shortFormat = when().get("/api/preparations/?format=short").jsonPath();

        // then
        final List<String> values = shortFormat.getList("");
        assertThat(values.get(0), is("6726763ed6f12386064d41d61ff6580f1cfabc2d"));

        // when : long format
        final JsonPath longFormat = when().get("/api/preparations/?format=long").jsonPath();

        // then
        assertThat(longFormat.getList("dataSetId").size(), is(1));
        assertThat(longFormat.getList("dataSetId").get(0), is("1234"));
        assertThat(longFormat.getList("author").size(), is(1));
        assertThat(longFormat.getList("author").get(0), is("anonymousUser"));
        assertThat(longFormat.getList("id").size(), is(1));
        assertThat(longFormat.getList("id").get(0), is("6726763ed6f12386064d41d61ff6580f1cfabc2d"));
        assertThat(longFormat.getList("actions").size(), is(1));
        assertThat(((List) longFormat.getList("actions").get(0)).size(), is(0));
    }

    @Test
    public void testPreparationGet() throws Exception {
        // when
        final String preparationId = createPreparationFromDataset("1234", "testPreparation");

        // then
        final JsonPath longFormat = given().get("/api/preparations/{id}/details", preparationId).jsonPath();
        assertThat(longFormat.getString("dataSetId"), is("1234"));
        assertThat(longFormat.getString("author"), is("anonymousUser"));
        assertThat(longFormat.getString("id"), is("6726763ed6f12386064d41d61ff6580f1cfabc2d"));
        assertThat(longFormat.getList("actions").size(), is(0));
    }

    //------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------LIFECYCLE-----------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Test
    public void testPreparationUpdate() throws Exception {
        // given
        final String preparationId = createPreparationFromDataset("1234", "original_name");

        JsonPath longFormat = when().get("/api/preparations/?format=long").jsonPath();
        assertThat(longFormat.getList("name").size(), is(1));
        assertThat(longFormat.getList("name").get(0), is("original_name"));
        assertThat(longFormat.getList("id").size(), is(1));
        assertThat(longFormat.getList("id").get(0), is(preparationId));

        // when
        given().contentType(ContentType.JSON).body("{ \"name\": \"updated_name\", \"dataSetId\": \"1234\" }")
                .put("/api/preparations/{id}", preparationId).asString();

        // then
        longFormat = when().get("/api/preparations/?format=long").jsonPath();
        assertThat(longFormat.getList("name").size(), is(1));
        assertThat(longFormat.getList("name").get(0), is("updated_name"));
    }

    @Test
    public void testPreparationDelete() throws Exception {
        // given
        final String preparationId = createPreparationFromDataset("1234", "original_name");

        String list = when().get("/api/preparations").asString();
        assertTrue(list.contains(preparationId));

        // when
        when().delete("/api/preparations/" + preparationId).asString();

        // then
        list = when().get("/api/preparations").asString();
        assertEquals("[]", list);
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------STEPS-------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Test
    public void should_append_action_after_actual_head() throws Exception {
        // when
        final String preparationId = createPreparationFromDataset("1234", "testPreparation");

        // when
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        // then
        final List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath()
                .getList("steps");
        assertThat(steps.size(), is(2));
        assertThat(steps.get(0), is(ROOT_STEP.id()));
        assertThat(steps.get(1), is("3c7b40baca3680c22f8bd7142c95697f7424e37f"));
    }

    @Test
    public void should_save_created_columns_ids_on_append() throws Exception {
        // when
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparation", "text/csv");

        // when
        applyActionFromFile(preparationId, "transformation/copy_firstname.json");

        // then
        final JsonPath jsonPath = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath();
        final List<String> createdColumns = jsonPath.getList("diff[0].createdColumns");
        assertThat(createdColumns, hasSize(1));
        assertThat(createdColumns, hasItem("0006"));
    }

    @Test
    public void should_update_action() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparation", "text/csv");
        applyActionFromFile(preparationId, "transformation/upper_case_lastname.json");
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        assertThat(steps.size(), is(3));
        assertThat(steps.get(0), is(ROOT_STEP.id()));
        assertThat(steps.get(1), is("c713d4988879e2aaab916853b45e4ddf9debe303")); // <- transformation/upper_case_lastname
        assertThat(steps.get(2), is("a16245b478e70fdcc17621b892241ed1284f55ed")); // <- upper_case_firstname

        // when : Update first action (transformation/upper_case_lastname / "2b6ae58738239819df3d8c4063e7cb56f53c0d59") with another action
        final String actionContent3 = IOUtils.toString(PreparationAPITest.class.getResourceAsStream("transformation/lower_case_lastname.json"));
        given().contentType(ContentType.JSON)
                .body(actionContent3)
                .put("/api/preparations/{preparation}/actions/{action}", preparationId,
                        "c713d4988879e2aaab916853b45e4ddf9debe303").then().statusCode(is(200));

        // then : Steps id should have changed due to update
        steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        assertThat(steps.size(), is(3));
        assertThat(steps.get(0), is(ROOT_STEP.id()));
        assertThat(steps.get(1), is("cd3cefd02aa2eec8755bd6fdd77934a6ae958414"));
        assertThat(steps.get(2), is("1e76900b00817d10f81084b71dc97d023085a49b"));
    }

    @Test
    public void should_save_created_columns_ids_on_update() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparation", "text/csv");
        applyActionFromFile(preparationId, "transformation/upper_case_lastname.json");
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        final List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        assertThat(steps.get(1), is("c713d4988879e2aaab916853b45e4ddf9debe303")); // <- transformation/upper_case_lastname

        // when : Update first action (transformation/upper_case_lastname / "2b6ae58738239819df3d8c4063e7cb56f53c0d59")
        // with another action that create a column
        final String updateAction = IOUtils.toString(PreparationAPITest.class.getResourceAsStream("transformation/copy_firstname.json"));
        given().contentType(ContentType.JSON)
                .body(updateAction)
                .put("/api/preparations/{preparation}/actions/{action}", preparationId,
                        "c713d4988879e2aaab916853b45e4ddf9debe303").then().statusCode(is(200));

        // then
        final JsonPath jsonPath = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath();
        final List<String> createdColumns = jsonPath.getList("diff[0].createdColumns");
        assertThat(createdColumns, hasSize(1));
        assertThat(createdColumns, hasItem("0006"));
    }

    @Test
    public void should_delete_preparation_action() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparation", "text/csv");
        applyActionFromFile(preparationId, "transformation/upper_case_lastname.json");
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        final String firstStep = steps.get(1);

        // when
        given().delete("/api/preparations/{preparation}/actions/{action}", preparationId, firstStep)
                .then()
                .statusCode(is(200));

        // then : Steps id should have changed due to update
        steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        assertThat(steps.size(), is(2));
        assertThat(steps.get(0), is(ROOT_STEP.id()));
        assertThat(steps.get(1), is("3c7b40baca3680c22f8bd7142c95697f7424e37f"));
    }

    @Test
    public void should_throw_error_when_preparation_doesnt_exist_on_delete() throws Exception {
        // when : delete unknown preparation action
        final Response response = given().delete("/api/preparations/{preparation}/actions/{action}", "unknown_prep", "unkown_step");

        //then : should have preparation service error
        response.then()
                .statusCode(is(400))
                .body("code", is("TDP_PS_PREPARATION_DOES_NOT_EXIST"));
    }

    @Test
    public void shoud_change_preparation_head() throws Exception {
        // when
        final String preparationId = createPreparationFromDataset("1234", "testPreparation");

        // when
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        // then
        final List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath()
                .getList("steps");
        assertThat(steps.size(), is(2));
        assertThat(steps.get(0), is(ROOT_STEP.id()));
        assertThat(steps.get(1), is("3c7b40baca3680c22f8bd7142c95697f7424e37f"));
    }

    @Test
    public void should_change_preparation_head() throws Exception {
        //given
        final String preparationId = createPreparationFromDataset("1234", "testPreparation");
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        Preparation preparation = preparationRepository.get(preparationId, Preparation.class);
        final String newHead = preparation.getStep().getParent();

        //when
        given().when()//
                .put("/api/preparations/{id}/head/{stepId}", preparationId, newHead)//
                .then()//
                .statusCode(200);

        //then
        preparation = preparationRepository.get(preparationId, Preparation.class);
        assertThat(preparation.getStep().id(), is(newHead));
    }

    @Test
    public void should_throw_exception_on_preparation_head_change_with_unknown_step() throws Exception {
        //given
        final String preparationId = createPreparationFromDataset("1234", "testPreparation");

        //when
        final Response response = given().when()//
                .put("/api/preparations/{id}/head/{stepId}", preparationId, "unknown_step_id");

        //then
        response.then()//
                .statusCode(400)//
                .assertThat()//
                .body("code", is("TDP_PS_PREPARATION_STEP_DOES_NOT_EXIST"));
    }

    //------------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------CONTENT------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------
    @Test
    public void testPreparationInitialContent() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparationContentGet", "text/csv");
        String json = given().get("/api/preparations/{preparation}/details", preparationId).asString();
        Preparation preparation = builder.build().reader(Preparation.class).readValue(json);

        final InputStream expected = PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_with_columns.json");

        // when
        ContentCacheKey key = new ContentCacheKey(preparation, ROOT_STEP.id());
        assertThat(cache.has(key), is(false));
        final String content = when().get("/api/preparations/{id}/content", preparationId).asString();
        assertThat(cache.has(key), is(true));

        // then
        assertThat(content, sameJSONAsFile(expected));
    }

    @Test
    public void testPreparationContentWithActions() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("dataset/dataset.csv", "testPreparationContentGet", "text/csv");
        String json = given().get("/api/preparations/{preparation}/details", preparationId).asString();
        Preparation preparation = builder.build().reader(Preparation.class).readValue(json);
        List<String> steps = preparation.getSteps();

        assertThat(steps.size(), is(1));
        assertThat(steps.get(0), is(ROOT_STEP.id()));

        // when
        applyActionFromFile(preparationId, "transformation/upper_case_firstname.json");

        // then
        steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath().getList("steps");
        assertThat(steps.size(), is(2));
        assertThat(steps.get(0), is(ROOT_STEP.id()));

        // Cache is lazily populated
        ContentCacheKey rootKey = new ContentCacheKey(preparation, ROOT_STEP.id());
        assertThat(cache.has(rootKey), is(false));
        ContentCacheKey step0Key = new ContentCacheKey(preparation, steps.get(0));
        assertThat(cache.has(step0Key), is(false));
        ContentCacheKey step1Key = new ContentCacheKey(preparation, steps.get(1));
        assertThat(cache.has(step1Key), is(false));

        // Request preparation content at different versions (preparation has 2 steps -> Root + Upper Case).
        assertThat(when().get("/api/preparations/{id}/content", preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_firstname_uppercase_with_column.json")));
        assertThat(when().get("/api/preparations/{id}/content?version=head", preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_firstname_uppercase_with_column.json")));
        assertThat(when().get("/api/preparations/{id}/content?version=" + steps.get(0), preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_with_columns.json")));
        assertThat(when().get("/api/preparations/{id}/content?version=" + steps.get(1), preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_firstname_uppercase_with_column.json")));
        assertThat(when().get("/api/preparations/{id}/content?version=origin", preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_with_columns.json")));
        assertThat(when().get("/api/preparations/{id}/content?version=" + ROOT_STEP.id(), preparationId).asString(),
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("dataset/expected_dataset_with_columns.json")));

        // After all these preparation get content, cache should be populated with content
        assertThat(cache.has(rootKey), is(true));
        assertThat(cache.has(step0Key), is(true));
        assertThat(cache.has(step1Key), is(true));
    }

    @Test
    public void shouldGetPreparationContent() throws IOException {
        // given
        final String preparationId = createPreparationFromFile("t-shirt_100.csv", "testPreparationContentGet", "text/csv");

        // when
        String preparationContent = given().get("/api/preparations/{preparation}/content", preparationId).asString();

        // then
        assertThat(preparationContent,
                sameJSONAsFile(PreparationAPITest.class.getResourceAsStream("t-shirt_100.csv.expected.json")));
    }

    @Test
    public void shouldGetPreparationContentSample() throws IOException {
        // given
        final String preparationId = createPreparationFromFile("t-shirt_100.csv", "testPreparationContentGet", "text/csv");

        // when
        String preparationContent = given().get("/api/preparations/{preparation}/content?sample=53", preparationId).asString();

        // then
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(preparationContent);
        JsonNode records = rootNode.findPath("records");
        assertThat(records.size(), is(53));
    }

    @Test
    public void shouldGetPreparationContentWhenInvalidSample() throws IOException {
        // given
        final String preparationId = createPreparationFromFile("t-shirt_100.csv", "testPreparationContentGet", "text/csv");

        // when
        String preparationContent = given().get("/api/preparations/{preparation}/content?sample=mdljshf", preparationId)
                .asString();

        // then
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(preparationContent);
        JsonNode records = rootNode.findPath("records");
        assertThat(records.size(), is(100));
    }

    //------------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------PREVIEW------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Test
    public void testPreparationDiffPreview() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("preview/preview_dataset.csv", "testPreview", "text/csv");
        applyActionFromFile(preparationId, "preview/upper_case_lastname.json");
        applyActionFromFile(preparationId, "preview/upper_case_firstname.json");
        applyActionFromFile(preparationId, "preview/delete_city.json");

        final List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath()
                .getList("steps");
        final String firstActionStep = steps.get(1);
        final String lastStep = steps.get(steps.size() - 1);

        final String input = "{" //
                + "   \"preparationId\": \"" + preparationId + "\",\n" //
                + "   \"currentStepId\": \"" + firstActionStep + "\",\n" // action 1
                + "   \"previewStepId\": \"" + lastStep + "\",\n" // action 1 + 2 + 3
                + "   \"tdpIds\": [2, 4, 6]" //
                + "}";

        final InputStream expectedDiffStream = PreparationAPITest.class
                .getResourceAsStream("preview/expected_diff_preview.json");

        // when
        final String diff = given().contentType(ContentType.JSON).body(input).when().post("/api/preparations/preview/diff")
                .asString();

        // then
        assertThat(diff, sameJSONAsFile(expectedDiffStream));
    }

    @Test
    public void testPreparationUpdatePreview() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("preview/preview_dataset.csv", "testPreview", "text/csv");
        applyActionFromFile(preparationId, "preview/upper_case_lastname.json");
        applyActionFromFile(preparationId, "preview/upper_case_firstname.json");
        applyActionFromFile(preparationId, "preview/delete_city.json");

        final List<String> steps = given().get("/api/preparations/{preparation}/details", preparationId).jsonPath()
                .getList("steps");
        final String lastStep = steps.get(steps.size() - 1);

        final String input = "{" //
                + "   \"preparationId\": \"" + preparationId + "\",\n" //
                + "   \"currentStepId\": \"" + lastStep + "\",\n" // action 1 + 2 + 3
                + "   \"updateStepId\": \"" + lastStep + "\",\n" // action 3
                + "   \"tdpIds\": [2, 4, 6]," //
                + "   \"action\": {" //
                + "       \"action\": \"delete_on_value\",\n"//
                + "       \"parameters\": {" //
                + "           \"column_id\": \"0006\"," //
                + "           \"value\": \"Coast city\","//
                + "           \"scope\": \"column\""//
                + "       }" //
                + "   }"//
                + "}";

        final InputStream expectedDiffStream = PreparationAPITest.class
                .getResourceAsStream("preview/expected_update_preview.json");

        // when
        final String diff = given().contentType(ContentType.JSON).body(input).when().post("/api/preparations/preview/update")
                .asString();

        // then
        assertThat(diff, sameJSONAsFile(expectedDiffStream));
    }

    @Test
    public void testPreparationAddPreviewOnPreparation() throws Exception {
        // given
        final String preparationId = createPreparationFromFile("preview/preview_dataset.csv", "testPreview", "text/csv");
        applyActionFromFile(preparationId, "preview/upper_case_lastname.json");
        applyActionFromFile(preparationId, "preview/upper_case_firstname.json");
        applyActionFromFile(preparationId, "preview/delete_city.json");

        final String input = "{" //
                + "   \"preparationId\": \"" + preparationId + "\",\n" //
                + "   \"tdpIds\": [2, 4, 6],\n" //
                + "   \"action\": {\n"
                + "         \"action\": \"uppercase\",\n"
                + "         \"parameters\": {\n"
                + "             \"column_id\": \"0005\",\n"
                + "             \"column_name\": \"alive\"\n,"
                + "             \"scope\": \"column\"\n"
                + "         }\n" //
                + "    }\n" //
                + "}";
        final InputStream expectedPreviewStream = PreparationAPITest.class
                .getResourceAsStream("preview/expected_add_preview.json");

        // when
        final String preview = given().contentType(ContentType.JSON).body(input).when().post("/api/preparations/preview/add")
                .asString();

        // then
        assertThat(preview, sameJSONAsFile(expectedPreviewStream));
    }

    @Test
    public void testPreparationAddPreviewOnDataset() throws Exception {
        // given
        final String datasetId = createDataset("preview/preview_dataset.csv", "testPreview", "text/csv");

        final String input = "{" //
                + "   \"datasetId\": \"" + datasetId + "\",\n" //
                + "   \"tdpIds\": [2, 4, 6],\n" //
                + "   \"action\": {\n"
                + "         \"action\": \"uppercase\",\n"
                + "         \"parameters\": {\n"
                + "             \"column_id\": \"0005\",\n"
                + "             \"column_name\": \"alive\"\n,"
                + "             \"scope\": \"column\"\n"
                + "         }\n" //
                + "    }\n" //
                + "}";
        final InputStream expectedPreviewStream = PreparationAPITest.class
                .getResourceAsStream("preview/expected_add_preview_on_dataset.json");

        // when
        final String preview = given().contentType(ContentType.JSON).body(input).when().post("/api/preparations/preview/add")
                .asString();

        // then
        assertThat(preview, sameJSONAsFile(expectedPreviewStream));
    }
}
