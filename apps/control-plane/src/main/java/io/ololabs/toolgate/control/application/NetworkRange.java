// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

/** Numeric-only CIDR matching. Never performs a DNS lookup during authorization. */
public record NetworkRange(byte[] address,int prefix) {
    public NetworkRange { address=address.clone(); if(prefix<0||prefix>address.length*8)throw new IllegalArgumentException("Invalid network prefix"); }
    public byte[] address() { return address.clone(); }
    public static byte[] numeric(String value) {
        if(value==null || !value.matches("[0-9a-fA-F:.]{2,64}") || !value.contains(":")&&!value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IllegalArgumentException("Numeric address required");
        try { return java.net.InetAddress.getByName(value).getAddress(); } catch(java.net.UnknownHostException e) { throw new IllegalArgumentException("Invalid numeric address"); }
    }
    public static NetworkRange parse(String value) {
        var parts=value.split("/",-1);if(parts.length!=2)throw new IllegalArgumentException("CIDR required");
        try {return new NetworkRange(numeric(parts[0]),Integer.parseInt(parts[1]));} catch(NumberFormatException e){throw new IllegalArgumentException("Invalid network prefix");}
    }
    public boolean contains(String value) {
        byte[] candidate; try {candidate=numeric(value);}catch(IllegalArgumentException e){return false;} if(candidate.length!=address.length)return false;
        for(int i=0;i<prefix;i++)if((candidate[i/8]&(1<<(7-i%8)))!=(address[i/8]&(1<<(7-i%8))))return false; return true;
    }
}
