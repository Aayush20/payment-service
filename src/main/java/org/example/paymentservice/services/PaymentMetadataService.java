package org.example.paymentservice.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.CountrySpec;
import com.stripe.model.CountrySpecCollection;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PaymentMetadataService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMetadataService.class);

    private static final String STRIPE_COUNTRIES_CACHE_KEY = "metadata:stripe:countries";
    private static final String RAZORPAY_PLANS_CACHE_KEY = "metadata:razorpay:plans";

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${razorpay.api.key}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret}")
    private String razorpayApiSecret;

    @Value("${cache.ttl.paymentMetadata:43200}") // 12h default
    private long metadataTtl;

    private final RedisTemplate<String, Object> redisTemplate;

    public PaymentMetadataService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> getStripeSupportedCountries() {
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(STRIPE_COUNTRIES_CACHE_KEY);
        if (cached != null) {
            log.info("✅ Stripe metadata cache hit");
            return cached;
        }

        Stripe.apiKey = stripeApiKey;
        try {
            CountrySpecCollection countries = CountrySpec.list(Collections.emptyMap());
            List<String> result = countries.getData().stream()
                    .map(CountrySpec::getId)
                    .collect(Collectors.toList());

            redisTemplate.opsForValue().set(STRIPE_COUNTRIES_CACHE_KEY, result, metadataTtl, TimeUnit.SECONDS);
            return result;
        } catch (StripeException e) {
            log.error("❌ Stripe metadata fetch failed: {}", e.getMessage());
            return List.of("US"); // fallback
        }
    }

    public List<String> getRazorpayPlanIds() {
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(RAZORPAY_PLANS_CACHE_KEY);
        if (cached != null) {
            log.info("✅ Razorpay metadata cache hit");
            return cached;
        }

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            List<Plan> plans = razorpayClient.plans.fetchAll();

            List<String> planIds = plans.stream()
                    .map(plan -> plan.get("id").toString())
                    .collect(Collectors.toList());

            redisTemplate.opsForValue().set(RAZORPAY_PLANS_CACHE_KEY, planIds, metadataTtl, TimeUnit.SECONDS);
            return planIds;
        } catch (RazorpayException e) {
            log.error("❌ Razorpay metadata fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    // Optional: Auto-refresh metadata every 12 hours
    @Scheduled(fixedDelayString = "${cache.refresh.paymentMetadata:43200000}") // 12 hours
    public void refreshMetadata() {
        log.info("🔁 Scheduled metadata refresh started...");

        getStripeSupportedCountries();
        getRazorpayPlanIds();

        log.info("✅ Scheduled metadata refresh complete.");
    }
}
