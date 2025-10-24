package com.DareUs.app;

import android.app.Activity;
import android.util.Log;
import com.android.billingclient.api.*;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private Activity activity;
    private PurchaseListener listener;

    public interface PurchaseListener {
        void onPurchaseSuccess(String productId);
        void onPurchaseError(String error);
    }

    // Product IDs for your subscriptions (updated to match Cloud Functions)
    public static final String SKU_PREMIUM_SINGLE = "premium_single_monthly";
    public static final String SKU_PREMIUM_COUPLE = "premium_couple_monthly";
    public static final String SKU_PREMIUM_COUPLE_PLUS = "premium_couple_plus_monthly";

    public BillingManager(Activity activity, PurchaseListener listener) {
        this.activity = activity;
        this.listener = listener;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        startConnection();
    }

    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "Billing client connected");
                    queryPurchases();
                } else {
                    Log.e("Billing", "Billing setup failed: " + billingResult.getDebugMessage());
                    if (listener != null) {
                        listener.onPurchaseError("Billing setup failed");
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d("Billing", "Billing client disconnected - will retry");
                // Retry connection
                startConnection();
            }
        });
    }

    public void launchPurchaseFlow(String productId) {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params,
                (billingResult, productDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && productDetailsList != null && !productDetailsList.isEmpty()) {

                        ProductDetails productDetails = productDetailsList.get(0);

                        // Get subscription offer details
                        List<ProductDetails.SubscriptionOfferDetails> offers =
                                productDetails.getSubscriptionOfferDetails();

                        if (offers != null && !offers.isEmpty()) {
                            String offerToken = offers.get(0).getOfferToken();

                            BillingFlowParams.ProductDetailsParams productDetailsParams =
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(offerToken)
                                            .build();

                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(
                                            ImmutableList.of(productDetailsParams)
                                    )
                                    .build();

                            billingClient.launchBillingFlow(activity, flowParams);
                        } else {
                            Log.e("Billing", "No subscription offers found");
                            if (listener != null) {
                                listener.onPurchaseError("No subscription offers available");
                            }
                        }
                    } else {
                        Log.e("Billing", "Failed to query product details: " +
                                billingResult.getDebugMessage());
                        if (listener != null) {
                            listener.onPurchaseError("Failed to load product details");
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("Billing", "User canceled purchase");
            if (listener != null) {
                listener.onPurchaseError("Purchase canceled");
            }
        } else {
            Log.e("Billing", "Purchase failed: " + billingResult.getDebugMessage());
            if (listener != null) {
                listener.onPurchaseError("Purchase failed: " + billingResult.getDebugMessage());
            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Get product ID from purchase
            String productId = purchase.getProducts().get(0);

            // Acknowledge purchase if not already acknowledged
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

                billingClient.acknowledgePurchase(params, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d("Billing", "Purchase acknowledged successfully");
                        if (listener != null) {
                            listener.onPurchaseSuccess(productId);
                        }
                    } else {
                        Log.e("Billing", "Failed to acknowledge purchase: " +
                                billingResult.getDebugMessage());
                    }
                });
            } else {
                // Already acknowledged, just notify success
                if (listener != null) {
                    listener.onPurchaseSuccess(productId);
                }
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d("Billing", "Purchase pending");
            if (listener != null) {
                listener.onPurchaseError("Purchase is pending");
            }
        }
    }

    private void queryPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                (billingResult, purchasesList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d("Billing", "Found " + purchasesList.size() + " existing purchases");
                        for (Purchase purchase : purchasesList) {
                            // Restore existing purchases
                            handlePurchase(purchase);
                        }
                    } else {
                        Log.e("Billing", "Failed to query purchases: " +
                                billingResult.getDebugMessage());
                    }
                }
        );
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}