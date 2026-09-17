package io.ololabs.toolgate.contracts;

/**
 * Identifies the shared ToolGate contract set used by a component.
 *
 * <p>Generated records, enums and validators will be added by the
 * Foundation/Contracts implementation module.</p>
 */
public record ContractSet(String name, String version) {

    public static final String NAME = "olo-toolgate-contracts";
    public static final String VERSION = "0.1.0-SNAPSHOT";

    public static ContractSet current() {
        return new ContractSet(NAME, VERSION);
    }
}
