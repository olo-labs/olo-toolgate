// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace OloLabs.ToolGate.Shared;

/// <summary>
/// Canonical form of descriptor §3: every string NFC, then RFC 8785 (keys by UTF-16 code units, ECMAScript
/// numbers and strings), UTF-8. Values are IDictionary&lt;string, object&gt;, IList, string, bool, int, long or double.
/// Compiled into both the source generator and the runtime.
/// </summary>
internal static class Jcs
{
    public static string Canonical(object? value)
    {
        var output = new StringBuilder();
        Write(output, value);
        return output.ToString();
    }

    public static byte[] Bytes(object? value) => Encoding.UTF8.GetBytes(Canonical(value));

    public static string Sha256(byte[] bytes)
    {
        using var sha = SHA256.Create();
        byte[] hash = sha.ComputeHash(bytes);
        var hex = new StringBuilder("sha256:", 71);
        foreach (byte b in hash) hex.Append(b.ToString("x2", CultureInfo.InvariantCulture));
        return hex.ToString();
    }

    public static string Nfc(string s) => s.IsNormalized(NormalizationForm.FormC) ? s : s.Normalize(NormalizationForm.FormC);

    /// <summary>Sorts strings by UTF-16 code units after NFC, the RFC 8785 order.</summary>
    public static List<string> Sorted(IEnumerable<string> values)
    {
        var output = new List<string>();
        foreach (string v in values) output.Add(Nfc(v));
        output.Sort(string.CompareOrdinal);
        return output;
    }

    private static void Write(StringBuilder output, object? value)
    {
        switch (value)
        {
            case null:
                output.Append("null");
                break;
            case string s:
                WriteString(output, Nfc(s));
                break;
            case bool b:
                output.Append(b ? "true" : "false");
                break;
            case int i:
                output.Append(i.ToString(CultureInfo.InvariantCulture));
                break;
            case long l:
                if (l > 9007199254740991L || l < -9007199254740991L) throw new ArgumentException("integer beyond 2^53-1: " + l);
                output.Append(l.ToString(CultureInfo.InvariantCulture));
                break;
            case double d:
                output.Append(Number(d));
                break;
            case IDictionary<string, object?> map:
            {
                var sorted = new SortedDictionary<string, object?>(StringComparer.Ordinal);
                foreach (var e in map)
                {
                    string key = Nfc(e.Key);
                    if (sorted.ContainsKey(key)) throw new ArgumentException("keys are equal after NFC: " + key);
                    sorted[key] = e.Value;
                }
                output.Append('{');
                bool first = true;
                foreach (var e in sorted)
                {
                    if (!first) output.Append(',');
                    first = false;
                    WriteString(output, e.Key);
                    output.Append(':');
                    Write(output, e.Value);
                }
                output.Append('}');
                break;
            }
            case IList list:
                output.Append('[');
                for (int i = 0; i < list.Count; i++)
                {
                    if (i > 0) output.Append(',');
                    Write(output, list[i]);
                }
                output.Append(']');
                break;
            default:
                throw new ArgumentException("not a JSON value: " + value.GetType());
        }
    }

    public static void WriteString(StringBuilder output, string s)
    {
        output.Append('"');
        foreach (char c in s)
        {
            switch (c)
            {
                case '"': output.Append("\\\""); break;
                case '\\': output.Append("\\\\"); break;
                case '\b': output.Append("\\b"); break;
                case '\f': output.Append("\\f"); break;
                case '\n': output.Append("\\n"); break;
                case '\r': output.Append("\\r"); break;
                case '\t': output.Append("\\t"); break;
                default:
                    if (c < 0x20) output.Append("\\u").Append(((int)c).ToString("x4", CultureInfo.InvariantCulture));
                    else output.Append(c);
                    break;
            }
        }
        output.Append('"');
    }

    /// <summary>ECMAScript Number::toString with the shortest round-tripping digits.</summary>
    public static string Number(double v)
    {
        if (double.IsNaN(v) || double.IsInfinity(v)) throw new ArgumentException("not a finite number");
        if (v == 0) return "0";
        string sign = v < 0 ? "-" : "";
        double a = Math.Abs(v);
        string digits = "";
        int exponent = 0;
        for (int p = 1; p <= 17; p++)
        {
            // "E" formatting rounds correctly on .NET Core 3.0 and later.
            string e = a.ToString("E" + (p - 1).ToString(CultureInfo.InvariantCulture), CultureInfo.InvariantCulture);
            if (double.Parse(e, NumberStyles.Float, CultureInfo.InvariantCulture) != a) continue;
            int at = e.IndexOf('E');
            digits = e.Substring(0, at).Replace(".", "");
            exponent = int.Parse(e.Substring(at + 1), NumberStyles.AllowLeadingSign, CultureInfo.InvariantCulture);
            break;
        }
        digits = digits.TrimEnd('0');
        if (digits.Length == 0) digits = "0";
        int k = digits.Length;
        int n = exponent + 1;
        var output = new StringBuilder(sign);
        if (k <= n && n <= 21)
        {
            output.Append(digits).Append('0', n - k);
        }
        else if (0 < n && n <= 21)
        {
            output.Append(digits, 0, n).Append('.').Append(digits, n, k - n);
        }
        else if (-6 < n && n <= 0)
        {
            output.Append("0.").Append('0', -n).Append(digits);
        }
        else
        {
            output.Append(digits[0]);
            if (k > 1) output.Append('.').Append(digits, 1, k - 1);
            output.Append('e').Append(n - 1 >= 0 ? "+" : "-").Append(Math.Abs(n - 1).ToString(CultureInfo.InvariantCulture));
        }
        return output.ToString();
    }
}
