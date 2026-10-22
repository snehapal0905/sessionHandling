package com.spendly.spendly.repository;

import com.spendly.spendly.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUser(Long id, AppUser user);

    @Query("""
            select t from Transaction t
            where t.user = :user
              and t.date between :from and :to
              and (:type is null or t.type = :type)
              and (:accountId is null or t.account.id = :accountId or t.toAccount.id = :accountId)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:q = '' or lower(t.description) like lower(concat('%', :q, '%'))
                           or lower(coalesce(t.notes, '')) like lower(concat('%', :q, '%')))
            """)
    Page<Transaction> search(@Param("user") AppUser user, @Param("from") LocalDate from, @Param("to") LocalDate to,
                             @Param("type") TxnType type, @Param("accountId") Long accountId,
                             @Param("categoryId") Long categoryId, @Param("q") String q, Pageable pageable);

    @Query("""
            select t from Transaction t
            where t.user = :user
              and t.date between :from and :to
              and (:type is null or t.type = :type)
              and (:accountId is null or t.account.id = :accountId or t.toAccount.id = :accountId)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:q = '' or lower(t.description) like lower(concat('%', :q, '%'))
                           or lower(coalesce(t.notes, '')) like lower(concat('%', :q, '%')))
            order by t.date desc, t.id desc
            """)
    List<Transaction> searchAll(@Param("user") AppUser user, @Param("from") LocalDate from, @Param("to") LocalDate to,
                                @Param("type") TxnType type, @Param("accountId") Long accountId,
                                @Param("categoryId") Long categoryId, @Param("q") String q);

    /** Income and expense lines (no transfers) in a date range, for analysis. */
    @Query("select t from Transaction t left join fetch t.category where t.user = :user and t.type <> :transfer and t.date between :from and :to")
    List<Transaction> findFlows(@Param("user") AppUser user, @Param("from") LocalDate from, @Param("to") LocalDate to,
                                @Param("transfer") TxnType transfer);

    List<Transaction> findTop8ByUserOrderByDateDescIdDesc(AppUser user);

    /** Rows of [accountId, type, sum] for money leaving or arriving through the main account column. */
    @Query("select t.account.id, t.type, sum(t.amount) from Transaction t where t.user = :user group by t.account.id, t.type")
    List<Object[]> sumByAccountAndType(@Param("user") AppUser user);

    /** Rows of [toAccountId, sum] for money arriving by transfer. */
    @Query("select t.toAccount.id, sum(t.amount) from Transaction t where t.user = :user and t.toAccount is not null group by t.toAccount.id")
    List<Object[]> sumTransfersIn(@Param("user") AppUser user);

    @Query("select min(t.date) from Transaction t where t.user = :user")
    LocalDate firstDate(@Param("user") AppUser user);

    @Query("select count(t) > 0 from Transaction t where t.account = :account or t.toAccount = :account")
    boolean usesAccount(@Param("account") Account account);

    boolean existsByCategory(Category category);

    long countByUser(AppUser user);
}
