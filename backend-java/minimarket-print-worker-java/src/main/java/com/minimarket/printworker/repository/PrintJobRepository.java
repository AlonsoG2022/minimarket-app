package com.minimarket.printworker.repository;

import com.minimarket.printworker.entity.PrintJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrintJobRepository extends JpaRepository<PrintJob, Integer> {
    Optional<PrintJob> findFirstByStatusOrderByRequestedAtAscIdAsc(String status);
}
