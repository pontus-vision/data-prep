package org.talend.dataprep.user.store.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.talend.dataprep.api.user.UserData;

/**
 * Unit test for the FileSystemUserDataRepository.
 * 
 * @see FileSystemUserDataRepository
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FileSystemUserDataRepository.class)
@ComponentScan(basePackages = "org.talend.dataprep.user.store.file")
@TestPropertySource(inheritLocations = false, inheritProperties = false, properties = { "user.data.store=file",
        "user.data.store.file.location=${java.io.tmpdir}/test/store/userdata" })
public class FileSystemUserDataRepositoryTest {

    /** The user data repository to test. */
    @Autowired
    private FileSystemUserDataRepository repository;

    /**
     * Clean up repository after each test.
     */
    @After
    public void cleanUpAfterTests() {
        repository.clear();
    }

    @Test
    public void shouldGetWhatWasAdded() {

        UserData expected = new UserData("123");
        expected.addFavoriteDataset("dataset#987654");
        expected.addFavoriteDataset("dataset#lkj-sfdgs-63563-sfgsfg'");

        repository.save(expected);

        final UserData actual = repository.get(expected.getUserId());
        assertEquals(expected, actual);
    }

    @Test
    public void saveTwiceShouldUpdateUserData() {

        UserData expected = new UserData("123");
        expected.addFavoriteDataset("dataset#987654");
        expected.addFavoriteDataset("dataset#lkj-sfdgs-63563-sfgsfg'");

        repository.save(expected);

        final Set<String> favorites = expected.getFavoritesDatasets();
        favorites.clear();
        favorites.add("dataset#kljkdflkdjqsfhqlkjsdfhqslkj");
        favorites.add("dataset#kiuyftuozyfgtzeuifytzeaiufyt");

        repository.save(expected);

        final UserData actual = repository.get(expected.getUserId());
        assertEquals(expected, actual);
    }

    @Test
    public void saveNullShouldNotThrowException() {
        repository.save(null);
    }

    @Test
    public void removeShouldRemoveFile() {
        UserData userData = new UserData("123");
        userData.addFavoriteDataset("dataset#1");
        userData.addFavoriteDataset("dataset#2");

        repository.save(userData);
        repository.remove(userData.getUserId());

        assertNull(repository.get(userData.getUserId()));
    }

    @Test
    public void clearShouldRemoveAllFiles() {

        int count = 24;
        for (int i = 0; i < count; i++) {
            UserData userData = new UserData(String.valueOf(i));
            userData.addFavoriteDataset("dataset#" + i);
            userData.addFavoriteDataset("dataset#" + i + 1);
            repository.save(userData);
        }

        repository.clear();
        for (int i = 0; i < count; i++) {
            assertNull(repository.get(String.valueOf(i)));
        }

    }

}