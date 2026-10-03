// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
using System.Text.Json;
using System;
using var request = JsonDocument.Parse(Console.In.ReadToEnd());
Console.WriteLine(JsonSerializer.Serialize(new {protocolVersion=1, requestId=request.RootElement.GetProperty("requestId").GetString(), output=new {text=request.RootElement.GetProperty("arguments").GetProperty("text").GetString()}}));
