package com.sirelon.marsroverphotos;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Mock
    DataManager dataManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);

        assertNotNull(dataManager);
    }
}