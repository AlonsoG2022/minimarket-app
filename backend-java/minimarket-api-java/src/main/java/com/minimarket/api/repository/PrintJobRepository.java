package com.minimarket.api.repository;

import com.minimarket.api.entity.PrintJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintJobRepository extends JpaRepository<PrintJob, Integer> {

    @EntityGraph(attributePaths = {"sale", "sale.user", "sale.details", "sale.details.product"})
    PrintJob findFirstById(Integer id);

    List<PrintJob> findTop20ByOrderByRequestedAtDesc();
}
