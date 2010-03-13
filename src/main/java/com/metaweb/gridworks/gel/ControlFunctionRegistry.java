package com.metaweb.gridworks.gel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.metaweb.gridworks.expr.functions.Get;
import com.metaweb.gridworks.expr.functions.Length;
import com.metaweb.gridworks.expr.functions.Slice;
import com.metaweb.gridworks.expr.functions.ToDate;
import com.metaweb.gridworks.expr.functions.ToNumber;
import com.metaweb.gridworks.expr.functions.ToString;
import com.metaweb.gridworks.expr.functions.Type;
import com.metaweb.gridworks.expr.functions.arrays.Join;
import com.metaweb.gridworks.expr.functions.arrays.Reverse;
import com.metaweb.gridworks.expr.functions.arrays.Sort;
import com.metaweb.gridworks.expr.functions.booleans.And;
import com.metaweb.gridworks.expr.functions.booleans.Not;
import com.metaweb.gridworks.expr.functions.booleans.Or;
import com.metaweb.gridworks.expr.functions.date.Inc;
import com.metaweb.gridworks.expr.functions.date.Now;
import com.metaweb.gridworks.expr.functions.math.Ceil;
import com.metaweb.gridworks.expr.functions.math.Exp;
import com.metaweb.gridworks.expr.functions.math.Floor;
import com.metaweb.gridworks.expr.functions.math.Ln;
import com.metaweb.gridworks.expr.functions.math.Log;
import com.metaweb.gridworks.expr.functions.math.Max;
import com.metaweb.gridworks.expr.functions.math.Min;
import com.metaweb.gridworks.expr.functions.math.Mod;
import com.metaweb.gridworks.expr.functions.math.Pow;
import com.metaweb.gridworks.expr.functions.math.Round;
import com.metaweb.gridworks.expr.functions.strings.Contains;
import com.metaweb.gridworks.expr.functions.strings.Diff;
import com.metaweb.gridworks.expr.functions.strings.EndsWith;
import com.metaweb.gridworks.expr.functions.strings.Fingerprint;
import com.metaweb.gridworks.expr.functions.strings.IndexOf;
import com.metaweb.gridworks.expr.functions.strings.LastIndexOf;
import com.metaweb.gridworks.expr.functions.strings.MD5;
import com.metaweb.gridworks.expr.functions.strings.NGramFingerprint;
import com.metaweb.gridworks.expr.functions.strings.Partition;
import com.metaweb.gridworks.expr.functions.strings.Phonetic;
import com.metaweb.gridworks.expr.functions.strings.RPartition;
import com.metaweb.gridworks.expr.functions.strings.Reinterpret;
import com.metaweb.gridworks.expr.functions.strings.Replace;
import com.metaweb.gridworks.expr.functions.strings.ReplaceChars;
import com.metaweb.gridworks.expr.functions.strings.SHA1;
import com.metaweb.gridworks.expr.functions.strings.Split;
import com.metaweb.gridworks.expr.functions.strings.SplitByCharType;
import com.metaweb.gridworks.expr.functions.strings.StartsWith;
import com.metaweb.gridworks.expr.functions.strings.ToLowercase;
import com.metaweb.gridworks.expr.functions.strings.ToTitlecase;
import com.metaweb.gridworks.expr.functions.strings.ToUppercase;
import com.metaweb.gridworks.expr.functions.strings.Trim;
import com.metaweb.gridworks.expr.functions.strings.Unescape;
import com.metaweb.gridworks.expr.functions.strings.Unicode;
import com.metaweb.gridworks.expr.functions.strings.UnicodeType;
import com.metaweb.gridworks.gel.controls.ForEach;
import com.metaweb.gridworks.gel.controls.ForNonBlank;
import com.metaweb.gridworks.gel.controls.If;
import com.metaweb.gridworks.gel.controls.IsBlank;
import com.metaweb.gridworks.gel.controls.IsError;
import com.metaweb.gridworks.gel.controls.IsNonBlank;
import com.metaweb.gridworks.gel.controls.IsNotNull;
import com.metaweb.gridworks.gel.controls.IsNull;
import com.metaweb.gridworks.gel.controls.IsNumeric;
import com.metaweb.gridworks.gel.controls.With;

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

    static protected void registerFunction(String name, Function f) {
        s_nameToFunction.put(name, f);
        s_functionToName.put(f, name);
    }

    static protected void registerControl(String name, Control c) {
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
        
        registerFunction("get", new Get());
        registerFunction("slice", new Slice());
        registerFunction("substring", new Slice());
        registerFunction("replace", new Replace());
        registerFunction("replaceChars", new ReplaceChars());
        registerFunction("split", new Split());
        registerFunction("splitByCharType", new SplitByCharType());
        registerFunction("partition", new Partition());
        registerFunction("rpartition", new RPartition());
        registerFunction("trim", new Trim());
        registerFunction("strip", new Trim());
        registerFunction("contains", new Contains());
        registerFunction("unescape", new Unescape());
        registerFunction("length", new Length());
        registerFunction("sha1", new SHA1());
        registerFunction("md5", new MD5());
        registerFunction("unicode", new Unicode());
        registerFunction("unicodeType", new UnicodeType());
        registerFunction("diff", new Diff());
        registerFunction("chomp", new Diff());
        registerFunction("fingerprint", new Fingerprint());
        registerFunction("ngramFingerprint", new NGramFingerprint());
        registerFunction("phonetic", new Phonetic());
        registerFunction("reinterpret", new Reinterpret());
        
        registerFunction("indexOf", new IndexOf());
        registerFunction("lastIndexOf", new LastIndexOf());
        registerFunction("startsWith", new StartsWith());
        registerFunction("endsWith", new EndsWith());
        registerFunction("join", new Join());
        registerFunction("reverse", new Reverse());
        registerFunction("sort", new Sort());

        registerFunction("now", new Now());
        registerFunction("inc", new Inc());
        
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
        
        registerFunction("and", new And());
        registerFunction("or", new Or());
        registerFunction("not", new Not());

        registerControl("if", new If());
        registerControl("with", new With());
        registerControl("forEach", new ForEach());
        registerControl("forNonBlank", new ForNonBlank());
        
        registerControl("isNull", new IsNull());
        registerControl("isNotNull", new IsNotNull());
        registerControl("isBlank", new IsBlank());
        registerControl("isNonBlank", new IsNonBlank());
        registerControl("isNumeric", new IsNumeric());
        registerControl("isError", new IsError());
    }
}
