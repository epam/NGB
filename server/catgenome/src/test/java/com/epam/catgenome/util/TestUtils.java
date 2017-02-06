/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.epam.catgenome.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;

/**
 * Source:      TestUtils
 * Created:     15.12.16, 12:18
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * <p>
 * An utility class for common test operations
 * </p>
 */
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Asserts that specified task failed with one of specified exception classes
     * @param task a task, represented by {@code TestTask} functional interface
     * @param exceptions a list of Exception classes, that should happen
     */
    public static void assertFail(TestTask task, List<Class<? extends Exception>> exceptions) {
        boolean fail = false;

        try {
            task.doTest();
        } catch (Exception e) {
            for (Class<? extends Exception> eClass : exceptions) {
                if (e.getClass().equals(eClass)) {
                    fail = true;
                    break;
                }
            }
        }

        Assert.assertTrue("An exception should be thrown, but nothing happened", fail);
    }

    public static void warmUp(TestTask task, int warmingCount) throws Exception {
        for (int i = 0; i < warmingCount; i++) {
            task.doTest();
        }
    }

    public static <A> void warmUp(PreparationTask<A> preparation, TestFunction<A> task, int warmingCount)
        throws Exception {
        for (int i = 0; i < warmingCount; i++) {
            A arg = preparation.doPrapare();
            task.doTest(arg);
        }
    }

    public static double measurePerformance(TestTask task, int attemptsCount) throws Exception {
        List<Double> timings = new ArrayList<>();
        for (int i = 0; i < attemptsCount; i++) {
            double time1 = Utils.getSystemTimeMilliseconds();
            task.doTest();
            double time2 = Utils.getSystemTimeMilliseconds();

            timings.add(time2 - time1);
        }

        return timings.stream().collect(Collectors.averagingDouble(x -> x));
    }

    public static <T> double measurePerformance(PreparationTask<T> preparation, TestFunction<T> task,
                                                int attemptsCount) throws Exception {
        List<Double> timings = measurePerformanceTimings(preparation, task, attemptsCount);

        return calculateAverage(timings);
    }

    public static <T> List<Double> measurePerformanceTimings(PreparationTask<T> preparation, TestFunction<T> task,
                                                             int attemptsCount) throws Exception {
        List<Double> timings = new ArrayList<>();
        for (int i = 0; i < attemptsCount; i++) {
            T arg = preparation.doPrapare();
            double time1 = Utils.getSystemTimeMilliseconds();
            task.doTest(arg);
            double time2 = Utils.getSystemTimeMilliseconds();

            timings.add(time2 - time1);
        }

        return timings;
    }

    public static double calculateAverage(List<Double> values) {
        return values.stream().collect(Collectors.averagingDouble(x -> x));
    }

    @FunctionalInterface
    public interface TestTask {
        void doTest() throws Exception;
    }

    @FunctionalInterface
    public interface PreparationTask<T> {
        T doPrapare() throws Exception;
    }

    @FunctionalInterface
    public interface TestFunction<T> {
        void doTest(T arg) throws Exception;
    }
}
