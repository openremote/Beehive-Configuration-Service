package org.openremote.beehive.configuration.model;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountTest {

    private static List<ControllerConfiguration> configurations;
    private static Account account;


    @BeforeClass
    public static void setupClass() {
        account = new Account();

        configurations = new ArrayList<>(3);
        configurations.add(mockConfiguration("name0", "category0"));
        configurations.add(mockConfiguration("name1", "category0"));
        configurations.add(mockConfiguration("name2", "category1"));

        account.setControllerConfigurations(configurations);
    }

    private static ControllerConfiguration mockConfiguration(String name, String category) {
        ControllerConfiguration configuration = mock(ControllerConfiguration.class);
        when(configuration.getName()).thenReturn(name);
        when(configuration.getCategory()).thenReturn(category);
        return configuration;
    }

    @Test
    public void testFindWithNoNameAndNoCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory(null, null);
        assertEquals(3, results.size());
        assertThat(results, containsInAnyOrder(configurations.toArray()));
    }

    @Test
    public void testFindWithNameAndNoCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory("name0", null);
        assertEquals(1, results.size());
        assertThat(results, containsInAnyOrder(configurations.get(0)));
    }

    @Test
    public void testFindWithNoNameAndCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory(null, "category0");
        assertEquals(2, results.size());
        assertThat(results, containsInAnyOrder(configurations.get(0), configurations.get(1)));
    }

    @Test
    public void testFindWithNameAndCategoryFound() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory("name1", "category0");
        assertEquals(1, results.size());
        assertThat(results, containsInAnyOrder(configurations.get(1)));
    }

    @Test
    public void testFindWithCorrentNameAndWrongCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory("name1", "n/a");
        assertEquals(0, results.size());
    }

    @Test
    public void testFindWithWrongNameAndCorrectCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory("n/a", "category2");
        assertEquals(0, results.size());
    }

    @Test
    public void testFindWithWrongNameAndWrongCategory() {
        Collection<ControllerConfiguration> results = account.getControllerConfigurationsByNameAndCategory("foo", "bar");
        assertEquals(0, results.size());
    }

}