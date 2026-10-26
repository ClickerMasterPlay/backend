package com.resourcemagnate.app.economy.catalog;

import com.resourcemagnate.app.common.exception.BusinessException;
import com.resourcemagnate.app.economy.dto.CatalogItemResponse;
import com.resourcemagnate.app.economy.dto.CatalogResponse;
import com.resourcemagnate.app.economy.model.ProductType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductCatalogService {

    private final Map<String, ProductDefinition> products;

    public ProductCatalogService() {
        Map<String, ProductDefinition> definitions = new LinkedHashMap<>();
        add(definitions, new ProductDefinition(
                ProductType.CLICK_UPGRADE, "click_power_1", 100,
                0.5, null, null, "+0.5 click power"
        ));
        add(definitions, new ProductDefinition(
                ProductType.TOOL, "wooden_pickaxe", 250,
                null, 1.2, null, "x1.2 click damage"
        ));
        add(definitions, new ProductDefinition(
                ProductType.TOOL, "iron_pickaxe", 800,
                null, 1.8, null, "x1.8 click damage"
        ));
        add(definitions, new ProductDefinition(
                ProductType.MACHINE, "basic_miner", 1500,
                null, null, 10L, "10 resources per hour"
        ));
        products = Map.copyOf(definitions);
    }

    public ProductDefinition get(ProductType type, String id) {
        ProductDefinition product = products.get(key(type, id));
        if (product == null) {
            throw new BusinessException(
                    "PRODUCT_NOT_FOUND",
                    "Product not found: " + type + "/" + id
            );
        }
        return product;
    }

    public CatalogResponse getCatalog() {
        List<CatalogItemResponse> items = products.values().stream()
                .map(product -> new CatalogItemResponse(
                        product.type(), product.id(), product.price(), product.effect()
                ))
                .toList();
        return new CatalogResponse(items);
    }

    private void add(Map<String, ProductDefinition> target, ProductDefinition product) {
        target.put(key(product.type(), product.id()), product);
    }

    private String key(ProductType type, String id) {
        return type.name() + ":" + id;
    }
}
