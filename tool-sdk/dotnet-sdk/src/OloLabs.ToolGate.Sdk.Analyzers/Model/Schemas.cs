// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text.RegularExpressions;
using Microsoft.CodeAnalysis;
using OloLabs.ToolGate.Shared;

namespace OloLabs.ToolGate.Sdk.Analyzers.Model;

/// <summary>Schema generation rules of descriptor §5, generation 1, over Roslyn type symbols.</summary>
internal sealed class Schemas
{
    public const long IntMin = int.MinValue;
    public const long IntMax = int.MaxValue;
    public const long Safe = 9007199254740991L;

    private readonly Compilation compilation;
    private readonly List<string> stack = new();
    private readonly HashSet<string> recursive = new();
    private readonly SortedDictionary<string, object?> defs = new(StringComparer.Ordinal);

    public Schemas(Compilation compilation)
    {
        this.compilation = compilation;
    }

    /// <summary>The closed input object over the given properties.</summary>
    public Dictionary<string, object?> Inputs(Dictionary<string, object?> properties, List<string> required)
    {
        var schema = Closed(properties, required);
        if (defs.Count > 0) schema["$defs"] = new Dictionary<string, object?>(defs);
        return schema;
    }

    /// <summary>The output schema of a return type.</summary>
    public Dictionary<string, object?> Output(ITypeSymbol type, Location at)
    {
        var schema = Schema(type, null, at, "result");
        if (!Equals(schema.TryGetValue("type", out var t) ? t : null, "object") && !schema.ContainsKey("$ref"))
        {
            throw new Problem(at, "the return type must be a record, a class, a string-keyed dictionary or ToolResult");
        }
        if (defs.Count > 0) schema["$defs"] = new Dictionary<string, object?>(defs);
        return schema;
    }

    /// <summary>Whether a declared type is Nullable&lt;T&gt; or a nullable-annotated reference type.</summary>
    public static bool Optional(ITypeSymbol type, NullableAnnotation annotation)
    {
        if (IsNullableValue(type)) return true;
        return !type.IsValueType && annotation == NullableAnnotation.Annotated;
    }

    private static bool IsNullableValue(ITypeSymbol type) =>
        type is INamedTypeSymbol n && n.OriginalDefinition.SpecialType == SpecialType.System_Nullable_T;

    public static ITypeSymbol Unwrap(ITypeSymbol type) =>
        IsNullableValue(type) ? ((INamedTypeSymbol)type).TypeArguments[0] : type;

    /// <summary>The schema of one value; spec is null for unannotated properties and items.</summary>
    public Dictionary<string, object?> Schema(ITypeSymbol type, Spec? spec, Location at, string name)
    {
        type = Unwrap(type);
        string inferred = Infer(type, at);
        string vt = Resolve(inferred, spec, at);
        var schema = new Dictionary<string, object?>();
        switch (vt)
        {
            case "STRING": schema["type"] = "string"; break;
            case "INT":
            case "LONG": schema["type"] = "integer"; break;
            case "NUMBER": schema["type"] = "number"; break;
            case "BOOLEAN": schema["type"] = "boolean"; break;
            case "DATE": Format(schema, "date"); break;
            case "DATE_TIME": Format(schema, "date-time"); break;
            case "DURATION": Format(schema, "duration"); break;
            case "URI": Format(schema, "uri"); break;
            case "BINARY":
                schema["type"] = "string";
                schema["contentEncoding"] = "base64";
                break;
            case "ENUM":
                schema["type"] = "string";
                schema["enum"] = inferred == "ENUM" ? Constants(type) : new List<object?>(spec!.EnumValues);
                break;
            case "ARRAY":
                schema["type"] = "array";
                schema["items"] = Schema(ElementType(type, at), null, at, name);
                break;
            case "OBJECT":
                if (MapTypes(type) is { } map)
                {
                    if (map.Key.SpecialType != SpecialType.System_String) throw new Problem(at, "dictionary keys must be string");
                    schema["type"] = "object";
                    // A dictionary of object (or a JSON node) holds any JSON value: the empty schema.
                    schema["additionalProperties"] = AnyJson(map.Value) ? new Dictionary<string, object?>() : Schema(map.Value, null, at, name);
                }
                else
                {
                    return WithSpec(Object((INamedTypeSymbol)type, at), spec, vt, at, name);
                }
                break;
            default:
                throw new Problem(at, "unsupported type " + type.ToDisplayString());
        }
        return WithSpec(schema, spec, vt, at, name);
    }

    private static bool AnyJson(ITypeSymbol type)
    {
        type = Unwrap(type);
        if (type.SpecialType == SpecialType.System_Object) return true;
        string n = type.ToDisplayString();
        return n is "System.Text.Json.Nodes.JsonNode" or "System.Text.Json.JsonElement";
    }

    private static void Format(Dictionary<string, object?> schema, string format)
    {
        schema["type"] = "string";
        schema["format"] = format;
    }

    private static Dictionary<string, object?> WithSpec(Dictionary<string, object?> schema, Spec? spec, string vt, Location at, string name)
    {
        if (spec == null)
        {
            if (vt == "INT") Bounds(schema, IntMin, IntMax);
            if (vt == "LONG") Bounds(schema, -Safe, Safe);
            return schema;
        }
        if (spec.Description.Length > 0) schema["description"] = spec.Description;
        Constraints(schema, vt, spec, at, true, name);
        return schema;
    }

    private static void Bounds(Dictionary<string, object?> schema, long min, long max)
    {
        schema["minimum"] = min;
        schema["maximum"] = max;
    }

    /// <summary>
    /// Adds the declared constraint keywords; with materialise, INT and LONG get their range bounds unless
    /// narrower ones are declared.
    /// </summary>
    public static void Constraints(Dictionary<string, object?> schema, string vt, Spec spec, Location at, bool materialise, string elementName)
    {
        string name = spec.Name.Length == 0 ? elementName : spec.Name;
        bool numeric = vt is "INT" or "LONG" or "NUMBER";
        bool text = vt is "STRING" or "URI";
        if (!numeric && (spec.Min.Length > 0 || spec.Max.Length > 0))
        {
            throw new Problem(at, name + ": Min and Max apply to INT, LONG and NUMBER only");
        }
        if (!text && (spec.MinLength >= 0 || spec.MaxLength >= 0 || spec.Pattern.Length > 0))
        {
            throw new Problem(at, name + ": MinLength, MaxLength and Pattern apply to STRING and URI only");
        }
        if (spec.Format.Length > 0 && vt != "STRING")
        {
            throw new Problem(at, name + ": Format applies to STRING only; " + vt + " has its own format");
        }
        if (spec.EnumValues.Count > 0 && vt != "ENUM")
        {
            throw new Problem(at, name + ": EnumValues applies to a string typed as ENUM");
        }
        if (vt is "INT" or "LONG")
        {
            long lo = vt == "INT" ? IntMin : -Safe;
            long hi = vt == "INT" ? IntMax : Safe;
            long? min = spec.Min.Length == 0 ? null : Integer(spec.Min, lo, hi, name, at);
            long? max = spec.Max.Length == 0 ? null : Integer(spec.Max, lo, hi, name, at);
            if (min != null && max != null && min > max) throw new Problem(at, name + ": Min is greater than Max");
            if (min != null || materialise) schema["minimum"] = min ?? lo;
            if (max != null || materialise) schema["maximum"] = max ?? hi;
        }
        else if (vt == "NUMBER")
        {
            double? min = spec.Min.Length == 0 ? null : Number(spec.Min, name, at);
            double? max = spec.Max.Length == 0 ? null : Number(spec.Max, name, at);
            if (min != null && max != null && min > max) throw new Problem(at, name + ": Min is greater than Max");
            if (min != null) schema["minimum"] = min.Value;
            if (max != null) schema["maximum"] = max.Value;
        }
        if (spec.MinLength < -1 || spec.MaxLength < -1) throw new Problem(at, name + ": lengths must be at least 0");
        if (spec.MinLength >= 0) schema["minLength"] = (long)spec.MinLength;
        if (spec.MaxLength >= 0) schema["maxLength"] = (long)spec.MaxLength;
        if (spec.MinLength >= 0 && spec.MaxLength >= 0 && spec.MinLength > spec.MaxLength)
        {
            throw new Problem(at, name + ": MinLength is greater than MaxLength");
        }
        if (spec.Pattern.Length > 0)
        {
            if (!Compiles(spec.Pattern, out string error)) throw new Problem(at, name + ": Pattern does not compile: " + error);
            schema["pattern"] = spec.Pattern;
        }
        if (spec.Format.Length > 0) schema["format"] = spec.Format;
        if (spec.DefaultValue.Length > 0) schema["default"] = TypedDefault(vt, spec, schema, name, at);
        if (spec.Sensitive) schema["x-toolgate-sensitive"] = true;
    }

    public static bool Compiles(string pattern, out string error)
    {
        try
        {
            _ = new Regex(pattern, RegexOptions.CultureInvariant);
            error = "";
            return true;
        }
        catch (ArgumentException e)
        {
            error = e.Message;
            return false;
        }
    }

    private static object TypedDefault(string vt, Spec spec, Dictionary<string, object?> schema, string name, Location at)
    {
        string raw = spec.DefaultValue;
        switch (vt)
        {
            case "INT":
            case "LONG":
            {
                long lo = schema.TryGetValue("minimum", out var mn) && mn is long a ? a : vt == "INT" ? IntMin : -Safe;
                long hi = schema.TryGetValue("maximum", out var mx) && mx is long b ? b : vt == "INT" ? IntMax : Safe;
                return Integer(raw, lo, hi, name + " default", at);
            }
            case "NUMBER":
                return Number(raw, name + " default", at);
            case "BOOLEAN":
                if (raw != "true" && raw != "false") throw new Problem(at, name + ": default must be true or false");
                return raw == "true";
            case "ENUM":
            {
                var values = schema.TryGetValue("enum", out var e) ? e as List<object?> : null;
                if (values == null || !values.Contains(raw)) throw new Problem(at, name + ": default " + raw + " is not an enum value");
                return raw;
            }
            case "STRING":
            case "DATE":
            case "DATE_TIME":
            case "DURATION":
            case "URI":
                return raw;
            default:
                throw new Problem(at, name + ": " + vt + " values cannot have a default");
        }
    }

    private static long Integer(string raw, long lo, long hi, string name, Location at)
    {
        if (!long.TryParse(raw.Trim(), NumberStyles.AllowLeadingSign, CultureInfo.InvariantCulture, out long value))
        {
            throw new Problem(at, name + ": " + raw + " is not an integer");
        }
        if (value < lo || value > hi) throw new Problem(at, name + ": " + raw + " is outside " + lo + ".." + hi);
        return value;
    }

    private static double Number(string raw, string name, Location at)
    {
        if (!double.TryParse(raw.Trim(), NumberStyles.Float, CultureInfo.InvariantCulture, out double value))
        {
            throw new Problem(at, name + ": " + raw + " is not a number");
        }
        if (double.IsNaN(value) || double.IsInfinity(value)) throw new Problem(at, name + ": " + raw + " is not finite");
        return value;
    }

    private static string Resolve(string inferred, Spec? spec, Location at)
    {
        if (spec == null) return inferred;
        string declared = spec.Type;
        if (declared == "INFER") return inferred == "STRING" && spec.EnumValues.Count > 0 ? "ENUM" : inferred;
        if (declared == inferred) return declared;
        if (inferred == "STRING" && declared is "DATE" or "DATE_TIME" or "DURATION" or "URI") return declared;
        if (inferred == "STRING" && declared == "ENUM")
        {
            if (spec.EnumValues.Count == 0) throw new Problem(at, "a string typed as ENUM needs EnumValues");
            return declared;
        }
        throw new Problem(at, "type " + declared + " does not match the .NET type, which maps to " + inferred);
    }

    /// <summary>The value type a .NET type maps to (authoring README §7).</summary>
    public string Infer(ITypeSymbol type, Location at)
    {
        type = Unwrap(type);
        switch (type.SpecialType)
        {
            case SpecialType.System_Int32:
            case SpecialType.System_Int16:
            case SpecialType.System_UInt16:
            case SpecialType.System_Byte:
            case SpecialType.System_SByte:
                return "INT";
            case SpecialType.System_Int64:
            case SpecialType.System_UInt32:
                return "LONG";
            case SpecialType.System_Double:
            case SpecialType.System_Single:
            case SpecialType.System_Decimal:
                return "NUMBER";
            case SpecialType.System_Boolean:
                return "BOOLEAN";
            case SpecialType.System_String:
                return "STRING";
        }
        if (type is IArrayTypeSymbol array)
        {
            if (array.Rank != 1) throw new Problem(at, "unsupported type " + type.ToDisplayString());
            return array.ElementType.SpecialType == SpecialType.System_Byte ? "BINARY" : "ARRAY";
        }
        switch (type.ToDisplayString())
        {
            case "System.DateOnly": return "DATE";
            case "System.DateTimeOffset": return "DATE_TIME";
            case "System.TimeSpan": return "DURATION";
            case "System.Uri": return "URI";
        }
        if (type.TypeKind == TypeKind.Enum) return "ENUM";
        if (MapTypes(type) != null) return "OBJECT";
        if (Enumerable(type) != null) return "ARRAY";
        if (type is not INamedTypeSymbol named || type.TypeKind is not (TypeKind.Class or TypeKind.Struct or TypeKind.Interface))
        {
            throw new Problem(at, "unsupported type " + type.ToDisplayString());
        }
        string ns = named.ContainingNamespace?.ToDisplayString() ?? "";
        if (ns == "System" || ns.StartsWith("System.", StringComparison.Ordinal) || ns == "Microsoft" || ns.StartsWith("Microsoft.", StringComparison.Ordinal))
        {
            throw new Problem(at, "unsupported type " + type.ToDisplayString());
        }
        return "OBJECT";
    }

    private Dictionary<string, object?> Object(INamedTypeSymbol type, Location at)
    {
        string name = type.Name;
        if (stack.Contains(name))
        {
            recursive.Add(name);
            return Ref(name);
        }
        stack.Add(name);
        var properties = new Dictionary<string, object?>();
        var required = new List<string>();
        try
        {
            var members = Properties(type);
            if (members.Count == 0 && !type.IsRecord)
            {
                throw new Problem(at, name + " has no properties; use a record, a class with public properties, or a dictionary");
            }
            foreach (var p in members)
            {
                Location pat = p.Property.Locations.FirstOrDefault() ?? at;
                if (!pat.IsInSource) pat = at;
                Spec? spec = p.Param == null ? null : Spec.OfParam(p.Param);
                Property(properties, p.JsonName, Schema(p.Property.Type, spec, pat, p.Property.Name), pat);
                bool optional = Optional(p.Property.Type, p.Property.NullableAnnotation)
                    || (spec != null && (!spec.Required || spec.DefaultValue.Length > 0));
                if (!optional) required.Add(p.JsonName);
            }
        }
        finally
        {
            stack.RemoveAt(stack.Count - 1);
        }
        var schema = Closed(properties, required);
        if (recursive.Contains(name))
        {
            defs[name] = schema;
            return Ref(name);
        }
        return schema;
    }

    /// <summary>A serialized property: the JSON name System.Text.Json uses with the SDK's options, and its [Param].</summary>
    internal sealed class JsonProperty
    {
        public JsonProperty(IPropertySymbol property, string jsonName, Attr? param)
        {
            Property = property;
            JsonName = jsonName;
            Param = param;
        }

        public IPropertySymbol Property { get; }
        public string JsonName { get; }
        public Attr? Param { get; }
    }

    /// <summary>
    /// The public instance properties with a public getter, most-derived first, without [JsonIgnore]. A positional
    /// record's [Param] sits on the constructor parameter of the same name.
    /// </summary>
    public static List<JsonProperty> Properties(INamedTypeSymbol type)
    {
        var output = new List<JsonProperty>();
        var seen = new HashSet<string>();
        for (INamedTypeSymbol? t = type; t != null && t.SpecialType != SpecialType.System_Object && t.SpecialType != SpecialType.System_ValueType; t = t.BaseType)
        {
            foreach (var p in t.GetMembers().OfType<IPropertySymbol>())
            {
                if (p.IsStatic || p.IsIndexer || p.DeclaredAccessibility != Accessibility.Public) continue;
                if (p.GetMethod == null || p.GetMethod.DeclaredAccessibility != Accessibility.Public) continue;
                if (!seen.Add(p.Name)) continue;
                if (Ignored(p)) continue;
                Location fallback = p.Locations.FirstOrDefault() ?? Location.None;
                Attr? param = AttributeOf(p.GetAttributes(), "ParamAttribute", fallback);
                if (param == null)
                {
                    foreach (var ctor in type.InstanceConstructors)
                    {
                        var cp = ctor.Parameters.FirstOrDefault(x => x.Name == p.Name);
                        if (cp == null) continue;
                        param = AttributeOf(cp.GetAttributes(), "ParamAttribute", cp.Locations.FirstOrDefault() ?? fallback);
                        if (param != null) break;
                    }
                }
                string? jsonName = null;
                foreach (var a in p.GetAttributes())
                {
                    if (a.AttributeClass?.ToDisplayString() == "System.Text.Json.Serialization.JsonPropertyNameAttribute"
                        && a.ConstructorArguments.Length == 1 && a.ConstructorArguments[0].Value is string s)
                    {
                        jsonName = s;
                    }
                }
                if (jsonName == null && param != null && param.String("Name").Length > 0) jsonName = param.String("Name");
                output.Add(new JsonProperty(p, jsonName ?? CamelCase(p.Name), param));
            }
        }
        return output;
    }

    private static bool Ignored(IPropertySymbol p)
    {
        foreach (var a in p.GetAttributes())
        {
            if (a.AttributeClass?.ToDisplayString() != "System.Text.Json.Serialization.JsonIgnoreAttribute") continue;
            foreach (var n in a.NamedArguments)
            {
                // JsonIgnoreCondition.Never is 0; WhenWritingDefault and WhenWritingNull still serialize.
                if (n.Key == "Condition" && n.Value.Value is int c && c != 1) return false;
            }
            return true;
        }
        return false;
    }

    public static Attr? AttributeOf(IEnumerable<AttributeData> attributes, string name, Location fallback)
    {
        foreach (var a in attributes)
        {
            if (a.AttributeClass?.Name == name && a.AttributeClass.ContainingNamespace?.ToDisplayString() == "OloLabs.ToolGate.Sdk")
            {
                return new Attr(a, fallback);
            }
        }
        return null;
    }

    /// <summary>System.Text.Json's JsonNamingPolicy.CamelCase, reproduced exactly so schemas and serialization agree.</summary>
    public static string CamelCase(string name)
    {
        if (string.IsNullOrEmpty(name) || !char.IsUpper(name[0])) return name;
        char[] chars = name.ToCharArray();
        for (int i = 0; i < chars.Length; i++)
        {
            if (i == 1 && !char.IsUpper(chars[i])) break;
            bool hasNext = i + 1 < chars.Length;
            if (i > 0 && hasNext && !char.IsUpper(chars[i + 1]))
            {
                if (chars[i + 1] == ' ') chars[i] = char.ToLowerInvariant(chars[i]);
                break;
            }
            chars[i] = char.ToLowerInvariant(chars[i]);
        }
        return new string(chars);
    }

    private static void Property(Dictionary<string, object?> properties, string name, Dictionary<string, object?> schema, Location at)
    {
        if (properties.ContainsKey(name)) throw new Problem(at, "duplicate property " + name);
        properties[name] = schema;
    }

    private static Dictionary<string, object?> Ref(string name) => new() { ["$ref"] = "#/$defs/" + name };

    public static Dictionary<string, object?> Closed(Dictionary<string, object?> properties, List<string> required) => new()
    {
        ["type"] = "object",
        ["properties"] = properties,
        ["required"] = Jcs.Sorted(required).Cast<object?>().ToList(),
        ["additionalProperties"] = false,
    };

    private static List<object?> Constants(ITypeSymbol type)
    {
        var values = new List<object?>();
        foreach (var f in type.GetMembers().OfType<IFieldSymbol>())
        {
            if (f.HasConstantValue && f.IsStatic) values.Add(f.Name);
        }
        return values;
    }

    private ITypeSymbol ElementType(ITypeSymbol type, Location at)
    {
        if (type is IArrayTypeSymbol array) return array.ElementType;
        return Enumerable(type) ?? throw new Problem(at, "cannot find the element type of " + type.ToDisplayString());
    }

    /// <summary>The T of IEnumerable&lt;T&gt; for a collection type other than string and dictionaries.</summary>
    private ITypeSymbol? Enumerable(ITypeSymbol type)
    {
        if (type.SpecialType == SpecialType.System_String) return null;
        var enumerable = compilation.GetSpecialType(SpecialType.System_Collections_Generic_IEnumerable_T);
        if (type is INamedTypeSymbol n && SymbolEqualityComparer.Default.Equals(n.OriginalDefinition, enumerable)) return n.TypeArguments[0];
        foreach (var i in type.AllInterfaces)
        {
            if (SymbolEqualityComparer.Default.Equals(i.OriginalDefinition, enumerable)) return i.TypeArguments[0];
        }
        return null;
    }

    /// <summary>The key and value types of a dictionary type, or null.</summary>
    private KeyValuePair<ITypeSymbol, ITypeSymbol>? MapTypes(ITypeSymbol type)
    {
        var dictionary = compilation.GetTypeByMetadataName("System.Collections.Generic.IDictionary`2");
        var readOnly = compilation.GetTypeByMetadataName("System.Collections.Generic.IReadOnlyDictionary`2");
        bool Is(INamedTypeSymbol i) => SymbolEqualityComparer.Default.Equals(i.OriginalDefinition, dictionary)
            || SymbolEqualityComparer.Default.Equals(i.OriginalDefinition, readOnly);
        if (type is INamedTypeSymbol n && Is(n)) return new KeyValuePair<ITypeSymbol, ITypeSymbol>(n.TypeArguments[0], n.TypeArguments[1]);
        foreach (var i in type.AllInterfaces)
        {
            if (Is(i)) return new KeyValuePair<ITypeSymbol, ITypeSymbol>(i.TypeArguments[0], i.TypeArguments[1]);
        }
        return null;
    }
}
