package org.openl.rules.testmethod.result;

import java.lang.reflect.Array;

public class ArrayComparator implements TestResultComparator {

    public boolean compareResult(Object actualResult, Object expectedResult, Double delta) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        }
        int len = Array.getLength(actualResult);
        if (len != Array.getLength(expectedResult)) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            Object actualArrayResult = Array.get(actualResult, i);
            Object expectedArrayResult = Array.get(expectedResult, i);

            TestResultComparator comp = TestResultComparatorFactory.getComparator(actualArrayResult,
                    expectedArrayResult);
            if (!comp.compareResult(actualArrayResult, expectedArrayResult, delta)) {
                return false;
            }
        }

        return true;
    }

}
