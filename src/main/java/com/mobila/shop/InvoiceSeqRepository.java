package com.mobila.shop;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceSeqRepository extends JpaRepository<InvoiceSeq, Long> {
    /** Blochează rândul contorului până la sfârșitul tranzacției: două comenzi nu pot primi același număr. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InvoiceSeq s where s.id = 1")
    Optional<InvoiceSeq> lockNext();
}
