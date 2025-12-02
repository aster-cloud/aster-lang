package io.aster.ecommerce.integration;

import aster.ecommerce.FulfillmentService;
import aster.ecommerce.InventoryAdapter;
import aster.ecommerce.PaymentGateway;
import aster.ecommerce.stub.InMemoryFulfillmentService;
import aster.ecommerce.stub.InMemoryInventoryAdapter;
import aster.ecommerce.stub.InMemoryPaymentGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * 測試環境專用的 stub 產生器，將內存實現註冊為 CDI Bean。
 */
@ApplicationScoped
public class TestStubProducers {

    private final InMemoryPaymentGateway paymentGateway = new InMemoryPaymentGateway();
    private final InMemoryInventoryAdapter inventoryAdapter = new InMemoryInventoryAdapter();
    private final InMemoryFulfillmentService fulfillmentService = new InMemoryFulfillmentService();

    @Produces
    @Singleton
    public PaymentGateway paymentGateway() {
        return paymentGateway;
    }

    @Produces
    @Singleton
    public InMemoryPaymentGateway inMemoryPaymentGateway() {
        return paymentGateway;
    }

    @Produces
    @Singleton
    public InventoryAdapter inventoryAdapter() {
        return inventoryAdapter;
    }

    @Produces
    @Singleton
    public InMemoryInventoryAdapter inMemoryInventoryAdapter() {
        return inventoryAdapter;
    }

    @Produces
    @Singleton
    public FulfillmentService fulfillmentService() {
        return fulfillmentService;
    }

    @Produces
    @Singleton
    public InMemoryFulfillmentService inMemoryFulfillmentService() {
        return fulfillmentService;
    }
}
