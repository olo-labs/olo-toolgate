# Local Tool Execution Protocol

## Purpose

One versioned JSON request on stdin, one JSON result on stdout, stderr for diagnostics. Avoid unsafe shell interpolation.

Execution enters through a trusted [registered device](../control-plane/device-registry.md).
Before starting an effect, recheck owner/device enablement, installed-client
connection approval, tool/action/resource filters and the required fresh permit.
Device identity comes from authenticated service context; stdin arguments cannot
replace it. Registry approval does not replace runtime isolation or output limits.
