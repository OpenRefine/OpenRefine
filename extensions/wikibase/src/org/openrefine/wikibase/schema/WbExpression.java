/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
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
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueNoValueException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueSomeValueException;
import org.openrefine.wikibase.schema.validation.ValidationState;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The base interface for all expressions, which evaluate to a particular type T in an ExpressionContext.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = WbStringConstant.class, name = "wbstringconstant"),
        @Type(value = WbStringVariable.class, name = "wbstringvariable"),
        @Type(value = WbLocationConstant.class, name = "wblocationconstant"),
        @Type(value = WbLocationVariable.class, name = "wblocationvariable"),
        @Type(value = WbItemConstant.class, name = "wbitemconstant"),
        @Type(value = WbItemVariable.class, name = "wbitemvariable"),
        @Type(value = WbEntityVariable.class, name = "wbentityvariable"),
        @Type(value = WbLanguageConstant.class, name = "wblanguageconstant"),
        @Type(value = WbLanguageVariable.class, name = "wblanguagevariable"),
        @Type(value = WbDateConstant.class, name = "wbdateconstant"),
        @Type(value = WbDateVariable.class, name = "wbdatevariable"),
        @Type(value = WbMonolingualExpr.class, name = "wbmonolingualexpr"),
        @Type(value = WbPropConstant.class, name = "wbpropconstant"),
        @Type(value = WbEntityIdValueConstant.class, name = "wbentityidvalueconstant"),
        @Type(value = WbLanguageConstant.class, name = "wblanguageconstant"),
        @Type(value = WbLanguageVariable.class, name = "wblanguagevariable"),
        @Type(value = WbQuantityExpr.class, name = "wbquantityexpr"),
        @Type(value = WbItemEditExpr.class, name = "wbitemeditexpr"),
        @Type(value = WbMediaInfoEditExpr.class, name = "wbmediainfoeditexpr"), })
public interface WbExpression<T> {

    /**
     * Evaluates the value expression in a given context, returns a Wikibase value suitable to be the target of a claim.
     * 
     * As a premise to calling that method, we assume that calling {@link #validate(ValidationState)} did not log any
     * error in the validation state.
     */
    public T evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException, SpecialValueNoValueException, SpecialValueSomeValueException;

    /**
     * Check that this expression is fully formed and ready to be evaluated.
     * 
     * @param validation
     *            the state in which to log any validation errors
     */
    public void validate(ValidationState validation);
}
