package com.google.refine.gel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.refine.expr.functions.Cross;
import com.google.refine.expr.functions.FacetCount;
import com.google.refine.expr.functions.Get;
import com.google.refine.expr.functions.HasField;
import com.google.refine.expr.functions.Jsonize;
import com.google.refine.expr.functions.Length;
import com.google.refine.expr.functions.Slice;
import com.google.refine.expr.functions.ToDate;
import com.google.refine.expr.functions.ToNumber;
import com.google.refine.expr.functions.ToString;
import com.google.refine.expr.functions.Type;
import com.google.refine.expr.functions.arrays.Join;
import com.google.refine.expr.functions.arrays.Reverse;
import com.google.refine.expr.functions.arrays.Sort;
import com.google.refine.expr.functions.arrays.Uniques;
import com.google.refine.expr.functions.booleans.And;
import com.google.refine.expr.functions.booleans.Not;
import com.google.refine.expr.functions.booleans.Or;
import com.google.refine.expr.functions.date.DatePart;
import com.google.refine.expr.functions.date.Inc;
import com.google.refine.expr.functions.date.Now;
import com.google.refine.expr.functions.math.Ceil;
import com.google.refine.expr.functions.math.Exp;
import com.google.refine.expr.functions.math.Floor;
import com.google.refine.expr.functions.math.Ln;
import com.google.refine.expr.functions.math.Log;
import com.google.refine.expr.functions.math.Max;
import com.google.refine.expr.functions.math.Min;
import com.google.refine.expr.functions.math.Mod;
import com.google.refine.expr.functions.math.Pow;
import com.google.refine.expr.functions.math.Round;
import com.google.refine.expr.functions.math.Sum;
import com.google.refine.expr.functions.strings.Chomp;
import com.google.refine.expr.functions.strings.Contains;
import com.google.refine.expr.functions.strings.Diff;
import com.google.refine.expr.functions.strings.EndsWith;
import com.google.refine.expr.functions.strings.Escape;
import com.google.refine.expr.functions.strings.Fingerprint;
import com.google.refine.expr.functions.strings.IndexOf;
import com.google.refine.expr.functions.strings.LastIndexOf;
import com.google.refine.expr.functions.strings.MD5;
import com.google.refine.expr.functions.strings.Match;
import com.google.refine.expr.functions.strings.NGram;
import com.google.refine.expr.functions.strings.NGramFingerprint;
import com.google.refine.expr.functions.strings.ParseJson;
import com.google.refine.expr.functions.strings.Partition;
import com.google.refine.expr.functions.strings.Phonetic;
import com.google.refine.expr.functions.strings.RPartition;
import com.google.refine.expr.functions.strings.Reinterpret;
import com.google.refine.expr.functions.strings.Replace;
import com.google.refine.expr.functions.strings.ReplaceChars;
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
import com.google.refine.gel.controls.Filter;
import com.google.refine.gel.controls.ForEach;
import com.google.refine.gel.controls.ForEachIndex;
import com.google.refine.gel.controls.ForNonBlank;
import com.google.refine.gel.controls.ForRange;
import com.google.refine.gel.controls.If;
import com.google.refine.gel.controls.IsBlank;
import com.google.refine.gel.controls.IsError;
import com.google.refine.gel.controls.IsNonBlank;
import com.google.refine.gel.controls.IsNotNull;
import com.google.refine.gel.controls.IsNull;
import com.google.refine.gel.controls.IsNumeric;
import com.google.refine.gel.controls.With;

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

    static public Control getControl(String name) {
        return s_nameToControl.get(name);
    }
    static public String getControlName(Control f) {
        return s_controlToName.get(f);
    }
    static public Set<Entry<String, Control>> getControlMapping() {
        return s_nameToControl.entrySet();
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
        registerFunction("type", new Type());

        registerFunction("toString", new ToString());
        registerFunction("toNumber", new ToNumber());
        registerFunction("toDate", new ToDate());

        registerFunction("toUppercase", new ToUppercase());
        registerFunction("toLowercase", new ToLowercase());
        registerFunction("toTitlecase", new ToTitlecase());

        registerFunction("hasField", new HasField());
        registerFunction("get", new Get());
        registerFunction("slice", new Slice());
        registerFunction("substring", new Slice());
        registerFunction("replace", new Replace());
        registerFunction("replaceChars", new ReplaceChars());
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

        registerFunction("indexOf", new IndexOf());
        registerFunction("lastIndexOf", new LastIndexOf());
        registerFunction("startsWith", new StartsWith());
        registerFunction("endsWith", new EndsWith());
        registerFunction("join", new Join());
        registerFunction("reverse", new Reverse());
        registerFunction("sort", new Sort());
        registerFunction("uniques", new Uniques());

        registerFunction("now", new Now());
        registerFunction("inc", new Inc());
        registerFunction("datePart", new DatePart());

        registerFunction("round", new Round());
        registerFunction("floor", new Floor());
        registerFunction("ceil", new Ceil());
        registerFunction("mod", new Mod());
        registerFunction("max", new Max());
        registerFunction("min", new Min());
        registerFunction("log", new Log());
        registerFunction("ln", new Ln());
        registerFunction("pow", new Pow());
        registerFunction("exp", new Exp());
        registerFunction("sum", new Sum());

        registerFunction("and", new And());
        registerFunction("or", new Or());
        registerFunction("not", new Not());

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
        registerControl("isBlank", new IsBlank());
        registerControl("isNonBlank", new IsNonBlank());
        registerControl("isNumeric", new IsNumeric());
        registerControl("isError", new IsError());
    }
}
