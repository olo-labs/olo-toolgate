// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
/** Echo only the parent's canonical bounded input. No host or subprocess work. */
public final class Tool {
    public static void main(String[] args) throws Exception {
        String input = new String(System.in.readNBytes(65537), StandardCharsets.UTF_8).trim();
        String id = input.split("\"requestId\":\"")[1].split("\"")[0];
        String body = input.substring(input.indexOf(",\"arguments\":") + 13, input.length() - 1);
        body = body.replace("\"mode\":\"echo\",", "");
        System.out.println("{\"protocolVersion\":1,\"requestId\":\"" + id + "\",\"output\":" + body + "}");
    }
}
