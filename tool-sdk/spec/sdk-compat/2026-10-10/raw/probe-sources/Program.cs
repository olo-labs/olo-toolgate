using System.ComponentModel;
using System.Text.Json.Nodes;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using ModelContextProtocol.Server;

var builder = Host.CreateApplicationBuilder(args);
builder.Logging.ClearProviders();
builder.Services.AddMcpServer(o =>
{
    o.RequestHandlers = [ new McpServerRequestHandler {
        Method = "acme/echo",
        Handler = (req, ct) => ValueTask.FromResult<JsonNode?>(new JsonObject { ["echoed"] = req.Params?["text"]?.DeepClone() })
    } ];
}).WithStdioServerTransport().WithTools<EchoTools>();
await builder.Build().RunAsync();

[McpServerToolType]
public class EchoTools
{
    [McpServerTool(Name = "echo"), Description("echo")]
    public static string Echo(string text) => text;
}
