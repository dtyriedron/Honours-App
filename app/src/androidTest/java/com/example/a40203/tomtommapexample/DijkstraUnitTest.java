package com.example.a40203.tomtommapexample;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.tomtom.online.sdk.common.location.LatLng;
import com.tomtom.online.sdk.map.Route;
import com.tomtom.online.sdk.routing.data.FullRoute;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DijkstraUnitTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.example.a40203.tomtommapexample", appContext.getPackageName());
    }

    @Test
    public void testDijkstra(){

        double[][] adjacencyMatrix = { { 0, 4, 0, 0, 0, 0, 0, 8, 0 },
                { 4, 0, 8, 0, 0, 0, 0, 11, 0 },
                { 0, 8, 0, 7, 0, 4, 0, 0, 2 },
                { 0, 0, 7, 0, 9, 14, 0, 0, 0 },
                { 0, 0, 0, 9, 0, 10, 0, 0, 0 },
                { 0, 0, 4, 0, 10, 0, 2, 0, 0 },
                { 0, 0, 0, 14, 0, 2, 0, 1, 6 },
                { 8, 11, 0, 0, 0, 0, 1, 0, 7 },
                { 0, 0, 2, 0, 0, 0, 6, 7, 0 } };

        CalcDijkstra.calculate(adjacencyMatrix,0);

        String output = "com.example.a40203.tomtommapexample.Vertex  Distance   Path\n" +
                "0 -> 1       4.0        0 1 \n" +
                "0 -> 2       12.0       0 1 2 \n" +
                "0 -> 3       19.0       0 1 2 3 \n" +
                "0 -> 4       21.0       0 7 6 5 4 \n" +
                "0 -> 5       11.0       0 7 6 5 \n" +
                "0 -> 6       9.0        0 7 6 \n" +
                "0 -> 7       8.0        0 7 \n" +
                "0 -> 8       14.0       0 1 2 8 ";


        assertEquals(output, CalcDijkstra.getPrinter());
    }
}
