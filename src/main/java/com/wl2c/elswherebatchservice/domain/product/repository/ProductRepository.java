package com.wl2c.elswherebatchservice.domain.product.repository;

import com.wl2c.elswherebatchservice.domain.product.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p where p.productState = 'ACTIVE' and p.name = :name ")
    Optional<Product> findProductByName(@Param("name") String name);

}
