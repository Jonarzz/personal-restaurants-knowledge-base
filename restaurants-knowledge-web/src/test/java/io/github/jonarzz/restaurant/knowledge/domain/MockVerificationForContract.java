package io.github.jonarzz.restaurant.knowledge.domain;

import static org.mockito.Mockito.*;

import org.mockito.verification.*;

class MockVerificationForContract {

    private String contractId;

    private MockVerificationForContract(String contractId) {
        this.contractId = contractId;
    }

    static MockVerificationForContract asPartOfContract(String contractId) {
        return new MockVerificationForContract(contractId);
    }

    static VerificationMode once() {
        return times(1);
    }

    /**
     * Does not enforce the contract for which the verification is performed.
     * Distinction between contract scenarios should be handled by the developer writing tests.
     */
    VerificationMode shouldBeCalled(VerificationMode mode) {
        return mode.description("Verification failed for '" + contractId + "' contract");
    }
}
