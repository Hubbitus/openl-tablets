/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.ss.usermodel;

import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.formula.EvaluationWorkbook;

/**
 * An object that handles instantiating concrete
 *  classes of the various instances one needs for
 *  HSSF and XSSF.
 * Works around a major shortcoming in Java, where we
 *  can't have static methods on interfaces or abstract
 *  classes.
 * This allows you to get the appropriate class for
 *  a given interface, without you having to worry
 *  about if you're dealing with HSSF or XSSF, despite
 *  Java being quite rubbish.
 */
public interface CreationHelper {
    /**
     * Creates a new RichTextString instance
     * @param text The text to initialise the RichTextString with
     */
    RichTextString createRichTextString(String text);

    /**
     * Creates a new DataFormat instance
     */
    DataFormat createDataFormat();

    /**
     * Creates a new Hyperlink, of the given type
     */
    Hyperlink createHyperlink(int type);

    /**
     * Creates EvaluationWorkbook.
     *
     * @return a EvaluationWorkbook instance
     */
    EvaluationWorkbook createEvaluationWorkbook();

    /**
     * Creates FormulaEvaluator - an object that evaluates formula cells.
     *
     * @return a FormulaEvaluator instance
     */
    FormulaEvaluator createFormulaEvaluator();

    /**
     * Creates FormulaEvaluator - an object that evaluates formula cells.
     * 
     * @param udfFinder  pass <code>null</code> for default (AnalysisToolPak only)
     * @return a FormulaEvaluator instance
     */
    FormulaEvaluator createFormulaEvaluator(UDFFinder udfFinder);

    ClientAnchor createClientAnchor();
}