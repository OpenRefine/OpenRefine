/*

Copyright 2010,2011 Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.grel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.refine.expr.functions.Coalesce;
import com.google.refine.expr.functions.Cross;
import com.google.refine.expr.functions.FacetCount;
import com.google.refine.expr.functions.Get;
import com.google.refine.expr.functions.HasField;
import com.google.refine.expr.functions.Jsonize;
import com.google.refine.expr.functions.Length;
import com.google.refine.expr.functions.Slice;
import com.google.refine.expr.functions.ToDate;
import com.google.refine.expr.functions.TimeSinceUnixEpochToDate;
import com.google.refine.expr.functions.ToNumber;
import com.google.refine.expr.functions.ToString;
import com.google.refine.expr.functions.Type;
import com.google.refine.expr.functions.arrays.InArray;
import com.google.refine.expr.functions.arrays.Join;
import com.google.refine.expr.functions.arrays.Reverse;
import com.google.refine.expr.functions.arrays.Sort;
import com.google.refine.expr.functions.arrays.Uniques;
import com.google.refine.expr.functions.booleans.And;
import com.google.refine.expr.functions.booleans.Not;
import com.google.refine.expr.functions.booleans.Or;
import com.google.refine.expr.functions.booleans.Xor;
import com.google.refine.expr.functions.date.DatePart;
import com.google.refine.expr.functions.date.Inc;
import com.google.refine.expr.functions.date.Now;
import com.google.refine.expr.functions.html.InnerHtml;
import com.google.refine.expr.functions.html.ParseHtml;
import com.google.refine.expr.functions.math.ACos;
import com.google.refine.expr.functions.math.ASin;
import com.google.refine.expr.functions.math.ATan;
import com.google.refine.expr.functions.math.ATan2;
import com.google.refine.expr.functions.math.Abs;
import com.google.refine.expr.functions.math.Ceil;
import com.google.refine.expr.functions.math.Combin;
import com.google.refine.expr.functions.math.Cos;
import com.google.refine.expr.functions.math.Cosh;
import com.google.refine.expr.functions.math.Degrees;
import com.google.refine.expr.functions.math.Even;
import com.google.refine.expr.functions.math.Exp;
import com.google.refine.expr.functions.math.Fact;
import com.google.refine.expr.functions.math.FactN;
import com.google.refine.expr.functions.math.Floor;
import com.google.refine.expr.functions.math.GreatestCommonDenominator;
import com.google.refine.expr.functions.math.LeastCommonMultiple;
import com.google.refine.expr.functions.math.Ln;
import com.google.refine.expr.functions.math.Log;
import com.google.refine.expr.functions.math.Max;
import com.google.refine.expr.functions.math.Min;
import com.google.refine.expr.functions.math.Mod;
import com.google.refine.expr.functions.math.Multinomial;
import com.google.refine.expr.functions.math.Odd;
import com.google.refine.expr.functions.math.Pow;
import com.google.refine.expr.functions.math.Quotient;
import com.google.refine.expr.functions.math.Radians;
import com.google.refine.expr.functions.math.RandomNumber;
import com.google.refine.expr.functions.math.Round;
import com.google.refine.expr.functions.math.Sin;
import com.google.refine.expr.functions.math.Sinh;
import com.google.refine.expr.functions.math.Sum;
import com.google.refine.expr.functions.math.Tan;
import com.google.refine.expr.functions.math.Tanh;
import com.google.refine.expr.functions.strings.Chomp;
import com.google.refine.expr.functions.strings.Contains;
import com.google.refine.expr.functions.strings.Decode;
import com.google.refine.expr.functions.strings.DetectLanguage;
import com.google.refine.expr.functions.strings.Diff;
import com.google.refine.expr.functions.strings.Encode;
import com.google.refine.expr.functions.strings.EndsWith;
import com.google.refine.expr.functions.strings.Escape;
import com.google.refine.expr.functions.strings.Find;
import com.google.refine.expr.functions.strings.Fingerprint;
import com.google.refine.expr.functions.strings.IndexOf;
import com.google.refine.expr.functions.strings.LastIndexOf;
import com.google.refine.expr.functions.strings.MD5;
import com.google.refine.expr.functions.strings.Match;
import com.google.refine.expr.functions.strings.NGram;
import com.google.refine.expr.functions.strings.NGramFingerprint;
import com.google.refine.expr.functions.strings.ParseJson;
import com.google.refine.expr.functions.strings.ParseUri;
import com.google.refine.expr.functions.strings.Partition;
import com.google.refine.expr.functions.strings.Phonetic;
import com.google.refine.expr.functions.strings.RPartition;
import com.google.refine.expr.functions.strings.Range;
import com.google.refine.expr.functions.strings.Reinterpret;
import com.google.refine.expr.functions.strings.Replace;
import com.google.refine.expr.functions.strings.ReplaceChars;
import com.google.refine.expr.functions.strings.ReplaceEach;
import com.google.refine.expr.functions.strings.SHA1;
import com.google.refine.expr.functions.strings.SmartSplit;
import com.google.refine.expr.functions.strings.Split;
import com.google.refine.expr.functions.strings.SplitByCharType;
import com.google.refine.expr.functions.strings.SplitByLengths;
import com.google.refine.expr.functions.strings.StartsWith;
import com.google.refine.expr.functions.strings.ToLowercase;
import com.google.refine.expr.functions.strings.ToTitlecase;
import com.google.refine.expr.functions.strings.ToUppercase;
import com.google.refine.expr.functions.strings.Trim;
import com.google.refine.expr.functions.strings.Unescape;
import com.google.refine.expr.functions.strings.Unicode;
import com.google.refine.expr.functions.strings.UnicodeType;
import com.google.refine.expr.functions.xml.InnerXml;
import com.google.refine.expr.functions.xml.OwnText;
import com.google.refine.expr.functions.xml.ParseXml;
import com.google.refine.expr.functions.xml.SelectXml;
import com.google.refine.expr.functions.xml.WholeText;
import com.google.refine.expr.functions.xml.ScriptText;
import com.google.refine.expr.functions.xml.XmlAttr;
import com.google.refine.expr.functions.xml.XmlText;
import com.google.refine.expr.functions.xml.Parent;
import com.google.refine.grel.controls.Filter;
import com.google.refine.grel.controls.ForEach;
import com.google.refine.grel.controls.ForEachIndex;
import com.google.refine.grel.controls.ForNonBlank;
import com.google.refine.grel.controls.ForRange;
import com.google.refine.grel.controls.If;
import com.google.refine.grel.controls.IsBlank;
import com.google.refine.grel.controls.IsEmptyString;
import com.google.refine.grel.controls.IsError;
import com.google.refine.grel.controls.IsNonBlank;
import com.google.refine.grel.controls.IsNotNull;
import com.google.refine.grel.controls.IsNull;
import com.google.refine.grel.controls.IsNumeric;
import com.google.refine.grel.controls.With;

public class ControlFunctionRegistry {

    static private Map<String, Function> s_nameToFunction = new HashMap<String, Function>();
    static private Map<Function, String> s_functionToName = new HashMap<Function, String>();

    static private Map<String, Control> s_nameToControl = new HashMap<String, Control>();
    static private Map<Control, String> s_controlToName = new HashMap<Control, String>();

    static public Function getFunction(String name) {
        return s_nameToFunction.get(name);
    }

    static public String getFunctionName(Function f) {
        return s_functionToName.get(f);
    }

    static public Set<Entry<String, Function>> getFunctionMapping() {
        return s_nameToFunction.entrySet();
    }

    static public Map<String, Function> getFunctionMap() {
        return Collections.unmodifiableMap(s_nameToFunction);
    }

    static public Control getControl(String name) {
        return s_nameToControl.get(name);
    }

    static public String getControlName(Control f) {
        return s_controlToName.get(f);
    }

    static public Set<Entry<String, Control>> getControlMapping() {
        return s_nameToControl.entrySet();
    }

    static public Map<String, Control> getControlMap() {
        return Collections.unmodifiableMap(s_nameToControl);
    }

    static public void registerFunction(String name, Function f) {
        s_nameToFunction.put(name, f);
        s_functionToName.put(f, name);
    }

    static public void registerControl(String name, Control c) {
        s_nameToControl.put(name, c);
        s_controlToName.put(c, name);
    }

    static {
        registerFunction("coalesce", new Coalesce());
        registerFunction("type", new Type());

        registerFunction("toString", new ToString());
        registerFunction("toNumber", new ToNumber());
        registerFunction("toDate", new ToDate());
        registerFunction("timeSinceUnixEpochToDate", new TimeSinceUnixEpochToDate());

        registerFunction("toUppercase", new ToUppercase());
        registerFunction("toLowercase", new ToLowercase());
        registerFunction("toTitlecase", new ToTitlecase());

        registerFunction("detectLanguage", new DetectLanguage());

        registerFunction("hasField", new HasField());
        registerFunction("get", new Get());
        registerFunction("slice", new Slice());
        registerFunction("substring", new Slice());
        registerFunction("replace", new Replace());
        registerFunction("replaceChars", new ReplaceChars());
        registerFunction("replaceEach", new ReplaceEach());
        registerFunction("range", new Range());
        registerFunction("split", new Split());
        registerFunction("smartSplit", new SmartSplit());
        registerFunction("splitByCharType", new SplitByCharType());
        registerFunction("splitByLengths", new SplitByLengths());
        registerFunction("partition", new Partition());
        registerFunction("rpartition", new RPartition());
        registerFunction("trim", new Trim());
        registerFunction("strip", new Trim());
        registerFunction("contains", new Contains());
        registerFunction("escape", new Escape());
        registerFunction("unescape", new Unescape());
        registerFunction("encode", new Encode());
        registerFunction("decode", new Decode());
        registerFunction("length", new Length());
        registerFunction("sha1", new SHA1());
        registerFunction("md5", new MD5());
        registerFunction("unicode", new Unicode());
        registerFunction("unicodeType", new UnicodeType());
        registerFunction("diff", new Diff());
        registerFunction("chomp", new Chomp());
        registerFunction("fingerprint", new Fingerprint());
        registerFunction("ngramFingerprint", new NGramFingerprint());
        registerFunction("phonetic", new Phonetic());
        registerFunction("reinterpret", new Reinterpret());
        registerFunction("jsonize", new Jsonize());
        registerFunction("parseJson", new ParseJson());
        registerFunction("ngram", new NGram());
        registerFunction("match", new Match());
        registerFunction("find", new Find());

        registerFunction("parseUri", new ParseUri());

        // XML and HTML functions from JSoup
        registerFunction("parseXml", new ParseXml());
        registerFunction("parseHtml", new ParseHtml());
        registerFunction("select", new SelectXml());
        registerFunction("xmlAttr", new XmlAttr());
        registerFunction("htmlAttr", new XmlAttr());
        registerFunction("xmlText", new XmlText());
        registerFunction("htmlText", new XmlText());
        registerFunction("innerXml", new InnerXml());
        registerFunction("innerHtml", new InnerHtml());
        registerFunction("ownText", new OwnText());
        registerFunction("wholeText", new WholeText());
        registerFunction("parent", new Parent());
        registerFunction("scriptText", new ScriptText());

        registerFunction("indexOf", new IndexOf());
        registerFunction("lastIndexOf", new LastIndexOf());
        registerFunction("startsWith", new StartsWith());
        registerFunction("endsWith", new EndsWith());
        registerFunction("join", new Join());
        registerFunction("reverse", new Reverse());
        registerFunction("sort", new Sort());
        registerFunction("uniques", new Uniques());
        registerFunction("inArray", new InArray());

        registerFunction("now", new Now());
        registerFunction("inc", new Inc());
        registerFunction("datePart", new DatePart());

        registerFunction("acos", new ACos());
        registerFunction("asin", new ASin());
        registerFunction("atan", new ATan());
        registerFunction("atan2", new ATan2());
        registerFunction("cos", new Cos());
        registerFunction("cosh", new Cosh());
        registerFunction("sin", new Sin());
        registerFunction("sinh", new Sinh());
        registerFunction("tan", new Tan());
        registerFunction("tanh", new Tanh());
        registerFunction("round", new Round());
        registerFunction("floor", new Floor());
        registerFunction("ceil", new Ceil());
        registerFunction("even", new Even());
        registerFunction("odd", new Odd());
        registerFunction("abs", new Abs());
        registerFunction("mod", new Mod());
        registerFunction("max", new Max());
        registerFunction("min", new Min());
        registerFunction("log", new Log());
        registerFunction("ln", new Ln());
        registerFunction("pow", new Pow());
        registerFunction("exp", new Exp());
        registerFunction("sum", new Sum());
        registerFunction("fact", new Fact());
        registerFunction("factn", new FactN());
        registerFunction("combin", new Combin());
        registerFunction("degrees", new Degrees());
        registerFunction("radians", new Radians());
        registerFunction("randomNumber", new RandomNumber());
        registerFunction("random", new RandomNumber());
        registerFunction("gcd", new GreatestCommonDenominator());
        registerFunction("lcm", new LeastCommonMultiple());
        registerFunction("multinomial", new Multinomial());
        registerFunction("quotient", new Quotient());

        registerFunction("and", new And());
        registerFunction("or", new Or());
        registerFunction("not", new Not());
        registerFunction("xor", new Xor());

        registerFunction("cross", new Cross());

        registerFunction("facetCount", new FacetCount());

        registerControl("if", new If());
        registerControl("with", new With());
        registerControl("forEach", new ForEach());
        registerControl("forEachIndex", new ForEachIndex());
        registerControl("forRange", new ForRange());
        registerControl("filter", new Filter());
        registerControl("forNonBlank", new ForNonBlank());

        registerControl("isNull", new IsNull());
        registerControl("isNotNull", new IsNotNull());
        registerControl("isEmptyString", new IsEmptyString());
        registerControl("isBlank", new IsBlank());
        registerControl("isNonBlank", new IsNonBlank());
        registerControl("isNumeric", new IsNumeric());
        registerControl("isError", new IsError());
    }
}
