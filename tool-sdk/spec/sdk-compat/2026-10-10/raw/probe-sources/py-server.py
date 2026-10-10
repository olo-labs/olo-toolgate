# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
from mcp.server.mcpserver import MCPServer, Extension, MethodBinding
from mcp_types import RequestParams

class EchoParams(RequestParams):
    text: str

async def acme_echo(ctx, params: EchoParams):
    return {"echoed": params.text}

class Acme(Extension):
    identifier = "com.example/acme"
    def methods(self):
        return [MethodBinding("acme/echo", EchoParams, acme_echo)]

mcp = MCPServer("probe-py", extensions=[Acme()])

@mcp.tool()
def echo(text: str) -> str:
    return text

if __name__ == "__main__":
    mcp.run("stdio")
